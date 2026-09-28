# FinSight deployment: deterministic FastAPI runtime for Railway.
FROM python:3.11-slim
WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    TZ=Asia/Kuala_Lumpur

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app app
COPY models models

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
