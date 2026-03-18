package com.agentic.repository;

import com.agentic.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByEntityTypeAndEntityId(@Param("entityType") String entityType, 
                                               @Param("entityId") Long entityId, 
                                               Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.performedBy = :username ORDER BY a.createdAt DESC")
    List<AuditLog> findByPerformedBy(@Param("username") String username);
    
    @Query("SELECT a FROM AuditLog a WHERE a.action = :action ORDER BY a.createdAt DESC")
    Page<AuditLog> findByAction(@Param("action") String action, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType ORDER BY a.createdAt DESC")
    Page<AuditLog> findByEntityType(@Param("entityType") String entityType, Pageable pageable);
}
