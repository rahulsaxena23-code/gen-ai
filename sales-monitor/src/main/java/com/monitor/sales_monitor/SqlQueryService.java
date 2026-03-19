package com.monitor.sales_monitor;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * SqlQueryService
 *
 * Parses natural language requests and builds safe MySQL SELECT queries
 * for the sales_records table — no external AI API key required.
 *
 * Uses keyword/pattern matching to understand intent and construct queries.
 */
@Service
public class SqlQueryService {

    private static final String BASE_TABLE   = "`sales_records`";
    private static final String ALL_COLUMNS  = "`sales_id`, `sales_number`, `action`, `crreated_at`, `updated_at`";
    private static final int    DEFAULT_LIMIT = 100;

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    /**
     * Convert a natural language request into a MySQL SELECT query.
     *
     * @param request Human readable question about sales_records
     * @return A valid SQL SELECT string, or the "sorry" message
     */
    public String generateQuery(String request) {
        if (request == null || request.isBlank()) {
            return sorry();
        }

        String input = request.trim().toLowerCase();

        // --- COUNT queries ---
        if (isCountQuery(input)) {
            return buildCountQuery(input);
        }

        // --- Schema / structure questions ---
        if (isSchemaQuery(input)) {
            return buildSchemaDescription();
        }

        // --- Queries filtered by action ---
        if (hasActionFilter(input)) {
            return buildActionFilterQuery(input);
        }

        // --- Queries filtered by sales_number ---
        if (hasSalesNumberFilter(input)) {
            return buildSalesNumberFilterQuery(input);
        }

        // --- Queries filtered by sales_id ---
        if (hasSalesIdFilter(input)) {
            return buildSalesIdFilterQuery(input);
        }

        // --- Latest / recent / newest records ---
        if (isLatestQuery(input)) {
            return buildLatestQuery(input);
        }

        // --- Oldest / earliest records ---
        if (isOldestQuery(input)) {
            return buildOldestQuery(input);
        }

        // --- Updated recently ---
        if (isUpdatedQuery(input)) {
            return buildUpdatedQuery(input);
        }

        // --- Generic "show all / list all / get all" ---
        if (isShowAllQuery(input)) {
            return buildSelectAll(extractLimit(input));
        }

        // --- Today's records ---
        if (isTodayQuery(input)) {
            return buildTodayQuery();
        }

        // --- Fallback: if the input at least mentions sales ---
        if (input.contains("sales") || input.contains("sale") || input.contains("record")) {
            return buildSelectAll(extractLimit(input));
        }

        return sorry();
    }

    // -----------------------------------------------------------------------
    // Intent detectors
    // -----------------------------------------------------------------------

    private boolean isCountQuery(String input) {
        return input.contains("count") || input.contains("how many") || input.contains("total number")
                || input.contains("number of");
    }

    private boolean isSchemaQuery(String input) {
        return input.contains("schema") || input.contains("columns") || input.contains("structure")
                || input.contains("fields") || input.contains("table definition");
    }

    private boolean hasActionFilter(String input) {
        // Looks for action keywords or the word "action" with a value
        return input.contains("approved") || input.contains("rejected") || input.contains("pending")
                || input.contains("created") || input.contains("cancelled") || input.contains("completed")
                || input.contains("action =") || input.contains("action is") || input.contains("action:");
    }

    private boolean hasSalesNumberFilter(String input) {
        return input.contains("sales number") || input.contains("sales-") || input.contains("sales #")
                || Pattern.compile("sales\\s*\\d+").matcher(input).find();
    }

    private boolean hasSalesIdFilter(String input) {
        return input.contains("sales id") || input.contains("sales_id") || input.contains("id =")
                || input.contains("id is") || Pattern.compile("id\\s*\\d+").matcher(input).find();
    }

    private boolean isLatestQuery(String input) {
        return input.contains("latest") || input.contains("recent") || input.contains("newest")
                || input.contains("last") || input.contains("most recent");
    }

    private boolean isOldestQuery(String input) {
        return input.contains("oldest") || input.contains("earliest") || input.contains("first created");
    }

    private boolean isUpdatedQuery(String input) {
        return input.contains("updated") || input.contains("modified") || input.contains("changed");
    }

    private boolean isShowAllQuery(String input) {
        return input.contains("all") || input.contains("list") || input.contains("show")
                || input.contains("get") || input.contains("fetch") || input.contains("display")
                || input.contains("find") || input.contains("select");
    }

    private boolean isTodayQuery(String input) {
        return input.contains("today") || input.contains("this day");
    }

    // -----------------------------------------------------------------------
    // Query builders
    // -----------------------------------------------------------------------

    private String buildCountQuery(String input) {
        String condition = extractActionCondition(input);
        if (!condition.isEmpty()) {
            return String.format("SELECT COUNT(*) AS total FROM %s WHERE %s;", BASE_TABLE, condition);
        }
        return String.format("SELECT COUNT(*) AS total FROM %s;", BASE_TABLE);
    }

    private String buildActionFilterQuery(String input) {
        String condition = extractActionCondition(input);
        int limit = extractLimit(input);

        String orderBy = isLatestQuery(input) ? " ORDER BY `crreated_at` DESC" : "";
        String limitClause = " LIMIT " + limit;

        if (!condition.isEmpty()) {
            return String.format("SELECT %s FROM %s WHERE %s%s%s;",
                    ALL_COLUMNS, BASE_TABLE, condition, orderBy, limitClause);
        }
        return buildSelectAll(limit);
    }

    private String buildSalesNumberFilterQuery(String input) {
        // Try to extract a sales number like SALES-1001, SALES1001, or just 1001
        var matcher = Pattern.compile("sales[-\\s#]?(\\w+)").matcher(input);
        if (matcher.find()) {
            String salesNum = matcher.group(1).toUpperCase();
            return String.format("SELECT %s FROM %s WHERE `sales_number` LIKE '%%%s%%' LIMIT %d;",
                    ALL_COLUMNS, BASE_TABLE, salesNum, DEFAULT_LIMIT);
        }
        return buildSelectAll(DEFAULT_LIMIT);
    }

    private String buildSalesIdFilterQuery(String input) {
        var matcher = Pattern.compile("(?:id\\s*[=:]?\\s*|sales_id\\s*[=:]?\\s*)(\\d+)").matcher(input);
        if (matcher.find()) {
            String id = matcher.group(1);
            return String.format("SELECT %s FROM %s WHERE `sales_id` = %s;", ALL_COLUMNS, BASE_TABLE, id);
        }
        return buildSelectAll(DEFAULT_LIMIT);
    }

    private String buildLatestQuery(String input) {
        int limit = extractLimit(input);
        String condition = extractActionCondition(input);
        String where = condition.isEmpty() ? "" : " WHERE " + condition;
        return String.format("SELECT %s FROM %s%s ORDER BY `crreated_at` DESC LIMIT %d;",
                ALL_COLUMNS, BASE_TABLE, where, limit);
    }

    private String buildOldestQuery(String input) {
        int limit = extractLimit(input);
        String condition = extractActionCondition(input);
        String where = condition.isEmpty() ? "" : " WHERE " + condition;
        return String.format("SELECT %s FROM %s%s ORDER BY `crreated_at` ASC LIMIT %d;",
                ALL_COLUMNS, BASE_TABLE, where, limit);
    }

    private String buildUpdatedQuery(String input) {
        int limit = extractLimit(input);
        String condition = extractActionCondition(input);
        String where = condition.isEmpty() ? "" : " WHERE " + condition;
        return String.format("SELECT %s FROM %s%s ORDER BY `updated_at` DESC LIMIT %d;",
                ALL_COLUMNS, BASE_TABLE, where, limit);
    }

    private String buildSelectAll(int limit) {
        return String.format("SELECT %s FROM %s LIMIT %d;", ALL_COLUMNS, BASE_TABLE, limit);
    }

    private String buildTodayQuery() {
        return String.format(
                "SELECT %s FROM %s WHERE DATE(`crreated_at`) = CURDATE() ORDER BY `crreated_at` DESC LIMIT %d;",
                ALL_COLUMNS, BASE_TABLE, DEFAULT_LIMIT);
    }

    private String buildSchemaDescription() {
        return """
                -- sales_records table schema:
                SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT
                FROM   INFORMATION_SCHEMA.COLUMNS
                WHERE  TABLE_SCHEMA = DATABASE()
                  AND  TABLE_NAME   = 'sales_records'
                ORDER BY ORDINAL_POSITION;
                """.trim();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Extract a WHERE clause fragment for `action` column based on keywords.
     */
    private String extractActionCondition(String input) {
        if (input.contains("approved"))  return "`action` = 'approved'";
        if (input.contains("rejected"))  return "`action` = 'rejected'";
        if (input.contains("pending"))   return "`action` = 'pending'";
        if (input.contains("cancelled") || input.contains("canceled")) return "`action` = 'cancelled'";
        if (input.contains("completed")) return "`action` = 'completed'";
        if (input.contains("created"))   return "`action` = 'created'";

        // Regex: action is/= 'something' or action: something
        var m = Pattern.compile("action\\s*(?:is|=|:)\\s*['\"]?(\\w+)['\"]?").matcher(input);
        if (m.find()) {
            return String.format("`action` = '%s'", m.group(1));
        }

        return "";
    }

    /**
     * Parse a numeric LIMIT from phrases like "top 10", "last 5", "20 records".
     */
    private int extractLimit(String input) {
        var m = Pattern.compile(
                "(?:top|last|first|latest|recent|limit|show|get|fetch)?\\s*(\\d+)\\s*(?:records?|rows?|results?|sales?)?")
                .matcher(input);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n > 0 && n <= 10_000) return n;
        }
        return DEFAULT_LIMIT;
    }

    private String sorry() {
        return "Sorry, I didn't understand your request";
    }
}
