package com.plum.endorsements.domain.service;

import com.plum.endorsements.application.exception.InsurerNotFoundException;
import com.plum.endorsements.domain.model.InsurerConfiguration;
import com.plum.endorsements.domain.port.InsurerConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsurerRegistry {

    private final InsurerConfigurationRepository configurationRepository;

    @Cacheable(value = "insurerConfigurations", key = "#insurerId")
    public InsurerConfiguration getConfiguration(UUID insurerId) {
        log.debug("Loading insurer configuration for {}", insurerId);
        return configurationRepository.findByInsurerId(insurerId)
                .orElseThrow(() -> new InsurerNotFoundException(insurerId));
    }

    @Cacheable(value = "insurerConfigurationsByCode", key = "#insurerCode")
    public InsurerConfiguration getConfigurationByCode(String insurerCode) {
        log.debug("Loading insurer configuration for code {}", insurerCode);
        return configurationRepository.findByInsurerCode(insurerCode)
                .orElseThrow(() -> new InsurerNotFoundException(insurerCode));
    }

    public List<InsurerConfiguration> getAllActiveInsurers() {
        return configurationRepository.findAllActive();
    }

    @CacheEvict(value = {"insurerConfigurations", "insurerConfigurationsByCode"}, allEntries = true)
    public InsurerConfiguration createConfiguration(InsurerConfiguration config) {
        log.info("Creating insurer configuration for {} ({})",
                config.getInsurerCode(), config.getInsurerId());
        return configurationRepository.save(config);
    }

    @CacheEvict(value = {"insurerConfigurations", "insurerConfigurationsByCode"}, allEntries = true)
    public InsurerConfiguration updateConfiguration(InsurerConfiguration config) {
        log.info("Updating insurer configuration for {} ({}), evicting cache",
                config.getInsurerCode(), config.getInsurerId());
        return configurationRepository.save(config);
    }
}
