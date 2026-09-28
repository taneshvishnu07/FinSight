# FinSight File Notes: Estimates future monthly expenses from the user transaction history.

from __future__ import annotations

from typing import Any

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor


class ExpenseForecaster:
    """Forecast monthly expenses using Random Forest plus a damped trend adjustment.

    The previous implementation used only the month index as the model feature.
    With a short history this could make every future month receive the same
    prediction. The enhanced version keeps Random Forest as the main model but
    combines it with the observed linear trend and recent weighted average so
    forecasts respond to a clear upward/downward spending pattern.
    """

    def __init__(self) -> None:
        self.random_state = 42
        self.minimum_months_for_model = 3

    def forecast(
        self,
        transactions: list[dict[str, Any]],
        months_ahead: int = 1,
    ) -> dict[str, Any]:
        dataframe = self._prepare(transactions)
        if dataframe.empty:
            return self._empty()

        monthly = (
            dataframe.groupby(dataframe["transactionDate"].dt.to_period("M"))["amount"]
            .sum()
            .reset_index(name="amount")
            .sort_values("transactionDate")
            .reset_index(drop=True)
        )

        if monthly.empty:
            return self._empty()

        months_ahead = max(1, min(int(months_ahead), 12))

        if len(monthly) >= self.minimum_months_for_model:
            model_name = "HYBRID_RANDOM_FOREST_TREND"
            forecasts = self._hybrid_random_forest(monthly, months_ahead)
        else:
            model_name = "WEIGHTED_MOVING_AVERAGE"
            forecasts = self._weighted_average(monthly, months_ahead)

        return {
            "status": "SUCCESS",
            "message": "Expense forecasting completed.",
            "model": model_name,
            "historicalMonthCount": int(len(monthly)),
            "historicalAverageMonthlyExpense": round(float(monthly["amount"].mean()), 2),
            "forecastMonths": forecasts,
        }

    def _prepare(self, transactions: list[dict[str, Any]]) -> pd.DataFrame:
        dataframe = pd.DataFrame(transactions)
        required = {"amount", "type", "transactionDate"}
        if dataframe.empty or not required.issubset(dataframe.columns):
            return pd.DataFrame()

        dataframe["amount"] = pd.to_numeric(dataframe["amount"], errors="coerce")
        dataframe["type"] = dataframe["type"].fillna("").astype(str).str.upper().str.strip()
        dataframe["transactionDate"] = pd.to_datetime(
            dataframe["transactionDate"], errors="coerce"
        )
        dataframe = dataframe.dropna(subset=["amount", "transactionDate"])
        dataframe = dataframe[dataframe["type"] == "EXPENSE"].copy()
        dataframe["amount"] = dataframe["amount"].abs()
        return dataframe[dataframe["amount"] > 0].copy()

    def _hybrid_random_forest(
        self, monthly: pd.DataFrame, months_ahead: int
    ) -> list[dict[str, Any]]:
        y = monthly["amount"].astype(float).to_numpy()
        x = np.arange(len(y)).reshape(-1, 1)

        model = RandomForestRegressor(
            n_estimators=300,
            random_state=self.random_state,
            min_samples_leaf=1,
        )
        model.fit(x, y)

        rf_predictions = [
            max(float(model.predict([[len(y) - 1 + step]])[0]), 0.0)
            for step in range(1, months_ahead + 1)
        ]

        # Estimate the observed monthly trend. With only a few observations,
        # cap the trend effect so one unusual month cannot dominate the forecast.
        slope = float(np.polyfit(np.arange(len(y)), y, 1)[0]) if len(y) >= 2 else 0.0
        max_monthly_change = max(float(np.mean(y)) * 0.25, 50.0)
        slope = float(np.clip(slope, -max_monthly_change, max_monthly_change))

        recent = y[-min(3, len(y)):]
        weights = np.arange(1, len(recent) + 1, dtype=float)
        weighted_average = float(np.average(recent, weights=weights))

        # Blend the ML prediction with recent behaviour and the observed trend.
        # Trend influence increases slightly for later forecast months, while
        # remaining damped to avoid unrealistic extrapolation.
        result: list[dict[str, Any]] = []
        last_period = monthly["transactionDate"].iloc[-1]
        volatility = float(np.std(y, ddof=1)) if len(y) > 1 else 0.0
        volatility = max(volatility, float(np.mean(y)) * 0.05)

        for step, rf_prediction in enumerate(rf_predictions, start=1):
            trend_prediction = max(y[-1] + (slope * step * 0.75), 0.0)
            recent_prediction = max(weighted_average + (slope * max(step - 1, 0) * 0.25), 0.0)

            prediction = (
                (0.45 * rf_prediction)
                + (0.40 * trend_prediction)
                + (0.15 * recent_prediction)
            )

            # Keep the forecast within a sensible range relative to the recent
            # spending level while still allowing a clear trend to show.
            lower_cap = max(y[-1] * 0.50, 0.0)
            upper_cap = max(y[-1] * 1.75, weighted_average * 1.75)
            prediction = float(np.clip(prediction, lower_cap, upper_cap))

            # Uncertainty widens gradually for further-out months.
            uncertainty = volatility * (1.0 + 0.15 * (step - 1))
            lower_bound = max(prediction - uncertainty, 0.0)
            upper_bound = prediction + uncertainty

            result.append(
                {
                    "month": str(last_period + step),
                    "predictedExpense": round(prediction, 2),
                    "lowerBound": round(lower_bound, 2),
                    "upperBound": round(upper_bound, 2),
                }
            )

        return result

    def _weighted_average(
        self, monthly: pd.DataFrame, months_ahead: int
    ) -> list[dict[str, Any]]:
        values = monthly["amount"].astype(float).tolist()
        recent = values[-min(3, len(values)):]
        weights = list(range(1, len(recent) + 1))
        prediction = sum(
            value * weight for value, weight in zip(recent, weights)
        ) / sum(weights)
        std = float(np.std(recent, ddof=1)) if len(recent) > 1 else 0.0
        last_period = monthly["transactionDate"].iloc[-1]

        return [
            {
                "month": str(last_period + step),
                "predictedExpense": round(max(prediction, 0.0), 2),
                "lowerBound": round(max(prediction - std, 0.0), 2),
                "upperBound": round(prediction + std, 2),
            }
            for step in range(1, months_ahead + 1)
        ]

    def _empty(self) -> dict[str, Any]:
        return {
            "status": "SUCCESS",
            "message": "Insufficient expense data for forecasting.",
            "model": "NONE",
            "historicalMonthCount": 0,
            "historicalAverageMonthlyExpense": 0.0,
            "forecastMonths": [],
        }


expense_forecaster = ExpenseForecaster()
