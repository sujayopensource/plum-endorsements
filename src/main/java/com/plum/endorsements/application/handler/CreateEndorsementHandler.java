package com.plum.endorsements.application.handler;

import com.plum.endorsements.application.exception.InsufficientBalanceException;
import com.plum.endorsements.domain.model.EAAccount;
import com.plum.endorsements.domain.model.EATransaction;
import com.plum.endorsements.domain.model.EATransaction.EATransactionType;
import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.model.ProvisionalCoverage;
import com.plum.endorsements.domain.port.EAAccountRepository;
import com.plum.endorsements.domain.port.EndorsementRepository;
import com.plum.endorsements.domain.port.EventPublisher;
import com.plum.endorsements.domain.port.ProvisionalCoverageRepository;
import com.plum.endorsements.domain.service.EABalanceCalculator;
import com.plum.endorsements.domain.service.EndorsementStateMachine;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class CreateEndorsementHandler {

    private final EndorsementRepository endorsementRepository;
    private final EAAccountRepository eaAccountRepository;
    private final ProvisionalCoverageRepository provisionalCoverageRepository;
    private final EndorsementStateMachine stateMachine;
    private final EABalanceCalculator balanceCalculator;
    private final EventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    private final boolean blockOnInsufficientBalance;

    public CreateEndorsementHandler(
            EndorsementRepository endorsementRepository,
            EAAccountRepository eaAccountRepository,
            ProvisionalCoverageRepository provisionalCoverageRepository,
            EndorsementStateMachine stateMachine,
            EABalanceCalculator balanceCalculator,
            EventPublisher eventPublisher,
            MeterRegistry meterRegistry,
            @Value("${endorsement.ea.block-on-insufficient-balance:false}") boolean blockOnInsufficientBalance) {
        this.endorsementRepository = endorsementRepository;
        this.eaAccountRepository = eaAccountRepository;
        this.provisionalCoverageRepository = provisionalCoverageRepository;
        this.stateMachine = stateMachine;
        this.balanceCalculator = balanceCalculator;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
        this.blockOnInsufficientBalance = blockOnInsufficientBalance;
    }

    public Endorsement handle(Endorsement endorsement) {
        try {
            // 1. Check idempotency
            Optional<Endorsement> existing = endorsementRepository.findByIdempotencyKey(endorsement.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Duplicate endorsement detected for idempotency key: {}", endorsement.getIdempotencyKey());
                return existing.get();
            }

            // 2. Set initial status and timestamps
            endorsement.setStatus(EndorsementStatus.CREATED);
            Instant now = Instant.now();
            endorsement.setCreatedAt(now);
            endorsement.setUpdatedAt(now);

            // 3. Save the endorsement
            endorsement = endorsementRepository.save(endorsement);
            MDC.put("endorsementId", endorsement.getId().toString());
            MDC.put("employerId", endorsement.getEmployerId().toString());
            meterRegistry.counter("endorsement.created", "type", endorsement.getType().name()).increment();
            log.info("Created endorsement {} for employer {} employee {}",
                    endorsement.getId(), endorsement.getEmployerId(), endorsement.getEmployeeId());

            // 4. Publish Created event
            eventPublisher.publish(new EndorsementEvent.Created(
                    endorsement.getId(),
                    Instant.now(),
                    endorsement.getEmployerId(),
                    endorsement.getEmployeeId(),
                    endorsement.getType()
            ));

            // 5. Validate - transition to VALIDATED
            stateMachine.transition(endorsement, EndorsementStatus.VALIDATED);

            // 6. Save after validation
            endorsement = endorsementRepository.save(endorsement);
            log.info("Endorsement {} validated", endorsement.getId());

            // 7. Publish Validated event
            eventPublisher.publish(new EndorsementEvent.Validated(
                    endorsement.getId(),
                    Instant.now(),
                    endorsement.getEmployerId()
            ));

            // 8-9. Grant provisional coverage
            if (endorsement.getType() == EndorsementType.ADD) {
                ProvisionalCoverage coverage = ProvisionalCoverage.builder()
                        .endorsementId(endorsement.getId())
                        .employeeId(endorsement.getEmployeeId())
                        .employerId(endorsement.getEmployerId())
                        .coverageStart(endorsement.getCoverageStartDate())
                        .createdAt(Instant.now())
                        .build();
                provisionalCoverageRepository.save(coverage);
                log.info("Provisional coverage granted for endorsement {}", endorsement.getId());
            }

            // Transition to PROVISIONALLY_COVERED for all types
            stateMachine.transition(endorsement, EndorsementStatus.PROVISIONALLY_COVERED);
            endorsement = endorsementRepository.save(endorsement);

            // Publish ProvisionalCoverageGranted event
            eventPublisher.publish(new EndorsementEvent.ProvisionalCoverageGranted(
                    endorsement.getId(),
                    Instant.now(),
                    endorsement.getEmployerId(),
                    endorsement.getEmployeeId(),
                    endorsement.getCoverageStartDate()
            ));

            // 10. Check EA balance for ADD type
            if (endorsement.getType() == EndorsementType.ADD) {
                Optional<EAAccount> accountOpt = eaAccountRepository.findByEmployerIdAndInsurerIdForUpdate(
                        endorsement.getEmployerId(), endorsement.getInsurerId());

                if (accountOpt.isPresent()) {
                    EAAccount account = accountOpt.get();
                    if (account.canFund(endorsement.getPremiumAmount())) {
                        account.reserve(endorsement.getPremiumAmount());
                        eaAccountRepository.save(account);

                        EATransaction reserveTransaction = new EATransaction(
                                null,
                                endorsement.getEmployerId(),
                                endorsement.getInsurerId(),
                                endorsement.getId(),
                                EATransactionType.RESERVE,
                                endorsement.getPremiumAmount(),
                                account.availableBalance(),
                                "Reserved for endorsement " + endorsement.getId(),
                                Instant.now()
                        );
                        eaAccountRepository.saveTransaction(reserveTransaction);
                        meterRegistry.counter("endorsement.ea.reservation", "result", "success").increment();
                        log.info("Reserved {} for endorsement {} from EA account",
                                endorsement.getPremiumAmount(), endorsement.getId());
                    } else {
                        meterRegistry.counter("endorsement.ea.reservation", "result", "insufficient").increment();
                        if (blockOnInsufficientBalance) {
                            log.warn("Blocking endorsement {} due to insufficient EA balance", endorsement.getId());
                            throw new InsufficientBalanceException(
                                    endorsement.getEmployerId(),
                                    endorsement.getPremiumAmount(),
                                    account.availableBalance());
                        }
                        log.warn("Insufficient EA balance for endorsement {} (non-blocking)", endorsement.getId());
                    }
                }
            }

            // 11. Return the endorsement
            // NOTE: Anomaly detection is triggered from the controller after this
            // transaction commits, so the new REQUIRES_NEW transaction can see the data.
            return endorsement;
        } finally {
            MDC.remove("endorsementId");
            MDC.remove("employerId");
        }
    }
}
