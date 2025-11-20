package dev.fmcuttingboard.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TimestampedFileCreationTest {

    @Test
    void filenameHasEpochMillisAndXmlExtension() {
        String name = ProjectFiles.generateTimestampedXmlFileName();
        assertTrue(name.matches("\\d+\\.xml"), "Filename should be digits followed by .xml, got: " + name);
    }

    @Test
    void createsFileInCuttingBoardDir(@TempDir Path tmp) throws IOException {
        Path created = ProjectFiles.createTimestampedXmlFile(tmp);

        // Directory should exist
        Path dir = tmp.resolve(ProjectFiles.CUTTING_BOARD_DIR);
        assertTrue(Files.exists(dir) && Files.isDirectory(dir), ".fmCuttingBoard directory should exist");

        // File should exist and be inside the directory
        assertTrue(Files.exists(created) && Files.isRegularFile(created), "Created file should exist");
        assertEquals(dir, created.getParent(), "File should be created inside .fmCuttingBoard directory");

        // File should be empty initially
        assertEquals(0L, Files.size(created), "Newly created file should be empty");
    }

    @Test
    void nullProjectRootThrows() {
        assertThrows(IllegalArgumentException.class, () -> ProjectFiles.createTimestampedXmlFile(null));
    }
}
