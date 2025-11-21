package dev.fmcuttingboard.fm;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Phase 5.1 — XML Parsing & Model
 * Defines a DOM-based parser for fmxmlsnippet XML used by the plugin.
 * Provides minimal validation and a lightweight internal representation.
 */
public class FmXmlParser {

    /**
     * Parse the provided XML text into a {@link ParsedSnippet} after validating
     * that the root element is <fmxmlsnippet>.
     *
     * @throws ConversionException when the XML is malformed or not an fmxmlsnippet
     */
    public ParsedSnippet parse(String xmlText) throws ConversionException {
        if (xmlText == null || xmlText.isBlank()) {
            throw new ConversionException("XML text is empty.");
        }

        Document doc = toDocument(xmlText);
        Element root = doc.getDocumentElement();
        if (root == null) {
            throw new ConversionException("XML has no root element.");
        }
        if (!"fmxmlsnippet".equalsIgnoreCase(root.getTagName())) {
            throw new ConversionException("Root element is not <fmxmlsnippet>.");
        }

        ParsedSnippet model = new ParsedSnippet();
        model.setRawXml(xmlText.trim());
        // Basic metadata: version/type if present as attributes
        if (root.hasAttribute("version")) {
            model.setVersion(root.getAttribute("version"));
        }
        if (root.hasAttribute("type")) {
            model.setTypeHint(root.getAttribute("type"));
        }

        // Note: Mapping of specific child elements is implemented in a later step (Phase 5.1 task 2)
        return model;
    }

    private static Document toDocument(String xml) throws ConversionException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
                return builder.parse(bais);
            }
        } catch (Exception ex) {
            throw new ConversionException("Failed to parse XML.", ex);
        }
    }
}
