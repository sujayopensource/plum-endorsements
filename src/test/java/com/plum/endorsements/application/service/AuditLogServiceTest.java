package com.plum.endorsements.application.service;

import com.plum.endorsements.infrastructure.persistence.entity.AuditLogEntity;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataAuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService")
class AuditLogServiceTest {

    @Mock private SpringDataAuditLogRepository repository;

    @InjectMocks
    private AuditLogService service;

    @Test
    @DisplayName("log saves audit entry with correct fields")
    void log_SavesCorrectEntry() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.log("CREATE", "Endorsement", "123", "admin", "{\"key\":\"value\"}");

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());

        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getEntityType()).isEqualTo("Endorsement");
        assertThat(saved.getEntityId()).isEqualTo("123");
        assertThat(saved.getActor()).isEqualTo("admin");
        assertThat(saved.getDetails()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    @DisplayName("log defaults actor to SYSTEM when null")
    void log_NullActor_DefaultsToSystem() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.log("CREATE", "Endorsement", "123", null, null);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("findAll returns paginated audit logs")
    void findAll_ReturnsPaginated() {
        AuditLogEntity entity = AuditLogEntity.builder()
                .id(UUID.randomUUID())
                .action("CREATE")
                .entityType("Endorsement")
                .entityId("123")
                .actor("SYSTEM")
                .createdAt(Instant.now())
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAllByOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = service.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("CREATE");
    }

    @Test
    @DisplayName("findByEntity returns filtered logs")
    void findByEntity_ReturnsFiltered() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findByEntityTypeAndEntityId("Endorsement", "123", pageable))
                .thenReturn(new PageImpl<>(List.of()));

        var result = service.findByEntity("Endorsement", "123", pageable);

        assertThat(result.getContent()).isEmpty();
        verify(repository).findByEntityTypeAndEntityId("Endorsement", "123", pageable);
    }

    @Test
    @DisplayName("findByAction returns filtered logs")
    void findByAction_ReturnsFiltered() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findByAction("CREATE", pageable))
                .thenReturn(new PageImpl<>(List.of()));

        var result = service.findByAction("CREATE", pageable);

        assertThat(result.getContent()).isEmpty();
        verify(repository).findByAction("CREATE", pageable);
    }
}
