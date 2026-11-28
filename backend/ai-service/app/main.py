"""FinSight AI service - FastAPI entry point.

A small stateless HTTP service called by the Spring Boot backend (see the Ai*Client
classes in com.finsight.backend.client). The backend sends transactions as JSON; each
endpoint runs one analysis module and returns JSON. Nothing is stored here.

Run locally (from ai-service/):  uvicorn app.main:app --port 8000
The backend expects it at finsight.ai-service.url (default http://127.0.0.1:8000).

Endpoints: GET /health, POST /api/classification, /api/behaviour-analysis,
/api/forecast, /api/unusual-spending, /api/subscriptions, /api/recommendations,
/api/alerts
"""

from __future__ import annotations

from typing import Any

from fastapi import Body, FastAPI
from pydantic import BaseModel, Field

# Each module exposes a ready-made singleton (e.g. `transaction_classifier`) that is
# shared by all requests.
from app.behaviour.behaviour_analyzer import behaviour_analyzer
from app.classification.transaction_classifier import transaction_classifier
from app.forecasting.expense_forecaster import expense_forecaster
from app.anomaly.unusual_spending_detector import unusual_spending_detector
from app.subscription.subscription_detector import subscription_detector
from app.recommendation.recommendation_agent import recommendation_agent
from app.alert.alert_agent import alert_agent

app = FastAPI(title="FinSight AI Service", version="1.0.0")


def _as_dict(payload: Any) -> dict[str, Any]:
    """Return the payload if it is a dict, otherwise {} (tolerates empty or malformed
    request bodies).
    """
    if isinstance(payload, dict):
        return payload
    return {}


def _transactions(payload: Any) -> list[dict[str, Any]]:
    """Extract the transaction list from a request body. Accepts either a bare JSON
    array or an object with a "transactions" key; anything that is not a dict is
    dropped.
    """
    if isinstance(payload, list):
        return [item for item in payload if isinstance(item, dict)]
    if isinstance(payload, dict):
        values = payload.get("transactions", [])
        if isinstance(values, list):
            return [item for item in values if isinstance(item, dict)]
    return []


def _analysis_payload(payload: Any) -> dict[str, Any]:
    """Parse the body of /recommendations and /alerts requests (kept as a separate
    function so the intent is clear at the call site).
    """
    return _as_dict(payload)


# Pydantic request models. NOTE: the endpoints below accept a raw `Body` and parse it
# with the helpers above, so these models are currently only documentation of the
# expected payload shape.
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
    """Liveness probe, used by the backend's AiServiceHealthClient."""
    return {"status": "UP", "service": "finsight-ai-service"}


@app.post("/api/classification")
def classify_transactions(payload: Any = Body(default=None)) -> dict[str, Any]:
    """Assign a FinSight category (and confidence) to each transaction description."""
    return transaction_classifier.classify(_transactions(payload))


@app.post("/api/behaviour-analysis")
def analyse_behaviour(payload: Any = Body(default=None)) -> dict[str, Any]:
    """Spending summary: totals, category breakdown and K-Means spending clusters."""
    return behaviour_analyzer.analyze(_transactions(payload))


@app.post("/api/forecast")
def forecast_expenses(payload: Any = Body(default=None)) -> dict[str, Any]:
    """Predict monthly expenses. Optional body field `monthsAhead` (default 1;
    invalid values fall back to 1; the forecaster caps it at 12).
    """
    data = _as_dict(payload)
    months_ahead = data.get("monthsAhead", 1)
    try:
        months_ahead = max(int(months_ahead), 1)
    except (TypeError, ValueError):
        months_ahead = 1
    return expense_forecaster.forecast(_transactions(payload), months_ahead)


@app.post("/api/unusual-spending")
def detect_unusual_spending(payload: Any = Body(default=None)) -> dict[str, Any]:
    """Flag expenses that are unusually large compared with normal spending."""
    return unusual_spending_detector.detect(_transactions(payload))


@app.post("/api/subscriptions")
def detect_subscriptions(payload: Any = Body(default=None)) -> dict[str, Any]:
    """Find recurring monthly subscription payments."""
    return subscription_detector.detect(_transactions(payload))


@app.post("/api/recommendations")
def generate_recommendations(payload: Any = Body(default=None)) -> dict[str, Any]:
    """Generate advice. Body: transactions + financialProfile + subscriptionAnalysis
    + anomalyAnalysis (the outputs of the two detectors, supplied by the backend).
    """
    data = _analysis_payload(payload)
    return recommendation_agent.generate(
        _transactions(payload),
        data.get("subscriptionAnalysis") or {},
        data.get("anomalyAnalysis") or {},
        data.get("financialProfile") or {},
    )


@app.post("/api/alerts")
def generate_alerts(payload: Any = Body(default=None)) -> dict[str, Any]:
    """Generate warnings. Same body as /api/recommendations."""
    data = _analysis_payload(payload)
    return alert_agent.generate(
        _transactions(payload),
        data.get("subscriptionAnalysis") or {},
        data.get("anomalyAnalysis") or {},
        data.get("financialProfile") or {},
    )
