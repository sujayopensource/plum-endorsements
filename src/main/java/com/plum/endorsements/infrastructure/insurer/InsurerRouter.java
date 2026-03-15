package com.plum.endorsements.infrastructure.insurer;

import com.plum.endorsements.application.exception.InsurerNotFoundException;
import com.plum.endorsements.domain.model.InsurerConfiguration;
import com.plum.endorsements.domain.port.InsurerPort;
import com.plum.endorsements.domain.service.InsurerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InsurerRouter {

    private final InsurerRegistry insurerRegistry;
    private final Map<String, InsurerPort> adaptersByType;

    public InsurerRouter(InsurerRegistry insurerRegistry, List<InsurerPort> adapters) {
        this.insurerRegistry = insurerRegistry;
        this.adaptersByType = adapters.stream()
                .collect(Collectors.toMap(InsurerPort::getAdapterType, Function.identity()));
    }

    @PostConstruct
    void logRegisteredAdapters() {
        log.info("Registered insurer adapters: {}", adaptersByType.keySet());
    }

    public InsurerPort resolve(UUID insurerId) {
        InsurerConfiguration config = insurerRegistry.getConfiguration(insurerId);
        String adapterType = config.getAdapterType();

        InsurerPort adapter = adaptersByType.get(adapterType);
        if (adapter == null) {
            log.error("No adapter registered for type '{}' (insurer: {}, code: {})",
                    adapterType, insurerId, config.getInsurerCode());
            throw new InsurerNotFoundException(insurerId);
        }

        return adapter;
    }

    public InsurerPort resolveByCode(String insurerCode) {
        InsurerConfiguration config = insurerRegistry.getConfigurationByCode(insurerCode);
        String adapterType = config.getAdapterType();

        InsurerPort adapter = adaptersByType.get(adapterType);
        if (adapter == null) {
            throw new InsurerNotFoundException(insurerCode);
        }

        return adapter;
    }

    public boolean hasAdapter(String adapterType) {
        return adaptersByType.containsKey(adapterType);
    }
}
