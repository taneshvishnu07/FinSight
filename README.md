<!-- FinSight File Notes: Main project documentation covering setup, use, GitHub preparation, and Railway deployment. -->

# FinSight: A Multi-Agent AI System for Financial Behaviour Analysis & Hidden Cost Detection

FinSight is a web-based personal financial analysis system developed as a final-year software engineering project. It allows a user to create an account, complete a financial profile, upload transaction data, run a multi-agent analysis, and review financial insights through a set of focused interfaces.

The system is designed to do more than record transactions. It uses specialised AI components to classify transactions, analyse spending behaviour, detect recurring subscriptions, identify unusual discretionary spending, forecast future expenses, generate personalised recommendations, and generate financial alerts. User profile information and month-specific budgets are passed into the recommendation and alert logic so the results can be tailored to the user's circumstances.

## Main features

- Landing page, registration, and login
- Financial profile with Student, Employee, and Others occupation categories
- Separate January-December monthly budget fields
- CSV transaction upload with three-month minimum history validation
- Transaction classification with high-confidence description rules and a trained TF-IDF/Logistic Regression model
- Spending behaviour analysis
- Subscription detection
- Unusual spending detection with essential-payment exclusions
- Short-term expense forecasting
- Personalised recommendations with recommendation-basis labels
- Financial alerts and persistent alert popup
- Dashboard with spending mix and month-to-month comparison analysis
- Financial Report with charts, forecasts, key insights, recommendations, and alerts
- User Guide for first-time users
- Profile and Settings with profile and password updates

## Technology stack

### Backend

- Java 21
- Spring Boot 3.5.4
- Spring Data JPA / Hibernate
- Spring Security with JWT
- MySQL
- Maven
- REST APIs

### AI service

- Python 3.11 recommended
- FastAPI
- Uvicorn
- pandas
- NumPy
- scikit-learn 1.9.0 (matched to the included runtime classification model)
- joblib
- Trained transaction classification model stored in `backend/ai-service/models/`

### Frontend

- HTML
- CSS
- JavaScript
- Chart.js
- Bootstrap Icons

## Project structure

```text
FinSight/
├── .gitignore
├── README.md
├── backend/
│   ├── .env.example
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── railway.toml
│   ├── ai-service/
│   │   ├── app/
│   │   ├── models/
│   │   ├── tests/
│   │   ├── requirements.txt
│   │   └── railway.toml
│   ├── database/
│   │   ├── README.md
│   │   └── monthly_budgets_reconstruct.sql
│   └── src/main/
│       ├── java/com/finsight/backend/
│       └── resources/static/
└── test-data/
    ├── student_transactions_3_months.csv
    └── employee_transactions_3_months.csv
```

Generated folders such as Maven `target/`, Python `__pycache__/`, IDE settings, and local environment files are intentionally not included in the repository.

## Important notes about the current scope

The provided `transaction_classifier.joblib` file is required at runtime by the AI classification component and is kept in Git. The optional external training dataset and unused data-loader utility are not included in the GitHub-ready copy because they are not required to run the deployed service.

## Prerequisites for local development

Install the following before running FinSight locally:

1. JDK 21 or a compatible newer JDK that can compile Java 21 source.
2. Git.
3. MySQL 8.x.
4. Python 3.11.
5. A Python virtual environment or Conda environment.

Maven does not need to be installed separately because the repository includes the Maven Wrapper.

## Local database setup

Create a MySQL database named `finsight` if it does not already exist:

```sql
CREATE DATABASE finsight;
```

The local development profile uses Hibernate `ddl-auto: update`, so the application creates or updates its tables when it starts. The monthly-budget repair SQL is available at `backend/database/monthly_budgets_reconstruct.sql` if an older database requires manual repair.

## Local AI service setup

Open a terminal in `backend/ai-service` and create/activate your Python environment. Then install the dependencies:

```powershell
pip install -r requirements.txt
```

Start the AI service:

```powershell
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

A successful service should respond to:

```text
http://127.0.0.1:8000/health
```

with a JSON response showing that the service is up.

## Local Spring Boot setup

Set these environment variables before starting the backend:

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

Open FinSight at:

```text
http://localhost:8080
```

## Recommended test flow

Use the included student CSV to test the complete workflow:

```text
Register
  -> Login
  -> Financial Profile
  -> Set monthly budgets
  -> Upload the 3-month CSV
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

The included student data contains transactions across April, May, and June 2026 and includes non-essential high-value examples such as PC-game/gaming-related spending for testing unusual-spending behaviour.

## GitHub upload guide

1. Create a new GitHub repository. Do not upload passwords, `.env` files, database dumps containing real personal data, or private keys.
2. Extract the GitHub-ready FinSight folder.
3. Open PowerShell in that folder.
4. Run:

```powershell
git init
git add .
git status
git commit -m "Initial FinSight project"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
git push -u origin main
```

Replace `YOUR_USERNAME` and `YOUR_REPOSITORY` with your own GitHub details.

## Railway deployment

The recommended Railway architecture is three services inside one Railway project:

```text
Railway Project
├── MySQL
├── backend       Spring Boot + FinSight frontend
└── ai            FastAPI AI service
```

The frontend is already inside the Spring Boot application under `backend/src/main/resources/static`, so a separate frontend service is not required.

### Service 1: MySQL

Create a MySQL database service from the Railway project. Railway exposes connection variables such as `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, and `MYSQLDATABASE`. The backend can reference those variables instead of hard-coding database credentials.

### Service 2: AI

Create a service from the same GitHub repository and set its Root Directory to:

```text
/backend/ai-service
```

Use this Start Command if Railway does not automatically detect it:

```text
uvicorn app.main:app --host 0.0.0.0 --port $PORT
```

The AI service exposes `/health`. It does not need a public domain for normal operation because the backend can access it through Railway private networking.

### Service 3: backend

Create another service from the same GitHub repository and set its Root Directory to:

```text
/backend
```

Use this Build Command if needed:

```text
./mvnw -DskipTests clean package
```

Use this Start Command if needed:

```text
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

Set these Railway variables on the backend service:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?createDatabaseIfNotExist=true&serverTimezone=UTC
DATABASE_USERNAME=${{MySQL.MYSQLUSER}}
DATABASE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
JWT_SECRET=your_generated_random_secret
JWT_EXPIRATION=86400000
FINSIGHT_AI_SERVICE_URL=http://${{ai.RAILWAY_PRIVATE_DOMAIN}}:${{ai.PORT}}
```

The backend production profile uses the Railway-provided `$PORT` for the Spring Boot server and a month-specific budget table for financial budget data.

### Generate the public FinSight URL

After the backend deployment succeeds, open the backend service's Settings, go to Networking, and generate a public domain. This becomes the URL users open to access FinSight.

Do not expose the AI service publicly unless you have a specific reason to do so. Railway private networking allows services in the same project environment to communicate using an internal address such as `http://ai.railway.internal:PORT`.

### Railway config files included in this repository

`backend/railway.toml` contains the Spring Boot build/start configuration and `backend/ai-service/railway.toml` contains the FastAPI start configuration. If you use Railway's Root Directory setting, configure the service's config file with the absolute repository path `/backend/railway.toml` or `/backend/ai-service/railway.toml` when Railway asks for it.

Railway's current documentation describes `railway.toml`/`railway.json` as configuration-as-code and notes that this approach is being replaced by Infrastructure as Code; the service settings shown above remain the simplest manual deployment method for a beginner.

## Security checklist before making the repository public

- Never commit a real `.env` file.
- Never commit a real database password.
- Never commit API keys or private keys.
- Use a strong random `JWT_SECRET` in Railway Variables.
- Use `SPRING_PROFILES_ACTIVE=prod` on Railway.
- Keep the AI service private unless public access is required.
- Do not upload real personal financial records as test data.

## Troubleshooting

### Port 8080 already in use

On Windows PowerShell:

```powershell
netstat -ano | findstr :8080
taskkill /PID YOUR_PID /F
```

Then start the backend again.

### AI service connection error

Make sure the Python service is running on port 8000 locally and that:

```text
FINSIGHT_AI_SERVICE_URL=http://127.0.0.1:8000
```

is correct for local development.

On Railway, use the backend variable:

```text
FINSIGHT_AI_SERVICE_URL=http://${{ai.RAILWAY_PRIVATE_DOMAIN}}:${{ai.PORT}}
```

### Database connection error

Check the MySQL service is running and that the backend uses the Railway reference variables rather than `localhost`.

## Project status

This repository is the cleaned GitHub/deployment copy of FinSight. It contains the working application source, trained runtime model, tests, sample transaction files, database documentation, and Railway deployment configuration while excluding generated build files and local development caches.
