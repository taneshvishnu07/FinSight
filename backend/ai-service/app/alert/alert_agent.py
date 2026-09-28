# FinSight File Notes: Generates alerts from transaction patterns, budgets, profile information, and other analysis results.

from __future__ import annotations

from typing import Any


class AlertAgent:
    """Rule-based financial alert agent with natural wording."""

    generated_by = "PYTHON_RULE_BASED_ALERT_AGENT"

    def generate(
        self,
        transactions: list[dict[str, Any]],
        subscription_analysis: dict[str, Any] | None,
        anomaly_analysis: dict[str, Any] | None,
        financial_profile: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        transactions = transactions or []
        subscription_analysis = subscription_analysis or {}
        anomaly_analysis = anomaly_analysis or {}
        financial_profile = financial_profile or {}

        income = sum(abs(self._amount(t)) for t in transactions if self._type(t) == "INCOME")
        expense = sum(abs(self._amount(t)) for t in transactions if self._type(t) == "EXPENSE")
        alerts: list[dict[str, Any]] = []

        goal_months = max(int(self._number(financial_profile.get("financialGoalMonths"))) or 6, 1)
        occupation_type = self._text(financial_profile.get("occupationType"), "").upper()
        monthly_budget_map = self._monthly_budget_map(financial_profile)
        monthly_allowance = self._number(financial_profile.get("monthlyAllowance"))
        monthly_salary = self._number(financial_profile.get("monthlySalary"))
        period_months = self._analysis_months(transactions)
        monthly_expense = expense / period_months if period_months > 0 else expense
        monthly_savings = (income - expense) / period_months if period_months > 0 else (income - expense)

        monthly_expenses = self._monthly_expense_stats(transactions)
        for month, month_expense in monthly_expenses.items():
            budget = monthly_budget_map.get(month)
            if budget is None or budget <= 0:
                continue
            if month_expense > budget:
                over = month_expense - budget
                severity = "HIGH" if over >= budget * 0.20 else "MEDIUM"
                alerts.append(self._item(
                    "BUDGET", severity, "You exceeded your monthly budget",
                    f"In {month}, your expenses were about RM{month_expense:.2f}, which is RM{over:.2f} above your RM{budget:.2f} budget.",
                ))

        configured_budgets = [value for month, value in monthly_budget_map.items() if value > 0]
        average_configured_budget = (sum(configured_budgets) / len(configured_budgets)) if configured_budgets else 0
        if goal_months > 0 and monthly_savings > 0 and average_configured_budget > 0:
            projected = monthly_savings * goal_months
            goal_budget_reference = average_configured_budget * goal_months
            if projected < goal_budget_reference * 0.10:
                alerts.append(self._item(
                    "FINANCIAL_GOAL", "MEDIUM", "Your current savings pace may be too low",
                    f"At about RM{monthly_savings:.2f} saved per month, you would accumulate roughly RM{projected:.2f} over your {goal_months}-month goal period. Your configured monthly budgets average about RM{average_configured_budget:.2f}; reducing discretionary spending could improve your savings pace.",
                ))

        if occupation_type == "STUDENT" and monthly_allowance > 0 and monthly_expense > monthly_allowance:
            over = monthly_expense - monthly_allowance
            alerts.append(self._item(
                "BUDGET", "HIGH", "Your spending is above your monthly allowance",
                f"Your average monthly spending is about RM{monthly_expense:.2f}, exceeding your RM{monthly_allowance:.2f} allowance by RM{over:.2f}. Review discretionary spending first.",
            ))

        if occupation_type == "EMPLOYEE" and monthly_salary > 0 and income > 0:
            recorded_monthly_income = income / period_months
            if recorded_monthly_income < monthly_salary * 0.80:
                alerts.append(self._item(
                    "INCOME", "LOW", "Recorded income is lower than your profile salary",
                    f"Your uploaded transactions show about RM{recorded_monthly_income:.2f} average monthly income, compared with your RM{monthly_salary:.2f} profile salary. Check whether income transactions are missing from the upload.",
                ))

        if income > 0:
            expense_ratio = expense / income
            if expense_ratio >= 0.70:
                alerts.append(self._item(
                    "BUDGET", "HIGH", "Your spending is taking up most of your income",
                    f"You are spending about {expense_ratio * 100:.1f}% of your income. Cutting back on non-essential purchases could leave more money for savings.",
                ))
            elif expense_ratio >= 0.50:
                alerts.append(self._item(
                    "BUDGET", "MEDIUM", "Keep an eye on your spending",
                    f"You are using about {expense_ratio * 100:.1f}% of your income for expenses. A little less discretionary spending could help you save more.",
                ))

            savings_ratio = (income - expense) / income
            if savings_ratio <= 0:
                alerts.append(self._item(
                    "LOW_BALANCE", "CRITICAL", "Your expenses are higher than your income",
                    "You spent as much as or more than you earned during this period. Reviewing non-essential spending is a good next step.",
                ))
            elif savings_ratio < 0.10:
                alerts.append(self._item(
                    "LOW_BALANCE", "HIGH", "Your savings are quite low",
                    f"Only about {savings_ratio * 100:.1f}% of your income remained after expenses. Consider setting aside a little more by reducing optional spending.",
                ))

        anomalies = anomaly_analysis.get("anomalies") or []
        for anomaly in anomalies:
            merchant = self._text(anomaly.get("merchant"), "Unknown merchant")
            amount = self._number(anomaly.get("amount"))
            reason = self._text(
                anomaly.get("reason"),
                "This purchase is higher than your usual spending and is worth checking.",
            )
            severity = self._text(anomaly.get("severity"), "MEDIUM").upper()
            if severity not in {"LOW", "MEDIUM", "HIGH", "CRITICAL"}:
                severity = "MEDIUM"
            alerts.append(self._item(
                "UNUSUAL_SPENDING", severity, "This purchase looks unusual",
                f"We noticed {merchant} for RM{amount:.2f}. {reason}",
            ))

        monthly = self._number(subscription_analysis.get("estimatedMonthlySubscriptionCost"))
        count = int(self._number(subscription_analysis.get("subscriptionCount")))
        if count > 0 and monthly > 0:
            severity = "LOW"
            if income > 0:
                ratio = monthly / income
                severity = "HIGH" if ratio >= 0.20 else "MEDIUM" if ratio >= 0.10 else "LOW"
            alerts.append(self._item(
                "SUBSCRIPTION", severity, "Your recurring subscriptions are adding up",
                f"You have {count} recurring subscription(s) costing about RM{monthly:.2f} each month. Check whether you still use all of them.",
            ))

        summary = (
            f"I found {len(alerts)} alert(s) worth reviewing."
            if alerts else
            "I did not find any spending alerts that need your attention right now."
        )
        return {
            "status": "SUCCESS",
            "alertCount": len(alerts),
            "unreadCount": len(alerts),
            "alerts": alerts,
            "summary": summary,
            "generatedBy": self.generated_by,
        }

    def _item(self, alert_type: str, severity: str, title: str, message: str) -> dict[str, Any]:
        return {
            "id": None,
            "type": alert_type,
            "severity": severity,
            "title": title,
            "message": message,
            "isRead": False,
            "generatedBy": self.generated_by,
        }

    @staticmethod
    def _monthly_budget_map(financial_profile: dict[str, Any]) -> dict[str, float]:
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
        result: dict[str, float] = {}
        for transaction in transactions:
            if str(transaction.get("type") or "").upper() != "EXPENSE":
                continue
            month = str(transaction.get("transactionDate") or "")[:7]
            if not month:
                continue
            result[month] = result.get(month, 0.0) + abs(AlertAgent._amount(transaction))
        return result

    @staticmethod
    def _analysis_months(transactions: list[dict[str, Any]]) -> int:
        dates = {str(t.get("transactionDate"))[:7] for t in transactions if t.get("transactionDate")}
        return max(len(dates), 1)

    @staticmethod
    def _amount(transaction: dict[str, Any]) -> float:
        try:
            return float(transaction.get("amount") or 0)
        except (TypeError, ValueError):
            return 0.0

    @staticmethod
    def _number(value: Any) -> float:
        try:
            return float(value or 0)
        except (TypeError, ValueError):
            return 0.0

    @staticmethod
    def _type(transaction: dict[str, Any]) -> str:
        return str(transaction.get("type") or "").upper().strip()

    @staticmethod
    def _category(transaction: dict[str, Any]) -> str:
        return str(transaction.get("category") or "").upper().strip()

    @staticmethod
    def _text(value: Any, fallback: str) -> str:
        text = str(value or "").strip()
        return text if text else fallback


alert_agent = AlertAgent()
