package dev.fmcuttingboard.clipboard;

/**
 * fmxmlsnippet content types, used to pick FileMaker's native clipboard format.
 * Constant names must match the {@code id} values in {@code shared/data/clipboard-formats.json}.
 */
public enum SnippetType {
    SCRIPT,
    SCRIPT_STEPS,
    FIELD_DEFINITION,
    TABLE_DEFINITION,
    CUSTOM_FUNCTION,
    VALUE_LIST,
    LAYOUT_OBJECTS,
    UNKNOWN
}
