package com.example.qrgenerator.model;

import lombok.Data;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "scan_history")
public class ScanHistory {
    @Id
    private String id;
    private String productCode;
    private LocalDateTime scanTime;
    private boolean valid;
    private String validationMessage;
    private String scannedContent;
    private ProductStatus productStatus;
    private int scanCount;
    private int maxScanCount;
    private boolean hashValid;
    private boolean timeValid;

    public boolean isValid() {
        return valid;
    }
} 