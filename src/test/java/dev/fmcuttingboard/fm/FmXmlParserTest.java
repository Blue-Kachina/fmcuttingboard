package dev.fmcuttingboard.fm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FmXmlParserTest {

    private final FmXmlParser parser = new FmXmlParser();

    @Test
    void parsesFieldsSnippet() throws Exception {
        String xml = """
                <fmxmlsnippet version="1">
                  <FieldDefinition name="CustomerID"/>
                </fmxmlsnippet>
                """;
        ParsedSnippet sn = parser.parse(xml);
        assertNotNull(sn);
        assertEquals("1", sn.getVersion());
        assertTrue(sn.getElementTypes().contains(ElementType.FIELDS));
        assertTrue(sn.getFieldNames().contains("CustomerID"));
    }

    @Test
    void parsesScriptSnippet() throws Exception {
        String xml = """
                <fmxmlsnippet>
                  <Script name="DoWork">
                    <Step id="1"/>
                  </Script>
                </fmxmlsnippet>
                """;
        ParsedSnippet sn = parser.parse(xml);
        assertTrue(sn.getElementTypes().contains(ElementType.SCRIPTS));
        assertTrue(sn.getScriptNames().contains("DoWork"));
    }

    @Test
    void parsesTablesSnippet() throws Exception {
        String xml = """
                <fmxmlsnippet type="FMObjectList">
                  <BaseTable name="Customers">
                    <Field name="Name"><DataType>Text</DataType></Field>
                  </BaseTable>
                </fmxmlsnippet>
                """;
        ParsedSnippet sn = parser.parse(xml);
        assertTrue(sn.getElementTypes().contains(ElementType.TABLES));
    }

    @Test
    void parsesCustomFunctionSnippet() throws Exception {
        String xml = """
                <fmxmlsnippet type="FMObjectList">
                  <CustomFunction name="CF_SayHello">
                    <Parameters>name</Parameters>
                    <Definition>\"Hello, \" &amp; name</Definition>
                  </CustomFunction>
                </fmxmlsnippet>
                """;
        ParsedSnippet sn = parser.parse(xml);
        assertTrue(sn.getElementTypes().contains(ElementType.CUSTOM_FUNCTIONS));
    }

    @Test
    void parsesValueListSnippet() throws Exception {
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
        ParsedSnippet sn = parser.parse(xml);
        assertTrue(sn.getElementTypes().contains(ElementType.VALUE_LISTS));
    }

    @Test
    void rejectsNonSnippetRoot() {
        String xml = "<root><child/></root>";
        assertThrows(ConversionException.class, () -> parser.parse(xml));
    }

    @Test
    void rejectsEmptySnippetContent() {
        String xml = "<fmxmlsnippet/>";
        assertThrows(ConversionException.class, () -> parser.parse(xml));
    }
}
