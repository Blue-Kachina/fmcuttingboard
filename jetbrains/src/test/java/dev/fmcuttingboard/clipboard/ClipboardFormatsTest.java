package dev.fmcuttingboard.clipboard;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ClipboardFormatsTest {

    @Test
    void everySnippetTypeExceptUnknownHasExactlyOneRule() {
        Set<SnippetType> fromJson = ClipboardFormats.rules().stream()
                .map(ClipboardFormats.TypeRule::type)
                .collect(Collectors.toSet());
        assertEquals(ClipboardFormats.rules().size(), fromJson.size(), "duplicate snippet type in clipboard-formats.json");
        EnumSet<SnippetType> expected = EnumSet.allOf(SnippetType.class);
        expected.remove(SnippetType.UNKNOWN);
        assertEquals(expected, fromJson);
    }

    @Test
    void windowsFormatNamesMatchPluginBehaviourBeforeSharedData() {
        // The mapping hardcoded in DefaultClipboardService up to 1.0.6
        assertEquals(List.of("Mac-XMSC"), ClipboardFormats.windowsFormatNames(SnippetType.SCRIPT));
        assertEquals(List.of("Mac-XMSS"), ClipboardFormats.windowsFormatNames(SnippetType.SCRIPT_STEPS));
        assertEquals(List.of("Mac-XMFD"), ClipboardFormats.windowsFormatNames(SnippetType.FIELD_DEFINITION));
        assertEquals(List.of("Mac-XMTB"), ClipboardFormats.windowsFormatNames(SnippetType.TABLE_DEFINITION));
        assertEquals(List.of("Mac-XMFN"), ClipboardFormats.windowsFormatNames(SnippetType.CUSTOM_FUNCTION));
        assertEquals(List.of("Mac-XMVL"), ClipboardFormats.windowsFormatNames(SnippetType.VALUE_LIST));
        assertEquals(List.of("Mac-XML2", "Mac-XML"), ClipboardFormats.windowsFormatNames(SnippetType.LAYOUT_OBJECTS));
        assertEquals(List.of(), ClipboardFormats.windowsFormatNames(SnippetType.UNKNOWN));
    }

    @Test
    void knownFormatNameLookupIsCaseInsensitive() {
        assertTrue(ClipboardFormats.isFileMakerWindowsFormat("mac-xmss"));
        assertTrue(ClipboardFormats.isFileMakerWindowsFormat("Mac-XML"));
        assertFalse(ClipboardFormats.isFileMakerWindowsFormat("CF_UNICODETEXT"));
        assertFalse(ClipboardFormats.isFileMakerWindowsFormat(null));
    }

    @Test
    void maxPayloadIsTenMegabytes() {
        assertEquals(10 * 1024 * 1024, ClipboardFormats.maxCustomPayloadBytes());
    }
}
