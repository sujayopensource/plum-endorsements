package com.plum.endorsements.infrastructure.insurer.nivabupa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NivaBupaCsvMapper")
class NivaBupaCsvMapperTest {

    private NivaBupaCsvMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NivaBupaCsvMapper();
    }

    @Test
    @DisplayName("toCsvRow generates correct CSV format")
    void toCsvRow_GeneratesCorrectFormat() {
        Map<String, Object> data = Map.of(
                "policy_id", "POL-123",
                "employee_id", "EMP-001",
                "employee_name", "John Doe",
                "date_of_birth", "1990-01-15",
                "gender", "M",
                "relationship", "SELF",
                "type", "ADDITION",
                "coverage_start_date", "2026-04-01",
                "premium_amount", 50000
        );

        String csv = mapper.toCsvRow(data);

        assertThat(csv).contains("\"POL-123\"");
        assertThat(csv).contains("\"EMP-001\"");
        assertThat(csv).contains("\"John Doe\"");
        assertThat(csv).contains("\"A\""); // ADDITION -> A
    }

    @Test
    @DisplayName("toCsvBatch includes headers and all rows")
    void toCsvBatch_IncludesHeadersAndRows() {
        List<Map<String, Object>> endorsements = List.of(
                Map.of("policy_id", "POL-1", "employee_id", "EMP-1", "employee_name", "User 1",
                        "type", "ADDITION", "coverage_start_date", "2026-04-01",
                        "premium_amount", 50000, "date_of_birth", "1990-01-01",
                        "gender", "M", "relationship", "SELF"),
                Map.of("policy_id", "POL-2", "employee_id", "EMP-2", "employee_name", "User 2",
                        "type", "DELETION", "coverage_start_date", "2026-04-01",
                        "premium_amount", 30000, "date_of_birth", "1985-06-15",
                        "gender", "F", "relationship", "SPOUSE")
        );

        String csv = mapper.toCsvBatch(endorsements);
        String[] lines = csv.split("\n");

        assertThat(lines).hasSize(3); // header + 2 rows
        assertThat(lines[0]).isEqualTo("PolicyNo,MemberID,MemberName,DateOfBirth,Gender,Relationship,EndorsementType,EffectiveDate,SumInsured");
        assertThat(lines[1]).contains("\"A\"");
        assertThat(lines[2]).contains("\"D\"");
    }

    @Test
    @DisplayName("fromCsvRow parses CSV correctly")
    void fromCsvRow_ParsesCorrectly() {
        String csv = "\"POL-123\",\"EMP-001\",\"John Doe\",\"1990-01-15\",\"M\",\"SELF\",\"A\",\"2026-04-01\",\"50000\"";

        Map<String, Object> result = mapper.fromCsvRow(csv);

        assertThat(result.get("policy_id")).isEqualTo("POL-123");
        assertThat(result.get("employee_id")).isEqualTo("EMP-001");
        assertThat(result.get("employee_name")).isEqualTo("John Doe");
        assertThat(result.get("type")).isEqualTo("ADDITION");
    }

    @Test
    @DisplayName("endorsement type mapping is correct for all types")
    void endorsementTypeMapping_AllTypes() {
        assertThat(mapper.toCsvRow(Map.of("type", "ADDITION", "policy_id", "", "employee_id", "",
                "employee_name", "", "date_of_birth", "", "gender", "", "relationship", "",
                "coverage_start_date", "", "premium_amount", 0))).contains("\"A\"");
        assertThat(mapper.toCsvRow(Map.of("type", "DELETION", "policy_id", "", "employee_id", "",
                "employee_name", "", "date_of_birth", "", "gender", "", "relationship", "",
                "coverage_start_date", "", "premium_amount", 0))).contains("\"D\"");
        assertThat(mapper.toCsvRow(Map.of("type", "MODIFICATION", "policy_id", "", "employee_id", "",
                "employee_name", "", "date_of_birth", "", "gender", "", "relationship", "",
                "coverage_start_date", "", "premium_amount", 0))).contains("\"M\"");
    }

    @Test
    @DisplayName("roundtrip preserves essential data")
    void roundtrip_PreservesData() {
        Map<String, Object> original = Map.of(
                "policy_id", "POL-999",
                "employee_id", "EMP-100",
                "employee_name", "Test User",
                "type", "DELETION",
                "coverage_start_date", "2026-05-01",
                "premium_amount", 75000,
                "date_of_birth", "1995-03-20",
                "gender", "F",
                "relationship", "SELF"
        );

        String csv = mapper.toCsvRow(original);
        Map<String, Object> parsed = mapper.fromCsvRow(csv);

        assertThat(parsed.get("policy_id")).isEqualTo("POL-999");
        assertThat(parsed.get("employee_name")).isEqualTo("Test User");
        assertThat(parsed.get("type")).isEqualTo("DELETION");
    }
}
