package com.example.qrgenerator.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import com.example.qrgenerator.dto.ValidationResult;
import com.example.qrgenerator.model.Product;
import com.example.qrgenerator.repository.ProductRepository;
import com.example.qrgenerator.model.ProductStatus;
import lombok.RequiredArgsConstructor;
import com.example.qrgenerator.model.ScanHistory;
import com.example.qrgenerator.repository.ScanHistoryRepository;
import java.util.Optional;
import lombok.Getter;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SecurityService {
    private static final String SECRET_KEY = "your-secret-key";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final long QR_CODE_VALIDITY_HOURS = 24; // QR 코드 유효 시간 (24시간)

    private final ProductRepository productRepository;
    private final ScanHistoryRepository scanHistoryRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public String generateHash(String productCode, LocalDateTime timestamp) {
        try {
            String data = productCode + timestamp.format(formatter) + SECRET_KEY;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("해시 생성 실패", e);
        }
    }

    public String generateSecureQRContent(String productCode, LocalDateTime timestamp, String hash) {
        return String.format("http://localhost:8081/validate?content=%s_%s_%s", 
            productCode, 
            timestamp.format(formatter),
            hash.substring(0, 8)
        );
    }

    public boolean validateQRCode(String productCode, LocalDateTime timestamp, String hash) {
        // 1. 타임스탬프 유효성 검사 (예: 24시간 이내)
        if (timestamp.plusHours(24).isBefore(LocalDateTime.now())) {
            return false;
        }

        // 2. 해시 검증
        String expectedHash = generateHash(productCode, timestamp);
        return hash.equals(expectedHash.substring(0, 8));
    }

    public ValidationResult validateQRCodeWithDetails(String productCode, LocalDateTime timestamp, String hash) {
        Optional<Product> productOpt = productRepository.findByCode(productCode);
        
        if (productOpt.isEmpty()) {
            return ValidationResult.builder()
                .valid(false)
                .productCode(productCode)
                .message("제품을 찾을 수 없습니다.")
                .timeValid(false)
                .hashValid(false)
                .build();
        }

        Product product = productOpt.get();
        
        // 1. 타임스탬프 검증
        boolean timeValid = isTimestampValid(timestamp);
        if (!timeValid) {
            markProductAsSuspicious(product, "만료된 QR코드입니다.");
            return createValidationResult(product, false, 
                "만료된 QR코드입니다.", false, timeValid);
        }

        // 2. 해시 검증
        String expectedHash = generateHash(product.getCode(), timestamp);
        boolean hashValid = hash.equals(expectedHash.substring(0, 8));
        if (!hashValid) {
            markProductAsSuspicious(product, "위조된 QR코드입니다.");
            return createValidationResult(product, false, 
                "위조된 QR코드입니다.", hashValid, timeValid);
        }

        // 3. 제품 상태에 따른 처리
        if (product.getStatus() == ProductStatus.SUSPICIOUS) {
            return createValidationResult(product, false, 
                "의심스러운 제품입니다. 고객센터에 문의하세요.", 
                hashValid, timeValid);
        }

        // 4. 스캔 횟수 검증 (ACTIVATED 상태인 경우)
        if (product.getStatus() == ProductStatus.ACTIVATED) {
            return handleActivatedProduct(product, hashValid, timeValid);
        }

        // 5. 상태 전환
        return handleStatusTransition(product, hashValid, timeValid);
    }

    @Getter
    @AllArgsConstructor
    private static class ValidationCheckResult {
        private final boolean valid;
        private final boolean hashValid;
        private final boolean timeValid;
        
        public ValidationResult getValidationResult() {
            return null; // 실제 검증 결과는 상위 메서드에서 생성
        }
    }

    private ValidationCheckResult validateBasicChecks(Product product, LocalDateTime timestamp, String hash) {
        // 1. 타임스탬프 검증
        boolean timeValid = isTimestampValid(timestamp);
        if (!timeValid) {
            markProductAsSuspicious(product, "만료된 QR코드입니다.");
            return new ValidationCheckResult(false, false, timeValid);
        }

        // 2. 해시 검증
        String expectedHash = generateHash(product.getCode(), timestamp);
        boolean hashValid = hash.equals(expectedHash.substring(0, 8));
        if (!hashValid) {
            markProductAsSuspicious(product, "위조된 QR코드입니다.");
            return new ValidationCheckResult(false, hashValid, timeValid);
        }

        return new ValidationCheckResult(true, hashValid, timeValid);
    }

    private ValidationResult handleActivatedProduct(Product product, boolean hashValid, boolean timeValid) {
        int newScanCount = product.getCurrentScanCount() + 1;
        
        if (newScanCount > product.getMaxScanCount()) {
            markProductAsSuspicious(product, "스캔 횟수를 초과했습니다.");
            return createValidationResult(product, false, 
                "스캔 횟수를 초과했습니다.", hashValid, timeValid);
        }

        product.setCurrentScanCount(newScanCount);
        product = productRepository.save(product);
        
        String message = String.format("인증된 정품입니다. (스캔 횟수: %d/%d)", 
            newScanCount, product.getMaxScanCount());
        saveScanHistory(product, true, message, hashValid, timeValid, newScanCount);
            
        return createValidationResult(product, true, message, hashValid, timeValid);
    }

    private ValidationResult handleStatusTransition(Product product, boolean hashValid, boolean timeValid) {
        String message;
        switch (product.getStatus()) {
            case CREATED:
                message = "제품이 유통 단계로 전환되었습니다.";
                product.setStatus(ProductStatus.IN_TRANSIT);
                break;
            case IN_TRANSIT:
                message = "제품이 판매 준비 상태로 전환되었습니다.";
                product.setStatus(ProductStatus.READY_FOR_SALE);
                break;
            case READY_FOR_SALE:
                message = "정품 인증이 완료되었습니다.";
                product.setStatus(ProductStatus.ACTIVATED);
                product.setActivated(true);
                product.setFirstScanTime(LocalDateTime.now());
                product.setCurrentScanCount(1);
                break;
            default:
                message = "알 수 없는 상태입니다.";
                return createValidationResult(product, false, message, hashValid, timeValid);
        }

        product = productRepository.save(product);
        saveScanHistory(product, true, message, hashValid, timeValid, product.getCurrentScanCount());
            
        return createValidationResult(product, true, message, hashValid, timeValid);
    }

    private boolean isValidDistributionPath(Product product) {
        LocalDateTime now = LocalDateTime.now();
        
        // 제조 시점보다 이른 스캔
        if (now.isBefore(product.getManufacturingTime())) {
            return false;
        }
        
        // 예상 배송시간보다 너무 이른 스캔
        if (now.isBefore(product.getExpectedDeliveryTime().minusDays(7))) {
            return false;
        }

        // 추가 검증 로직 (위치 기반 등)
        return true;
    }

    private void markProductAsSuspicious(Product product, String reason) {
        product.setStatus(ProductStatus.SUSPICIOUS);
        productRepository.save(product);
        
        ScanHistory scanHistory = ScanHistory.builder()
            .productCode(product.getCode())
            .scanTime(LocalDateTime.now())
            .validationMessage(reason)
            .valid(false)
            .productStatus(ProductStatus.SUSPICIOUS)
            .build();
        
        scanHistoryRepository.save(scanHistory);
        
        // 의심스러운 스캔 알림 발송
        messagingTemplate.convertAndSend("/topic/suspicious-scan", 
            Map.of(
                "productCode", product.getCode(),
                "reason", reason,
                "timestamp", LocalDateTime.now()
            ));
    }

    private String getStatusMessage(ProductStatus status) {
        switch (status) {
            case CREATED: return "제품 생성됨";
            case IN_TRANSIT: return "유통중";
            case READY_FOR_SALE: return "판매 준비";
            case ACTIVATED: return "정품 인증됨";
            case SUSPICIOUS: return "의심 제품";
            default: return "알 수 없음";
        }
    }

    private ValidationResult createValidationResult(Product product, boolean valid, String message, boolean hashValid, boolean timeValid) {
        return ValidationResult.builder()
            .valid(valid)
            .productCode(product.getCode())
            .status(product.getStatus())
            .currentScanCount(product.getCurrentScanCount())
            .maxScanCount(product.getMaxScanCount())
            .manufacturingLocation(product.getManufacturingLocation())
            .manufacturingTime(product.getManufacturingTime())
            .message(message)
            .hashValid(hashValid)
            .timeValid(timeValid)
            .build();
    }

    private void saveScanHistory(Product product, boolean valid, String message, 
        boolean hashValid, boolean timeValid, int currentScanCount) {
        System.out.println("스캔 이력 저장: " + 
            "valid=" + valid + 
            ", message=" + message + 
            ", status=" + product.getStatus());
        ScanHistory history = ScanHistory.builder()
            .productCode(product.getCode())
            .scanTime(LocalDateTime.now())
            .valid(valid)
            .validationMessage(message)
            .productStatus(product.getStatus())
            .scanCount(currentScanCount)
            .maxScanCount(product.getMaxScanCount())
            .hashValid(hashValid)
            .timeValid(timeValid)
            .build();
        scanHistoryRepository.save(history);
        
        // 스캔 이력 알림 발송
        messagingTemplate.convertAndSend("/topic/scan-history", history);
        
        // 제품 상태 변경 알림 발송
        messagingTemplate.convertAndSend("/topic/product-status", 
            Map.of(
                "productCode", product.getCode(),
                "status", product.getStatus(),
                "timestamp", LocalDateTime.now()
            ));
    }

    private boolean isTimestampValid(LocalDateTime timestamp) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validUntil = timestamp.plusHours(QR_CODE_VALIDITY_HOURS);
        return now.isBefore(validUntil);
    }
} 