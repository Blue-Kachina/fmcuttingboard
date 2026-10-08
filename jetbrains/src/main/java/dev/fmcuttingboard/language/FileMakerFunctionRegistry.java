package dev.fmcuttingboard.language;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.fmcuttingboard.shared.SharedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central registry of FileMaker functions and their parameter metadata.
 *
 * The data is the repository's {@code shared/data/fm-calc-catalogue.json}, vendored from fmscriptinventory by
 * {@code shared/tools/sync-calc-catalogue.mjs} (docs/fm-calc-catalogue-contract.md), which the VS Code extension
 * also reads. It lists every built-in function, so a name that is not here is a custom or plug-in function.
 */
public final class FileMakerFunctionRegistry {

    /**
     * Display names for the catalogue's data types. Must match vscode/src/core/FileMakerFunctionRegistry.ts and
     * shared/tools/generate-function-signatures.mjs, which writes the shared baseline.
     */
    private static final Map<String, String> TYPE_LABELS = Map.ofEntries(
            Map.entry("text", "Text"),
            Map.entry("number", "Number"),
            Map.entry("date", "Date"),
            Map.entry("time", "Time"),
            Map.entry("timestamp", "Timestamp"),
            Map.entry("container", "Container"),
            Map.entry("boolean", "Boolean"),
            Map.entry("json", "JSON"),
            Map.entry("any", "Any"),
            Map.entry("expression", "Expression"),
            Map.entry("fieldReference", "Field"),
            Map.entry("variableBindings", "Bindings"));

    private static final Map<String, FunctionMetadata> BY_NAME;
    private static final Map<String, List<FunctionMetadata>> BY_CATEGORY;

    static {
        Map<String, FunctionMetadata> map = new LinkedHashMap<>();
        for (FunctionMetadata m : parse(SharedData.readJson(SharedData.CALC_CATALOGUE))) {
            add(map, m);
        }
        BY_NAME = Collections.unmodifiableMap(map);
        BY_CATEGORY = BY_NAME.values().stream().collect(Collectors.groupingBy(FunctionMetadata::getCategory, LinkedHashMap::new, Collectors.toList()));
    }

    private FileMakerFunctionRegistry() {}

    static @NotNull String typeLabel(@NotNull String type) {
        return TYPE_LABELS.getOrDefault(type, type);
    }

    /** "Text functions" -> "Text" */
    static @NotNull String categoryLabel(@NotNull String label) {
        return label.replaceFirst("(?i) functions$", "");
    }

    /** Converts the vendored catalogue into metadata objects, preserving its order (sorted by name). */
    static @NotNull List<FunctionMetadata> parse(@NotNull JsonObject root) {
        Map<String, String> categories = new HashMap<>();
        for (JsonElement ce : root.getAsJsonArray("categories")) {
            JsonObject c = ce.getAsJsonObject();
            categories.put(c.get("key").getAsString(), categoryLabel(c.get("label").getAsString()));
        }
        List<FunctionMetadata> out = new ArrayList<>();
        for (JsonElement fe : root.getAsJsonArray("functions")) {
            JsonObject f = fe.getAsJsonObject();
            List<FunctionParameter> params = new ArrayList<>();
            for (JsonElement pe : f.getAsJsonArray("parameters")) {
                JsonObject p = pe.getAsJsonObject();
                params.add(new FunctionParameter(
                        p.get("name").getAsString(),
                        typeLabel(p.get("type").getAsString()),
                        p.get("optional").getAsBoolean(),
                        p.get("repeatable").getAsBoolean(),
                        p.has("group") ? p.get("group").getAsString() : null));
            }
            String category = f.get("category").getAsString();
            JsonElement max = f.get("maxArgs");
            out.add(new FunctionMetadata.Builder(f.get("name").getAsString())
                    .parameters(params)
                    .category(categories.getOrDefault(category, category))
                    .returnType(typeLabel(f.get("returnType").getAsString()))
                    .description(f.has("summary") ? f.get("summary").getAsString() : "")
                    .argumentCounts(f.get("minArgs").getAsInt(), max == null || max.isJsonNull() ? null : max.getAsInt())
                    .helpUrl(f.get("helpUrl").getAsString())
                    .build());
        }
        return out;
    }

    private static void add(Map<String, FunctionMetadata> map, FunctionMetadata m) {
        map.put(m.getName().toLowerCase(Locale.ROOT), m);
    }

    public static @NotNull Collection<FunctionMetadata> getAll() {
        return BY_NAME.values();
    }

    public static int size() { return BY_NAME.size(); }

    public static @Nullable FunctionMetadata findByName(@NotNull String name) {
        return BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    public static @NotNull List<FunctionMetadata> getByCategory(@NotNull String category) {
        return BY_CATEGORY.getOrDefault(category, Collections.emptyList());
    }

    public static @NotNull Set<String> getFunctionNames() {
        return BY_NAME.values().stream().map(FunctionMetadata::getName).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
