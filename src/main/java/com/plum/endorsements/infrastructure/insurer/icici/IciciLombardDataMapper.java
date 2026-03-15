package com.plum.endorsements.infrastructure.insurer.icici;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class IciciLombardDataMapper {

    public Map<String, Object> toInsurerFormat(Map<String, Object> endorsementData) {
        Map<String, Object> mapped = new HashMap<>();
        mapped.put("memberName", endorsementData.getOrDefault("employee_name", ""));
        mapped.put("memberId", endorsementData.getOrDefault("employee_id", ""));
        mapped.put("policyNumber", endorsementData.getOrDefault("policy_id", ""));
        mapped.put("endorsementType", mapEndorsementType(
                (String) endorsementData.getOrDefault("type", "ADDITION")));
        mapped.put("effectiveDate", endorsementData.getOrDefault("coverage_start_date", ""));
        mapped.put("sumInsured", endorsementData.getOrDefault("premium_amount", 0));
        mapped.put("dateOfBirth", endorsementData.getOrDefault("date_of_birth", ""));
        mapped.put("gender", endorsementData.getOrDefault("gender", ""));
        mapped.put("relationship", endorsementData.getOrDefault("relationship", "SELF"));
        return mapped;
    }

    public Map<String, Object> fromInsurerFormat(Map<String, Object> insurerData) {
        Map<String, Object> mapped = new HashMap<>();
        mapped.put("employee_name", insurerData.getOrDefault("memberName", ""));
        mapped.put("employee_id", insurerData.getOrDefault("memberId", ""));
        mapped.put("policy_id", insurerData.getOrDefault("policyNumber", ""));
        mapped.put("type", reverseMapEndorsementType(
                (String) insurerData.getOrDefault("endorsementType", "ADD")));
        mapped.put("coverage_start_date", insurerData.getOrDefault("effectiveDate", ""));
        mapped.put("premium_amount", insurerData.getOrDefault("sumInsured", 0));
        mapped.put("insurer_reference", insurerData.getOrDefault("transactionId", ""));
        return mapped;
    }

    private String mapEndorsementType(String type) {
        return switch (type) {
            case "ADDITION" -> "ADD";
            case "DELETION" -> "DELETE";
            case "MODIFICATION" -> "MODIFY";
            case "CORRECTION" -> "CORRECT";
            default -> type;
        };
    }

    private String reverseMapEndorsementType(String insurerType) {
        return switch (insurerType) {
            case "ADD" -> "ADDITION";
            case "DELETE" -> "DELETION";
            case "MODIFY" -> "MODIFICATION";
            case "CORRECT" -> "CORRECTION";
            default -> insurerType;
        };
    }
}
