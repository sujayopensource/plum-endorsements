package com.plum.endorsements.infrastructure.insurer.nivabupa;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NivaBupaCsvMapper {

    private static final String[] CSV_HEADERS = {
            "PolicyNo", "MemberID", "MemberName", "DateOfBirth", "Gender",
            "Relationship", "EndorsementType", "EffectiveDate", "SumInsured"
    };

    public String toCsvRow(Map<String, Object> endorsementData) {
        return String.join(",",
                quote(str(endorsementData.get("policy_id"))),
                quote(str(endorsementData.get("employee_id"))),
                quote(str(endorsementData.get("employee_name"))),
                quote(str(endorsementData.get("date_of_birth"))),
                quote(str(endorsementData.get("gender"))),
                quote(str(endorsementData.get("relationship"))),
                quote(mapEndorsementType(str(endorsementData.get("type")))),
                quote(str(endorsementData.get("coverage_start_date"))),
                quote(str(endorsementData.get("premium_amount")))
        );
    }

    public String toCsvBatch(List<Map<String, Object>> endorsements) {
        String header = String.join(",", CSV_HEADERS);
        String rows = endorsements.stream()
                .map(this::toCsvRow)
                .collect(Collectors.joining("\n"));
        return header + "\n" + rows;
    }

    public Map<String, Object> fromCsvRow(String csvRow) {
        String[] parts = csvRow.split(",");
        Map<String, Object> mapped = new HashMap<>();
        if (parts.length >= 9) {
            mapped.put("policy_id", unquote(parts[0]));
            mapped.put("employee_id", unquote(parts[1]));
            mapped.put("employee_name", unquote(parts[2]));
            mapped.put("date_of_birth", unquote(parts[3]));
            mapped.put("gender", unquote(parts[4]));
            mapped.put("relationship", unquote(parts[5]));
            mapped.put("type", reverseMapEndorsementType(unquote(parts[6])));
            mapped.put("coverage_start_date", unquote(parts[7]));
            mapped.put("premium_amount", unquote(parts[8]));
        }
        return mapped;
    }

    private String mapEndorsementType(String type) {
        return switch (type) {
            case "ADDITION" -> "A";
            case "DELETION" -> "D";
            case "MODIFICATION" -> "M";
            case "CORRECTION" -> "C";
            default -> type;
        };
    }

    private String reverseMapEndorsementType(String code) {
        return switch (code) {
            case "A" -> "ADDITION";
            case "D" -> "DELETION";
            case "M" -> "MODIFICATION";
            case "C" -> "CORRECTION";
            default -> code;
        };
    }

    private String quote(String value) {
        return "\"" + (value != null ? value : "") + "\"";
    }

    private String unquote(String value) {
        if (value == null) return "";
        return value.replaceAll("^\"|\"$", "").trim();
    }

    private String str(Object value) {
        return value != null ? value.toString() : "";
    }
}
