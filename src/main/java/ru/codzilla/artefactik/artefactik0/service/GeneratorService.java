package ru.codzilla.artefactik.artefactik0.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.codzilla.artefactik.artefactik0.generator.TestGenerator;

import javax.tools.*;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.*;

@Slf4j
@Service
public class GeneratorService {
    private final Map<String, TestGenerator> generatorCache = new HashMap<>();

    public String executeGenerator(String javaSourceCode, String input) {
        String hash = sha256(javaSourceCode);
        TestGenerator generator = generatorCache.get(hash);

        if (generator == null) {
            try {
                generator = compileAndInstantiate(javaSourceCode);
                generatorCache.put(hash, generator);
            } catch (Exception e) {
                throw new RuntimeException("Ошибка компиляции генератора: " + e.getMessage(), e);
            }
        }

        try {
            return generator.generate(input);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка выполнения генератора: " + e.getMessage(), e);
        }
    }

    private TestGenerator compileAndInstantiate(String javaSourceCode) throws Exception {
        String className = extractClassName(javaSourceCode);
        if (className == null) {
            throw new RuntimeException("Не найден public класс в коде генератора");
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("Запустите на JDK, а не JRE");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(diagnostics, null, null);
        JavaFileObject sourceFile = new JavaSourceFromString(className, javaSourceCode);
        String classpath = buildClasspath();

        Iterable<String> options = List.of("-classpath", classpath);
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, stdFileManager, diagnostics,
                options, null, List.of(sourceFile)
        );

        if (!task.call()) {
            StringBuilder sb = new StringBuilder("Ошибка компиляции:\n");
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                sb.append(d.getMessage(null)).append("\n");
            }
            throw new RuntimeException(sb.toString());
        }

        Path classFile = Path.of(className + ".class");
        if (!Files.exists(classFile)) {
            throw new RuntimeException("Скомпилированный файл не найден");
        }

        byte[] classBytes = Files.readAllBytes(classFile);
        Files.deleteIfExists(classFile);

        ByteArrayClassLoader classLoader = new ByteArrayClassLoader(getClass().getClassLoader());
        Class<?> clazz = classLoader.defineClass(className, classBytes);

        if (!TestGenerator.class.isAssignableFrom(clazz)) {
            throw new RuntimeException("Класс " + className + " не реализует TestGenerator");
        }

        return (TestGenerator) clazz.getDeclaredConstructor().newInstance();
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String extractClassName(String code) {
        Matcher m = Pattern.compile("public\\s+class\\s+(\\w+)").matcher(code);
        return m.find() ? m.group(1) : null;
    }

    static class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;
        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) { return code; }
    }

    static class ByteArrayClassLoader extends ClassLoader {
        ByteArrayClassLoader(ClassLoader parent) { super(parent); }
        Class<?> defineClass(String name, byte[] bytes) { return defineClass(name, bytes, 0, bytes.length); }
    }
    private String buildClasspath() {
        // В fat jar классы лежат в BOOT-INF/classes, либы в BOOT-INF/lib
        // Пробуем найти реальный путь к app.jar
        String jarPath = null;
        try {
            URI location = GeneratorService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            // location может быть внутри jar: jar:file:/app/app.jar!/BOOT-INF/classes/
            String path = location.toString();
            if (path.contains("!")) {
                // Вытаскиваем путь до jar файла
                jarPath = path.substring(path.indexOf("file:") + 5, path.indexOf("!"));
            } else {
                jarPath = location.getPath();
            }
        } catch (Exception e) {
            log.warn("Cannot determine jar path: {}", e.getMessage());
        }

        StringBuilder cp = new StringBuilder();

        // Добавляем системный classpath
        String sysClasspath = System.getProperty("java.class.path");
        if (sysClasspath != null && !sysClasspath.isBlank()) {
            cp.append(sysClasspath);
        }

        // Добавляем сам app.jar если нашли
        if (jarPath != null) {
            cp.append(File.pathSeparator).append(jarPath);
        }

        // Добавляем /app/app.jar напрямую (знаем путь из Dockerfile)
        cp.append(File.pathSeparator).append("/app/app.jar");

        log.info("Compiler classpath: {}", cp);
        return cp.toString();
    }
}