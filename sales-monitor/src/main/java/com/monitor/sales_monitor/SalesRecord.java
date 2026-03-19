package com.monitor.sales_monitor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesRecord {

    private Integer salesId;
    private String salesNumber;
    private String action;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
