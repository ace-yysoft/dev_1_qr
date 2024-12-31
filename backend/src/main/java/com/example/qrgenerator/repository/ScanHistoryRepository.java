package com.example.qrgenerator.repository;

import com.example.qrgenerator.model.ScanHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScanHistoryRepository extends MongoRepository<ScanHistory, String> {
    List<ScanHistory> findByProductCodeOrderByScanTimeDesc(String productCode);
} 