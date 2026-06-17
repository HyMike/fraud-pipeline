package com.fraudpipeline.payment.controller;

import com.fraudpipeline.payment.entity.Case;
import com.fraudpipeline.payment.service.CaseService;
import com.fraudpipeline.payment.service.CaseService.DecisionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @GetMapping
    public Page<Case> getCases(
            @RequestParam(required = false) Case.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "createdAt"));
        return caseService.getCases(status, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Case> getCase(@PathVariable UUID id) {
        return ResponseEntity.ok(caseService.getCase(id));
    }

    @PostMapping("/{id}/decision")
    public ResponseEntity<Case> decide(
            @PathVariable UUID id,
            @RequestBody DecisionRequest request) {
        return ResponseEntity.ok(caseService.decide(id, request));
    }
}
