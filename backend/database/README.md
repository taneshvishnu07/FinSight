# FinSight monthly budget database

The current application stores each user's budget separately for each `YYYY-MM` month in `monthly_budgets`.

For a local MySQL database, the application attempts to create/repair the table automatically during startup. The explicit SQL migration is also provided in `monthly_budgets_reconstruct.sql`.

To reconstruct the table manually:

```sql
USE finsight;
SOURCE backend/database/monthly_budgets_reconstruct.sql;
SELECT id, financial_profile_id, budget_month, amount FROM monthly_budgets ORDER BY financial_profile_id, budget_month;
```

A user can store different values such as `2026-04 = 900`, `2026-05 = 1100`, and `2026-06 = 1000`. The recommendation and alert agents receive these exact month-specific budgets and compare them with the corresponding transaction month.
