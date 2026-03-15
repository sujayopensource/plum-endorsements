package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.EndorsementEvent;

public interface EventPublisher {
    void publish(EndorsementEvent event);
}
