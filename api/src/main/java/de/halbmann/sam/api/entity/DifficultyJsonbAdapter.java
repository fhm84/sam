package de.halbmann.sam.api.entity;

import jakarta.json.bind.adapter.JsonbAdapter;

public class DifficultyJsonbAdapter implements JsonbAdapter<DifficultyLevel, Integer> {

    @Override
    public Integer adaptToJson(DifficultyLevel level) {
        return level == null ? null : level.getGrade();
    }

    @Override
    public DifficultyLevel adaptFromJson(Integer grade) {
        return grade == null ? null : DifficultyLevel.fromGrade(grade);
    }
}
