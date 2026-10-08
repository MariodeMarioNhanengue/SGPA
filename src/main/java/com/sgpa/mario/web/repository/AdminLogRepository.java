package com.example.demo.web.repository;

import com.example.demo.web.domain.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    /** Todos os logs de um admin, do mais recente para o mais antigo */
    List<AdminLog> findByAdminIdOrderByDataAcaoDesc(Long adminId);

    /** Todos os logs de todos os admins, do mais recente para o mais antigo (SuperAdmin) */
    List<AdminLog> findAllByOrderByDataAcaoDesc();
}
