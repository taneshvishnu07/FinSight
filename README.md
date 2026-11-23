# FinSight: A Multi-Agent AI System for Financial Behaviour Analysis & Hidden Cost Detection

FinSight is a web-based personal financial analysis system developed as a final-year software engineering project. It helps users understand their spending behaviour by combining transaction classification, behavioural analysis, subscription detection, unusual-spending detection, expense forecasting, personalised recommendations, and financial alerts in one application.

Rather than focusing only on recording transactions, FinSight analyses patterns across a user's transaction history and financial profile to highlight spending behaviour that may otherwise be difficult to notice.

## Main Features

- Landing page, registration, and login
- Financial profile with Student, Employee, and Others occupation categories
- Separate monthly budget settings for January to December
- CSV transaction upload with a minimum three-month transaction history
- Transaction classification using rule-based checks and a trained TF-IDF/Logistic Regression model
- Spending behaviour analysis
- Recurring subscription detection
- Unusual spending detection with essential-payment exclusions
- Short-term expense forecasting
- Personalised recommendations with recommendation-basis labels
- Financial alerts and persistent alert notifications
- Dashboard with spending summaries and month-to-month comparisons
- Financial Report with charts, forecasts, key insights, recommendations, and alerts
- User Guide for first-time users
- Profile and Settings with financial-profile and password updates

## Technology Stack

### Backend

- Java 21
- Spring Boot 3.5.4
- Spring Data JPA / Hibernate
- Spring Security with JWT
- MySQL
- Maven
- REST APIs

### AI Service

- Python 3.11
- FastAPI
- Uvicorn
- pandas
- NumPy
- scikit-learn
- joblib
- Trained transaction-classification model stored in `backend/ai-service/models/`

### Frontend

- HTML5
- CSS3
- JavaScript
- Chart.js
- Bootstrap Icons

## System Architecture

FinSight uses a separated backend and AI-service architecture. The Spring Boot backend manages authentication, user profiles, transaction storage, analysis workflows, API endpoints, and the web interface. The Python FastAPI service provides the specialised AI processing components.

```text
User
  |
  v
FinSight Web Interface
  |
  v
Spring Boot Backend
  |----------------------|
  |                      |
  v                      v
MySQL Database       Python AI Service
                     |
                     +-- Transaction Classification
                     +-- Spending Behaviour Analysis
                     +-- Subscription Detection
                     +-- Unusual Spending Detection
                     +-- Expense Forecasting
                     +-- Recommendation Generation
                     +-- Alert Generation
```

## Multi-Agent AI Components

### Transaction Classification Agent

Classifies transaction descriptions into financial categories. High-confidence description rules are applied for known transaction patterns, while the trained classification model handles general cases.

### Spending Behaviour Analysis Agent

Examines spending patterns, category totals, month-to-month changes, and behaviour trends to identify meaningful financial patterns.

### Subscription Detection Agent

Detects recurring payments and identifies subscriptions that may require review based on their recurrence and usage information.

### Financial Forecasting Agent

Uses historical expense information to estimate upcoming expenses and provide a short-term view of expected spending.

### Unusual Spending Detection Agent

Identifies unusual discretionary transactions and spending patterns while excluding categories treated as essential payments by the project rules.

### Recommendation Generation Agent

Generates recommendations using transaction behaviour, financial-profile information, monthly budgets, savings patterns, subscriptions, cash flow, and other analysis results. Each recommendation includes a priority level and an indication of the main basis used to generate it.

### Alert Generation Agent

Produces actionable financial alerts based on detected spending conditions and user-specific financial information.

## Project Structure

```text
FinSight/
├── .gitignore
├── README.md
├── backend/
│   ├── .env.example
│   ├── .dockerignore
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .mvn/
│   │   └── wrapper/
│   ├── ai-service/
│   │   ├── Dockerfile
│   │   ├── .dockerignore
│   │   ├── .python-version
│   │   ├── app/
│   │   ├── models/
│   │   ├── tests/
│   │   └── requirements.txt
│   ├── database/
│   │   ├── README.md
│   │   └── monthly_budgets_reconstruct.sql
│   └── src/main/
│       ├── java/com/finsight/backend/
│       └── resources/static/
├── student_transactions_3_months.csv
└── employee_transactions_3_months.csv
```

Generated files such as Maven `target/`, Python `__pycache__/`, IDE metadata, test caches, and local environment files are excluded from the project repository.

## Prerequisites

Install the following before running FinSight locally:

- JDK 21
- Git
- MySQL 8.x
- Python 3.11
- A Python virtual environment or Conda environment

Maven does not need to be installed separately because the project includes the Maven Wrapper.

## Local Database Setup

Create a MySQL database named `finsight`:

```sql
CREATE DATABASE finsight;
```

The development configuration uses Hibernate schema updates to create or update the required tables when the application starts.

The SQL file `backend/database/monthly_budgets_reconstruct.sql` is provided only for repairing an older database structure when required.

## Local AI Service Setup

Open a terminal in `backend/ai-service` and install the Python dependencies:

```powershell
pip install -r requirements.txt
```

Start the AI service:

```powershell
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

The AI service health endpoint is:

```text
http://127.0.0.1:8000/health
```

## Local Spring Boot Setup

Configure the required environment variables before starting the backend:

```text
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password
JWT_SECRET=your_long_random_secret
JWT_EXPIRATION=86400000
FINSIGHT_AI_SERVICE_URL=http://127.0.0.1:8000
SPRING_PROFILES_ACTIVE=dev
```

Then open another terminal:

```powershell
cd backend
.\mvnw.cmd clean spring-boot:run
```

Open the application at:

```text
http://localhost:8080
```

## Recommended Test Flow

Use one of the included three-month CSV files to test the complete workflow:

```text
Register
  -> Login
  -> Financial Profile
  -> Set monthly budgets
  -> Upload 3-month CSV
  -> Start Analysis
  -> View Analysis
  -> Dashboard
  -> Financial Report
  -> Transactions
  -> Subscription
  -> Unusual Spending
  -> Recommendation
  -> Alerts
```

The included student and employee datasets cover April, May, and June 2026 and contain different spending patterns for testing the analysis features.

## Data Input Scope

The current version accepts transaction data through CSV upload and requires at least three months of transaction history for analysis.

The included `transaction_classifier.joblib` model is required by the transaction-classification component and is therefore kept in the project.

## Author

**Tanesh Vishnu**

## Disclaimer

FinSight is an academic software project developed for educational and demonstration purposes. Its analysis, forecasts, recommendations, and alerts are generated from the available transaction data and user-provided financial profile information and should not be treated as professional financial advice.
