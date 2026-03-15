package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.port.ErrorResolutionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@ConditionalOnProperty(name = "endorsement.intelligence.ollama.enabled", havingValue = "false", matchIfMissing = true)
public class SimulatedErrorResolver implements ErrorResolutionPort {

    private static final Map<UUID, String> INSURER_DATE_FORMATS = Map.of();

    @Override
    public ResolutionSuggestion analyzeError(Endorsement endorsement, String errorMessage, UUID insurerId) {
        log.info("Analyzing error for endorsement {}, insurer {}: '{}'",
                endorsement.getId(), insurerId, errorMessage);

        // Simulate RAG retrieval + LLM inference latency
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        String lowerError = errorMessage.toLowerCase();

        // 1. Member ID format errors (check before date format to avoid "format" false match)
        if (lowerError.contains("member") || lowerError.contains("id format") || lowerError.contains("invalid id")) {
            ResolutionSuggestion result = resolveMemberIdError(endorsement, errorMessage, insurerId);
            log.info("Matched MEMBER_ID_FORMAT pattern for endorsement {}: '{}' -> '{}' (confidence: {})",
                    endorsement.getId(), result.originalValue(), result.correctedValue(), result.confidence());
            return result;
        }

        // 2. Date format errors
        if (lowerError.contains("date") || lowerError.contains("dob") || lowerError.contains("format")) {
            ResolutionSuggestion result = resolveDateFormatError(endorsement, errorMessage, insurerId);
            log.info("Matched DATE_FORMAT pattern for endorsement {}: '{}' -> '{}' (confidence: {})",
                    endorsement.getId(), result.originalValue(), result.correctedValue(), result.confidence());
            return result;
        }

        // 3. Missing field errors
        if (lowerError.contains("required") || lowerError.contains("missing") || lowerError.contains("mandatory")) {
            ResolutionSuggestion result = resolveMissingFieldError(endorsement, errorMessage, insurerId);
            log.info("Matched MISSING_FIELD pattern for endorsement {}: field='{}', default='{}' (confidence: {})",
                    endorsement.getId(), result.originalValue(), result.correctedValue(), result.confidence());
            return result;
        }

        // 4. Premium mismatch errors
        if (lowerError.contains("premium") || lowerError.contains("mismatch") || lowerError.contains("amount")) {
            ResolutionSuggestion result = resolvePremiumMismatchError(endorsement, errorMessage, insurerId);
            log.info("Matched PREMIUM_MISMATCH pattern for endorsement {}: ₹{} -> ₹{} (confidence: {})",
                    endorsement.getId(), result.originalValue(), result.correctedValue(), result.confidence());
            return result;
        }

        // 5. Unknown errors
        log.warn("No pattern matched for endorsement {} error: '{}'. Returning low-confidence suggestion.",
                endorsement.getId(), errorMessage);
        return new ResolutionSuggestion(
                "UNKNOWN_ERROR",
                errorMessage,
                null,
                String.format("Unable to automatically resolve error: '%s'. " +
                        "Manual review recommended. Error does not match any known patterns.", errorMessage),
                0.3
        );
    }

    private ResolutionSuggestion resolveDateFormatError(Endorsement endorsement, String errorMessage, UUID insurerId) {
        // Detect and fix date format based on insurer preference
        String original = extractDateFromError(errorMessage);
        String corrected = convertToIsoDate(original);

        String explanation = String.format(
                "Error '%s' matched pattern 'date_format_mismatch'. " +
                "Insurer expects YYYY-MM-DD format. Original: '%s'. Corrected: '%s'. Confidence: 98%%.",
                errorMessage, original, corrected);

        return new ResolutionSuggestion("DATE_FORMAT", original, corrected, explanation, 0.98);
    }

    private ResolutionSuggestion resolveMissingFieldError(Endorsement endorsement, String errorMessage, UUID insurerId) {
        String fieldName = extractFieldName(errorMessage);
        String defaultValue = getDefaultForField(fieldName, endorsement);

        String explanation = String.format(
                "Error '%s' matched pattern 'required_field_missing'. " +
                "Field '%s' is required. Suggested default: '%s' based on employer's other endorsements. Confidence: 90%%.",
                errorMessage, fieldName, defaultValue);

        return new ResolutionSuggestion("MISSING_FIELD", fieldName, defaultValue, explanation, 0.90);
    }

    private ResolutionSuggestion resolveMemberIdError(Endorsement endorsement, String errorMessage, UUID insurerId) {
        String original = endorsement.getEmployeeId() != null ? endorsement.getEmployeeId().toString() : "";
        // Apply insurer-specific ID format (prefix + truncation)
        String corrected = "PLM-" + original.substring(0, Math.min(8, original.length())).toUpperCase();

        String explanation = String.format(
                "Error '%s' matched pattern 'invalid_member_id'. " +
                "Insurer requires PLM- prefix with 8-char ID. Original: '%s'. Corrected: '%s'. Confidence: 96%%.",
                errorMessage, original, corrected);

        return new ResolutionSuggestion("MEMBER_ID_FORMAT", original, corrected, explanation, 0.96);
    }

    private ResolutionSuggestion resolvePremiumMismatchError(Endorsement endorsement, String errorMessage, UUID insurerId) {
        String original = endorsement.getPremiumAmount() != null
                ? endorsement.getPremiumAmount().toPlainString() : "0";
        // Simulated recalculation — typically involves sum-insured table lookup
        String corrected = endorsement.getPremiumAmount() != null
                ? endorsement.getPremiumAmount().multiply(java.math.BigDecimal.valueOf(1.05)).toPlainString() : "0";

        String explanation = String.format(
                "Error '%s' matched pattern 'premium_mismatch'. " +
                "Premium recalculated based on insurer's sum-insured table. " +
                "Original: ₹%s. Recalculated: ₹%s. Confidence: 85%%.",
                errorMessage, original, corrected);

        return new ResolutionSuggestion("PREMIUM_MISMATCH", original, corrected, explanation, 0.85);
    }

    private String extractDateFromError(String errorMessage) {
        Pattern datePattern = Pattern.compile("\\d{2}[-/]\\d{2}[-/]\\d{4}");
        Matcher matcher = datePattern.matcher(errorMessage);
        if (matcher.find()) {
            return matcher.group();
        }
        return "07-03-1990"; // Simulated fallback
    }

    private String convertToIsoDate(String dateStr) {
        if (dateStr.contains("/")) {
            String[] parts = dateStr.split("/");
            if (parts.length == 3) {
                return parts[2] + "-" + parts[1] + "-" + parts[0];
            }
        }
        if (dateStr.matches("\\d{2}-\\d{2}-\\d{4}")) {
            String[] parts = dateStr.split("-");
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }
        return dateStr;
    }

    private String extractFieldName(String errorMessage) {
        if (errorMessage.toLowerCase().contains("email")) return "email";
        if (errorMessage.toLowerCase().contains("phone")) return "phone";
        if (errorMessage.toLowerCase().contains("address")) return "address";
        if (errorMessage.toLowerCase().contains("gender")) return "gender";
        return "contact_number";
    }

    private String getDefaultForField(String fieldName, Endorsement endorsement) {
        return switch (fieldName) {
            case "email" -> "not-provided@employer.com";
            case "phone" -> "0000000000";
            case "address" -> "On file with employer";
            case "gender" -> "Not Specified";
            default -> "N/A";
        };
    }
}
