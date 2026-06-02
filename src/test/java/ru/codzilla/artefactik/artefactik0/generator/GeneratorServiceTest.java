package ru.codzilla.artefactik.artefactik0.generator;

import org.junit.jupiter.api.Test;
import ru.codzilla.artefactik.artefactik0.service.GeneratorService;

import static org.assertj.core.api.Assertions.*;


class GeneratorServiceTest {

    private final GeneratorService generatorService = new GeneratorService();

    @Test
    void shouldExecuteSimpleGenerator() {
        String code = "public class SumGen implements ru.codzilla.artefactik.artefactik0.generator.TestGenerator {\n" +
                "    public String generate(String input) {\n" +
                "        String[] parts = input.split(\" \");\n" +
                "        int a = Integer.parseInt(parts[0]);\n" +
                "        int b = Integer.parseInt(parts[1]);\n" +
                "        return String.valueOf(a + b);\n" +
                "    }\n" +
                "}";
        String result = generatorService.executeGenerator(code, "5 7");
        assertThat(result).isEqualTo("12");
    }

    @Test
    void shouldThrowOnCompilationError() {
        String badCode = "public class Broken {"; // синтаксическая ошибка
        assertThatThrownBy(() -> generatorService.executeGenerator(badCode, "input"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ошибка компиляции");
    }

    @Test
    void shouldThrowOnRuntimeError() {
        String code = "public class DivByZero implements ru.codzilla.artefactik.artefactik0.generator.TestGenerator {\n" +
                "    public String generate(String input) {\n" +
                "        return String.valueOf(1/0);\n" +
                "    }\n" +
                "}";
        assertThatThrownBy(() -> generatorService.executeGenerator(code, "anything"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ошибка выполнения генератора");
    }
}