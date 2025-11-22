package dev.fmcuttingboard.fm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultXmlToClipboardConverterTest {

    private XmlToClipboardConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DefaultXmlToClipboardConverter();
    }

    @Test
    void convertsFieldsSnippetToClipboardPayload() throws Exception {
        String xml = """
                <fmxmlsnippet version="1">
                  <FieldDefinition name="CustomerID"/>
                </fmxmlsnippet>
                """;

        String payload = converter.convertToClipboardPayload(xml);
        assertNotNull(payload);
        assertTrue(payload.contains("<fmxmlsnippet"));
        assertTrue(payload.contains("FieldDefinition"));
    }

    @Test
    void convertsScriptSnippetToClipboardPayload() throws Exception {
        String xml = """
                <fmxmlsnippet>
                  <Script name="DoWork">
                    <Step id="1"/>
                  </Script>
                </fmxmlsnippet>
                """;

        String payload = converter.convertToClipboardPayload(xml);
        assertNotNull(payload);
        assertTrue(payload.contains("<Script"));
        assertTrue(payload.contains("<Step"));
    }

    @Test
    void convertsStandaloneScriptStepsSnippetToClipboardPayload() throws Exception {
        // Some fmxmlsnippets contain Script Steps without a wrapping <Script> element
        String xml = """
                <fmxmlsnippet type="FMObjectList">
                  <Step enable=\"True\" id=\"1\" name=\"Show Custom Dialog\">
                    <Text>Example</Text>
                  </Step>
                </fmxmlsnippet>
                """;

        String payload = converter.convertToClipboardPayload(xml);
        assertNotNull(payload);
        assertTrue(payload.contains("<fmxmlsnippet"));
        assertTrue(payload.contains("<Step"));
    }

    @Test
    void convertsTablesSnippetToClipboardPayload() throws Exception {
        String xml = """
                <fmxmlsnippet type="FMObjectList">
                  <BaseTable name="TestTable">
                    <Field name="Id">
                      <DataType>Number</DataType>
                    </Field>
                  </BaseTable>
                </fmxmlsnippet>
                """;

        String payload = converter.convertToClipboardPayload(xml);
        assertNotNull(payload);
        assertTrue(payload.contains("<BaseTable"));
    }

    @Test
    void convertsLayoutObjectListSnippetToClipboardPayload() throws Exception {
        String xml = """
                <fmxmlsnippet type="FMObjectList">
                  <LayoutObjectList>
                    <LayoutObject type="field" name="MyField"/>
                  </LayoutObjectList>
                </fmxmlsnippet>
                """;

        String payload = converter.convertToClipboardPayload(xml);
        assertNotNull(payload);
        assertTrue(payload.contains("<LayoutObjectList"));
    }

    @Test
    void throwsForMalformedXml() {
        String xml = "<fmxmlsnippet><Script></fmxmlsnippet>"; // unclosed <Script>
        assertThrows(ConversionException.class, () -> converter.convertToClipboardPayload(xml));
    }

    @Test
    void convertsCustomFunctionSnippetToClipboardPayload() throws Exception {
        String xml = """
                <fmxmlsnippet type="FMObjectList">
                  <CustomFunction name="CF_SayHello">
                    <Parameters>name</Parameters>
                    <Definition>\\"Hello, \\" &amp; name</Definition>
                  </CustomFunction>
                </fmxmlsnippet>
                """;

        String payload = converter.convertToClipboardPayload(xml);
        assertNotNull(payload);
        assertTrue(payload.contains("<CustomFunction"));
    }

    @Test
    void convertsValueListSnippetToClipboardPayload() throws Exception {
        String xml = """
                <fmxmlsnippet type="FMObjectList">
                  <ValueList name="VL_Status">
                    <CustomValues>
                      <CustomValue>Open</CustomValue>
                      <CustomValue>Closed</CustomValue>
                    </CustomValues>
                  </ValueList>
                </fmxmlsnippet>
                """;

        String payload = converter.convertToClipboardPayload(xml);
        assertNotNull(payload);
        assertTrue(payload.contains("<ValueList"));
    }
}
