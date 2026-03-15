package com.plum.endorsements.domain.service;

import com.plum.endorsements.application.exception.InsurerNotFoundException;
import com.plum.endorsements.domain.model.InsurerConfiguration;
import com.plum.endorsements.domain.port.InsurerConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsurerRegistry")
class InsurerRegistryTest {

    @Mock
    private InsurerConfigurationRepository configurationRepository;

    private InsurerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InsurerRegistry(configurationRepository);
    }

    @Test
    @DisplayName("getConfiguration returns config for known insurer")
    void getConfiguration_KnownInsurer_ReturnsConfig() {
        UUID insurerId = UUID.randomUUID();
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(insurerId)
                .insurerName("Test Insurer")
                .insurerCode("TEST")
                .adapterType("MOCK")
                .active(true)
                .build();
        when(configurationRepository.findByInsurerId(insurerId)).thenReturn(Optional.of(config));

        InsurerConfiguration result = registry.getConfiguration(insurerId);

        assertThat(result.getInsurerId()).isEqualTo(insurerId);
        assertThat(result.getInsurerCode()).isEqualTo("TEST");
        verify(configurationRepository).findByInsurerId(insurerId);
    }

    @Test
    @DisplayName("getConfiguration throws for unknown insurer")
    void getConfiguration_UnknownInsurer_ThrowsException() {
        UUID unknownId = UUID.randomUUID();
        when(configurationRepository.findByInsurerId(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registry.getConfiguration(unknownId))
                .isInstanceOf(InsurerNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    @DisplayName("getConfigurationByCode returns config for known code")
    void getConfigurationByCode_KnownCode_ReturnsConfig() {
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(UUID.randomUUID())
                .insurerName("ICICI Lombard")
                .insurerCode("ICICI_LOMBARD")
                .adapterType("ICICI_LOMBARD")
                .active(true)
                .build();
        when(configurationRepository.findByInsurerCode("ICICI_LOMBARD")).thenReturn(Optional.of(config));

        InsurerConfiguration result = registry.getConfigurationByCode("ICICI_LOMBARD");

        assertThat(result.getInsurerCode()).isEqualTo("ICICI_LOMBARD");
    }

    @Test
    @DisplayName("getConfigurationByCode throws for unknown code")
    void getConfigurationByCode_UnknownCode_ThrowsException() {
        when(configurationRepository.findByInsurerCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registry.getConfigurationByCode("UNKNOWN"))
                .isInstanceOf(InsurerNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("getAllActiveInsurers returns active configurations")
    void getAllActiveInsurers_ReturnsActiveConfigs() {
        List<InsurerConfiguration> configs = List.of(
                InsurerConfiguration.builder()
                        .insurerId(UUID.randomUUID())
                        .insurerName("Mock")
                        .insurerCode("MOCK")
                        .adapterType("MOCK")
                        .active(true)
                        .build(),
                InsurerConfiguration.builder()
                        .insurerId(UUID.randomUUID())
                        .insurerName("ICICI")
                        .insurerCode("ICICI_LOMBARD")
                        .adapterType("ICICI_LOMBARD")
                        .active(true)
                        .build()
        );
        when(configurationRepository.findAllActive()).thenReturn(configs);

        List<InsurerConfiguration> result = registry.getAllActiveInsurers();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("updateConfiguration saves and returns updated config")
    void updateConfiguration_SavesAndReturns() {
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(UUID.randomUUID())
                .insurerName("Updated")
                .insurerCode("TEST")
                .adapterType("MOCK")
                .active(true)
                .build();
        when(configurationRepository.save(config)).thenReturn(config);

        InsurerConfiguration result = registry.updateConfiguration(config);

        assertThat(result.getInsurerName()).isEqualTo("Updated");
        verify(configurationRepository).save(config);
    }
}
