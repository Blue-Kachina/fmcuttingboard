package dev.fmcuttingboard.fm;

/**
 * Common FileMaker fmxmlsnippet element groupings we care about at a high level.
 * This is intentionally minimal and can be expanded as the plugin evolves.
 */
public enum ElementType {
    FIELDS,
    SCRIPTS,
    TABLES,
    LAYOUTS,
    CUSTOM_FUNCTIONS,
    VALUE_LISTS,
    UNKNOWN
}
