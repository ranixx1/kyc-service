package com.example.kyc_service.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

/**
 * JPA converter that serializes Map<String, String> to a JSON string for storage,
 * and deserializes it back on read.
 *
 * Used by KycSubmission.extractedFields to avoid a separate join table.
 */
@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE =
            new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return null;
        }
    }
}