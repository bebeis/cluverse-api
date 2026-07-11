package cluverse.recruitment.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cluverse.recruitment.exception.RecruitmentExceptionMessage;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class RecruitmentPositionListConverter implements AttributeConverter<List<RecruitmentPosition>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<RecruitmentPosition>> TYPE_REFERENCE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<RecruitmentPosition> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(RecruitmentExceptionMessage.RECRUITMENT_POSITION_SERIALIZE_FAILED.getMessage(), exception);
        }
    }

    @Override
    public List<RecruitmentPosition> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) {
                return List.of();
            }
            return OBJECT_MAPPER.readValue(dbData, TYPE_REFERENCE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(RecruitmentExceptionMessage.RECRUITMENT_POSITION_DESERIALIZE_FAILED.getMessage(), exception);
        }
    }
}
