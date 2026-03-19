package com.monitor.sales_monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SalesRecordsTool
 *
 * Exposes MCP tools via Spring AI @Tool annotations.
 * These tools are automatically discovered and registered with the MCP server.
 *
 * Tools available to GitHub Copilot:
 *  1. generateSqlQuery    — Convert natural language → SQL SELECT
 *  2. executeSalesQuery   — Run a SQL query and return results
 *  3. getTableSchema      — Describe the sales_records table columns
 *  4. findByAction        — Shortcut: find records by action value
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesRecordsTool {

    private final SqlQueryService sqlQueryService;
    private final JdbcTemplate    jdbcTemplate;

    // ------------------------------------------------------------------
    // Tool 1 – Generate SQL from natural language
    // ------------------------------------------------------------------

    @Tool(
        name        = "generateSqlQuery",
        description = """
            Converts a natural language request into a MySQL SELECT query
            for the sales_records table (columns: sales_id, sales_number, action,
            crreated_at, updated_at). Returns the SQL string or
            'Sorry, I didn't understand your request' if the intent is unclear.
            """
    )
    public String generateSqlQuery(
            @ToolParam(description = "Natural language description of the sales data you want, e.g. 'show all approved sales' or 'count pending sales orders'")
            String request) {

        log.info("[MCP] generateSqlQuery called with: {}", request);
        String sql = sqlQueryService.generateQuery(request);
        log.info("[MCP] Generated SQL: {}", sql);
        return sql;
    }

    // ------------------------------------------------------------------
    // Tool 2 – Execute a SQL query and return formatted results
    // ------------------------------------------------------------------

    @Tool(
        name        = "executeSalesQuery",
        description = """
            Executes a given MySQL SELECT query against the sales_records table
            and returns the results as a formatted string.
            Only SELECT statements are permitted for safety.
            Combine with generateSqlQuery to first get the query, then run it.
            """
    )
    public String executeSalesQuery(
            @ToolParam(description = "A valid MySQL SELECT statement targeting the sales_records table")
            String sql) {

        log.info("[MCP] executeSalesQuery called with: {}", sql);

        // Safety guard — only allow SELECT
        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT")) {
            return "Error: Only SELECT queries are permitted.";
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            if (rows.isEmpty()) {
                return "No records found.";
            }

            // Format as a readable table-like string
            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(rows.size()).append(" record(s):\n\n");

            for (int i = 0; i < rows.size(); i++) {
                sb.append("--- Record ").append(i + 1).append(" ---\n");
                rows.get(i).forEach((col, val) ->
                        sb.append("  ").append(col).append(": ").append(val).append("\n")
                );
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("[MCP] Query execution error: {}", e.getMessage());
            return "Query execution failed: " + e.getMessage();
        }
    }

    // ------------------------------------------------------------------
    // Tool 3 – Describe the table schema
    // ------------------------------------------------------------------

    @Tool(
        name        = "getTableSchema",
        description = "Returns the column definitions and DDL of the sales_records table."
    )
    public String getTableSchema() {
        log.info("[MCP] getTableSchema called");

        return """
                Table: sales_records
                ┌─────────────┬─────────────┬────────────────────────────────────────┐
                │ Column      │ Type        │ Description                            │
                ├─────────────┼─────────────┼────────────────────────────────────────┤
                │ sales_id    │ INT         │ Primary identifier for the sales record │
                │ sales_number│ VARCHAR(45) │ Sales order number (e.g. SALES-1001)   │
                │ action      │ VARCHAR(45) │ Action: created/approved/rejected/etc  │
                │ crreated_at │ DATETIME    │ Timestamp when record was created      │
                │ updated_at  │ DATETIME    │ Timestamp when record was last updated │
                └─────────────┴─────────────┴────────────────────────────────────────┘
                
                DDL:
                CREATE TABLE `sales_records` (
                  `sales_id`       INT          NOT NULL DEFAULT '1',
                  `sales_number`   VARCHAR(45)  DEFAULT NULL,
                  `action`         VARCHAR(45)  DEFAULT NULL,
                  `crreated_at`    DATETIME     DEFAULT NULL,
                  `updated_at`     DATETIME     DEFAULT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
    }

    // ------------------------------------------------------------------
    // Tool 4 – Convenience: find records by action
    // ------------------------------------------------------------------

    @Tool(
        name        = "findByAction",
        description = """
            Shortcut tool: fetches sales_records rows matching a specific action value
            (e.g. 'approved', 'rejected', 'pending', 'created', 'cancelled').
            Returns results directly without needing to write SQL.
            """
    )
    public String findByAction(
            @ToolParam(description = "The action value to filter by, e.g. 'approved', 'pending', 'rejected'")
            String action,

            @ToolParam(description = "Maximum number of records to return (default 100, max 500)")
            Integer limit) {

        log.info("[MCP] findByAction called: action={}, limit={}", action, limit);

        if (action == null || action.isBlank()) {
            return "Please provide a valid action value (e.g. approved, pending, rejected).";
        }

        int safeLimit = (limit == null || limit <= 0) ? 100 : Math.min(limit, 500);

        String sql = String.format(
                "SELECT `sales_id`, `sales_number`, `action`, `crreated_at`, `updated_at` " +
                "FROM `sales_records` WHERE `action` = ? ORDER BY `crreated_at` DESC LIMIT %d",
                safeLimit);

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, action.toLowerCase());

            if (rows.isEmpty()) {
                return String.format("No sales_records found with action = '%s'.", action);
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Found %d record(s) with action = '%s':\n\n", rows.size(), action));

            rows.forEach(row -> {
                sb.append(String.format("  Sales ID: %-5s | Sales#: %-12s | Action: %-12s | Created: %s | Updated: %s\n",
                        row.get("sales_id"),
                        row.get("sales_number"),
                        row.get("action"),
                        row.get("crreated_at"),
                        row.get("updated_at")));
            });

            return sb.toString();

        } catch (Exception e) {
            log.error("[MCP] findByAction error: {}", e.getMessage());
            return "Query failed: " + e.getMessage();
        }
    }
}
