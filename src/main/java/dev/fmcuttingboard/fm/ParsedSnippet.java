package dev.fmcuttingboard.fm;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Lightweight internal model for a parsed fmxmlsnippet.
 * Designed to be a bridge between XML and FileMaker clipboard format reconstruction.
 */
public class ParsedSnippet {
    private String rawXml;
    private String version;
    private String typeHint;

    private final EnumSet<ElementType> elementTypes = EnumSet.noneOf(ElementType.class);

    // Minimal collections for supported groups — extendable in later phases
    private final List<String> fieldNames = new ArrayList<>();
    private final List<String> layoutNames = new ArrayList<>();
    private final List<String> scriptNames = new ArrayList<>();

    public String getRawXml() {
        return rawXml;
    }

    public void setRawXml(String rawXml) {
        this.rawXml = Objects.requireNonNull(rawXml, "rawXml");
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTypeHint() {
        return typeHint;
    }

    public void setTypeHint(String typeHint) {
        this.typeHint = typeHint;
    }

    public EnumSet<ElementType> getElementTypes() {
        return EnumSet.copyOf(elementTypes);
    }

    public void addElementType(ElementType type) {
        if (type != null) this.elementTypes.add(type);
    }

    public List<String> getFieldNames() { return List.copyOf(fieldNames); }
    public List<String> getLayoutNames() { return List.copyOf(layoutNames); }
    public List<String> getScriptNames() { return List.copyOf(scriptNames); }

    public void addFieldName(String name) {
        if (name != null && !name.isBlank()) {
            fieldNames.add(name);
            elementTypes.add(ElementType.FIELDS);
        }
    }

    public void addLayoutName(String name) {
        if (name != null && !name.isBlank()) {
            layoutNames.add(name);
            elementTypes.add(ElementType.LAYOUTS);
        }
    }

    public void addScriptName(String name) {
        if (name != null && !name.isBlank()) {
            scriptNames.add(name);
            elementTypes.add(ElementType.SCRIPTS);
        }
    }
}
