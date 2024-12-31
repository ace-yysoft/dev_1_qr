package com.example.qrgenerator.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private String code;
    private String qrCode;
    private String qrCodeText;
    private LocalDateTime timestamp;
    private String hash;
    private ProductStatus status = ProductStatus.CREATED;
    private boolean isActivated = false;
    private LocalDateTime firstScanTime;
    private int maxScanCount = 1;
    private int currentScanCount = 0;
    private String manufacturingLocation;
    private LocalDateTime manufacturingTime;
    private String expectedDeliveryLocation;
    private LocalDateTime expectedDeliveryTime;
} 