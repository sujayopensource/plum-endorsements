package com.plum.endorsements.infrastructure.insurer.icici;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IciciLombardDataMapper")
class IciciLombardDataMapperTest {

    private IciciLombardDataMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new IciciLombardDataMapper();
    }

    @Test
    @DisplayName("toInsurerFormat maps employee_name to memberName")
    void toInsurerFormat_MapsFieldNames() {
        Map<String, Object> input = Map.of(
                "employee_name", "John Doe",
                "employee_id", "EMP-001",
                "policy_id", "POL-123",
                "type", "ADDITION",
                "coverage_start_date", "2026-04-01",
                "premium_amount", 50000
        );

        Map<String, Object> result = mapper.toInsurerFormat(input);

        assertThat(result.get("memberName")).isEqualTo("John Doe");
        assertThat(result.get("memberId")).isEqualTo("EMP-001");
        assertThat(result.get("policyNumber")).isEqualTo("POL-123");
        assertThat(result.get("endorsementType")).isEqualTo("ADD");
        assertThat(result.get("effectiveDate")).isEqualTo("2026-04-01");
        assertThat(result.get("sumInsured")).isEqualTo(50000);
    }

    @Test
    @DisplayName("toInsurerFormat maps DELETION type correctly")
    void toInsurerFormat_MapsDeletionType() {
        Map<String, Object> input = Map.of("type", "DELETION");

        Map<String, Object> result = mapper.toInsurerFormat(input);

        assertThat(result.get("endorsementType")).isEqualTo("DELETE");
    }

    @Test
    @DisplayName("toInsurerFormat maps MODIFICATION type correctly")
    void toInsurerFormat_MapsModificationType() {
        Map<String, Object> input = Map.of("type", "MODIFICATION");

        Map<String, Object> result = mapper.toInsurerFormat(input);

        assertThat(result.get("endorsementType")).isEqualTo("MODIFY");
    }

    @Test
    @DisplayName("fromInsurerFormat maps memberName to employee_name")
    void fromInsurerFormat_MapsFieldNames() {
        Map<String, Object> input = Map.of(
                "memberName", "Jane Doe",
                "memberId", "EMP-002",
                "policyNumber", "POL-456",
                "endorsementType", "ADD",
                "effectiveDate", "2026-05-01",
                "sumInsured", 75000,
                "transactionId", "ICICI-12345678"
        );

        Map<String, Object> result = mapper.fromInsurerFormat(input);

        assertThat(result.get("employee_name")).isEqualTo("Jane Doe");
        assertThat(result.get("employee_id")).isEqualTo("EMP-002");
        assertThat(result.get("policy_id")).isEqualTo("POL-456");
        assertThat(result.get("type")).isEqualTo("ADDITION");
        assertThat(result.get("insurer_reference")).isEqualTo("ICICI-12345678");
    }

    @Test
    @DisplayName("roundtrip mapping preserves data")
    void roundtrip_PreservesData() {
        Map<String, Object> original = Map.of(
                "employee_name", "Test User",
                "employee_id", "EMP-100",
                "policy_id", "POL-200",
                "type", "ADDITION",
                "coverage_start_date", "2026-06-01",
                "premium_amount", 100000
        );

        Map<String, Object> insurerFormat = mapper.toInsurerFormat(original);
        Map<String, Object> backToPlum = mapper.fromInsurerFormat(insurerFormat);

        assertThat(backToPlum.get("employee_name")).isEqualTo("Test User");
        assertThat(backToPlum.get("employee_id")).isEqualTo("EMP-100");
        assertThat(backToPlum.get("policy_id")).isEqualTo("POL-200");
        assertThat(backToPlum.get("type")).isEqualTo("ADDITION");
    }
}
