package com.plum.endorsements.infrastructure.insurer.bajaj;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BajajAllianzXmlMapper {

    public String toXmlEnvelope(Map<String, Object> endorsementData) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        xml.append("xmlns:ws=\"http://bajajallianz.com/endorsement/ws\">\n");
        xml.append("  <soapenv:Header/>\n");
        xml.append("  <soapenv:Body>\n");
        xml.append("    <ws:SubmitEndorsement>\n");
        xml.append("      <ws:PolicyNumber>").append(esc(endorsementData.get("policy_id"))).append("</ws:PolicyNumber>\n");
        xml.append("      <ws:MemberCode>").append(esc(endorsementData.get("employee_id"))).append("</ws:MemberCode>\n");
        xml.append("      <ws:MemberName>").append(esc(endorsementData.get("employee_name"))).append("</ws:MemberName>\n");
        xml.append("      <ws:EndorsementType>").append(mapType(str(endorsementData.get("type")))).append("</ws:EndorsementType>\n");
        xml.append("      <ws:EffectiveDate>").append(esc(endorsementData.get("coverage_start_date"))).append("</ws:EffectiveDate>\n");
        xml.append("      <ws:SumInsured>").append(esc(endorsementData.get("premium_amount"))).append("</ws:SumInsured>\n");
        xml.append("      <ws:DateOfBirth>").append(esc(endorsementData.get("date_of_birth"))).append("</ws:DateOfBirth>\n");
        xml.append("      <ws:Gender>").append(esc(endorsementData.get("gender"))).append("</ws:Gender>\n");
        xml.append("      <ws:Relationship>").append(esc(endorsementData.get("relationship"))).append("</ws:Relationship>\n");
        xml.append("    </ws:SubmitEndorsement>\n");
        xml.append("  </soapenv:Body>\n");
        xml.append("</soapenv:Envelope>");
        return xml.toString();
    }

    public Map<String, Object> fromXmlResponse(String xmlResponse) {
        Map<String, Object> result = new HashMap<>();
        result.put("insurer_reference", extractTag(xmlResponse, "TransactionId"));
        result.put("status", extractTag(xmlResponse, "Status"));
        result.put("message", extractTag(xmlResponse, "Message"));
        return result;
    }

    public Map<String, Object> toInsurerFormat(Map<String, Object> endorsementData) {
        Map<String, Object> mapped = new HashMap<>();
        mapped.put("PolicyNumber", endorsementData.getOrDefault("policy_id", ""));
        mapped.put("MemberCode", endorsementData.getOrDefault("employee_id", ""));
        mapped.put("MemberName", endorsementData.getOrDefault("employee_name", ""));
        mapped.put("EndorsementType", mapType(str(endorsementData.get("type"))));
        mapped.put("EffectiveDate", endorsementData.getOrDefault("coverage_start_date", ""));
        mapped.put("SumInsured", endorsementData.getOrDefault("premium_amount", 0));
        mapped.put("DateOfBirth", endorsementData.getOrDefault("date_of_birth", ""));
        mapped.put("Gender", endorsementData.getOrDefault("gender", ""));
        mapped.put("Relationship", endorsementData.getOrDefault("relationship", ""));
        return mapped;
    }

    public Map<String, Object> fromInsurerFormat(Map<String, Object> insurerData) {
        Map<String, Object> mapped = new HashMap<>();
        mapped.put("policy_id", insurerData.getOrDefault("PolicyNumber", ""));
        mapped.put("employee_id", insurerData.getOrDefault("MemberCode", ""));
        mapped.put("employee_name", insurerData.getOrDefault("MemberName", ""));
        mapped.put("type", reverseMapType(str(insurerData.get("EndorsementType"))));
        mapped.put("coverage_start_date", insurerData.getOrDefault("EffectiveDate", ""));
        mapped.put("premium_amount", insurerData.getOrDefault("SumInsured", 0));
        mapped.put("insurer_reference", insurerData.getOrDefault("TransactionId", ""));
        return mapped;
    }

    private String mapType(String type) {
        return switch (type) {
            case "ADDITION" -> "ADD_MEMBER";
            case "DELETION" -> "DELETE_MEMBER";
            case "MODIFICATION" -> "MODIFY_MEMBER";
            case "CORRECTION" -> "CORRECT_MEMBER";
            default -> type;
        };
    }

    private String reverseMapType(String insurerType) {
        return switch (insurerType) {
            case "ADD_MEMBER" -> "ADDITION";
            case "DELETE_MEMBER" -> "DELETION";
            case "MODIFY_MEMBER" -> "MODIFICATION";
            case "CORRECT_MEMBER" -> "CORRECTION";
            default -> insurerType;
        };
    }

    private String extractTag(String xml, String tag) {
        String open = "<ws:" + tag + ">";
        String close = "</ws:" + tag + ">";
        int start = xml.indexOf(open);
        int end = xml.indexOf(close);
        if (start >= 0 && end >= 0) {
            return xml.substring(start + open.length(), end);
        }
        return "";
    }

    private String esc(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String str(Object value) {
        return value != null ? value.toString() : "";
    }
}
