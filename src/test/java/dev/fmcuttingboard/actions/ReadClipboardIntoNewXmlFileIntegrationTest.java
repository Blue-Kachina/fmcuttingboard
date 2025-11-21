package dev.fmcuttingboard.actions;

import dev.fmcuttingboard.clipboard.ClipboardAccessException;
import dev.fmcuttingboard.clipboard.ClipboardService;
import dev.fmcuttingboard.fm.ClipboardToXmlConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style test (headless) for Phase 4.3: conversion + file creation.
 *
 * This test avoids IntelliJ test harness by directly invoking the converter and
 * the action's file writing helper with a temporary directory as the project root.
 */
class ReadClipboardIntoNewXmlFileIntegrationTest {

    private static class NoOpClipboardService implements ClipboardService {
        @Override
        public java.util.Optional<String> readText() throws ClipboardAccessException {
            return java.util.Optional.empty();
        }

        @Override
        public void writeText(String text) throws ClipboardAccessException {
            // no-op
        }
    }

    @TempDir
    Path tmpDir;

    @Test
    void convertsClipboardLikePayloadAndWritesToTimestampedXmlFile() throws Exception {
        // Given a clipboard-like payload containing fmxmlsnippet content
        String payload = "Noise before\n<fmxmlsnippet>\n  <FieldDefinition name=\"Test\"/>\n</fmxmlsnippet>\nnoise after";

        // When converting to XML via the real converter
        ClipboardToXmlConverter converter = new ClipboardToXmlConverter();
        String xml = converter.convertToXml(payload);

        // And writing it to a new timestamped file under the project's .fmCuttingBoard directory
        ReadClipboardIntoNewXmlFileAction action = new ReadClipboardIntoNewXmlFileAction(
                new NoOpClipboardService(), new ClipboardToXmlConverter(), (p, t, title, content) -> {}
        );
        Path written = action.processIntoNewXmlFile(null, tmpDir, xml);

        // Then the file exists, is inside .fmCuttingBoard, and contains the XML snippet
        assertTrue(Files.exists(written), "Output file should exist");
        assertEquals(tmpDir.resolve(".fmCuttingBoard"), written.getParent(), "File should be inside .fmCuttingBoard directory");

        String content = Files.readString(written, StandardCharsets.UTF_8);
        assertEquals(xml, content, "File content should equal the converted XML");
        assertTrue(content.startsWith("<fmxmlsnippet"), "Content should be an fmxmlsnippet");
        assertTrue(content.endsWith("</fmxmlsnippet>"), "Content should end with closing fmxmlsnippet tag");
    }
}
