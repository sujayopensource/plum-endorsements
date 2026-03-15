package com.plum.endorsements.infrastructure.insurer;

import com.plum.endorsements.application.exception.InsurerNotFoundException;
import com.plum.endorsements.domain.model.InsurerConfiguration;
import com.plum.endorsements.domain.port.InsurerPort;
import com.plum.endorsements.domain.service.InsurerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsurerRouter")
class InsurerRouterTest {

    @Mock
    private InsurerRegistry insurerRegistry;

    @Mock
    private InsurerPort mockAdapter;

    @Mock
    private InsurerPort iciciAdapter;

    private InsurerRouter router;

    @BeforeEach
    void setUp() {
        when(mockAdapter.getAdapterType()).thenReturn("MOCK");
        when(iciciAdapter.getAdapterType()).thenReturn("ICICI_LOMBARD");
        router = new InsurerRouter(insurerRegistry, List.of(mockAdapter, iciciAdapter));
    }

    @Test
    @DisplayName("resolve returns correct adapter for known insurer")
    void resolve_KnownInsurer_ReturnsCorrectAdapter() {
        UUID insurerId = UUID.randomUUID();
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(insurerId)
                .insurerName("Mock Insurer")
                .insurerCode("MOCK")
                .adapterType("MOCK")
                .active(true)
                .build();
        when(insurerRegistry.getConfiguration(insurerId)).thenReturn(config);

        InsurerPort result = router.resolve(insurerId);

        assertThat(result).isSameAs(mockAdapter);
    }

    @Test
    @DisplayName("resolve returns ICICI adapter for ICICI insurer")
    void resolve_IciciInsurer_ReturnsIciciAdapter() {
        UUID insurerId = UUID.randomUUID();
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(insurerId)
                .insurerName("ICICI Lombard")
                .insurerCode("ICICI_LOMBARD")
                .adapterType("ICICI_LOMBARD")
                .active(true)
                .build();
        when(insurerRegistry.getConfiguration(insurerId)).thenReturn(config);

        InsurerPort result = router.resolve(insurerId);

        assertThat(result).isSameAs(iciciAdapter);
    }

    @Test
    @DisplayName("resolve throws when adapter type not registered")
    void resolve_UnregisteredAdapterType_ThrowsException() {
        UUID insurerId = UUID.randomUUID();
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(insurerId)
                .insurerName("Unknown")
                .insurerCode("UNKNOWN")
                .adapterType("NONEXISTENT")
                .active(true)
                .build();
        when(insurerRegistry.getConfiguration(insurerId)).thenReturn(config);

        assertThatThrownBy(() -> router.resolve(insurerId))
                .isInstanceOf(InsurerNotFoundException.class);
    }

    @Test
    @DisplayName("resolve throws when insurer not found in registry")
    void resolve_UnknownInsurer_ThrowsFromRegistry() {
        UUID unknownId = UUID.randomUUID();
        when(insurerRegistry.getConfiguration(unknownId))
                .thenThrow(new InsurerNotFoundException(unknownId));

        assertThatThrownBy(() -> router.resolve(unknownId))
                .isInstanceOf(InsurerNotFoundException.class);
    }

    @Test
    @DisplayName("resolveByCode returns correct adapter")
    void resolveByCode_KnownCode_ReturnsCorrectAdapter() {
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(UUID.randomUUID())
                .insurerName("ICICI Lombard")
                .insurerCode("ICICI_LOMBARD")
                .adapterType("ICICI_LOMBARD")
                .active(true)
                .build();
        when(insurerRegistry.getConfigurationByCode("ICICI_LOMBARD")).thenReturn(config);

        InsurerPort result = router.resolveByCode("ICICI_LOMBARD");

        assertThat(result).isSameAs(iciciAdapter);
    }

    @Test
    @DisplayName("hasAdapter returns true for registered type")
    void hasAdapter_RegisteredType_ReturnsTrue() {
        assertThat(router.hasAdapter("MOCK")).isTrue();
        assertThat(router.hasAdapter("ICICI_LOMBARD")).isTrue();
    }

    @Test
    @DisplayName("hasAdapter returns false for unregistered type")
    void hasAdapter_UnregisteredType_ReturnsFalse() {
        assertThat(router.hasAdapter("NONEXISTENT")).isFalse();
    }
}
