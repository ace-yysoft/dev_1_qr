package com.example.qrgenerator.controller;

import com.example.qrgenerator.model.Product;
import com.example.qrgenerator.repository.ProductRepository;
import com.example.qrgenerator.service.QRCodeService;
import com.example.qrgenerator.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.time.format.DateTimeFormatter;
import com.example.qrgenerator.dto.ValidationResult;
import com.example.qrgenerator.model.ScanHistory;
import com.example.qrgenerator.repository.ScanHistoryRepository;
import com.example.qrgenerator.model.ProductStatus;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8081", methods = {RequestMethod.GET, RequestMethod.POST})
public class ProductController {

    private final ProductRepository productRepository;
    private final QRCodeService qrCodeService;
    private final SecurityService securityService;
    private final ScanHistoryRepository scanHistoryRepository;

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        // 기본값 설정
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.CREATED);
        }
        if (product.getMaxScanCount() <= 0) {
            product.setMaxScanCount(1);
        }
        
        // 유효성 검사
        validateProductInfo(product);
        
        // 현재 시간을 ISO 형식으로 포맷팅
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalDateTime now = LocalDateTime.now();
        
        // 해시 생성 및 QR 코드 텍스트 설정
        product.setHash(securityService.generateHash(product.getCode(), now));
        product.setQrCodeText(String.format("%s_%s_%s", 
            product.getCode(), 
            timestamp,
            product.getHash()
        ));
        
        // QR 코드 생성 (크기 지정)
        Product updatedProduct = qrCodeService.generateQRCode(product, 200, 200);
        Product savedProduct = productRepository.save(updatedProduct);
        
        // 저장된 제품을 즉시 조회하여 반환
        return productRepository.findById(savedProduct.getId())
            .orElseThrow(() -> new RuntimeException("제품 저장 실패"));
    }

    private void validateProductInfo(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("제품명은 필수입니다.");
        }
        if (product.getCode() == null || product.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("제품코드는 필수입니다.");
        }
        if (product.getManufacturingTime() == null) {
            throw new IllegalArgumentException("제조 시간은 필수입니다.");
        }
        if (product.getExpectedDeliveryTime() == null) {
            throw new IllegalArgumentException("예상 판매/배송 시간은 필수입니다.");
        }
        if (product.getManufacturingLocation() == null || product.getManufacturingLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("제조 위치는 필수입니다.");
        }
        if (product.getExpectedDeliveryLocation() == null || product.getExpectedDeliveryLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("예상 판매/배송 위치는 필수입니다.");
        }
        // 제품코드 중복 검사
        if (productRepository.findByCode(product.getCode()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 제품코드입니다.");
        }
    }

    @PostMapping("/validate")
    public ValidationResult validateQRCode(@RequestParam String content) {
        try {
            // QR 코드 내용 파싱 (format: productCode_timestamp_hash)
            String[] parts = content.split("_");
            if (parts.length != 3) {
                return ValidationResult.builder()
                    .valid(false)
                    .message("잘못된 QR 코드 형식입니다.")
                    .build();
            }

            String productCode = parts[0];
            LocalDateTime timestamp = LocalDateTime.parse(parts[1], 
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String hash = parts[2];

            return securityService.validateQRCodeWithDetails(productCode, timestamp, hash);
        } catch (Exception e) {
            log.error("QR 코드 검증 실패", e);
            return ValidationResult.builder()
                .valid(false)
                .message("QR 코드 검증 중 오류가 발생했습니다: " + e.getMessage())
                .build();
        }
    }

    @GetMapping("/{productCode}/scan-history")
    public List<ScanHistory> getScanHistory(@PathVariable String productCode) {
        return scanHistoryRepository.findByProductCodeOrderByScanTimeDesc(productCode);
    }
} 