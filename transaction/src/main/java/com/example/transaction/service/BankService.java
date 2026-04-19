package com.example.transaction.service;

import com.example.transaction.entity.Account;
import com.example.transaction.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BankService {

    Logger logger
            = LoggerFactory.getLogger(BankService.class);

    private final AccountRepository accountRepository;
    private final AuditService auditService;

    // Utility: create 2 accounts
    @Transactional
    public void initDemoData() {
        accountRepository.deleteAll();

        Account a1 = new Account();
        a1.setId(1L);
        a1.setName("Alice");
        a1.setBalance(1000.0);

        Account a2 = new Account();
        a2.setId(2L);
        a2.setName("Bob");
        a2.setBalance(500.0);

        accountRepository.save(a1);
        accountRepository.save(a2);
    }

    // ----------------- PROPAGATION DEMOS -----------------

    // REQUIRED + REQUIRED – both participate in same tx
    @Transactional
    public void transferWithRequired(boolean failAudit) {
        debit(1L, 100.0);     // same tx
        credit(2L, 100.0);    // same tx
        auditService.logRequired("Transfer with REQUIRED", failAudit);
        // if audit fails, whole transaction (money + log) rolls back
    }

    // REQUIRED + REQUIRES_NEW – separate tx for audit
    @Transactional
    public void transferWithRequiresNew(boolean failAudit) {
        debit(1L, 100.0);   // T1
        credit(2L, 100.0);  // T1

        try {
            auditService.logRequiresNew("Transfer with REQUIRES_NEW", failAudit);  // T2
        } catch (Exception e) {
            // ignore audit error, main transaction still commits
        }
    }

    // NOT transaction at outer, but inner REQUIRED creates one
    public void nonTxCallerRequired() {
        auditService.logRequired("Called from NON-TX method", false);
    }

    // Outer tx calling SUPPORTS
    @Transactional
    public void txCallerSupports() {
        auditService.logSupports("Called from TX method");
    }

    // Non-tx calling MANDATORY -> error
    public void nonTxCallerMandatory() {
        auditService.logMandatory("This should fail (no tx here)");
    }

    // Tx calling NEVER -> error
    @Transactional
    public void txCallerNever() {
        auditService.logNever("This should fail (tx exists)");
    }

    // Tx calling NOT_SUPPORTED (tx suspended)
    @Transactional
    public void txCallerNotSupported() {
        debit(1L, 50.0); // under transaction

        // transaction suspended here
        auditService.logNotSupported("Non-transactional logging");

        // transaction resumes here
        credit(2L, 50.0);
    }

    // NESTED example: outer continues even if nested fails
    @Transactional
    public void transferWithNested(boolean failNested) {
        debit(1L, 100.0);  // in outer tx
        try {
            logger.info("In main transaction t1");
            auditService.logNested("Nested log inside transfer",failNested);
        } catch (Exception e) {
            // nested rolled back to savepoint, outer tx continues
        }
        credit(2L, 100.0); // still in outer tx
    }

    // ----------------- ISOLATION LEVEL DEMOS -----------------

    // DEFAULT isolation (DB default, usually READ_COMMITTED)
    @Transactional(isolation = Isolation.DEFAULT, readOnly = true)
    public Account getAccountDefault(Long id) {
        return accountRepository.findById(id).orElse(null);
    }

    // READ_COMMITTED: prevents dirty reads
    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Account getAccountReadCommitted(Long id) {
        return accountRepository.findById(id).orElse(null);
    }

    // REPEATABLE_READ: prevents non-repeatable reads
    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public Account getAccountRepeatableRead(Long id) {
        return accountRepository.findById(id).orElse(null);
    }

    // SERIALIZABLE: strongest, least concurrent
    @Transactional(isolation = Isolation.SERIALIZABLE, readOnly = true)
    public Account getAccountSerializable(Long id) {
        return accountRepository.findById(id).orElse(null);
    }

    // ----------------- INTERNAL HELPERS -----------------

    private void debit(Long id, Double amount) {
        Account acc = accountRepository.findById(id).orElseThrow();
        acc.setBalance(acc.getBalance() - amount);
        accountRepository.save(acc);
    }

    private void credit(Long id, Double amount) {
        Account acc = accountRepository.findById(id).orElseThrow();
        acc.setBalance(acc.getBalance() + amount);
        accountRepository.save(acc);
    }
}
