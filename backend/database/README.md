<!-- FinSight File Notes: Explains the database design and the optional monthly-budget repair script. -->
# FinSight database notes

FinSight stores each user's budget separately for each `YYYY-MM` month in the `monthly_budgets` table. This allows January, February, March, and later months to use different budget values.

The normal application startup creates or updates the current Hibernate schema and then performs a small best-effort repair for legacy database changes. For local development, `application-dev.yml` uses `ddl-auto: update`; the Railway production profile also uses `ddl-auto: update` so a fresh MySQL database can be initialised without a separate schema-import step. For a larger production system, replace this with a versioned migration tool such as Flyway or Liquibase.

The `monthly_budgets_reconstruct.sql` file is provided as a manual repair option for an existing `finsight` database. It creates the monthly budget table and its per-profile/per-month unique key.
