package ru.codzilla.artefactik.artefactik0.service;

import ru.codzilla.artefactik.artefactik0.dto.CreateProblemRequest;
import ru.codzilla.artefactik.artefactik0.dto.ProblemResponse;
import ru.codzilla.artefactik.artefactik0.dto.TestCaseDTO;
import ru.codzilla.artefactik.artefactik0.repository.ProblemTest;
import ru.codzilla.artefactik.artefactik0.repository.ProblemTestRepository;
import ru.codzilla.artefactik.artefactik0.repository.Problem;
import ru.codzilla.artefactik.artefactik0.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemTestRepository problemTestRepository;
    private final MinioStorageService storageService;
    private final GeneratorService generatorService;
    @Transactional
    public ProblemResponse createProblem(CreateProblemRequest request) {
        Problem problem = new Problem();
        problem.setName(request.getName());
        problem.setTimeLimit(request.getTimeLimit() != null ? request.getTimeLimit() : 1000);
        problem.setMemoryLimit(request.getMemoryLimit() != null ? request.getMemoryLimit() : 256);
        problem.setComplexity(request.getComplexity());
        problem.setTaskType(request.getTaskType() != null
                ? request.getTaskType()
                : Problem.TaskType.ALGORITHM);
        Problem saved = problemRepository.save(problem);
        Long problemId = saved.getId();

        String genKey = MinioStorageService.generatorKey(problemId);
        storageService.save(genKey, request.getGeneratorCode());
        saved.setGeneratorKey(genKey);

        if (request.getStatement() != null && !request.getStatement().isBlank()) {
            String stmtKey = MinioStorageService.statementKey(problemId);
            storageService.save(stmtKey, request.getStatement());
            saved.setStatementKey(stmtKey);
        }
        saved = problemRepository.save(saved);

        List<String> inputs = request.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            int idx = i + 1;
            String output = generatorService.executeGenerator(request.getGeneratorCode(), input);

            String inKey  = MinioStorageService.inputKey(problemId, idx);
            String outKey = MinioStorageService.outputKey(problemId, idx);
            storageService.save(inKey, input);
            storageService.save(outKey, output);

            ProblemTest pt = new ProblemTest();
            pt.setProblemId(problemId);
            pt.setTestIndex(idx);
            pt.setInputKey(inKey);
            pt.setOutputKey(outKey);
            problemTestRepository.save(pt);
        }

        int testCount = problemTestRepository.findAllByProblemIdOrderByTestIndex(problemId).size();
        return ProblemResponse.from(saved, testCount);
    }

    @Transactional(readOnly = true)
    public ProblemResponse getProblem(Long id) {
        Problem problem = findOrThrow(id);
        int testCount = problemTestRepository.findAllByProblemIdOrderByTestIndex(id).size();
        return ProblemResponse.from(problem, testCount);
    }

    @Transactional(readOnly = true)
    public List<TestCaseDTO> getTests(Long problemId) {
        findOrThrow(problemId);

        return problemTestRepository
                .findAllByProblemIdOrderByTestIndex(problemId)
                .stream()
                .map(t -> new TestCaseDTO(
                        t.getTestIndex(),
                        storageService.get(t.getInputKey()),
                        storageService.get(t.getOutputKey())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public String getStatement(Long problemId) {
        Problem problem = findOrThrow(problemId);
        if (problem.getStatementKey() == null) {
            throw new RuntimeException("No statement for problem: " + problemId);
        }
        return storageService.get(problem.getStatementKey());
    }

    @Transactional(readOnly = true)
    public List<Problem> getAllTaskWithComplexity(Problem.TaskComplexity complexity) {
        return problemRepository.findAllByComplexity(complexity);
    }

    public void updateTestOutput(Long problemId, int testIndex, String output) {
        findOrThrow(problemId);
        ProblemTest test = problemTestRepository
                .findAllByProblemIdOrderByTestIndex(problemId)
                .stream()
                .filter(t -> t.getTestIndex() == testIndex)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Test " + testIndex + " not found for problem " + problemId));
        storageService.save(test.getOutputKey(), output);
        log.info("Output updated — problem {} test {}", problemId, testIndex);
    }

    private Problem findOrThrow(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found: " + id));
    }


    @Transactional(readOnly = true)
    public Problem getRandomProblemByComplexityAndType(Problem.TaskComplexity complexity, String type) {
        Problem.TaskType taskType;
        try {
            taskType = Problem.TaskType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            taskType = Problem.TaskType.ALGORITHM;
        }

        Problem.TaskType finalTaskType = taskType;
        return problemRepository.findRandomByTaskTypeAndComplexity(
                        taskType.name(), complexity.name())
                .or(() -> {
                    log.warn("No {} problem with complexity {}, trying any complexity",
                            finalTaskType, complexity);
                    return problemRepository.findRandomByTaskType(finalTaskType.name());
                })
                .or(() -> {
                    log.warn("No {} problems at all, falling back to any problem", finalTaskType);
                    return problemRepository.findRandomByComplexity(complexity.name());
                })
                .orElseThrow(() -> new RuntimeException(
                        "No problems found for type=" + type + " complexity=" + complexity));
    }
}
