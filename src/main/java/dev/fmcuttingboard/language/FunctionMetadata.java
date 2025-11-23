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

    public FunctionMetadata(String name, List<FunctionParameter> parameters, String category, String returnType, String description) {
        this.name = name;
        this.parameters = parameters != null ? Collections.unmodifiableList(parameters) : Collections.emptyList();
        this.category = category;
        this.returnType = returnType;
        this.description = description;
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

        public FunctionMetadata build() {
            return new FunctionMetadata(name, parameters, category, returnType, description);
        }
    }
}
