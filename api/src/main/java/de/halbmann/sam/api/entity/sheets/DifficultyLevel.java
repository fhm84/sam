package de.halbmann.sam.api.entity.sheets;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum DifficultyLevel {
    VERY_EASY(1),
    EASY(2),
    MEDIUM(3),
    ADVANCED(4),
    DIFFICULT(5),
    VERY_DIFFICULT(6);

    private final int grade;

    DifficultyLevel(int grade) {
        this.grade = grade;
    }

    public static DifficultyLevel fromGrade(int grade) {
        return Arrays.stream(values())
                .filter(l -> l.grade == grade)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid difficulty grade: " + grade));
    }
}
