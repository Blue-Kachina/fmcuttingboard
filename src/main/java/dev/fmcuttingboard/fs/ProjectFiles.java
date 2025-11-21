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
     * Generates a timestamp-based filename with the default prefix and .xml extension.
     * Default format: "fmclip-{timestamp}.xml" where timestamp is epoch milliseconds.
     */
    public static String generateTimestampedXmlFileName() {
        return "fmclip-" + System.currentTimeMillis() + ".xml";
    }

    /**
     * Creates a new empty XML file with a timestamped filename inside the `.fmCuttingBoard` directory.
     * Basic implementation (robust error handling added in next task step).
     * @param projectRoot project root (non-null)
     * @return path to created file
     * @throws IOException on I/O errors
     * @throws IllegalArgumentException if projectRoot is null
     */
    public static Path createTimestampedXmlFile(Path projectRoot) throws IOException {
        if (projectRoot == null) {
            throw new IllegalArgumentException("projectRoot must not be null");
        }
        EnsureResult res = ensureCuttingBoardDir(projectRoot);
        Path dir = res.directory();

        String baseName = generateTimestampedXmlFileName();
        Path candidate = dir.resolve(baseName);

        // Handle unlikely collisions by appending -N suffix before .xml
        int attempt = 0;
        while (Files.exists(candidate)) {
            attempt++;
            String withSuffix = baseName.replaceFirst("\\.xml$", "-" + attempt + ".xml");
            candidate = dir.resolve(withSuffix);
            if (attempt > 1000) {
                throw new IOException("Unable to create a unique timestamped filename after 1000 attempts");
            }
        }

        try {
            return Files.createFile(candidate);
        } catch (IOException ioe) {
            throw new IOException("Failed to create timestamped XML file at: " + candidate, ioe);
        }
    }

    // ----- Phase 7.1 — Settings-aware helpers -----
    /**
     * Ensures a custom-named base directory exists under the provided project root.
     * If created, also creates a .gitignore inside with a single "*" line.
     */
    public static EnsureResult ensureCustomBaseDir(Path projectRoot, String baseDirName) throws IOException {
        if (projectRoot == null) throw new IllegalArgumentException("projectRoot must not be null");
        if (baseDirName == null || baseDirName.isBlank()) baseDirName = CUTTING_BOARD_DIR;
        Path dir = projectRoot.resolve(baseDirName);
        boolean createdDir = false;
        boolean createdGitignore = false;
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
            createdDir = true;
            Path gi = dir.resolve(GITIGNORE);
            Files.writeString(gi, "*\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            createdGitignore = true;
        }
        return new EnsureResult(dir, createdDir, createdGitignore);
    }

    /**
     * Creates a new empty XML file using a settings-provided base directory and filename pattern.
     * Supported pattern tokens: {timestamp} (epoch millis). Any other text remains literal.
     * If pattern is null/blank, defaults to fmclip-{timestamp}.xml
     */
    public static Path createSettingsBasedXmlFile(Path projectRoot, String baseDirName, String fileNamePattern) throws IOException {
        if (projectRoot == null) throw new IllegalArgumentException("projectRoot must not be null");
        EnsureResult res = ensureCustomBaseDir(projectRoot, baseDirName);
        Path dir = res.directory();

        String pattern = (fileNamePattern == null || fileNamePattern.isBlank())
                ? "fmclip-{timestamp}.xml"
                : fileNamePattern;
        String baseName = pattern.replace("{timestamp}", String.valueOf(System.currentTimeMillis()));
        if (!baseName.endsWith(".xml")) {
            baseName = baseName + ".xml"; // ensure extension
        }

        Path candidate = dir.resolve(baseName);
        int attempt = 0;
        while (Files.exists(candidate)) {
            attempt++;
            String withSuffix;
            int dot = baseName.lastIndexOf('.')
                    ;
            if (dot > 0) {
                withSuffix = baseName.substring(0, dot) + "-" + attempt + baseName.substring(dot);
            } else {
                withSuffix = baseName + "-" + attempt;
            }
            candidate = dir.resolve(withSuffix);
            if (attempt > 1000) {
                throw new IOException("Unable to create a unique filename after 1000 attempts for baseName=" + baseName);
            }
        }
        try {
            return Files.createFile(candidate);
        } catch (IOException ioe) {
            throw new IOException("Failed to create XML file at: " + candidate, ioe);
        }
    }
}
