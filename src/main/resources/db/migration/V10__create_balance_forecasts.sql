-- Phase 3: Balance Forecast table
CREATE TABLE balance_forecasts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employer_id       UUID NOT NULL,
    insurer_id        UUID NOT NULL,
    forecast_date     DATE NOT NULL,
    forecasted_amount DECIMAL(12,2) NOT NULL,
    actual_amount     DECIMAL(12,2),
    accuracy          DECIMAL(5,4),
    narrative         TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_forecast_employer ON balance_forecasts(employer_id);
CREATE INDEX idx_forecast_employer_insurer ON balance_forecasts(employer_id, insurer_id);
CREATE INDEX idx_forecast_date ON balance_forecasts(forecast_date);
