"""Rule-based recommendation agent (AI service).

Turns a user's transactions, financial profile and the earlier subscription /
unusual-spending results into a prioritised list of plain-language suggestions
(reduce spending, increase savings, review subscriptions, ...). No ML model is
involved: every recommendation comes from a threshold rule in
RecommendationAgent.generate(). Exposed via POST /api/recommendations.
"""

from __future__ import annotations

from collections import defaultdict
from typing import Any


class RecommendationAgent:
    """Rule-based recommendation agent with natural, user-friendly wording."""

    # Tag copied onto every item so the UI/database can tell which component created
    # it.
    generated_by = "PYTHON_RULE_BASED_RECOMMENDATION_AGENT"

    def generate(
        self,
        transactions: list[dict[str, Any]],
        subscription_analysis: dict[str, Any] | None,
        anomaly_analysis: dict[str, Any] | None,
        financial_profile: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        """Build the recommendation list.

        Inputs (all optional, missing values are treated as empty):
          transactions          - dicts with type, amount, category, transactionDate, ...
          subscription_analysis - result of the subscription detector
          anomaly_analysis      - result of the unusual-spending detector
          financial_profile     - occupationType, monthlyAllowance, monthlySalary, financialGoalMonths, monthlyBudgets

        Rules, in order: monthly budget overruns/room -> savings goal projection
        -> student allowance -> employee salary vs recorded income -> expense and
        savings ratios -> dominant spending category -> subscriptions -> largest
        unusual purchase. If no rule fires, a generic "keep going" item is
        returned.

        Items are sorted HIGH -> MEDIUM -> LOW. Returns the envelope consumed by
        the Java AiRecommendationClient.
        """
        transactions = transactions or []
        subscription_analysis = subscription_analysis or {}
        anomaly_analysis = anomaly_analysis or {}
        financial_profile = financial_profile or {}

        # Period totals. Amounts are compared by absolute value, so the sign
        # convention used in the uploaded file does not matter.
        income = sum(
            abs(self._amount(t))
            for t in transactions
            if self._type(t) == "INCOME"
        )
        expense = sum(
            abs(self._amount(t))
            for t in transactions
            if self._type(t) == "EXPENSE"
        )
        savings = income - expense

        expenses = [t for t in transactions if self._type(t) == "EXPENSE"]
        # Expense total per category, with names made readable ("PERSONAL_CARE" ->
        # "Personal Care").
        category_totals: dict[str, float] = defaultdict(float)
        for transaction in expenses:
            category = self._text(transaction.get("category"), "OTHER").replace("_", " ").title()
            category_totals[category] += abs(self._amount(transaction))

        recommendations: list[dict[str, Any]] = []

        # Profile values. The savings goal defaults to 6 months when not set.
        goal_months = max(int(self._number(financial_profile.get("financialGoalMonths"))) or 6, 1)
        occupation_type = self._text(financial_profile.get("occupationType"), "").upper()
        monthly_budget_map = self._monthly_budget_map(financial_profile)
        monthly_allowance = self._number(financial_profile.get("monthlyAllowance"))
        monthly_salary = self._number(financial_profile.get("monthlySalary"))

        # Average monthly expense/savings over the months covered by the upload.
        period_months = self._analysis_months(transactions)
        monthly_expense = expense / period_months if period_months > 0 else expense
        monthly_savings = savings / period_months if period_months > 0 else savings

        # Prefer the saved budget for each exact month when available.
        monthly_stats = self._monthly_expense_stats(transactions)
        for month, month_expense in monthly_stats.items():
            budget = monthly_budget_map.get(month)
            if budget is None or budget <= 0:
                continue
            if month_expense > budget:
                over = month_expense - budget
                recommendations.append(self._item(
                    "BUDGET_ADJUSTMENT",
                    "HIGH" if over >= budget * 0.20 else "MEDIUM",
                    "Bring this month's spending back within budget",
                    f"In {month}, you spent about RM{month_expense:.2f} against your RM{budget:.2f} budget, exceeding it by RM{over:.2f}. Focus on discretionary spending to close the gap.",
                    related_amount=over,
                ))
            elif budget - month_expense >= budget * 0.20:
                room = budget - month_expense
                recommendations.append(self._item(
                    "FINANCIAL_GOAL",
                    "LOW",
                    "Use this month's budget room to strengthen savings",
                    f"In {month}, you spent about RM{month_expense:.2f} of your RM{budget:.2f} budget, leaving roughly RM{room:.2f} of budget room.",
                    related_amount=room,
                ))

        # Project how much the user would save over their goal period at the current
        # savings rate.
        if goal_months > 0 and monthly_savings > 0:
            projected_goal_savings = monthly_savings * goal_months
            recommendations.append(self._item(
                "FINANCIAL_GOAL", "MEDIUM",
                f"Work toward your {goal_months}-month financial goal",
                f"At your current average monthly savings of about RM{monthly_savings:.2f}, you could set aside approximately RM{projected_goal_savings:.2f} over {goal_months} months. Keeping discretionary spending controlled will help you stay on track.",
                related_amount=projected_goal_savings,
            ))

        # Students: warn when average monthly spending is above their monthly
        # allowance.
        if occupation_type == "STUDENT" and monthly_allowance > 0:
            allowance_variance = monthly_expense - monthly_allowance
            if allowance_variance > 0:
                recommendations.append(self._item(
                    "BUDGET_ADJUSTMENT", "HIGH",
                    "Keep student spending within your monthly allowance",
                    f"Your average monthly spending is about RM{monthly_expense:.2f}, which is RM{allowance_variance:.2f} above your RM{monthly_allowance:.2f} monthly allowance. Review food, shopping, entertainment and other discretionary spending first.",
                    related_amount=allowance_variance,
                ))

        # Employees: flag when recorded income differs from profile salary by more
        # than 20% (likely missing income transactions).
        if occupation_type == "EMPLOYEE" and monthly_salary > 0 and income > 0:
            salary_gap = abs(income / period_months - monthly_salary)
            if salary_gap > monthly_salary * 0.20:
                recommendations.append(self._item(
                    "CASHFLOW_IMPROVEMENT", "LOW",
                    "Review the difference between salary and recorded income",
                    f"Your uploaded transactions show about RM{income / period_months:.2f} average monthly income, while your profile records RM{monthly_salary:.2f} monthly salary. The difference may come from side income or missing income transactions, so keeping income records complete will improve the analysis.",
                    related_amount=salary_gap,
                ))

        # Without income data the savings rate cannot be judged, so ask for complete
        # income records instead.
        if income <= 0:
            recommendations.append(self._item(
                "CASHFLOW_IMPROVEMENT", "HIGH", "Start with a clear income picture",
                "There is not enough income data in this upload to judge your savings rate reliably. Adding complete income transactions will make the financial advice more useful.",
            ))
        else:
            # expense_ratio = share of income spent. >=70% is high priority, >=50%
            # medium; a savings rate under 10% is also high priority.
            expense_ratio = expense / income
            savings_ratio = savings / income

            if expense_ratio >= 0.70:
                recommendations.append(self._item(
                    "REDUCE_SPENDING", "HIGH", "Give yourself more monthly breathing room",
                    f"About {expense_ratio * 100:.1f}% of your income went to expenses in this period. Cutting a few non-essential purchases could leave more money available for savings.",
                    related_amount=expense,
                ))
            elif expense_ratio >= 0.50:
                recommendations.append(self._item(
                    "INCREASE_SAVINGS", "MEDIUM", "Create a little more room for savings",
                    f"You used about {expense_ratio * 100:.1f}% of your income on expenses. Reducing discretionary spending even slightly could strengthen your monthly savings.",
                    related_amount=expense,
                ))

            if savings_ratio < 0.10:
                recommendations.append(self._item(
                    "INCREASE_SAVINGS", "HIGH", "Build a stronger savings cushion",
                    f"Only about {max(savings_ratio, 0) * 100:.1f}% of your income remained after expenses. Try setting aside a small amount first and then adjust optional spending around it.",
                    related_amount=max(savings, 0),
                ))

        # Check the biggest spending category (categories are sorted largest first):
        # >=30% of expenses is HIGH, >=20% is MEDIUM. Only the top category is
        # reported (break).
        if expense > 0:
            for category, amount in sorted(category_totals.items(), key=lambda pair: pair[1], reverse=True):
                ratio = amount / expense
                if ratio >= 0.30:
                    recommendations.append(self._item(
                        "REDUCE_SPENDING", "HIGH", f"Take a closer look at {category}",
                        f"{category} accounts for about {ratio * 100:.1f}% of your expenses. Reviewing the larger purchases in this category could make a noticeable difference to your monthly spending.",
                        related_amount=amount,
                        related_category=category,
                    ))
                    break
                if ratio >= 0.20:
                    recommendations.append(self._item(
                        "BUDGET_ADJUSTMENT", "MEDIUM", f"Keep an eye on {category}",
                        f"About {ratio * 100:.1f}% of your expenses went to {category}. Setting a simple spending limit for this category could help keep it under control.",
                        related_amount=amount,
                        related_category=category,
                    ))
                    break

        # Subscription cost relative to income sets the priority: >=20% HIGH, >=10%
        # MEDIUM, otherwise LOW.
        monthly_subscription = self._number(subscription_analysis.get("estimatedMonthlySubscriptionCost"))
        subscription_count = int(self._number(subscription_analysis.get("subscriptionCount")))
        if subscription_count > 0 and monthly_subscription > 0:
            ratio = monthly_subscription / income if income > 0 else 0
            priority = "HIGH" if ratio >= 0.20 else "MEDIUM" if ratio >= 0.10 else "LOW"
            recommendations.append(self._item(
                "SUBSCRIPTION_OPTIMIZATION", priority, "Review your recurring subscriptions",
                f"Your detected subscriptions add up to about RM{monthly_subscription:.2f} a month across {subscription_count} service(s). If any are rarely used, cancelling one or two could free up money every month.",
                related_amount=monthly_subscription,
            ))

        # Mention only the single largest unusual purchase; HIGH/CRITICAL severity
        # becomes a HIGH priority item.
        anomalies = anomaly_analysis.get("anomalies") or []
        if anomalies:
            highest = max(anomalies, key=lambda item: self._number(item.get("amount")))
            merchant = self._text(highest.get("merchant"), "an unusual purchase")
            amount = self._number(highest.get("amount"))
            severity = self._text(highest.get("severity"), "MEDIUM").upper()
            priority = "HIGH" if severity in {"HIGH", "CRITICAL"} else "MEDIUM"
            recommendations.append(self._item(
                "REDUCE_SPENDING", priority, "Check the unusual purchase",
                f"{merchant} involved a RM{amount:.2f} expense that stands out from your usual spending. If it was not essential, consider whether a similar purchase can be avoided in the future.",
                related_amount=amount,
                related_merchant=merchant,
            ))

        # Fallback so the user never sees an empty list.
        if not recommendations:
            recommendations.append(self._item(
                "FINANCIAL_GOAL", "LOW", "Keep building healthy spending habits",
                "Your current transaction pattern does not show a major issue that needs immediate attention. Keep tracking your spending and review your largest expenses regularly.",
            ))

        # Most urgent first; unknown priorities go last.
        recommendations.sort(key=lambda item: {"HIGH": 0, "MEDIUM": 1, "LOW": 2}.get(item["priority"], 3))
        high = sum(item["priority"] == "HIGH" for item in recommendations)
        medium = sum(item["priority"] == "MEDIUM" for item in recommendations)
        low = sum(item["priority"] == "LOW" for item in recommendations)

        if high:
            summary = f"I found {len(recommendations)} areas worth reviewing. {high} need closer attention, while {medium + low} are lower-priority improvements you can work on over time."
        else:
            summary = f"I found {len(recommendations)} practical suggestions based on your recent transactions. Nothing appears urgent, so you can work through them gradually."

        return {
            "status": "SUCCESS",
            "uploadHistoryId": None,
            "fileName": "",
            "analysedFrom": self._earliest_date(transactions),
            "analysedTo": self._latest_date(transactions),
            "totalIncome": round(income, 2),
            "totalExpense": round(expense, 2),
            "totalSavings": round(savings, 2),
            "estimatedMonthlySubscriptionCost": round(monthly_subscription, 2),
            "estimatedYearlySubscriptionCost": round(monthly_subscription * 12, 2),
            "unusualTransactionCount": len(anomalies),
            "recommendationCount": len(recommendations),
            "recommendations": recommendations,
            "summary": summary,
            "generatedBy": self.generated_by,
        }

    def _item(
        self,
        recommendation_type: str,
        priority: str,
        title: str,
        message: str,
        related_amount: float | None = None,
        related_category: str | None = None,
        related_merchant: str | None = None,
    ) -> dict[str, Any]:
        """Build one recommendation dict in the shape the backend expects. `id` is
        None here - the Java side assigns it when saving.
        """
        return {
            "id": None,
            "type": recommendation_type,
            "priority": priority,
            "title": title,
            "message": message,
            "relatedAmount": round(related_amount, 2) if related_amount is not None else None,
            "relatedCategory": related_category,
            "relatedMerchant": related_merchant,
            "generatedBy": self.generated_by,
        }

    @staticmethod
    def _amount(transaction: dict[str, Any]) -> float:
        """Safe float read of transaction["amount"]; 0.0 if missing or not numeric."""
        try:
            return float(transaction.get("amount") or 0)
        except (TypeError, ValueError):
            return 0.0

    @staticmethod
    def _number(value: Any) -> float:
        """Safe float conversion for any value; 0.0 if None or not numeric."""
        try:
            return float(value or 0)
        except (TypeError, ValueError):
            return 0.0

    @staticmethod
    def _type(transaction: dict[str, Any]) -> str:
        """Transaction type normalised to upper case (INCOME / EXPENSE), "" if
        missing.
        """
        return str(transaction.get("type") or "").upper().strip()

    @staticmethod
    def _text(value: Any, fallback: str) -> str:
        """Trimmed string of `value`, or `fallback` when it is empty."""
        text = str(value or "").strip()
        return text if text else fallback

    @staticmethod
    def _monthly_budget_map(financial_profile: dict[str, Any]) -> dict[str, float]:
        """Convert the profile's `monthlyBudgets` list into {"YYYY-MM":
        budget_amount}. Rows that are not dicts, have no month, or have a non-
        numeric amount are skipped.
        """
        rows = financial_profile.get("monthlyBudgets") or []
        result: dict[str, float] = {}
        if isinstance(rows, list):
            for row in rows:
                if not isinstance(row, dict):
                    continue
                month = str(row.get("budgetMonth") or "")[:7]
                if not month:
                    continue
                try:
                    result[month] = float(row.get("amount") or 0)
                except (TypeError, ValueError):
                    continue
        return result

    @staticmethod
    def _monthly_expense_stats(transactions: list[dict[str, Any]]) -> dict[str, float]:
        """Total EXPENSE amount per month, keyed "YYYY-MM" (the first 7 characters of
        transactionDate).
        """
        result: dict[str, float] = {}
        for transaction in transactions:
            if str(transaction.get("type") or "").upper() != "EXPENSE":
                continue
            month = str(transaction.get("transactionDate") or "")[:7]
            if not month:
                continue
            result[month] = result.get(month, 0.0) + abs(RecommendationAgent._amount(transaction))
        return result

    @staticmethod
    def _analysis_months(transactions: list[dict[str, Any]]) -> int:
        """Number of distinct calendar months present in the data (minimum 1). Used
        to turn period totals into monthly averages.
        """
        dates = {str(t.get("transactionDate"))[:7] for t in transactions if t.get("transactionDate")}
        return max(len(dates), 1)

    @staticmethod
    def _earliest_date(transactions: list[dict[str, Any]]) -> str | None:
        """Earliest transactionDate as "YYYY-MM-DD", or None when there are no dates."""
        dates = [str(t.get("transactionDate"))[:10] for t in transactions if t.get("transactionDate")]
        return min(dates) if dates else None

    @staticmethod
    def _latest_date(transactions: list[dict[str, Any]]) -> str | None:
        """Latest transactionDate as "YYYY-MM-DD", or None when there are no dates."""
        dates = [str(t.get("transactionDate"))[:10] for t in transactions if t.get("transactionDate")]
        return max(dates) if dates else None


# Shared instance imported by app/main.py.
recommendation_agent = RecommendationAgent()
