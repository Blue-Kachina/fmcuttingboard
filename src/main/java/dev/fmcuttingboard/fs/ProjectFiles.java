package dev.fmcuttingboard.fs;

import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Utilities for interacting with files within the current IntelliJ project.
 *
 * Phase 4.1 — `.fmCuttingBoard` Directory Management
 */
public final class ProjectFiles {
    public static final String CUTTING_BOARD_DIR = ".fmCuttingBoard";
    public static final String GITIGNORE = ".gitignore";

    private ProjectFiles() {}

    /**
     * Locate the root directory for the given IntelliJ project.
     * @param project The IntelliJ project instance (must not be null and must have a base path)
     * @return Path to the project root
     * @throws IllegalArgumentException if project or its base path is null
     */
    public static Path getProjectRoot(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project must not be null");
        }
        String basePath = project.getBasePath();
        if (basePath == null) {
            throw new IllegalArgumentException("Project base path is null");
        }
        return Path.of(basePath);
    }

    /**
     * Ensures that the `.fmCuttingBoard` directory exists under the provided project root.
     * If the directory is created by this call, also creates a `.gitignore` file inside it with a
     * single line: "*". If the directory already exists, this method does not create or modify the
     * `.gitignore` file.
     *
     * @param projectRoot path to project root
     * @return result describing what was created
     * @throws IOException if file operations fail
     * @throws IllegalArgumentException if projectRoot is null
     */
    public static EnsureResult ensureCuttingBoardDir(Path projectRoot) throws IOException {
        if (projectRoot == null) {
            throw new IllegalArgumentException("projectRoot must not be null");
        }

        Path dir = projectRoot.resolve(CUTTING_BOARD_DIR);
        boolean createdDir = false;
        boolean createdGitignore = false;

        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
            createdDir = true;
            // Create .gitignore with only "*" to keep the dir empty in VCS
            Path gi = dir.resolve(GITIGNORE);
            Files.writeString(gi, "*\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            createdGitignore = true;
            // Created directory and .gitignore
        } else {
            // Directory already existed; do nothing
        }

        return new EnsureResult(dir, createdDir, createdGitignore);
    }

    /** Result data for ensureCuttingBoardDir. */
    public record EnsureResult(Path directory, boolean createdDirectory, boolean createdGitignore) {
    }

    // ----- Phase 4.2 — Timestamped XML File Creation -----
    /**
     * Generates a timestamp-based filename with an .xml extension.
     * Uses epoch milliseconds, e.g. "1732100000123.xml".
     */
    public static String generateTimestampedXmlFileName() {
        return System.currentTimeMillis() + ".xml";
    }
}
