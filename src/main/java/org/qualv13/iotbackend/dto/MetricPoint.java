package org.qualv13.iotbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricPoint {
    private long idx; // Timestamp w milisekundach (oś X)
    private int val;  // Wartość (oś Y)
}