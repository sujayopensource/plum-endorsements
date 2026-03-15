package com.plum.endorsements.infrastructure.persistence.adapter;

import com.plum.endorsements.domain.model.EAAccount;
import com.plum.endorsements.domain.model.EATransaction;
import com.plum.endorsements.domain.port.EAAccountRepository;
import com.plum.endorsements.infrastructure.persistence.mapper.EndorsementMapper;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataEAAccountRepository;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataEATransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaEAAccountRepositoryAdapter implements EAAccountRepository {

    private final SpringDataEAAccountRepository accountRepo;
    private final SpringDataEATransactionRepository transactionRepo;
    private final EndorsementMapper mapper;

    @Override
    public Optional<EAAccount> findByEmployerIdAndInsurerId(UUID employerId, UUID insurerId) {
        return accountRepo.findByEmployerIdAndInsurerId(employerId, insurerId).map(mapper::toDomain);
    }

    @Override
    public Optional<EAAccount> findByEmployerIdAndInsurerIdForUpdate(UUID employerId, UUID insurerId) {
        return accountRepo.findByEmployerIdAndInsurerIdForUpdate(employerId, insurerId).map(mapper::toDomain);
    }

    @Override
    public List<EAAccount> findByEmployerId(UUID employerId) {
        return accountRepo.findByEmployerId(employerId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<EAAccount> findAll() {
        return accountRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public EAAccount save(EAAccount account) {
        var entity = mapper.toEntity(account);
        var saved = accountRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public EATransaction saveTransaction(EATransaction transaction) {
        var entity = mapper.toEntity(transaction);
        var saved = transactionRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<EATransaction> findTransactionsByEmployerId(UUID employerId, UUID insurerId) {
        return transactionRepo.findByEmployerIdAndInsurerIdOrderByCreatedAtDesc(employerId, insurerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
