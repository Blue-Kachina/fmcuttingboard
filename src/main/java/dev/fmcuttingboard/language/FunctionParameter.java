package dev.fmcuttingboard.language;

/**
 * Represents a parameter for a FileMaker function.
 */
public class FunctionParameter {
    private final String name;
    private final String type;
    private final boolean optional;
    private final boolean repeating;

    public FunctionParameter(String name, String type, boolean optional, boolean repeating) {
        this.name = name;
        this.type = type;
        this.optional = optional;
        this.repeating = repeating;
    }

    public FunctionParameter(String name, String type) {
        this(name, type, false, false);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isRepeating() {
        return repeating;
    }

    /**
     * Returns a formatted parameter string for display in completion/hints.
     * Examples: "number", "[optional]", "field..."
     */
    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        if (optional) sb.append("[");
        sb.append(name);
        if (repeating) sb.append("...");
        if (optional) sb.append("]");
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}
