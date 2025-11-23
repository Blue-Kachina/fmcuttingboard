package dev.fmcuttingboard.language;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 1 – 1.2 Build Function Metadata Registry
 *
 * Central registry of FileMaker functions and their parameter metadata.
 * This initial implementation seeds a representative subset of core functions
 * to establish structure and APIs. Subsequent iterations will expand coverage
 * using the VSCode snippets and official documentation.
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

    static {
        Map<String, FunctionMetadata> map = new LinkedHashMap<>();

        // Logical/control
        add(map, new FunctionMetadata.Builder("If")
                .parameters(
                        new FunctionParameter("test", "Any"),
                        new FunctionParameter("resultTrue", "Any"),
                        new FunctionParameter("resultFalse", "Any", true, false)
                )
                .category(CAT_LOGICAL)
                .returnType("Any")
                .description("If(test; resultTrue; [resultFalse])")
                .build());

        add(map, new FunctionMetadata.Builder("Case")
                .parameters(
                        new FunctionParameter("test1", "Any"),
                        new FunctionParameter("result1", "Any"),
                        new FunctionParameter("testN; resultN", "Any", true, true),
                        new FunctionParameter("default", "Any", true, false)
                )
                .category(CAT_LOGICAL)
                .returnType("Any")
                .description("Case(test1; result1; [test2; result2; …]; [default])")
                .build());

        add(map, new FunctionMetadata.Builder("Let")
                .parameters(
                        new FunctionParameter("bindings", "Array"),
                        new FunctionParameter("result", "Any")
                )
                .category(CAT_LOGICAL)
                .returnType("Any")
                .description("Let([name = expr; …]; result)")
                .build());

        // Text
        add(map, new FunctionMetadata.Builder("Left")
                .parameters(new FunctionParameter("text", "Text"), new FunctionParameter("count", "Number"))
                .category(CAT_TEXT)
                .returnType("Text")
                .build());
        add(map, new FunctionMetadata.Builder("Right")
                .parameters(new FunctionParameter("text", "Text"), new FunctionParameter("count", "Number"))
                .category(CAT_TEXT)
                .returnType("Text")
                .build());
        add(map, new FunctionMetadata.Builder("Middle")
                .parameters(
                        new FunctionParameter("text", "Text"),
                        new FunctionParameter("start", "Number"),
                        new FunctionParameter("count", "Number")
                )
                .category(CAT_TEXT)
                .returnType("Text")
                .build());
        add(map, new FunctionMetadata.Builder("Substitute")
                .parameters(
                        new FunctionParameter("text", "Text"),
                        new FunctionParameter("search", "Text"),
                        new FunctionParameter("replace", "Text")
                )
                .category(CAT_TEXT)
                .returnType("Text")
                .build());

        // Math
        add(map, new FunctionMetadata.Builder("Round")
                .parameters(new FunctionParameter("number", "Number"), new FunctionParameter("numDecimals", "Number"))
                .category(CAT_MATH)
                .returnType("Number")
                .build());
        add(map, new FunctionMetadata.Builder("Abs")
                .parameters(new FunctionParameter("number", "Number"))
                .category(CAT_MATH)
                .returnType("Number")
                .build());
        add(map, new FunctionMetadata.Builder("Sum")
                .parameters(new FunctionParameter("field", "Number"), new FunctionParameter("field...", "Number", true, true))
                .category(CAT_AGGREGATE)
                .returnType("Number")
                .build());
        add(map, new FunctionMetadata.Builder("Average")
                .parameters(new FunctionParameter("field", "Number"), new FunctionParameter("field...", "Number", true, true))
                .category(CAT_AGGREGATE)
                .returnType("Number")
                .build());

        // Date/Time
        add(map, new FunctionMetadata.Builder("Date")
                .parameters(new FunctionParameter("month", "Number"), new FunctionParameter("day", "Number"), new FunctionParameter("year", "Number"))
                .category(CAT_DATE_TIME)
                .returnType("Date")
                .build());
        add(map, new FunctionMetadata.Builder("Time")
                .parameters(new FunctionParameter("hour", "Number"), new FunctionParameter("minute", "Number"), new FunctionParameter("second", "Number"))
                .category(CAT_DATE_TIME)
                .returnType("Time")
                .build());
        add(map, new FunctionMetadata.Builder("Timestamp")
                .parameters(new FunctionParameter("date", "Date"), new FunctionParameter("time", "Time"))
                .category(CAT_DATE_TIME)
                .returnType("Timestamp")
                .build());
        add(map, new FunctionMetadata.Builder("Year")
                .parameters(new FunctionParameter("date", "Date"))
                .category(CAT_DATE_TIME)
                .returnType("Number")
                .build());
        add(map, new FunctionMetadata.Builder("Month")
                .parameters(new FunctionParameter("date", "Date"))
                .category(CAT_DATE_TIME)
                .returnType("Number")
                .build());
        add(map, new FunctionMetadata.Builder("Day")
                .parameters(new FunctionParameter("date", "Date"))
                .category(CAT_DATE_TIME)
                .returnType("Number")
                .build());

        // Lists
        add(map, new FunctionMetadata.Builder("List")
                .parameters(new FunctionParameter("value1", "Any"), new FunctionParameter("valueN", "Any", true, true))
                .category(CAT_LIST)
                .returnType("Text")
                .build());
        add(map, new FunctionMetadata.Builder("ValueCount")
                .parameters(new FunctionParameter("values", "Text"))
                .category(CAT_LIST)
                .returnType("Number")
                .build());

        // Data/Fields
        add(map, new FunctionMetadata.Builder("GetField")
                .parameters(new FunctionParameter("fieldName", "Text"))
                .category(CAT_DATA)
                .returnType("Any")
                .build());
        add(map, new FunctionMetadata.Builder("GetValue")
                .parameters(new FunctionParameter("values", "Text"), new FunctionParameter("index", "Number"))
                .category(CAT_LIST)
                .returnType("Text")
                .build());

        // System / Get family
        add(map, new FunctionMetadata.Builder("Get")
                .parameters(new FunctionParameter("name", "Text"))
                .category(CAT_SYSTEM)
                .returnType("Any")
                .description("Get(name) — name is one of the FileMaker environment constants, e.g., AccountName, HostName, LastError, …")
                .build());

        BY_NAME = Collections.unmodifiableMap(map);
        BY_CATEGORY = BY_NAME.values().stream().collect(Collectors.groupingBy(FunctionMetadata::getCategory, LinkedHashMap::new, Collectors.toList()));
    }

    private FileMakerFunctionRegistry() {}

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
