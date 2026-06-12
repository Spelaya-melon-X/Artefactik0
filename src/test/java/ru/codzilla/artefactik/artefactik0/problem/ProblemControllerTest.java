package ru.codzilla.artefactik.artefactik0.problem;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.codzilla.artefactik.artefactik0.controller.ProblemController;
import ru.codzilla.artefactik.artefactik0.dto.CreateProblemRequest;
import ru.codzilla.artefactik.artefactik0.dto.ProblemResponse;
import ru.codzilla.artefactik.artefactik0.dto.TestCaseDTO;
import ru.codzilla.artefactik.artefactik0.repository.Problem;
import ru.codzilla.artefactik.artefactik0.repository.ProblemRepository;
import ru.codzilla.artefactik.artefactik0.repository.ProblemTestRepository;
import ru.codzilla.artefactik.artefactik0.service.ProblemService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProblemController.class)
@AutoConfigureMockMvc
class ProblemControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @MockBean private ProblemService problemService;
    @MockBean private ProblemTestRepository problemTestRepository;
    @MockBean private ProblemRepository problemRepository;   // ← добавлен мок

    @Test
    void shouldCreateProblem() throws Exception {
        CreateProblemRequest request = new CreateProblemRequest();
        request.setName("Sum");
        request.setGeneratorCode("class Gen implements TestGenerator {...}");
        request.setInputs(List.of("1 2", "3 4"));
        request.setComplexity(Problem.TaskComplexity.MEDIUM);

        ProblemResponse response = new ProblemResponse();
        response.setId(1L);
        response.setName("Sum");
        response.setTestCount(2);

        when(problemService.createProblem(any())).thenReturn(response);

        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.testCount").value(2));
    }

    @Test
    void shouldReturn400IfNameMissing() throws Exception {
        CreateProblemRequest request = new CreateProblemRequest();
        request.setGeneratorCode("code");
        request.setInputs(List.of("1"));
        request.setComplexity(Problem.TaskComplexity.MEDIUM);

        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("name")));
    }

    @Test
    void shouldReturn400IfGeneratorCodeMissing() throws Exception {
        CreateProblemRequest request = new CreateProblemRequest();
        request.setName("Task");
        request.setInputs(List.of("1"));
        request.setComplexity(Problem.TaskComplexity.MEDIUM);

        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("generatorCode")));
    }

    @Test
    void shouldReturn400IfInputsEmpty() throws Exception {
        CreateProblemRequest request = new CreateProblemRequest();
        request.setName("Task");
        request.setGeneratorCode("code");
        request.setInputs(List.of());
        request.setComplexity(Problem.TaskComplexity.MEDIUM);

        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("inputs")));
    }

    @Test
    void shouldGetProblem() throws Exception {
        ProblemResponse response = new ProblemResponse();
        response.setId(1L);
        response.setName("Test");
        response.setTimeLimit(1000);
        response.setMemoryLimit(256);
        response.setHasStatement(false);
        response.setTestCount(2);
        when(problemService.getProblem(1L)).thenReturn(response);

        mockMvc.perform(get("/api/problems/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test"))
                .andExpect(jsonPath("$.testCount").value(2));
    }

    @Test
    void shouldGetTests() throws Exception {
        TestCaseDTO dto1 = new TestCaseDTO(1, "1 2", "3");
        TestCaseDTO dto2 = new TestCaseDTO(2, "3 4", "7");
        when(problemService.getTests(1L)).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/problems/1/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].input").value("1 2"))
                .andExpect(jsonPath("$[1].output").value("7"));
    }

    @Test
    void shouldGetStatement() throws Exception {
        when(problemService.getStatement(1L)).thenReturn("# Task");

        mockMvc.perform(get("/api/problems/1/statement"))
                .andExpect(status().isOk())
                .andExpect(content().string("# Task"));
    }

    @Test
    void shouldUpdateOutput() throws Exception {
        mockMvc.perform(patch("/api/problems/1/tests/2/output")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("new output"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn400WhenServiceThrowsRuntimeException() throws Exception {
        when(problemService.getProblem(999L)).thenThrow(new RuntimeException("Problem not found"));

        mockMvc.perform(get("/api/problems/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Problem not found"));
    }
}