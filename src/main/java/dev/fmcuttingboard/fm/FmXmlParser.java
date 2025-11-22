package dev.fmcuttingboard.fm;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

        // Map child elements into the internal representation
        mapChildren(root, model);

        // Minimal structural validation: ensure snippet has at least one child element
        if (!hasChildElements(root)) {
            throw new ConversionException("<fmxmlsnippet> has no content elements.");
        }
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

    private static void mapChildren(Element root, ParsedSnippet model) {
        walkElement(root, model);
        // After walking, if no types detected but we are under fmxmlsnippet, keep UNKNOWN (handled in validation task)
    }

    private static void walkElement(Element el, ParsedSnippet model) {
        String tag = el.getTagName();
        if (tag != null) {
            String lower = tag.toLowerCase();
            if (lower.equals("field") || lower.equals("fielddefinition")) {
                String name = el.getAttribute("name");
                if (name != null && !name.isBlank()) model.addFieldName(name);
                // Even if name is missing, presence of a Field/FieldDefinition indicates a Fields snippet
                model.addElementType(ElementType.FIELDS);
            } else if (lower.equals("layout")) {
                String name = el.getAttribute("name");
                if (name != null && !name.isBlank()) model.addLayoutName(name);
                model.addElementType(ElementType.LAYOUTS);
            } else if (lower.equals("script")) {
                String name = el.getAttribute("name");
                if (name != null && !name.isBlank()) model.addScriptName(name);
                model.addElementType(ElementType.SCRIPTS);
            } else if (lower.equals("step")) {
                // Standalone Script Steps snippets may not include a <Script> wrapper. Treat <Step> as script content
                // so that converters recognize it as a supported Script Steps fmxmlsnippet.
                model.addElementType(ElementType.SCRIPTS);
            }
        }

        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element) {
                walkElement((Element) n, model);
            }
        }
    }

    private static boolean hasChildElements(Element el) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) return true;
        }
        return false;
    }
}
