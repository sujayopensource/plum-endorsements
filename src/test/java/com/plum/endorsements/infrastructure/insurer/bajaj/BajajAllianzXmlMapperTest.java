package com.plum.endorsements.infrastructure.insurer.bajaj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BajajAllianzXmlMapper")
class BajajAllianzXmlMapperTest {

    private BajajAllianzXmlMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BajajAllianzXmlMapper();
    }

    @Test
    @DisplayName("toXmlEnvelope generates valid SOAP XML")
    void toXmlEnvelope_GeneratesValidXml() {
        Map<String, Object> data = Map.of(
                "policy_id", "POL-123",
                "employee_id", "EMP-001",
                "employee_name", "John Doe",
                "type", "ADDITION",
                "coverage_start_date", "2026-04-01",
                "premium_amount", 50000
        );

        String xml = mapper.toXmlEnvelope(data);

        assertThat(xml).contains("soapenv:Envelope");
        assertThat(xml).contains("ws:SubmitEndorsement");
        assertThat(xml).contains("<ws:PolicyNumber>POL-123</ws:PolicyNumber>");
        assertThat(xml).contains("<ws:MemberCode>EMP-001</ws:MemberCode>");
        assertThat(xml).contains("<ws:MemberName>John Doe</ws:MemberName>");
        assertThat(xml).contains("<ws:EndorsementType>ADD_MEMBER</ws:EndorsementType>");
    }

    @Test
    @DisplayName("toXmlEnvelope escapes XML special characters")
    void toXmlEnvelope_EscapesSpecialChars() {
        Map<String, Object> data = Map.of(
                "employee_name", "O'Brien & Sons <Ltd>",
                "type", "ADDITION"
        );

        String xml = mapper.toXmlEnvelope(data);

        assertThat(xml).contains("O'Brien &amp; Sons &lt;Ltd&gt;");
        assertThat(xml).doesNotContain("O'Brien & Sons <Ltd>");
    }

    @Test
    @DisplayName("fromXmlResponse extracts transaction ID and status")
    void fromXmlResponse_ExtractsFields() {
        String xmlResponse = """
                <soapenv:Envelope>
                  <soapenv:Body>
                    <ws:SubmitEndorsementResponse>
                      <ws:TransactionId>BAJAJ-12345678</ws:TransactionId>
                      <ws:Status>ACCEPTED</ws:Status>
                      <ws:Message>Endorsement processed successfully</ws:Message>
                    </ws:SubmitEndorsementResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """;

        Map<String, Object> result = mapper.fromXmlResponse(xmlResponse);

        assertThat(result.get("insurer_reference")).isEqualTo("BAJAJ-12345678");
        assertThat(result.get("status")).isEqualTo("ACCEPTED");
        assertThat(result.get("message")).isEqualTo("Endorsement processed successfully");
    }

    @Test
    @DisplayName("toInsurerFormat maps field names correctly")
    void toInsurerFormat_MapsFields() {
        Map<String, Object> input = Map.of(
                "policy_id", "POL-456",
                "employee_id", "EMP-002",
                "employee_name", "Jane Doe",
                "type", "DELETION",
                "coverage_start_date", "2026-05-01",
                "premium_amount", 30000
        );

        Map<String, Object> result = mapper.toInsurerFormat(input);

        assertThat(result.get("PolicyNumber")).isEqualTo("POL-456");
        assertThat(result.get("MemberCode")).isEqualTo("EMP-002");
        assertThat(result.get("EndorsementType")).isEqualTo("DELETE_MEMBER");
    }

    @Test
    @DisplayName("fromInsurerFormat maps back to Plum format")
    void fromInsurerFormat_MapsBack() {
        Map<String, Object> input = Map.of(
                "PolicyNumber", "POL-789",
                "MemberCode", "EMP-003",
                "EndorsementType", "MODIFY_MEMBER",
                "TransactionId", "BAJAJ-ABCDEF12"
        );

        Map<String, Object> result = mapper.fromInsurerFormat(input);

        assertThat(result.get("policy_id")).isEqualTo("POL-789");
        assertThat(result.get("employee_id")).isEqualTo("EMP-003");
        assertThat(result.get("type")).isEqualTo("MODIFICATION");
        assertThat(result.get("insurer_reference")).isEqualTo("BAJAJ-ABCDEF12");
    }

    @Test
    @DisplayName("roundtrip preserves data through toInsurerFormat/fromInsurerFormat")
    void roundtrip_PreservesData() {
        Map<String, Object> original = Map.of(
                "policy_id", "POL-100",
                "employee_id", "EMP-50",
                "employee_name", "Roundtrip User",
                "type", "CORRECTION",
                "coverage_start_date", "2026-06-15",
                "premium_amount", 80000
        );

        Map<String, Object> insurerFormat = mapper.toInsurerFormat(original);
        Map<String, Object> backToPlum = mapper.fromInsurerFormat(insurerFormat);

        assertThat(backToPlum.get("policy_id")).isEqualTo("POL-100");
        assertThat(backToPlum.get("employee_id")).isEqualTo("EMP-50");
        assertThat(backToPlum.get("type")).isEqualTo("CORRECTION");
    }
}
