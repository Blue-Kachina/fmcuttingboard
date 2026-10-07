package dev.fmcuttingboard.clipboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnippetDetectionTest {

    @Test
    void detectsScriptSteps() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><Step id=\"1\"/></fmxmlsnippet>";
        assertEquals(SnippetType.SCRIPT_STEPS,
                ClipboardFormats.detectSnippetType(xml));
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
        assertEquals(SnippetType.SCRIPT,
                ClipboardFormats.detectSnippetType(xml));
    }

    @Test
    void detectsFieldDefinition() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><FieldDefinition name=\"X\"/></fmxmlsnippet>";
        assertEquals(SnippetType.FIELD_DEFINITION,
                ClipboardFormats.detectSnippetType(xml));
    }

    @Test
    void detectsFieldTagVariant() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><Field name=\"Y\"/></fmxmlsnippet>";
        assertEquals(SnippetType.FIELD_DEFINITION,
                ClipboardFormats.detectSnippetType(xml));
    }

    @Test
    void detectsTableDefinition() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><BaseTable name=\"T\"/></fmxmlsnippet>";
        assertEquals(SnippetType.TABLE_DEFINITION,
                ClipboardFormats.detectSnippetType(xml));
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
        assertEquals(SnippetType.TABLE_DEFINITION,
                ClipboardFormats.detectSnippetType(xml));
    }

    @Test
    void detectsLayoutObjects() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><Layout name=\"L\"/><ObjectList/></fmxmlsnippet>";
        assertEquals(SnippetType.LAYOUT_OBJECTS,
                ClipboardFormats.detectSnippetType(xml));
    }

    @Test
    void detectsLayoutObjectsWhenOnlyGenericObjectPresent() {
        String xml = "<fmxmlsnippet type=\"LayoutObjectList\"><Layout><Object type=\"Text\"/></Layout></fmxmlsnippet>";
        assertEquals(SnippetType.LAYOUT_OBJECTS,
                ClipboardFormats.detectSnippetType(xml));
    }

    @Test
    void unknownWhenNoHeuristicsMatch() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><UnknownTag/></fmxmlsnippet>";
        assertEquals(SnippetType.UNKNOWN,
                ClipboardFormats.detectSnippetType(xml));
    }

    @Test
    void detectsCustomFunction() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><CustomFunction name=\"CF\"/></fmxmlsnippet>";
        assertEquals(SnippetType.CUSTOM_FUNCTION,
                ClipboardFormats.detectSnippetType(xml));
    }

    @Test
    void detectsValueList() {
        String xml = "<fmxmlsnippet type=\"FMObjectList\"><ValueList name=\"VL\"/></fmxmlsnippet>";
        assertEquals(SnippetType.VALUE_LIST,
                ClipboardFormats.detectSnippetType(xml));
    }
}
