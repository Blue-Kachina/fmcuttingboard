package dev.fmcuttingboard.fm;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Internal data model representing a parsed FileMaker clipboard snippet.
 */
public class FmSnippet {
    private final String xml;
    private final EnumSet<ElementType> elementTypes;

    public FmSnippet(String xml, EnumSet<ElementType> elementTypes) {
        this.xml = Objects.requireNonNull(xml, "xml");
        this.elementTypes = elementTypes == null ? EnumSet.of(ElementType.UNKNOWN) : EnumSet.copyOf(elementTypes);
    }

    public String getXml() {
        return xml;
    }

    public EnumSet<ElementType> getElementTypes() {
        return EnumSet.copyOf(elementTypes);
    }

    /**
     * Very lightweight heuristic to infer common element groupings from the snippet.
     */
    public static EnumSet<ElementType> detectTypes(String xml) {
        if (xml == null || xml.isBlank()) return EnumSet.of(ElementType.UNKNOWN);
        String s = xml.toLowerCase();
        EnumSet<ElementType> set = EnumSet.noneOf(ElementType.class);

        // Fields — FileMaker exports often contain <Field> or <FieldDefinition>
        if (s.contains("<field") || s.contains("<fielddefinition")) {
            set.add(ElementType.FIELDS);
        }

        // Scripts — presence of <Script> or <Step>
        if (s.contains("<script") || s.contains("<step")) {
            set.add(ElementType.SCRIPTS);
        }

        // Layouts — presence of <Layout> or LayoutObjects
        if (s.contains("<layout") || s.contains("layoutobjects") || s.contains("<object")) {
            set.add(ElementType.LAYOUTS);
        }

        if (set.isEmpty()) set.add(ElementType.UNKNOWN);
        return set;
    }
}
