# FinSight File Notes: Analyses spending behaviour and produces category and trend information for the main system.

from __future__ import annotations

from typing import Any

import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler


class BehaviourAnalyzer:
    def __init__(self) -> None:
        self.random_state = 42

    def analyze(self, transactions: list[dict[str, Any]]) -> dict[str, Any]:
        dataframe = pd.DataFrame(transactions)
        if dataframe.empty:
            return self._empty_response()

        required = {"amount", "type", "category"}
        missing = required.difference(dataframe.columns)
        if missing:
            return {
                "status": "ERROR",
                "message": "Missing required transaction fields: " + ", ".join(sorted(missing)),
                "transactionCount": 0,
                "expenseCount": 0,
                "totalExpense": 0.0,
                "averageExpense": 0.0,
                "largestExpense": 0.0,
                "largestExpenseDescription": "",
                "categoryBreakdown": [],
                "clusters": [],
            }

        dataframe["amount"] = pd.to_numeric(dataframe["amount"], errors="coerce")
        dataframe["type"] = dataframe["type"].fillna("").astype(str).str.upper().str.strip()
        dataframe["category"] = dataframe["category"].fillna("OTHER").astype(str).str.upper().str.strip()
        dataframe = dataframe.dropna(subset=["amount"])

        expenses = dataframe[dataframe["type"] == "EXPENSE"].copy()
        expenses["amount"] = expenses["amount"].abs()
        expenses = expenses[expenses["amount"] > 0].copy()

        if expenses.empty:
            return self._empty_response(len(dataframe))

        total = float(expenses["amount"].sum())
        average = float(expenses["amount"].mean())
        largest_index = expenses["amount"].idxmax()
        largest = float(expenses.loc[largest_index, "amount"])
        description = ""
        if "description" in expenses.columns:
            value = expenses.loc[largest_index, "description"]
            if pd.notna(value):
                description = str(value)

        return {
            "status": "SUCCESS",
            "message": "Spending behaviour analysis completed.",
            "transactionCount": int(len(dataframe)),
            "expenseCount": int(len(expenses)),
            "totalExpense": round(total, 2),
            "averageExpense": round(average, 2),
            "largestExpense": round(largest, 2),
            "largestExpenseDescription": description,
            "categoryBreakdown": self._category_breakdown(expenses, total),
            "clusters": self._clusters(expenses),
        }

    def _category_breakdown(self, expenses: pd.DataFrame, total: float) -> list[dict[str, Any]]:
        grouped = expenses.groupby("category")["amount"].sum().sort_values(ascending=False)
        return [
            {
                "category": str(category),
                "amount": round(float(amount), 2),
                "percentage": round((float(amount) / total) * 100.0, 2) if total > 0 else 0.0,
            }
            for category, amount in grouped.items()
        ]

    def _clusters(self, expenses: pd.DataFrame) -> list[dict[str, Any]]:
        if len(expenses) < 3:
            return []

        values = expenses[["amount"]].astype(float).to_numpy()
        cluster_count = min(3, len(values))
        scaler = StandardScaler()
        scaled = scaler.fit_transform(values)
        model = KMeans(n_clusters=cluster_count, random_state=self.random_state, n_init=10)
        labels = model.fit_predict(scaled)
        expenses = expenses.copy()
        expenses["cluster"] = labels
        centers = scaler.inverse_transform(model.cluster_centers_).flatten()

        ordered = sorted(range(cluster_count), key=lambda index: centers[index])
        names = ["LOW_VALUE_SPENDING", "MODERATE_SPENDING", "HIGH_VALUE_SPENDING"]
        total = float(expenses["amount"].sum())
        result: list[dict[str, Any]] = []

        for position, cluster_index in enumerate(ordered):
            rows = expenses[expenses["cluster"] == cluster_index] if "cluster" in expenses.columns else expenses.iloc[labels == cluster_index].copy()
            if "cluster" not in expenses.columns:
                rows = expenses.iloc[np.where(labels == cluster_index)[0]].copy()
            if rows.empty:
                continue
            amount = float(rows["amount"].sum())
            result.append(
                {
                    "cluster": int(cluster_index),
                    "label": names[min(position, len(names) - 1)],
                    "transactionCount": int(len(rows)),
                    "totalAmount": round(amount, 2),
                    "averageAmount": round(float(rows["amount"].mean()), 2),
                    "percentage": round((amount / total) * 100.0, 2) if total > 0 else 0.0,
                }
            )
        return result

    def _empty_response(self, transaction_count: int = 0) -> dict[str, Any]:
        return {
            "status": "SUCCESS",
            "message": "No expense transactions were available for behaviour analysis.",
            "transactionCount": transaction_count,
            "expenseCount": 0,
            "totalExpense": 0.0,
            "averageExpense": 0.0,
            "largestExpense": 0.0,
            "largestExpenseDescription": "",
            "categoryBreakdown": [],
            "clusters": [],
        }


behaviour_analyzer = BehaviourAnalyzer()
