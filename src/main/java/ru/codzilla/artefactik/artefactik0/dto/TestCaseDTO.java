package ru.codzilla.artefactik.artefactik0.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseDTO {
    private int testIndex;
    private String input;
    private String output;
}
