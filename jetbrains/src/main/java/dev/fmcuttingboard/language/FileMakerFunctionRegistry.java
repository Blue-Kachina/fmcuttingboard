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
 * The data itself lives in the repository's {@code shared/data/filemaker-functions.json}, which the
 * VS Code extension also reads. Add or correct functions there, not here.
 */
public final class FileMakerFunctionRegistry {

    public static final String CAT_LOGICAL = "Logical";
    public static final String CAT_TEXT = "Text";
    public static final String CAT_MATH = "Math";
    public static final String CAT_DATE_TIME = "Date/Time";
    public static final String CAT_AGGREGATE = "Aggregate";
    public static final String CAT_DATA = "Data/Fields";
    public static final String CAT_LIST = "List";
    public static final String CAT_SYSTEM = "Get()";

    private static final Map<String, FunctionMetadata> BY_NAME;
    private static final Map<String, List<FunctionMetadata>> BY_CATEGORY;
    private static final boolean COMPLETE;

    static {
        Map<String, FunctionMetadata> map = new LinkedHashMap<>();
        JsonObject root = SharedData.readJson(SharedData.FILEMAKER_FUNCTIONS);
        COMPLETE = root.get("complete").getAsBoolean();
        for (FunctionMetadata m : parse(root)) {
            add(map, m);
        }
        BY_NAME = Collections.unmodifiableMap(map);
        BY_CATEGORY = BY_NAME.values().stream().collect(Collectors.groupingBy(FunctionMetadata::getCategory, LinkedHashMap::new, Collectors.toList()));
    }

    private FileMakerFunctionRegistry() {}

    /** Converts the shared {@code filemaker-functions.json} document into metadata objects, preserving file order. */
    static @NotNull List<FunctionMetadata> parse(@NotNull JsonObject root) {
        List<FunctionMetadata> out = new ArrayList<>();
        for (JsonElement fe : root.getAsJsonArray("functions")) {
            JsonObject f = fe.getAsJsonObject();
            List<FunctionParameter> params = new ArrayList<>();
            for (JsonElement pe : f.getAsJsonArray("parameters")) {
                JsonObject p = pe.getAsJsonObject();
                params.add(new FunctionParameter(
                        p.get("name").getAsString(),
                        p.get("type").getAsString(),
                        p.has("optional") && p.get("optional").getAsBoolean(),
                        p.has("repeating") && p.get("repeating").getAsBoolean()));
            }
            out.add(new FunctionMetadata.Builder(f.get("name").getAsString())
                    .parameters(params)
                    .category(f.get("category").getAsString())
                    .returnType(f.get("returnType").getAsString())
                    .description(f.has("description") ? f.get("description").getAsString() : "")
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

    /** True only when the shared data lists every FileMaker function, so an unknown name is really unknown. */
    public static boolean isComplete() { return COMPLETE; }

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
