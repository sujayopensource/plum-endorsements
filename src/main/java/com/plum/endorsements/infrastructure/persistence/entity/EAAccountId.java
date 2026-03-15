package com.plum.endorsements.infrastructure.persistence.entity;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class EAAccountId implements Serializable {
    private UUID employerId;
    private UUID insurerId;
}
