# FinSight File Notes: Starts the FastAPI AI service and exposes the REST endpoints used by the Spring Boot backend.

from __future__ import annotations

from typing import Any

from fastapi import Body, FastAPI
from pydantic import BaseModel, Field

from app.behaviour.behaviour_analyzer import behaviour_analyzer
from app.classification.transaction_classifier import transaction_classifier
from app.forecasting.expense_forecaster import expense_forecaster
from app.anomaly.unusual_spending_detector import unusual_spending_detector
from app.subscription.subscription_detector import subscription_detector
from app.recommendation.recommendation_agent import recommendation_agent
from app.alert.alert_agent import alert_agent

app = FastAPI(title="FinSight AI Service", version="1.0.0")


def _as_dict(payload: Any) -> dict[str, Any]:
    if isinstance(payload, dict):
        return payload
    return {}


def _transactions(payload: Any) -> list[dict[str, Any]]:
    if isinstance(payload, list):
        return [item for item in payload if isinstance(item, dict)]
    if isinstance(payload, dict):
        values = payload.get("transactions", [])
        if isinstance(values, list):
            return [item for item in values if isinstance(item, dict)]
    return []


def _analysis_payload(payload: Any) -> dict[str, Any]:
    return _as_dict(payload)


class TransactionRequest(BaseModel):
    transactions: list[dict[str, Any]] = Field(default_factory=list)


class ForecastRequest(BaseModel):
    transactions: list[dict[str, Any]] = Field(default_factory=list)
    monthsAhead: int = Field(default=1, ge=1, le=12)


class AnalysisRequest(BaseModel):
    transactions: list[dict[str, Any]] = Field(default_factory=list)
    financialProfile: dict[str, Any] = Field(default_factory=dict)
    subscriptionAnalysis: dict[str, Any] = Field(default_factory=dict)
    anomalyAnalysis: dict[str, Any] = Field(default_factory=dict)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP", "service": "finsight-ai-service"}


@app.post("/api/classification")
def classify_transactions(payload: Any = Body(default=None)) -> dict[str, Any]:
    return transaction_classifier.classify(_transactions(payload))


@app.post("/api/behaviour-analysis")
def analyse_behaviour(payload: Any = Body(default=None)) -> dict[str, Any]:
    return behaviour_analyzer.analyze(_transactions(payload))


@app.post("/api/forecast")
def forecast_expenses(payload: Any = Body(default=None)) -> dict[str, Any]:
    data = _as_dict(payload)
    months_ahead = data.get("monthsAhead", 1)
    try:
        months_ahead = max(int(months_ahead), 1)
    except (TypeError, ValueError):
        months_ahead = 1
    return expense_forecaster.forecast(_transactions(payload), months_ahead)


@app.post("/api/unusual-spending")
def detect_unusual_spending(payload: Any = Body(default=None)) -> dict[str, Any]:
    return unusual_spending_detector.detect(_transactions(payload))


@app.post("/api/subscriptions")
def detect_subscriptions(payload: Any = Body(default=None)) -> dict[str, Any]:
    return subscription_detector.detect(_transactions(payload))


@app.post("/api/recommendations")
def generate_recommendations(payload: Any = Body(default=None)) -> dict[str, Any]:
    data = _analysis_payload(payload)
    return recommendation_agent.generate(
        _transactions(payload),
        data.get("subscriptionAnalysis") or {},
        data.get("anomalyAnalysis") or {},
        data.get("financialProfile") or {},
    )


@app.post("/api/alerts")
def generate_alerts(payload: Any = Body(default=None)) -> dict[str, Any]:
    data = _analysis_payload(payload)
    return alert_agent.generate(
        _transactions(payload),
        data.get("subscriptionAnalysis") or {},
        data.get("anomalyAnalysis") or {},
        data.get("financialProfile") or {},
    )
