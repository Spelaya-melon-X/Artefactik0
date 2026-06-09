package ru.codzilla.artefactik.artefactik0.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.codzilla.artefactik.artefactik0.service.GeneratorService;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

class GeneratorServiceTest {

    private GeneratorService generatorService;

    @BeforeEach
    void setUp() throws Exception {
        generatorService = new GeneratorService();
        // Подменяем classpath на системный чтобы тесты работали без /app/app.jar
        injectTestClasspath(generatorService);
    }

    // Устанавливает флаг useSystemClasspath = true через reflection
    // чтобы buildClasspath() вернул системный classpath
    private void injectTestClasspath(GeneratorService service) throws Exception {
        Field field = GeneratorService.class.getDeclaredField("testMode");
        field.setAccessible(true);
        field.set(service, true);
    }

    @Test
    void shouldExecuteSimpleGenerator() {
        String code = """
                public class SumGen implements ru.codzilla.artefactik.artefactik0.generator.TestGenerator {
                    public String generate(String input) {
                        String[] parts = input.split(" ");
                        int a = Integer.parseInt(parts[0]);
                        int b = Integer.parseInt(parts[1]);
                        return String.valueOf(a + b);
                    }
                }
                """;
        String result = generatorService.executeGenerator(code, "5 7");
        assertThat(result).isEqualTo("12");
    }

    @Test
    void shouldThrowOnCompilationError() {
        String badCode = "public class Broken {";
        assertThatThrownBy(() -> generatorService.executeGenerator(badCode, "input"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ошибка компиляции");
    }

    @Test
    void shouldThrowOnRuntimeError() {
        String code = """
                public class DivByZero implements ru.codzilla.artefactik.artefactik0.generator.TestGenerator {
                    public String generate(String input) {
                        return String.valueOf(1/0);
                    }
                }
                """;
        assertThatThrownBy(() -> generatorService.executeGenerator(code, "anything"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ошибка выполнения генератора");
    }
}