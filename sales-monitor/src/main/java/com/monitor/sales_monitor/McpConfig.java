package com.monitor.sales_monitor;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * McpConfig
 *
 * Registers @Tool-annotated methods from SalesRecordsTool with the MCP server.
 *
 * Spring AI's auto-configuration picks up any ToolCallbackProvider bean
 * and exposes its tools over the SSE transport automatically.
 * No manual wiring of SseServerTransport or RouterFunction needed.
 */
@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider salesRecordsTools(SalesRecordsTool salesRecordsTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(salesRecordsTool)
                .build();
    }
}
