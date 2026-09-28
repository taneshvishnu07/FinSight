# FinSight AI Service

<!-- FinSight File Notes: Explains how the Python AI service works and how to start it locally. -->

This folder contains the Python FastAPI service used by the FinSight Spring Boot backend. The service exposes separate endpoints for transaction classification, spending behaviour analysis, subscriptions, unusual spending, forecasting, recommendations, and alerts.

## Install

```powershell
pip install -r requirements.txt
```

## Start

```powershell
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

For Railway, the start command is:

```text
uvicorn app.main:app --host 0.0.0.0 --port $PORT
```

## Health check

Open:

```text
http://127.0.0.1:8000/health
```

The trained transaction model in `models/transaction_classifier.joblib` is loaded by the classification agent at runtime.
