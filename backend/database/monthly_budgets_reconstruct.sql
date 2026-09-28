-- FinSight File Notes: Creates or repairs the database structures required by FinSight.
CREATE TABLE IF NOT EXISTS monthly_budgets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    budget_month VARCHAR(7) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    financial_profile_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_budgets_profile_month (financial_profile_id, budget_month),
    KEY idx_monthly_budgets_profile_id (financial_profile_id),
    CONSTRAINT fk_monthly_budgets_profile
        FOREIGN KEY (financial_profile_id) REFERENCES financial_profiles(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;
