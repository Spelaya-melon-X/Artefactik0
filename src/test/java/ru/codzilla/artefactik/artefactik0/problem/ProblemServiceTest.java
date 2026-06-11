package ru.codzilla.artefactik.artefactik0.problem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import ru.codzilla.artefactik.artefactik0.dto.CreateProblemRequest;
import ru.codzilla.artefactik.artefactik0.dto.ProblemResponse;
import ru.codzilla.artefactik.artefactik0.dto.TestCaseDTO;
import ru.codzilla.artefactik.artefactik0.repository.ProblemTest;
import ru.codzilla.artefactik.artefactik0.repository.ProblemTestRepository;
import ru.codzilla.artefactik.artefactik0.service.GeneratorService;
import ru.codzilla.artefactik.artefactik0.repository.Problem;
import ru.codzilla.artefactik.artefactik0.repository.ProblemRepository;
import ru.codzilla.artefactik.artefactik0.service.ProblemService;
import ru.codzilla.artefactik.artefactik0.service.MinioStorageService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock private ProblemRepository problemRepository;
    @Mock private ProblemTestRepository problemTestRepository;
    @Mock private MinioStorageService storageService;
    @Mock private GeneratorService generatorService;
    @InjectMocks private ProblemService problemService;

    private Problem savedProblem;

    @BeforeEach
    void setUp() {
        savedProblem = new Problem();
        savedProblem.setId(1L);
        savedProblem.setName("Test");
        savedProblem.setTimeLimit(1000);
        savedProblem.setMemoryLimit(256);
        savedProblem.setCreatedAt(java.time.LocalDateTime.now());
        reset(problemRepository, problemTestRepository, storageService, generatorService);
    }

    @Test
    void shouldCreateProblemWithGenerator() {
        CreateProblemRequest request = new CreateProblemRequest();
        request.setName("Sum");
        request.setGeneratorCode("class SumGen implements TestGenerator {...}");
        request.setInputs(List.of("1 2", "3 4"));

        when(problemRepository.save(any(Problem.class))).thenReturn(savedProblem);
        when(generatorService.executeGenerator(eq(request.getGeneratorCode()), eq("1 2")))
                .thenReturn("3");
        when(generatorService.executeGenerator(eq(request.getGeneratorCode()), eq("3 4")))
                .thenReturn("7");
        when(problemTestRepository.findAllByProblemIdOrderByTestIndex(1L))
                .thenReturn(List.of(mockTest(1), mockTest(2)));

        ProblemResponse response = problemService.createProblem(request);

        assertThat(response.getName()).isEqualTo("Test");
        assertThat(response.getTestCount()).isEqualTo(2);

        verify(storageService).save(contains("generator"), eq(request.getGeneratorCode()));
        verify(storageService, times(2)).save(contains("input.txt"), anyString());
        verify(storageService, times(2)).save(contains("output.txt"), anyString());
        verify(problemTestRepository, times(2)).save(any(ProblemTest.class));
    }

    @Test
    void shouldFailIfGeneratorThrowsException() {
        CreateProblemRequest request = new CreateProblemRequest();
        request.setName("Bad");
        request.setGeneratorCode("broken");
        request.setInputs(List.of("1"));

        when(problemRepository.save(any())).thenReturn(savedProblem);
        when(generatorService.executeGenerator(anyString(), anyString()))
                .thenThrow(new RuntimeException("Compilation error"));

        assertThatThrownBy(() -> problemService.createProblem(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Compilation error");
    }

    @Test
    void shouldGetProblem() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(savedProblem));
        when(problemTestRepository.findAllByProblemIdOrderByTestIndex(1L))
                .thenReturn(List.of(mockTest(1), mockTest(2)));

        ProblemResponse response = problemService.getProblem(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTestCount()).isEqualTo(2);
    }

    @Test
    void shouldThrowIfProblemNotFound() {
        when(problemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.getProblem(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Problem not found");
    }

    @Test
    void shouldGetTests() {
        ProblemTest t1 = new ProblemTest();
        t1.setTestIndex(1);
        t1.setInputKey("in/1");
        t1.setOutputKey("out/1");
        ProblemTest t2 = new ProblemTest();
        t2.setTestIndex(2);
        t2.setInputKey("in/2");
        t2.setOutputKey("out/2");

        when(problemRepository.findById(1L)).thenReturn(Optional.of(savedProblem));
        when(problemTestRepository.findAllByProblemIdOrderByTestIndex(1L))
                .thenReturn(List.of(t1, t2));
        when(storageService.get("in/1")).thenReturn("1 2");
        when(storageService.get("out/1")).thenReturn("3");
        when(storageService.get("in/2")).thenReturn("3 4");
        when(storageService.get("out/2")).thenReturn("7");

        List<TestCaseDTO> tests = problemService.getTests(1L);

        assertThat(tests).hasSize(2);
        assertThat(tests.get(0).getInput()).isEqualTo("1 2");
        assertThat(tests.get(0).getOutput()).isEqualTo("3");
    }

    @Test
    void shouldGetStatement() {
        savedProblem.setStatementKey("problems/1/statement.md");
        when(problemRepository.findById(1L)).thenReturn(Optional.of(savedProblem));
        when(storageService.get("problems/1/statement.md")).thenReturn("# Markdown");

        String stmt = problemService.getStatement(1L);
        assertThat(stmt).isEqualTo("# Markdown");
    }

    @Test
    void shouldThrowIfNoStatement() {
        savedProblem.setStatementKey(null);
        when(problemRepository.findById(1L)).thenReturn(Optional.of(savedProblem));

        assertThatThrownBy(() -> problemService.getStatement(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No statement");
    }

    @Test
    void shouldUpdateTestOutput() {
        ProblemTest test = new ProblemTest();
        test.setTestIndex(1);
        test.setOutputKey("problems/1/tests/1/output.txt");

        when(problemRepository.findById(1L)).thenReturn(Optional.of(savedProblem));
        when(problemTestRepository.findAllByProblemIdOrderByTestIndex(1L))
                .thenReturn(List.of(test));

        problemService.updateTestOutput(1L, 1, "new output");

        verify(storageService).save("problems/1/tests/1/output.txt", "new output");
        verify(problemTestRepository).findAllByProblemIdOrderByTestIndex(1L);
    }

    @Test
    void shouldThrowIfTestNotFoundWhenUpdatingOutput() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(savedProblem));
        when(problemTestRepository.findAllByProblemIdOrderByTestIndex(1L))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> problemService.updateTestOutput(1L, 99, "output"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Test 99 not found");
    }

    private ProblemTest mockTest(int index) {
        ProblemTest t = new ProblemTest();
        t.setTestIndex(index);
        return t;
    }

    @Transactional(readOnly = true)
    public Problem getRandomProblemByComplexityAndType(Problem.TaskComplexity complexity, String type) {
        // type пока игнорируется, используем только complexity
        return problemRepository.findRandomByComplexity(String.valueOf(complexity))
                .orElseThrow(() -> new RuntimeException("No problems found for complexity: " + complexity));
    }
}