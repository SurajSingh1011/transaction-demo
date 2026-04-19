package com.example.transaction.service;

import com.example.transaction.entity.AuditLog;
import com.example.transaction.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    Logger logger
            = LoggerFactory.getLogger(BankService.class);
    private final AuditLogRepository auditLogRepository;

    // 1) REQUIRED (default) – joins existing transaction or creates new
    @Transactional(propagation = Propagation.REQUIRED)
    public void logRequired(String msg, boolean fail) {
        saveLog("REQUIRED: " + msg);
        if (fail) {
            throw new RuntimeException("Fail in REQUIRED audit");
        }
    }

    // 2) REQUIRES_NEW – ignores current tx, always starts new tx
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRequiresNew(String msg, boolean fail) {
        saveLog("REQUIRES_NEW: " + msg);
        if (fail) {
            throw new RuntimeException("Fail in REQUIRES_NEW audit");
        }
    }

    // 3) SUPPORTS – joins existing tx if present, else non-transactional
    @Transactional(propagation = Propagation.SUPPORTS)
    public void logSupports(String msg) {
        saveLog("SUPPORTS: " + msg);
    }

    // 4) MANDATORY – must be called in an existing tx
    @Transactional(propagation = Propagation.MANDATORY)
    public void logMandatory(String msg) {
        saveLog("MANDATORY: " + msg);
    }

    // 5) NEVER – must NOT run inside a transaction
    @Transactional(propagation = Propagation.NEVER)
    public void logNever(String msg) {
        saveLog("NEVER: " + msg);
    }

    // 6) NOT_SUPPORTED – suspend tx, run non-transactionally
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void logNotSupported(String msg) {
        saveLog("NOT_SUPPORTED: " + msg);
    }

    // 7) NESTED – create nested tx with savepoint (if supported)
    @Transactional(propagation = Propagation.NESTED)
    public void logNested(String msg, boolean fail) {
        logger.info("In t2 transaction to save log for nested");
        saveLog("NESTED: " + msg);
        logger.info("log saves for nested");
        if (fail) {
            throw new RuntimeException("Fail in NESTED audit");
        }
    }

    private void saveLog(String msg) {
        AuditLog log = new AuditLog();
        log.setMessage(msg);
        auditLogRepository.save(log);
    }
}
