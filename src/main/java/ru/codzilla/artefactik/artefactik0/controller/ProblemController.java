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
import ru.codzilla.artefactik.artefactik0.repository.ProblemTestRepository;
import ru.codzilla.artefactik.artefactik0.service.ProblemService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;
    private final ProblemTestRepository problemTestRepository;


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
}
