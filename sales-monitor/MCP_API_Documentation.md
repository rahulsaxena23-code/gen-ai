# Sales Monitor MCP Server - API Documentation

## Overview

The Sales Monitor MCP Server provides a Model Context Protocol (MCP) interface for querying sales records data. It exposes four main tools that allow AI assistants like GitHub Copilot to interact with a MySQL database containing sales information.

## Server Configuration

- **Framework**: Spring Boot with Spring AI
- **Transport**: Server-Sent Events (SSE)
- **Database**: MySQL
- **Table**: `sales_records`

## Available Tools

### 1. generateSqlQuery

Converts natural language requests into MySQL SELECT queries for the sales_records table.

#### Request
```json
{
  "name": "generateSqlQuery",
  "arguments": {
    "request": "show all approved sales from today"
  }
}
```

#### Response
```json
{
  "content": [
    {
      "type": "text",
      "text": "SELECT `sales_id`, `sales_number`, `action`, `crreated_at`, `updated_at` FROM `sales_records` WHERE `action` = 'approved' AND DATE(`crreated_at`) = CURDATE() ORDER BY `crreated_at` DESC LIMIT 100;"
    }
  ]
}
```

#### Supported Natural Language Patterns
- **Count queries**: "count approved sales", "how many pending orders"
- **Action filters**: "show approved sales", "find rejected orders"
- **Sales number**: "find SALES-1001", "sales number 1234"
- **Sales ID**: "sales id 567", "id = 123"
- **Time-based**: "latest sales", "oldest records", "updated recently"
- **Limiting**: "top 10 sales", "last 5 records"

---

### 2. executeSalesQuery

Executes a MySQL SELECT query against the sales_records table and returns formatted results.

#### Request
```json
{
  "name": "executeSalesQuery",
  "arguments": {
    "sql": "SELECT * FROM sales_records WHERE action = 'approved' LIMIT 5"
  }
}
```

#### Response
```json
{
  "content": [
    {
      "type": "text",
      "text": "Found 2 record(s):\n\n--- Record 1 ---\n  sales_id: 1001\n  sales_number: SALES-2024-001\n  action: approved\n  crreated_at: 2024-03-19 10:30:00\n  updated_at: 2024-03-19 14:15:00\n--- Record 2 ---\n  sales_id: 1002\n  sales_number: SALES-2024-002\n  action: approved\n  crreated_at: 2024-03-19 09:45:00\n  updated_at: 2024-03-19 13:20:00\n"
    }
  ]
}
```

#### Safety Features
- **Only SELECT statements allowed** - INSERT, UPDATE, DELETE are blocked
- **Error handling** - Returns descriptive error messages for invalid queries
- **Result formatting** - Automatically formats results in a readable table-like structure

---

### 3. getTableSchema

Returns the column definitions and DDL of the sales_records table.

#### Request
```json
{
  "name": "getTableSchema",
  "arguments": {}
}
```

#### Response
```json
{
  "content": [
    {
      "type": "text",
      "text": "Table: sales_records\n┌─────────────┬─────────────┬────────────────────────────────────────┐\n│ Column      │ Type        │ Description                            │\n├─────────────┼─────────────┼────────────────────────────────────────┤\n│ sales_id    │ INT         │ Primary identifier for the sales record │\n│ sales_number│ VARCHAR(45) │ Sales order number (e.g. SALES-1001)   │\n│ action      │ VARCHAR(45) │ Action: created/approved/rejected/etc  │\n│ crreated_at │ DATETIME    │ Timestamp when record was created      │\n│ updated_at  │ DATETIME    │ Timestamp when record was last updated │\n└─────────────┴─────────────┴────────────────────────────────────────┘\n\nDDL:\nCREATE TABLE `sales_records` (\n  `sales_id`       INT          NOT NULL DEFAULT '1',\n  `sales_number`   VARCHAR(45)  DEFAULT NULL,\n  `action`         VARCHAR(45)  DEFAULT NULL,\n  `crreated_at`    DATETIME     DEFAULT NULL,\n  `updated_at`     DATETIME     DEFAULT NULL\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
    }
  ]
}
```

---

### 4. findByAction

Shortcut tool to fetch sales records by a specific action value without writing SQL.

#### Request
```json
{
  "name": "findByAction",
  "arguments": {
    "action": "approved",
    "limit": 10
  }
}
```

#### Response
```json
{
  "content": [
    {
      "type": "text",
      "text": "Found 3 record(s) with action = 'approved':\n\n  Sales ID: 1001   | Sales#: SALES-2024-001 | Action: approved    | Created: 2024-03-19 10:30:00 | Updated: 2024-03-19 14:15:00\n  Sales ID: 1002   | Sales#: SALES-2024-002 | Action: approved    | Created: 2024-03-19 09:45:00 | Updated: 2024-03-19 13:20:00\n  Sales ID: 1003   | Sales#: SALES-2024-003 | Action: approved    | Created: 2024-03-19 08:15:00 | Updated: 2024-03-19 12:30:00\n"
    }
  ]
}
```

#### Parameters
- **action** (required): The action value to filter by (e.g., 'approved', 'pending', 'rejected', 'created', 'cancelled')
- **limit** (optional): Maximum records to return (default: 100, max: 500)

## Common Usage Patterns

### Pattern 1: Natural Language → SQL → Execute
```bash
# Step 1: Generate SQL from natural language
Request: "How many sales were approved today?"
Response: "SELECT COUNT(*) AS total FROM `sales_records` WHERE `action` = 'approved' AND DATE(`crreated_at`) = CURDATE();"

# Step 2: Execute the generated SQL
Request: Execute the above SQL
Response: "Found 1 record(s):\n\n--- Record 1 ---\n  total: 25\n"
```

### Pattern 2: Direct Action Lookup
```bash
Request: "Show me the latest 5 approved sales"
Response: Uses findByAction with action="approved", limit=5
```

### Pattern 3: Schema Exploration
```bash
Request: "What columns are available in the sales table?"
Response: Uses getTableSchema to show table structure
```

## Error Handling

### Common Error Responses

#### Invalid SQL (non-SELECT)
```json
{
  "content": [
    {
      "type": "text",
      "text": "Error: Only SELECT queries are permitted."
    }
  ]
}
```

#### Unrecognized Natural Language
```json
{
  "content": [
    {
      "type": "text",
      "text": "Sorry, I didn't understand your request"
    }
  ]
}
```

#### Database Query Errors
```json
{
  "content": [
    {
      "type": "text",
      "text": "Query execution failed: Table 'database.sales_records' doesn't exist"
    }
  ]
}
```

#### No Results Found
```json
{
  "content": [
    {
      "type": "text",
      "text": "No records found."
    }
  ]
}
```

## Integration Examples

### GitHub Copilot Integration
When integrated with GitHub Copilot, the tools appear as available functions that can be called to answer questions about sales data:

```
User: "How many sales orders were rejected last week?"
Copilot: [Calls generateSqlQuery] → [Calls executeSalesQuery] → "Last week there were 12 rejected sales orders."
```

### MCP Client Integration
```javascript
// Example MCP client usage
const response = await mcpClient.callTool('generateSqlQuery', {
  request: 'show pending sales orders'
});

const sql = response.content[0].text;
const results = await mcpClient.callTool('executeSalesQuery', { sql });
```

## Data Model

### SalesRecord Entity
```java
public class SalesRecord {
    private Integer salesId;        // Primary identifier
    private String salesNumber;     // Order number (e.g., SALES-1001)
    private String action;          // Status: approved/rejected/pending/etc
    private LocalDateTime createdAt;  // Creation timestamp
    private LocalDateTime updatedAt;  // Last update timestamp
}
```

### Action Values
Common action values in the system:
- `created` - New sales order created
- `approved` - Order approved
- `rejected` - Order rejected
- `pending` - Awaiting approval
- `cancelled` - Order cancelled
- `completed` - Order completed

## Security Considerations

- **Read-only access**: Only SELECT queries are permitted
- **SQL injection protection**: Parameters are properly escaped
- **Limit enforcement**: Automatic limits prevent large result sets
- **Input validation**: Natural language inputs are validated and sanitized

## Performance Notes

- Default query limit: 100 records
- Maximum allowed limit: 500 records (for findByAction)
- Queries are automatically ordered by creation date when appropriate
- Index recommendations: `action`, `crreated_at`, `updated_at` columns should be indexed

## Troubleshooting

### Common Issues
1. **"Table doesn't exist"** - Check database connection and table creation
2. **"Connection refused"** - Verify MCP server is running on correct port
3. **Empty results** - Check if data exists in the table
4. **SQL syntax errors** - Use generateSqlQuery first to validate query structure

### Debug Logging
The server logs all MCP tool calls with `[MCP]` prefix for debugging purposes.
