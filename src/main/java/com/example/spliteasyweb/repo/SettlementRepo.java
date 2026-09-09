package com.example.spliteasyweb.repo;

import com.example.spliteasyweb.model.SettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepo extends JpaRepository<SettlementEntity, Long> {
    List<SettlementEntity> findByGroupIdOrderBySettledAtDesc(Long groupId);
}
