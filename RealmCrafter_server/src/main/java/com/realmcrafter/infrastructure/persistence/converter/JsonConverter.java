package com.realmcrafter.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realmcrafter.domain.asset.dto.SettingContentDTO;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * SettingContentDTO <-> JSON 字符串 的 JPA 转换器。
 */
@Converter(autoApply = false)
public class JsonConverter implements AttributeConverter<SettingContentDTO, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(SettingContentDTO attribute) {
        try {
            return attribute == null ? null : objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    @Override
    public SettingContentDTO convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : objectMapper.readValue(dbData, SettingContentDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }
}

