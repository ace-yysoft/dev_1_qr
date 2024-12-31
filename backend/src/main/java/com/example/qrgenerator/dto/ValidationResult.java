package com.example.qrgenerator.dto;

import com.example.qrgenerator.model.ProductStatus;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ValidationResult {
    private boolean valid;
    private String productCode;
    private ProductStatus status;
    private String message;
    private boolean hashValid;
    private boolean timeValid;
    private int currentScanCount;
    private int maxScanCount;
    private String manufacturingLocation;
    private LocalDateTime manufacturingTime;
} 