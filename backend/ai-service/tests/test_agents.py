# FinSight File Notes: Project resource file used by FinSight.

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.behaviour.behaviour_analyzer import behaviour_analyzer
from app.forecasting.expense_forecaster import expense_forecaster
from app.anomaly.unusual_spending_detector import unusual_spending_detector
from app.subscription.subscription_detector import subscription_detector


TRANSACTIONS = [
    {"transactionDate": "2026-04-01T00:00:00", "description": "Shopping", "amount": 500, "type": "EXPENSE", "category": "SHOPPING"},
    {"transactionDate": "2026-05-01T00:00:00", "description": "Shopping", "amount": 700, "type": "EXPENSE", "category": "SHOPPING"},
    {"transactionDate": "2026-06-01T00:00:00", "description": "Shopping", "amount": 900, "type": "EXPENSE", "category": "SHOPPING"},
]


def test_behaviour_agent():
    result = behaviour_analyzer.analyze(TRANSACTIONS)
    assert result["status"] == "SUCCESS"
    assert result["expenseCount"] == 3
    assert result["totalExpense"] == 2100.0
    assert len(result["clusters"]) == 3


def test_forecast_agent():
    result = expense_forecaster.forecast(TRANSACTIONS, 1)
    assert result["status"] == "SUCCESS"
    assert result["model"] == "HYBRID_RANDOM_FOREST_TREND"
    assert result["historicalMonthCount"] == 3
    assert len(result["forecastMonths"]) == 1
    assert result["forecastMonths"][0]["predictedExpense"] >= 0


def test_forecast_changes_with_clear_upward_trend():
    result = expense_forecaster.forecast(
        [
            {"transactionDate": "2026-05-01", "amount": 566.95, "type": "EXPENSE"},
            {"transactionDate": "2026-06-01", "amount": 595.80, "type": "EXPENSE"},
            {"transactionDate": "2026-07-01", "amount": 624.40, "type": "EXPENSE"},
            {"transactionDate": "2026-08-01", "amount": 799.40, "type": "EXPENSE"},
        ],
        3,
    )

    forecasts = result["forecastMonths"]
    assert len(forecasts) == 3
    assert forecasts[0]["predictedExpense"] < forecasts[1]["predictedExpense"]
    assert forecasts[1]["predictedExpense"] < forecasts[2]["predictedExpense"]
    assert all(item["lowerBound"] <= item["predictedExpense"] <= item["upperBound"] for item in forecasts)


def test_unusual_spending_agent():
    transactions = [
        {"id": 1, "transactionDate": "2026-04-01T00:00:00", "description": "Groceries", "amount": 100, "type": "EXPENSE", "category": "FOOD"},
        {"id": 2, "transactionDate": "2026-05-01T00:00:00", "description": "Groceries", "amount": 120, "type": "EXPENSE", "category": "FOOD"},
        {"id": 3, "transactionDate": "2026-06-01T00:00:00", "description": "Laptop", "amount": 600, "type": "EXPENSE", "category": "SHOPPING"},
    ]
    result = unusual_spending_detector.detect(transactions)
    assert result["status"] == "SUCCESS"
    assert result["anomalyCount"] >= 1
    assert result["anomalies"][0]["amount"] == 600.0


def test_subscription_agent():
    transactions = [
        {"id": 1, "transactionDate": "2026-01-05T00:00:00", "description": "Netflix", "amount": 55, "type": "EXPENSE", "category": "SUBSCRIPTION"},
        {"id": 2, "transactionDate": "2026-02-05T00:00:00", "description": "Netflix", "amount": 55, "type": "EXPENSE", "category": "SUBSCRIPTION"},
        {"id": 3, "transactionDate": "2026-03-05T00:00:00", "description": "Netflix", "amount": 55, "type": "EXPENSE", "category": "SUBSCRIPTION"},
    ]
    result = subscription_detector.detect(transactions)
    assert result["status"] == "SUCCESS"
    assert result["subscriptionCount"] == 1
    assert result["estimatedMonthlySubscriptionCost"] == 55.0


def test_subscription_agent_excludes_recurring_everyday_expenses():
    transactions = [
        {"id": 1, "transactionDate": "2026-05-25T00:00:00", "description": "Fuel", "amount": 84, "type": "EXPENSE", "category": "TRANSPORTATION"},
        {"id": 2, "transactionDate": "2026-06-25T00:00:00", "description": "Fuel", "amount": 85, "type": "EXPENSE", "category": "TRANSPORTATION"},
        {"id": 3, "transactionDate": "2026-07-24T00:00:00", "description": "Fuel", "amount": 84, "type": "EXPENSE", "category": "TRANSPORTATION"},
        {"id": 4, "transactionDate": "2026-05-12T00:00:00", "description": "Coffee", "amount": 7, "type": "EXPENSE", "category": "FOOD"},
        {"id": 5, "transactionDate": "2026-06-12T00:00:00", "description": "Coffee", "amount": 7, "type": "EXPENSE", "category": "FOOD"},
        {"id": 6, "transactionDate": "2026-07-12T00:00:00", "description": "Coffee", "amount": 7, "type": "EXPENSE", "category": "FOOD"},
    ]
    result = subscription_detector.detect(transactions)
    assert result["subscriptionCount"] == 0
    assert result["subscriptions"] == []


def test_transaction_classifier_uses_high_confidence_description_rules():
    from app.classification.transaction_classifier import transaction_classifier

    transactions = [
        {"id": 1, "description": "SPOTIFY", "amount": 55, "type": "EXPENSE"},
        {"id": 2, "description": "COFFEE", "amount": 7, "type": "EXPENSE"},
        {"id": 3, "description": "ONLINE SHOPPING", "amount": 80, "type": "EXPENSE"},
        {"id": 4, "description": "ELECTRICITY BILL", "amount": 120, "type": "EXPENSE"},
        {"id": 5, "description": "TRAIN FARE", "amount": 20, "type": "EXPENSE"},
        {"id": 6, "description": "NETFLIX", "amount": 55, "type": "EXPENSE"},
        {"id": 7, "description": "SUPERMARKET GROCERIES", "amount": 100, "type": "EXPENSE"},
        {"id": 8, "description": "MONTHLY SALARY", "amount": 3000, "type": "INCOME"},
        {"id": 9, "description": "FUEL", "amount": 90, "type": "EXPENSE"},
        {"id": 10, "description": "PHARMACY", "amount": 40, "type": "EXPENSE"},
        {"id": 11, "description": "MOVIE TICKETS", "amount": 30, "type": "EXPENSE"},
    ]

    result = transaction_classifier.classify(transactions)
    categories = {item["description"]: item["mappedCategory"] for item in result["classifications"]}

    assert categories["SPOTIFY"] == "SUBSCRIPTION"
    assert categories["NETFLIX"] == "SUBSCRIPTION"
    assert categories["COFFEE"] == "FOOD"
    assert categories["ONLINE SHOPPING"] == "SHOPPING"
    assert categories["ELECTRICITY BILL"] == "UTILITIES"
    assert categories["TRAIN FARE"] == "TRANSPORTATION"
    assert categories["SUPERMARKET GROCERIES"] == "GROCERIES"
    assert categories["MONTHLY SALARY"] == "SALARY"
    assert categories["FUEL"] == "TRANSPORTATION"
    assert categories["PHARMACY"] == "HEALTHCARE"
    assert categories["MOVIE TICKETS"] == "ENTERTAINMENT"


def test_transaction_classifier_uses_high_confidence_rules_for_known_descriptions():
    from app.classification.transaction_classifier import transaction_classifier

    transactions = [
        {"id": 1, "description": "SPOTIFY", "amount": 55, "type": "EXPENSE"},
        {"id": 2, "description": "COFFEE", "amount": 7, "type": "EXPENSE"},
        {"id": 3, "description": "ONLINE SHOPPING", "amount": 80, "type": "EXPENSE"},
        {"id": 4, "description": "ELECTRICITY BILL", "amount": 120, "type": "EXPENSE"},
        {"id": 5, "description": "TRAIN FARE", "amount": 20, "type": "EXPENSE"},
        {"id": 6, "description": "NETFLIX", "amount": 55, "type": "EXPENSE"},
        {"id": 7, "description": "SUPERMARKET GROCERIES", "amount": 100, "type": "EXPENSE"},
        {"id": 8, "description": "MONTHLY SALARY", "amount": 3000, "type": "INCOME"},
        {"id": 9, "description": "FUEL", "amount": 90, "type": "EXPENSE"},
        {"id": 10, "description": "PHARMACY", "amount": 40, "type": "EXPENSE"},
        {"id": 11, "description": "MOVIE TICKETS", "amount": 30, "type": "EXPENSE"},
    ]

    result = transaction_classifier.classify(transactions)
    categories = {
        item["description"]: item["mappedCategory"]
        for item in result["classifications"]
    }

    assert categories["SPOTIFY"] == "SUBSCRIPTION"
    assert categories["NETFLIX"] == "SUBSCRIPTION"
    assert categories["COFFEE"] == "FOOD"
    assert categories["ONLINE SHOPPING"] == "SHOPPING"
    assert categories["ELECTRICITY BILL"] == "UTILITIES"
    assert categories["TRAIN FARE"] == "TRANSPORTATION"
    assert categories["SUPERMARKET GROCERIES"] == "GROCERIES"
    assert categories["MONTHLY SALARY"] == "SALARY"
    assert categories["FUEL"] == "TRANSPORTATION"
    assert categories["PHARMACY"] == "HEALTHCARE"
    assert categories["MOVIE TICKETS"] == "ENTERTAINMENT"
