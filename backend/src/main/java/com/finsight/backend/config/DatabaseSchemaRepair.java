/**
 * FinSight File Notes: Checks and repairs important database structures when the application starts.
 */
package com.finsight.backend.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Best-effort repair for legacy schema changes introduced during development.
 * Hibernate remains responsible for normal create/update operations.
 */
@Component
@RequiredArgsConstructor
public class DatabaseSchemaRepair implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        repairFinancialAnalysisProfileConstraint();
        repairLegacyFinancialProfileBudgetColumn();
        repairMonthlyBudgetsTable();
    }

    private void repairFinancialAnalysisProfileConstraint() {
        try {
            List<String> uniqueIndexes = jdbcTemplate.queryForList(
                    "SELECT DISTINCT INDEX_NAME "
                            + "FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'financial_analysis' "
                            + "AND NON_UNIQUE = 0 "
                            + "AND INDEX_NAME <> 'PRIMARY' "
                            + "AND COLUMN_NAME = 'financial_profile_id'",
                    String.class);

            for (String indexName : uniqueIndexes) {
                if (indexName == null || indexName.isBlank()) continue;
                String safeIndexName = indexName.replace("`", "``");
                jdbcTemplate.execute("ALTER TABLE financial_analysis DROP INDEX `" + safeIndexName + "`");
            }

            Integer normalIndexCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'financial_analysis' "
                            + "AND INDEX_NAME = 'idx_financial_analysis_profile_id'",
                    Integer.class);

            if (normalIndexCount == null || normalIndexCount == 0) {
                jdbcTemplate.execute(
                        "CREATE INDEX idx_financial_analysis_profile_id ON financial_analysis (financial_profile_id)");
            }
        } catch (DataAccessException ignored) {
            // Best effort; do not prevent the application from starting.
        }
    }

    private void repairLegacyFinancialProfileBudgetColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'financial_profiles' "
                            + "AND COLUMN_NAME = 'monthly_budget'",
                    Integer.class);
            if (count != null && count > 0) {
                jdbcTemplate.execute(
                        "ALTER TABLE financial_profiles MODIFY COLUMN monthly_budget DECIMAL(15,2) NOT NULL DEFAULT 0");
            }
        } catch (DataAccessException ignored) {
            // Legacy column is intentionally kept harmless for existing databases.
        }
    }

    private void repairMonthlyBudgetsTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS monthly_budgets ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT, "
                            + "created_at DATETIME(6) NULL, "
                            + "updated_at DATETIME(6) NULL, "
                            + "budget_month VARCHAR(7) NOT NULL, "
                            + "amount DECIMAL(15,2) NOT NULL, "
                            + "financial_profile_id BIGINT NOT NULL, "
                            + "PRIMARY KEY (id)"
                            + ") ENGINE=InnoDB");

            ensureColumn("monthly_budgets", "budget_month", "VARCHAR(7) NOT NULL");
            ensureColumn("monthly_budgets", "amount", "DECIMAL(15,2) NOT NULL DEFAULT 0");
            ensureColumn("monthly_budgets", "financial_profile_id", "BIGINT NOT NULL");
            ensureColumn("monthly_budgets", "created_at", "DATETIME(6) NULL");
            ensureColumn("monthly_budgets", "updated_at", "DATETIME(6) NULL");

            // Remove duplicate rows before creating the per-profile/per-month key.
            Integer uniqueIndexCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'monthly_budgets' "
                            + "AND INDEX_NAME = 'uk_monthly_budgets_profile_month'",
                    Integer.class);

            if (uniqueIndexCount == null || uniqueIndexCount == 0) {
                jdbcTemplate.execute(
                        "DELETE mb1 FROM monthly_budgets mb1 "
                                + "JOIN monthly_budgets mb2 "
                                + "ON mb1.financial_profile_id = mb2.financial_profile_id "
                                + "AND mb1.budget_month = mb2.budget_month "
                                + "AND mb1.id < mb2.id");
                jdbcTemplate.execute(
                        "CREATE UNIQUE INDEX uk_monthly_budgets_profile_month "
                                + "ON monthly_budgets (financial_profile_id, budget_month)");
            }

            Integer profileIndexCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'monthly_budgets' "
                            + "AND INDEX_NAME = 'idx_monthly_budgets_profile_id'",
                    Integer.class);
            if (profileIndexCount == null || profileIndexCount == 0) {
                jdbcTemplate.execute(
                        "CREATE INDEX idx_monthly_budgets_profile_id ON monthly_budgets (financial_profile_id)");
            }

            Integer foreignKeyCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'monthly_budgets' "
                            + "AND COLUMN_NAME = 'financial_profile_id' "
                            + "AND REFERENCED_TABLE_NAME = 'financial_profiles' "
                            + "AND REFERENCED_COLUMN_NAME = 'id'",
                    Integer.class);
            if (foreignKeyCount == null || foreignKeyCount == 0) {
                jdbcTemplate.execute(
                        "ALTER TABLE monthly_budgets ADD CONSTRAINT fk_monthly_budgets_profile "
                                + "FOREIGN KEY (financial_profile_id) REFERENCES financial_profiles(id) "
                                + "ON DELETE CASCADE");
            }
        } catch (DataAccessException ignored) {
            // Best effort; Hibernate/dev schema management remains the fallback.
        }
    }

    private void ensureColumn(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                table,
                column);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
