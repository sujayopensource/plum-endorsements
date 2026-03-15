package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.EAAccount;
import com.plum.endorsements.domain.model.EATransaction;
import java.util.*;

public interface EAAccountRepository {
    Optional<EAAccount> findByEmployerIdAndInsurerId(UUID employerId, UUID insurerId);
    Optional<EAAccount> findByEmployerIdAndInsurerIdForUpdate(UUID employerId, UUID insurerId);
    List<EAAccount> findByEmployerId(UUID employerId);
    List<EAAccount> findAll();
    EAAccount save(EAAccount account);
    EATransaction saveTransaction(EATransaction transaction);
    List<EATransaction> findTransactionsByEmployerId(UUID employerId, UUID insurerId);
}
