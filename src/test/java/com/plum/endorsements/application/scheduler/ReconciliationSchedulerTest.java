package com.plum.endorsements.application.scheduler;

import com.plum.endorsements.application.service.ReconciliationEngine;
import com.plum.endorsements.domain.model.InsurerConfiguration;
import com.plum.endorsements.domain.model.ReconciliationRun;
import com.plum.endorsements.domain.service.InsurerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationScheduler")
class ReconciliationSchedulerTest {

    @Mock private ReconciliationEngine reconciliationEngine;
    @Mock private InsurerRegistry insurerRegistry;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    private ReconciliationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ReconciliationScheduler(reconciliationEngine, insurerRegistry, meterRegistry);
    }

    @Test
    @DisplayName("runs reconciliation for all active insurers")
    void runReconciliation_AllActiveInsurers() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<InsurerConfiguration> configs = List.of(
                InsurerConfiguration.builder().insurerId(id1).insurerName("I1")
                        .insurerCode("C1").adapterType("MOCK").active(true).build(),
                InsurerConfiguration.builder().insurerId(id2).insurerName("I2")
                        .insurerCode("C2").adapterType("MOCK").active(true).build()
        );
        when(insurerRegistry.getAllActiveInsurers()).thenReturn(configs);
        when(reconciliationEngine.reconcileInsurer(any())).thenReturn(
                ReconciliationRun.builder().id(UUID.randomUUID()).insurerId(id1)
                        .status("COMPLETED").startedAt(Instant.now()).build());

        scheduler.runReconciliation();

        verify(reconciliationEngine).reconcileInsurer(id1);
        verify(reconciliationEngine).reconcileInsurer(id2);
    }

    @Test
    @DisplayName("continues processing remaining insurers when one fails")
    void runReconciliation_ContinuesOnFailure() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<InsurerConfiguration> configs = List.of(
                InsurerConfiguration.builder().insurerId(id1).insurerName("I1")
                        .insurerCode("C1").adapterType("MOCK").active(true).build(),
                InsurerConfiguration.builder().insurerId(id2).insurerName("I2")
                        .insurerCode("C2").adapterType("MOCK").active(true).build()
        );
        when(insurerRegistry.getAllActiveInsurers()).thenReturn(configs);
        when(reconciliationEngine.reconcileInsurer(id1))
                .thenThrow(new RuntimeException("Connection timeout"));
        when(reconciliationEngine.reconcileInsurer(id2)).thenReturn(
                ReconciliationRun.builder().id(UUID.randomUUID()).insurerId(id2)
                        .status("COMPLETED").startedAt(Instant.now()).build());

        scheduler.runReconciliation();

        verify(reconciliationEngine).reconcileInsurer(id1);
        verify(reconciliationEngine).reconcileInsurer(id2);
    }

    @Test
    @DisplayName("no-op when no active insurers")
    void runReconciliation_NoActiveInsurers() {
        when(insurerRegistry.getAllActiveInsurers()).thenReturn(List.of());

        scheduler.runReconciliation();

        verify(reconciliationEngine, never()).reconcileInsurer(any());
    }
}
