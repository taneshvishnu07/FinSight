# FinSight File Notes: Finds recurring subscription-like payments and estimates their ongoing cost.

from __future__ import annotations

from typing import Any

import pandas as pd


class SubscriptionDetector:
    """
    Rule-based subscription detector.

    A transaction is treated as a subscription only when there is strong
    evidence that it is a service/subscription payment. Recurrence alone is
    not enough because recurring expenses such as fuel, groceries, coffee,
    restaurant meals, train fares, electricity and pharmacy purchases are not
    subscriptions.
    """

    MIN_OCCURRENCES = 3
    MAX_INTERVAL_DAYS = 120
    MONTHLY_MIN_DAYS = 25.0
    MONTHLY_MAX_DAYS = 35.0
    MIN_CONFIDENCE = 0.70

    # Known merchants are explicit subscription/service providers. This list
    # intentionally does not contain everyday spending merchants such as fuel,
    # supermarkets, restaurants, transport, utilities or pharmacies.
    KNOWN = {
        "NETFLIX",
        "SPOTIFY",
        "DISNEY PLUS",
        "DISNEY+",
        "YOUTUBE PREMIUM",
        "YOUTUBE MUSIC",
        "APPLE MUSIC",
        "APPLE TV",
        "AMAZON PRIME",
        "PRIME VIDEO",
        "HBO",
        "HBO MAX",
        "MAX",
        "PARAMOUNT PLUS",
        "PARAMOUNT+",
        "ADOBE",
        "CANVA",
        "MICROSOFT 365",
        "MICROSOFT OFFICE 365",
        "GOOGLE ONE",
        "DROPBOX",
        "CHATGPT PLUS",
        "GYM MEMBERSHIP",
        "MEMBERSHIP",
    }

    # Categories that are normally recurring household/lifestyle expenses but
    # are not subscriptions by themselves.
    EXCLUDED_CATEGORIES = {
        "FUEL",
        "GAS",
        "GROCERIES",
        "FOOD",
        "RESTAURANT",
        "DINING",
        "TRANSPORTATION",
        "TRANSPORT",
        "HEALTHCARE",
        "PHARMACY",
        "UTILITIES",
        "UTILITY",
        "SHOPPING",
        "ENTERTAINMENT",
        "BILLS",
    }

    SUBSCRIPTION_KEYWORDS = {
        "SUBSCRIPTION",
        "MEMBERSHIP",
        "PREMIUM",
        "STREAMING",
        "MONTHLY PLAN",
        "ANNUAL PLAN",
        "MONTHLY FEE",
        "MEMBERSHIP FEE",
    }

    def detect(self, transactions: list[dict[str, Any]]) -> dict[str, Any]:
        dataframe = pd.DataFrame(transactions)
        if dataframe.empty:
            return self._empty()

        required = {"amount", "type", "transactionDate", "description"}
        if not required.issubset(dataframe.columns):
            return self._empty(
                "Transaction data is missing fields needed for subscription detection."
            )

        dataframe["amount"] = pd.to_numeric(dataframe["amount"], errors="coerce")
        dataframe["type"] = (
            dataframe["type"].fillna("").astype(str).str.upper().str.strip()
        )
        dataframe["description"] = (
            dataframe["description"]
            .fillna("Unknown merchant")
            .astype(str)
            .str.strip()
        )
        if "category" in dataframe.columns:
            dataframe["category"] = (
                dataframe["category"]
                .fillna("")
                .astype(str)
                .str.upper()
                .str.strip()
            )
        else:
            dataframe["category"] = ""
        dataframe["transactionDate"] = pd.to_datetime(
            dataframe["transactionDate"], errors="coerce"
        )

        expenses = dataframe[
            (dataframe["type"] == "EXPENSE")
            & dataframe["amount"].notna()
            & dataframe["transactionDate"].notna()
        ].copy()
        expenses["amount"] = expenses["amount"].abs()
        expenses = expenses[expenses["amount"] > 0]

        detected: list[dict[str, Any]] = []

        for merchant, group in expenses.groupby(
            expenses["description"].str.upper().str.strip()
        ):
            group = group.sort_values("transactionDate")
            if len(group) < self.MIN_OCCURRENCES:
                continue

            display = str(group["description"].iloc[0]).strip()
            category_values = {
                str(value).upper().strip()
                for value in group["category"].tolist()
                if str(value).strip()
            }

            known_merchant = merchant in self.KNOWN
            keyword_match = self._contains_subscription_keyword(merchant)
            explicit_subscription_category = "SUBSCRIPTION" in category_values
            excluded_category = bool(category_values & self.EXCLUDED_CATEGORIES)

            # Everyday spending is never promoted to a subscription merely
            # because it happens monthly. A known merchant, explicit
            # subscription category, or clear subscription wording is required.
            if not (known_merchant or explicit_subscription_category or keyword_match):
                continue
            if excluded_category and not (
                known_merchant or explicit_subscription_category or keyword_match
            ):
                continue

            dates = group["transactionDate"].dt.date.tolist()
            intervals = [
                (dates[index] - dates[index - 1]).days
                for index in range(1, len(dates))
            ]
            if not intervals or max(intervals) > self.MAX_INTERVAL_DAYS:
                continue

            mean_interval = sum(intervals) / len(intervals)
            if mean_interval < self.MONTHLY_MIN_DAYS or mean_interval > self.MONTHLY_MAX_DAYS:
                continue

            interval_series = pd.Series(intervals, dtype=float)
            interval_std = float(interval_series.std(ddof=0) or 0.0)
            interval_consistency = 1.0 - min(
                interval_std / max(mean_interval, 1.0), 1.0
            )

            amounts = group["amount"].astype(float)
            amount_mean = float(amounts.mean())
            amount_std = float(amounts.std(ddof=0) or 0.0)
            amount_consistency = 1.0 - min(
                amount_std / max(amount_mean, 1.0), 1.0
            )

            # Evidence is weighted toward recurrence and stable amount. Explicit
            # subscription evidence is rewarded, but recurrence still matters.
            confidence = (
                0.45 * interval_consistency
                + 0.30 * amount_consistency
                + 0.10
                + (0.15 if known_merchant else 0.0)
                + (0.10 if explicit_subscription_category else 0.0)
                + (0.05 if keyword_match else 0.0)
            )
            confidence = min(1.0, confidence)

            if confidence < self.MIN_CONFIDENCE:
                continue

            monthly = amount_mean
            yearly = monthly * 12.0
            detected.append(
                {
                    "merchant": display,
                    "averageAmount": round(monthly, 2),
                    "frequency": "MONTHLY",
                    "occurrences": int(len(group)),
                    "firstTransactionDate": dates[0].isoformat(),
                    "lastTransactionDate": dates[-1].isoformat(),
                    "estimatedMonthlyCost": round(monthly, 2),
                    "estimatedYearlyCost": round(yearly, 2),
                    "confidence": round(confidence, 2),
                    "transactionDates": [date.isoformat() for date in dates],
                }
            )

        detected.sort(key=lambda item: item["estimatedMonthlyCost"], reverse=True)
        monthly_total = sum(item["estimatedMonthlyCost"] for item in detected)

        return {
            "status": "SUCCESS",
            "message": self._message(len(detected)),
            "subscriptionCount": len(detected),
            "estimatedMonthlySubscriptionCost": round(monthly_total, 2),
            "estimatedYearlySubscriptionCost": round(monthly_total * 12.0, 2),
            "subscriptions": detected,
        }

    def _contains_subscription_keyword(self, merchant: str) -> bool:
        return any(keyword in merchant for keyword in self.SUBSCRIPTION_KEYWORDS)

    def _message(self, count: int) -> str:
        if count == 0:
            return "No recurring monthly subscriptions were detected."
        return f"Detected {count} recurring monthly subscription{'s' if count != 1 else ''}."

    def _empty(
        self,
        message: str = "No recurring monthly subscriptions were detected.",
    ) -> dict[str, Any]:
        return {
            "status": "SUCCESS",
            "message": message,
            "subscriptionCount": 0,
            "estimatedMonthlySubscriptionCost": 0.0,
            "estimatedYearlySubscriptionCost": 0.0,
            "subscriptions": [],
        }


subscription_detector = SubscriptionDetector()
