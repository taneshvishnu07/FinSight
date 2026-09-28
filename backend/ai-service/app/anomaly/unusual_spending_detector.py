# FinSight File Notes: Detects transactions that are unusual compared with the user financial data while ignoring essential expenses.

from __future__ import annotations

from typing import Any

import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest


class UnusualSpendingDetector:
    def __init__(self) -> None:
        self.random_state = 42
        self.minimum_amount = 100.0

    def detect(self, transactions: list[dict[str, Any]]) -> dict[str, Any]:
        dataframe = pd.DataFrame(transactions)
        if dataframe.empty:
            return self._empty("No transactions were available for unusual spending analysis.")

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

        candidate_amounts = expenses["amount"].astype(float).to_numpy()
        all_expenses = dataframe[dataframe["type"] == "EXPENSE"].copy()
        all_expenses["amount"] = all_expenses["amount"].abs()
        all_expenses = all_expenses[all_expenses["amount"] > 0].copy()
        benchmark_amounts = all_expenses["amount"].astype(float).to_numpy()
        average = float(np.mean(benchmark_amounts)) if len(benchmark_amounts) else float(np.mean(candidate_amounts))
        std = float(np.std(benchmark_amounts)) if len(benchmark_amounts) else float(np.std(candidate_amounts))
        contamination = min(max(2.0 / len(candidate_amounts), 0.05), 0.20)

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
            domain_flag = amount >= self.minimum_amount and amount >= average * 2.0
            if not bool(flags[index]) or not domain_flag:
                continue

            multiple = amount / average if average > 0 else 0.0
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
        if count == 0:
            return "No unusually high spending was detected in the transactions reviewed."
        if count == 1:
            return f"1 expense stands out as unusual compared with the average expense of RM {average:.2f}."
        return f"{count} expenses stand out as unusual compared with the average expense of RM {average:.2f}."

    def _empty(self, message: str, status: str = "SUCCESS") -> dict[str, Any]:
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


unusual_spending_detector = UnusualSpendingDetector()
