package com.example.qrgenerator.model;

public enum ProductStatus {
    CREATED,        // 생성됨
    IN_TRANSIT,     // 유통중
    READY_FOR_SALE, // 판매준비
    ACTIVATED,      // 정품인증완료
    SUSPICIOUS      // 의심스러운 스캔 발생
} 