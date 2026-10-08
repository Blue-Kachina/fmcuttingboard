package dev.fmcuttingboard.language;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Metadata for a FileMaker function including name, parameters, category, and description.
 */
public class FunctionMetadata {
    private final String name;
    private final List<FunctionParameter> parameters;
    private final String category;
    private final String returnType;
    private final String description;
    private final int minArgs;
    private final Integer maxArgs;
    private final String helpUrl;

    /** @param maxArgs null = unlimited */
    public FunctionMetadata(String name, List<FunctionParameter> parameters, String category, String returnType, String description,
                            int minArgs, Integer maxArgs, String helpUrl) {
        this.name = name;
        this.parameters = parameters != null ? Collections.unmodifiableList(parameters) : Collections.emptyList();
        this.category = category;
        this.returnType = returnType;
        this.description = description;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.helpUrl = helpUrl;
    }

    public FunctionMetadata(String name, List<FunctionParameter> parameters, String category, String returnType, String description) {
        this(name, parameters, category, returnType, description, 0, null, null);
    }

    public FunctionMetadata(String name, List<FunctionParameter> parameters, String category) {
        this(name, parameters, category, "Any", "");
    }

    public String getName() {
        return name;
    }

    public List<FunctionParameter> getParameters() {
        return parameters;
    }

    public String getCategory() {
        return category;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getDescription() {
        return description;
    }

    public int getMinArgs() {
        return minArgs;
    }

    /** null = unlimited */
    public Integer getMaxArgs() {
        return maxArgs;
    }

    /** Claris help page, or null */
    public String getHelpUrl() {
        return helpUrl;
    }

    /**
     * Maps an argument index onto the parameter to highlight. Arguments past a repeating parameter keep pointing
     * at it (Sum's "field..."), cycling through a repeating group (Case's test, result, test, ...), and the last
     * parameter absorbs any extra arguments. Mirrors activeParameterIndex in vscode/src/core/CallContext.ts.
     */
    public int activeParameterIndex(int argIndex) {
        for (int p = 0; p < parameters.size(); p++) {
            if (p == argIndex) return p;
            FunctionParameter param = parameters.get(p);
            if (param.isRepeating() && argIndex > p) {
                int size = 1;
                String group = param.getGroup();
                while (group != null && p + size < parameters.size() && parameters.get(p + size).isRepeating()
                        && group.equals(parameters.get(p + size).getGroup())) {
                    size++;
                }
                return p + (argIndex - p) % size;
            }
        }
        return Math.max(0, parameters.size() - 1);
    }

    /**
     * Returns the function signature for display in completion/hints.
     * Example: "If(test; resultTrue; resultFalse)"
     */
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append("; ");
            sb.append(parameters.get(i).getDisplayText());
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Returns a simplified signature using parameter names only.
     * Example: "If(test; resultTrue; resultFalse)"
     */
    public String getSimpleSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append("; ");
            sb.append(parameters.get(i).getName());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        return getSignature();
    }

    // Builder pattern for easier construction
    public static class Builder {
        private String name;
        private List<FunctionParameter> parameters;
        private String category;
        private String returnType = "Any";
        private String description = "";
        private int minArgs;
        private Integer maxArgs;
        private String helpUrl;

        public Builder(String name) {
            this.name = name;
        }

        public Builder parameters(FunctionParameter... params) {
            this.parameters = Arrays.asList(params);
            return this;
        }

        public Builder parameters(List<FunctionParameter> params) {
            this.parameters = params;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /** @param maxArgs null = unlimited */
        public Builder argumentCounts(int minArgs, Integer maxArgs) {
            this.minArgs = minArgs;
            this.maxArgs = maxArgs;
            return this;
        }

        public Builder helpUrl(String helpUrl) {
            this.helpUrl = helpUrl;
            return this;
        }

        public FunctionMetadata build() {
            return new FunctionMetadata(name, parameters, category, returnType, description, minArgs, maxArgs, helpUrl);
        }
    }
}
