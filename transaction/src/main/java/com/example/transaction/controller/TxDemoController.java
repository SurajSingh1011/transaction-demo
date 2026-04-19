package com.example.transaction.controller;

import com.example.transaction.entity.Account;
import com.example.transaction.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class TxDemoController {

    private final BankService bankService;

    @PostMapping("/init")
    public String init() {
        bankService.initDemoData();
        return "Demo data initialized";
    }

    // ----- PROPAGATION DEMOS -----

    // REQUIRED – pass failAudit=true to see rollback
    @PostMapping("/required")
    public String required(@RequestParam(defaultValue = "false") boolean failAudit) {
        try {
            bankService.transferWithRequired(failAudit);
            return "Transfer with REQUIRED done";
        } catch (Exception e) {
            return "FAILED: " + e.getMessage();
        }
    }

    // REQUIRES_NEW – audit failure does NOT rollback money transfer
    @PostMapping("/requires-new")
    public String requiresNew(@RequestParam(defaultValue = "false") boolean failAudit) {
        bankService.transferWithRequiresNew(failAudit);
        return "Transfer with REQUIRES_NEW done (even if audit failed)";
    }

    // SUPPORTS – just to show it works with and without tx
    @PostMapping("/supports")
    public String supports() {
        bankService.txCallerSupports();      // inside tx
        bankService.nonTxCallerRequired();   // separate tx
        return "SUPPORTS demo done";
    }

    // MANDATORY – this should throw error (no tx in caller)
    @PostMapping("/mandatory-error")
    public String mandatoryError() {
        try {
            bankService.nonTxCallerMandatory();
            return "Unexpected success";
        } catch (Exception e) {
            return "Expected failure: " + e.getClass().getSimpleName();
        }
    }

    // NEVER – this should throw error (tx exists)
    @PostMapping("/never-error")
    public String neverError() {
        try {
            bankService.txCallerNever();
            return "Unexpected success";
        } catch (Exception e) {
            return "Expected failure: " + e.getClass().getSimpleName();
        }
    }

    // NOT_SUPPORTED – tx suspended during logging
    @PostMapping("/not-supported")
    public String notSupported() {
        bankService.txCallerNotSupported();
        return "NOT_SUPPORTED demo done";
    }

    // NESTED – nested failure does NOT rollback outer tx
    @PostMapping("/nested")
    public String nested(@RequestParam(defaultValue = "false") boolean failNested) {
        bankService.transferWithNested(failNested);
        return "NESTED demo done";
    }

    // ----- ISOLATION DEMOS -----

    @GetMapping("/account/default/{id}")
    public Account getDefault(@PathVariable Long id) {
        return bankService.getAccountDefault(id);
    }

    @GetMapping("/account/read-committed/{id}")
    public Account getReadCommitted(@PathVariable Long id) {
        return bankService.getAccountReadCommitted(id);
    }

    @GetMapping("/account/repeatable-read/{id}")
    public Account getRepeatable(@PathVariable Long id) {
        return bankService.getAccountRepeatableRead(id);
    }

    @GetMapping("/account/serializable/{id}")
    public Account getSerializable(@PathVariable Long id) {
        return bankService.getAccountSerializable(id);
    }
}
