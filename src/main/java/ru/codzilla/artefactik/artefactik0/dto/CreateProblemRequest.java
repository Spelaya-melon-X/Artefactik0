package ru.codzilla.artefactik.artefactik0.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateProblemRequest {

    @NotBlank(message = "name is required")
    private String name;

    private Integer timeLimit = 1000;
    private Integer memoryLimit = 256;

    private String statement;

    @NotBlank(message = "generatorCode is required")
    private String generatorCode;

    @NotEmpty(message = "inputs must not be empty")
    private List<String> inputs;
}