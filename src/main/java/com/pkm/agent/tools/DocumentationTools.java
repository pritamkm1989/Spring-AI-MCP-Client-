package com.pkm.agent.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.pkm.agent.tools.base.AgenticTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Filesystem tools that let the agent scan the source tree for Java elements that
 * lack Javadoc and rewrite files with the generated documentation. All access is
 * confined to the configured repository root and to {@code .java} files.
 */
@Slf4j
@Component
public class DocumentationTools implements AgenticTool {

    /** Matches a public/protected type, method or field declaration that should carry Javadoc. */
    private static final Pattern DOCUMENTABLE = Pattern.compile(
            "^\\s*(public|protected)\\s+.*"
                    + "(class|interface|enum|record|@interface|\\(.*\\)|=|;)\\s*\\{?\\s*$");

    /** Lines that look like a documentable declaration but never need their own Javadoc. */
    private static final Pattern SKIP = Pattern.compile(
            "^\\s*(package|import|@|//|\\*|/\\*|\\}|return|if|for|while|switch|case).*");

    private final Path root;

    public DocumentationTools(@Value("${agent.doc.root-path:.}") String rootPath) {
        this.root = Path.of(rootPath).toAbsolutePath().normalize();
        log.info("DocumentationTools rooted at {}", this.root);
    }

    @Tool(description = "Scan the repository for Java source files and report public classes, "
            + "methods and fields that are missing a Javadoc comment. Returns a plain-text "
            + "report grouped by file with the relative path and 1-based line numbers.")
    public String scanForMissingJavadoc() {
        log.info("Tool invoked: scanForMissingJavadoc");
        StringBuilder report = new StringBuilder();
        int fileCount = 0;
        int missingCount = 0;

        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/build/"))
                    .sorted()
                    .toList();

            for (Path file : javaFiles) {
                List<String> missing = findUndocumented(file);
                if (missing.isEmpty()) {
                    continue;
                }
                fileCount++;
                missingCount += missing.size();
                report.append(root.relativize(file)).append('\n');
                missing.forEach(m -> report.append("  ").append(m).append('\n'));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan repository at " + root, e);
        }

        if (missingCount == 0) {
            return "No missing Javadoc found. Every public element is documented.";
        }
        return "Found %d undocumented element(s) across %d file(s):%n%s"
                .formatted(missingCount, fileCount, report);
    }

    @Tool(description = "Read the full text of a Java source file, given its path relative to "
            + "the repository root (for example 'src/main/java/com/example/agent/AgentTools.java').")
    public String readJavaFile(String relativePath) {
        log.info("Tool invoked: readJavaFile path={}", relativePath);
        Path file = resolveJavaFile(relativePath);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + relativePath, e);
        }
    }

    @Tool(description = "Overwrite a Java source file with new content, given its path relative "
            + "to the repository root. Use this to save a file after adding Javadoc comments. "
            + "Only the documentation should change; the code must stay identical.")
    public String writeJavaFile(String relativePath, String content) {
        log.info("Tool invoked: writeJavaFile path={} ({} chars)", relativePath,
                content == null ? 0 : content.length());
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Refusing to write empty content to " + relativePath);
        }
        Path file = resolveJavaFile(relativePath);
        try {
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return "Wrote %d characters to %s".formatted(content.length(), relativePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + relativePath, e);
        }
    }

    /** Returns human-readable descriptions of undocumented declarations in a single file. */
    private List<String> findUndocumented(Path file) {
        List<String> result = new ArrayList<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + file, e);
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (SKIP.matcher(line).matches() || !DOCUMENTABLE.matcher(line).matches()) {
                continue;
            }
            if (!hasJavadocAbove(lines, i)) {
                result.add("line %d: %s".formatted(i + 1, line.strip()));
            }
        }
        return result;
    }

    /** True if the first non-annotation line above index closes a Javadoc block ({@code *&#47;}). */
    private boolean hasJavadocAbove(List<String> lines, int index) {
        for (int i = index - 1; i >= 0; i--) {
            String prev = lines.get(i).strip();
            if (prev.isEmpty() || prev.startsWith("@")) {
                continue;
            }
            return prev.endsWith("*/");
        }
        return false;
    }

    private Path resolveJavaFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("A relative path is required");
        }
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Path escapes the repository root: " + relativePath);
        }
        if (!target.toString().endsWith(".java")) {
            throw new IllegalArgumentException("Only .java files are allowed: " + relativePath);
        }
        return target;
    }
}
