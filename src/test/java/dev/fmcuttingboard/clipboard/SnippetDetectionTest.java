package dev.fmcuttingboard.clipboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnippetDetectionTest {

    @Test
    void detectsScriptSteps() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><Step id=\"1\"/></fmxmlsnippet>";
        assertEquals(DefaultClipboardService.SnippetType.SCRIPT_STEPS,
                DefaultClipboardService.detectSnippetType(xml));
    }

    @Test
    void detectsFullScriptOverSteps() {
        String xml = """
                <fmxmlsnippet>
                  <Script name="DoWork">
                    <Step id="1"/>
                  </Script>
                </fmxmlsnippet>
                """;
        assertEquals(DefaultClipboardService.SnippetType.SCRIPT,
                DefaultClipboardService.detectSnippetType(xml));
    }

    @Test
    void detectsFieldDefinition() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><FieldDefinition name=\"X\"/></fmxmlsnippet>";
        assertEquals(DefaultClipboardService.SnippetType.FIELD_DEFINITION,
                DefaultClipboardService.detectSnippetType(xml));
    }

    @Test
    void detectsFieldTagVariant() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><Field name=\"Y\"/></fmxmlsnippet>";
        assertEquals(DefaultClipboardService.SnippetType.FIELD_DEFINITION,
                DefaultClipboardService.detectSnippetType(xml));
    }

    @Test
    void detectsTableDefinition() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><BaseTable name=\"T\"/></fmxmlsnippet>";
        assertEquals(DefaultClipboardService.SnippetType.TABLE_DEFINITION,
                DefaultClipboardService.detectSnippetType(xml));
    }

    @Test
    void detectsTableDefinitionEvenWhenFieldsPresent() {
        String xml = """
                <fmxmlsnippet type="FMObjectList">
                  <BaseTable name="T">
                    <Field name="F"><DataType>Text</DataType></Field>
                  </BaseTable>
                </fmxmlsnippet>
                """;
        assertEquals(DefaultClipboardService.SnippetType.TABLE_DEFINITION,
                DefaultClipboardService.detectSnippetType(xml));
    }

    @Test
    void detectsLayoutObjects() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><Layout name=\"L\"/><ObjectList/></fmxmlsnippet>";
        assertEquals(DefaultClipboardService.SnippetType.LAYOUT_OBJECTS,
                DefaultClipboardService.detectSnippetType(xml));
    }

    @Test
    void unknownWhenNoHeuristicsMatch() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><UnknownTag/></fmxmlsnippet>";
        assertEquals(DefaultClipboardService.SnippetType.UNKNOWN,
                DefaultClipboardService.detectSnippetType(xml));
    }

    @Test
    void detectsCustomFunction() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><CustomFunction name=\"CF\"/></fmxmlsnippet>";
        assertEquals(DefaultClipboardService.SnippetType.CUSTOM_FUNCTION,
                DefaultClipboardService.detectSnippetType(xml));
    }

    @Test
    void detectsValueList() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><ValueList name=\"VL\"/></fmxmlsnippet>";
        assertEquals(DefaultClipboardService.SnippetType.VALUE_LIST,
                DefaultClipboardService.detectSnippetType(xml));
    }
}
