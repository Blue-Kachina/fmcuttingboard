package dev.fmcuttingboard.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectFilesTest {

    @Test
    void createsDirectoryAndGitignoreWhenMissing(@TempDir Path tmp) throws IOException {
        Path projectRoot = tmp;

        ProjectFiles.EnsureResult result = ProjectFiles.ensureCuttingBoardDir(projectRoot);

        Path dir = result.directory();
        assertTrue(Files.exists(dir) && Files.isDirectory(dir), "Directory should be created");
        assertTrue(result.createdDirectory(), "createdDirectory should be true when missing");
        assertTrue(result.createdGitignore(), "createdGitignore should be true when directory newly created");

        Path gi = dir.resolve(ProjectFiles.GITIGNORE);
        assertTrue(Files.exists(gi), ".gitignore should be created");
        String content = Files.readString(gi);
        assertEquals("*\n", content, ".gitignore should contain only '*' and a newline");
    }

    @Test
    void doesNotOverwriteExistingGitignore(@TempDir Path tmp) throws IOException {
        Path projectRoot = tmp;
        Path dir = projectRoot.resolve(ProjectFiles.CUTTING_BOARD_DIR);
        Files.createDirectories(dir);
        Path gi = dir.resolve(ProjectFiles.GITIGNORE);
        Files.writeString(gi, "DO-NOT-OVERWRITE\n");

        ProjectFiles.EnsureResult result = ProjectFiles.ensureCuttingBoardDir(projectRoot);

        assertFalse(result.createdDirectory(), "Directory should not be created if it exists");
        assertFalse(result.createdGitignore(), ".gitignore should not be created if dir already existed");
        assertEquals("DO-NOT-OVERWRITE\n", Files.readString(gi), "Existing .gitignore should be preserved");
    }

    @Test
    void whenDirExistsButGitignoreMissing_DoNotCreateGitignore(@TempDir Path tmp) throws IOException {
        Path projectRoot = tmp;
        Path dir = projectRoot.resolve(ProjectFiles.CUTTING_BOARD_DIR);
        Files.createDirectories(dir);
        // intentionally no .gitignore

        ProjectFiles.EnsureResult result = ProjectFiles.ensureCuttingBoardDir(projectRoot);

        assertFalse(result.createdDirectory(), "Directory should not be created if it exists");
        assertFalse(result.createdGitignore(), ".gitignore should not be created if dir already existed (per spec)");
        assertFalse(Files.exists(dir.resolve(ProjectFiles.GITIGNORE)), ".gitignore should remain absent");
    }
}
