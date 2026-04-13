package de.halbmann.sam.business.sheets.entity;

import de.halbmann.sam.api.entity.sheets.DifficultyLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DifficultyLevelConverter implements AttributeConverter<DifficultyLevel, Short> {

    @Override
    public Short convertToDatabaseColumn(DifficultyLevel level) {
        return level == null ? null : (short) level.getGrade();
    }

    @Override
    public DifficultyLevel convertToEntityAttribute(Short grade) {
        return grade == null ? null : DifficultyLevel.fromGrade(grade);
    }
}
