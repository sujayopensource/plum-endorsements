package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.Endorsement;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BalanceForecastPort {

    ForecastResult generateForecast(UUID employerId, UUID insurerId, List<Endorsement> history);

    record ForecastResult(BigDecimal forecastedNeed, int daysAhead, BigDecimal dailyBurnRate,
                          BigDecimal shortfall, boolean topUpRequired, String narrative) {}
}
