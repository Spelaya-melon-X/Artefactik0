package ru.codzilla.artefactik.artefactik0.controller;

import ru.codzilla.artefactik.artefactik0.dto.CreateProblemRequest;
import ru.codzilla.artefactik.artefactik0.dto.ProblemResponse;
import ru.codzilla.artefactik.artefactik0.dto.TestCaseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.codzilla.artefactik.artefactik0.repository.Problem;
import ru.codzilla.artefactik.artefactik0.repository.ProblemRepository;
import ru.codzilla.artefactik.artefactik0.repository.ProblemTestRepository;
import ru.codzilla.artefactik.artefactik0.service.ProblemService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;
    private final ProblemTestRepository problemTestRepository;
    private final ProblemRepository problemRepository;


    @PostMapping
    public ResponseEntity<ProblemResponse> createProblem(
            @Valid @RequestBody CreateProblemRequest request) {
        log.info("POST /api/problems — name={}", request.getName());
        return ResponseEntity.ok(problemService.createProblem(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemResponse> getProblem(@PathVariable Long id) {
        return ResponseEntity.ok(problemService.getProblem(id));
    }


    @GetMapping("/{id}/tests")
    public ResponseEntity<List<TestCaseDTO>> getTests(@PathVariable Long id) {
        return ResponseEntity.ok(problemService.getTests(id));
    }


    @GetMapping("/{id}/statement")
    public ResponseEntity<String> getStatement(@PathVariable Long id) {
        return ResponseEntity.ok(problemService.getStatement(id));
    }

    @PatchMapping("/{id}/tests/{testIndex}/output")
    public ResponseEntity<Void> updateTestOutput(
            @PathVariable Long id,
            @PathVariable int testIndex,
            @RequestBody String output) {
        problemService.updateTestOutput(id, testIndex, output);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ProblemResponse> getProblemsByComplexity(
            @RequestParam("complexity") Problem.TaskComplexity complexity) {
        return problemService.getAllTaskWithComplexity(complexity)
                .stream()
                .map(p -> ProblemResponse.from(p,
                        problemTestRepository.findAllByProblemIdOrderByTestIndex(p.getId()).size()))
                .toList();
    }

    @GetMapping(params = "taskType")
    public ResponseEntity<List<ProblemResponse>> getProblemsByTaskType(
            @RequestParam("taskType") Problem.TaskType taskType) {
        List<Problem> problems = problemRepository.findAllByTaskType(taskType);
        List<ProblemResponse> result = problems.stream()
                .map(p -> ProblemResponse.from(p,
                        problemTestRepository.findAllByProblemIdOrderByTestIndex(p.getId()).size()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/random")
    public ResponseEntity<Map<String, Object>> getRandomProblem(
            @RequestParam String type,
            @RequestParam String complexity) {
        Problem.TaskComplexity comp;
        try {
            comp = Problem.TaskComplexity.valueOf(complexity.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid complexity: " + complexity);
        }
        Problem problem = problemService.getRandomProblemByComplexityAndType(comp, type);

        Map<String, Object> response = Map.of(
                "id", problem.getId(),
                "name", problem.getName(),
                "level", problem.getComplexity().name(),
                "type", problem.getTaskType().name()
        );
        return ResponseEntity.ok(response);
    }
}
