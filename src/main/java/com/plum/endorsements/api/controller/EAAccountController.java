package com.plum.endorsements.api.controller;

import com.plum.endorsements.api.dto.EAAccountResponse;
import com.plum.endorsements.application.handler.EndorsementQueryHandler;
import com.plum.endorsements.domain.model.EAAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ea-accounts")
@RequiredArgsConstructor
public class EAAccountController {

    private final EndorsementQueryHandler queryHandler;

    @GetMapping
    public ResponseEntity<EAAccountResponse> getEAAccount(
            @RequestParam UUID employerId,
            @RequestParam UUID insurerId) {

        Optional<EAAccount> account = queryHandler.findEAAccount(employerId, insurerId);
        return account
                .map(a -> ResponseEntity.ok(EAAccountResponse.from(a)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
