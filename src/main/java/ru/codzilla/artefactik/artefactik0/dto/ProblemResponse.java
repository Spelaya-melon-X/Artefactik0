package ru.codzilla.artefactik.artefactik0.dto;

import ru.codzilla.artefactik.artefactik0.repository.Problem;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProblemResponse {

    private Long id;
    private String name;
    private Integer timeLimit;
    private Integer memoryLimit;
    private boolean hasStatement;
    private int testCount;
    private LocalDateTime createdAt;
    private String complexity;
    private String taskType;

    public static ProblemResponse from(Problem problem, int testCount) {
        ProblemResponse r = new ProblemResponse();
        r.setId(problem.getId());
        r.setName(problem.getName());
        r.setTimeLimit(problem.getTimeLimit());
        r.setMemoryLimit(problem.getMemoryLimit());
        r.setHasStatement(problem.getStatementKey() != null);
        r.setComplexity(String.valueOf(problem.getComplexity()));
        r.setTaskType(String.valueOf(problem.getTaskType()));
        r.setTestCount(testCount);
        r.setCreatedAt(problem.getCreatedAt());
        return r;
    }
}
