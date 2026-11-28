"""Unusual (anomalous) spending detection (AI service).

Flags individual expenses that are far above the user's normal spending using an
Isolation Forest, then applies a business rule on top so only meaningfully large
expenses are reported. Exposed via POST /api/unusual-spending.
"""

from __future__ import annotations

from typing import Any

import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest


class UnusualSpendingDetector:
    """Detects unusually large discretionary expenses. Use the shared
    `unusual_spending_detector` instance.
    """
    def __init__(self) -> None:
        self.random_state = 42
        # Expenses below RM 100 are never reported as unusual, however they score
        # statistically.
        self.minimum_amount = 100.0

    def detect(self, transactions: list[dict[str, Any]]) -> dict[str, Any]:
        """Detect unusual expenses in a list of transaction dicts (keys used: id,
        amount, type, transactionDate, category, description).

        How it works:
          1. Keep EXPENSE rows and drop essential categories (rent, bills, etc.).
          2. The "average" used for comparison is calculated from ALL expenses, including essential ones, so normal bills do not make everyday spending look odd.
          3. Isolation Forest flags statistical outliers (a simple rule is used when there are fewer than 3 candidates).
          4. An outlier is only reported if it is also >= minimum_amount AND >= 2x the average.
          5. Severity: MEDIUM (2x+), HIGH (3x+), CRITICAL (5x+ the average).

        Returns the response consumed by the Java AiAnomalyClient.
        """
        dataframe = pd.DataFrame(transactions)
        if dataframe.empty:
            return self._empty("No transactions were available for unusual spending analysis.")

        # Fields the backend must send for every transaction.
        required = {"id", "amount", "type", "transactionDate"}
        missing = required.difference(dataframe.columns)
        if missing:
            return self._empty("Missing required transaction fields: " + ", ".join(sorted(missing)), "ERROR")

        dataframe["amount"] = pd.to_numeric(dataframe["amount"], errors="coerce")
        dataframe["type"] = dataframe["type"].fillna("").astype(str).str.upper().str.strip()
        dataframe["transactionDate"] = pd.to_datetime(dataframe["transactionDate"], errors="coerce")
        dataframe = dataframe.dropna(subset=["amount", "transactionDate"])
        expenses = dataframe[dataframe["type"] == "EXPENSE"].copy()
        expenses["amount"] = expenses["amount"].abs()
        expenses = expenses[expenses["amount"] > 0].copy()
        # Rent, bills, groceries and similar planned costs are excluded from the
        # candidates (see comment below).
        # Essential living payments are valid planned obligations and should not
        # be presented as unusual/discretionary spending.
        essential_categories = {
            "HOUSING", "UTILITIES", "GROCERIES", "TRANSPORTATION",
            "HEALTHCARE", "EDUCATION", "FINANCIAL"
        }
        categories = expenses.get("category", pd.Series(index=expenses.index, dtype=str)).fillna("").astype(str).str.upper().str.strip()
        expenses = expenses[~categories.isin(essential_categories)].copy()

        if expenses.empty:
            return self._empty("No expense transactions were available for unusual spending analysis.")

        # candidate_amounts: expenses we may flag. benchmark: every expense, used only
        # to compute the average/std dev for comparison.
        candidate_amounts = expenses["amount"].astype(float).to_numpy()
        all_expenses = dataframe[dataframe["type"] == "EXPENSE"].copy()
        all_expenses["amount"] = all_expenses["amount"].abs()
        all_expenses = all_expenses[all_expenses["amount"] > 0].copy()
        benchmark_amounts = all_expenses["amount"].astype(float).to_numpy()
        average = float(np.mean(benchmark_amounts)) if len(benchmark_amounts) else float(np.mean(candidate_amounts))
        std = float(np.std(benchmark_amounts)) if len(benchmark_amounts) else float(np.std(candidate_amounts))
        # Expected share of outliers for the Isolation Forest: roughly 2 transactions,
        # kept between 5% and 20% of the data.
        contamination = min(max(2.0 / len(candidate_amounts), 0.05), 0.20)

        # Too few data points to train a model: flag anything at least 2x the average
        # (and >= minimum_amount).
        if len(candidate_amounts) < 3:
            flags = candidate_amounts >= max(self.minimum_amount, average * 2.0)
            scores = np.where(flags, 1.0, 0.0)
        else:
            model = IsolationForest(
                n_estimators=300,
                contamination=contamination,
                random_state=self.random_state,
            )
            model.fit(candidate_amounts.reshape(-1, 1))
            flags = model.predict(candidate_amounts.reshape(-1, 1)) == -1
            raw_scores = -model.score_samples(candidate_amounts.reshape(-1, 1))
            scores = raw_scores

        anomalies: list[dict[str, Any]] = []
        for index, (_, row) in enumerate(expenses.iterrows()):
            amount = float(row["amount"])
            # Business-rule filter applied on top of the model result - both must
            # agree before an expense is reported.
            domain_flag = amount >= self.minimum_amount and amount >= average * 2.0
            if not bool(flags[index]) or not domain_flag:
                continue

            multiple = amount / average if average > 0 else 0.0
            # Severity is based on how many times larger the expense is than the
            # average.
            severity = "CRITICAL" if multiple >= 5 else "HIGH" if multiple >= 3 else "MEDIUM"
            description = str(row.get("description", "Transaction")) if pd.notna(row.get("description", "")) else "Transaction"
            category = str(row.get("category", "OTHER")) if pd.notna(row.get("category", "OTHER")) else "OTHER"
            date = row["transactionDate"].date().isoformat()
            reason = (
                f"This expense is about {multiple:.1f} times the average expense of "
                f"RM {average:.2f}. It is much higher than the usual spending level in this data."
            )
            anomalies.append({
                "transactionId": int(row["id"]) if pd.notna(row["id"]) else None,
                "merchant": description,
                "amount": round(amount, 2),
                "category": category,
                "transactionDate": date,
                "anomalyScore": round(float(scores[index]), 4),
                "severity": severity,
                "reason": reason,
            })

        # Most anomalous first.
        anomalies.sort(key=lambda item: item.get("anomalyScore", 0), reverse=True)
        from_date = dataframe["transactionDate"].min().date().isoformat()
        to_date = dataframe["transactionDate"].max().date().isoformat()
        summary = self._summary(len(anomalies), average)
        return {
            "status": "SUCCESS",
            "message": "Unusual spending analysis completed.",
            "analysisId": None,
            "uploadHistoryId": None,
            "fileName": "",
            "analysedFrom": from_date,
            "analysedTo": to_date,
            "totalTransactions": int(len(dataframe)),
            "expenseTransactions": int(len(expenses)),
            "averageExpense": round(average, 2),
            "standardDeviation": round(std, 2),
            "anomalyCount": len(anomalies),
            "anomalies": anomalies,
            "summary": summary,
        }

    def _summary(self, count: int, average: float) -> str:
        """Human-readable one-line summary shown in the UI."""
        if count == 0:
            return "No unusually high spending was detected in the transactions reviewed."
        if count == 1:
            return f"1 expense stands out as unusual compared with the average expense of RM {average:.2f}."
        return f"{count} expenses stand out as unusual compared with the average expense of RM {average:.2f}."

    def _empty(self, message: str, status: str = "SUCCESS") -> dict[str, Any]:
        """Build a response with no anomalies; `status` is "ERROR" when input fields
        were missing.
        """
        return {
            "status": status,
            "message": message,
            "analysisId": None,
            "uploadHistoryId": None,
            "fileName": "",
            "analysedFrom": None,
            "analysedTo": None,
            "totalTransactions": 0,
            "expenseTransactions": 0,
            "averageExpense": 0.0,
            "standardDeviation": 0.0,
            "anomalyCount": 0,
            "anomalies": [],
            "summary": message,
        }


# Shared instance imported by app/main.py.
unusual_spending_detector = UnusualSpendingDetector()
