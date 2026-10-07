package dev.fmcuttingboard.clipboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.fmcuttingboard.shared.SharedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * FileMaker clipboard format knowledge: snippet-type detection rules and Windows format names.
 *
 * All of it comes from the shared {@code shared/data/clipboard-formats.json}, which the VS Code
 * extension also reads. Change format names or detection order there, not here.
 */
public final class ClipboardFormats {

    /** One entry of {@code snippetTypes}, in detection order. */
    public record TypeRule(@NotNull SnippetType type,
                           @NotNull String label,
                           @NotNull List<String> detectIfContainsAny,
                           @NotNull String windowsFormat,
                           @NotNull List<String> windowsAliases) {}

    private static final class Holder {
        static final List<TypeRule> RULES;
        static final int MAX_CUSTOM_PAYLOAD_BYTES;

        static {
            JsonObject root = SharedData.readJson(SharedData.CLIPBOARD_FORMATS);
            List<TypeRule> rules = new ArrayList<>();
            for (JsonElement e : root.getAsJsonArray("snippetTypes")) {
                JsonObject t = e.getAsJsonObject();
                JsonObject win = t.getAsJsonObject("windows");
                rules.add(new TypeRule(
                        SnippetType.valueOf(t.get("id").getAsString()),
                        t.get("label").getAsString(),
                        strings(t, "detectIfContainsAny"),
                        win.get("format").getAsString(),
                        strings(win, "aliases")));
            }
            RULES = Collections.unmodifiableList(rules);
            MAX_CUSTOM_PAYLOAD_BYTES = root.getAsJsonObject("windows")
                    .getAsJsonObject("customFormatPayload").get("maxBytes").getAsInt();
        }

        private static List<String> strings(JsonObject o, String key) {
            List<String> out = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray(key)) out.add(e.getAsString());
            return Collections.unmodifiableList(out);
        }
    }

    private ClipboardFormats() {}

    public static @NotNull List<TypeRule> rules() {
        return Holder.RULES;
    }

    /** First rule (in shared-data order) with a case-sensitive substring match wins. */
    public static @NotNull SnippetType detectSnippetType(@Nullable String text) {
        if (text == null || text.isEmpty()) return SnippetType.UNKNOWN;
        for (TypeRule rule : Holder.RULES) {
            for (String marker : rule.detectIfContainsAny()) {
                if (text.contains(marker)) return rule.type();
            }
        }
        return SnippetType.UNKNOWN;
    }

    /** Primary Windows format name followed by any aliases; empty for UNKNOWN. */
    public static @NotNull List<String> windowsFormatNames(@NotNull SnippetType type) {
        for (TypeRule rule : Holder.RULES) {
            if (rule.type() == type) {
                List<String> names = new ArrayList<>();
                names.add(rule.windowsFormat());
                names.addAll(rule.windowsAliases());
                return names;
            }
        }
        return Collections.emptyList();
    }

    /** Every FileMaker Windows format name (primaries and aliases), in shared-data order. */
    public static @NotNull Set<String> allWindowsFormatNames() {
        Set<String> names = new LinkedHashSet<>();
        for (TypeRule rule : Holder.RULES) {
            names.add(rule.windowsFormat());
            names.addAll(rule.windowsAliases());
        }
        return names;
    }

    public static boolean isFileMakerWindowsFormat(@Nullable String name) {
        if (name == null) return false;
        for (String known : allWindowsFormatNames()) {
            if (known.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static int maxCustomPayloadBytes() {
        return Holder.MAX_CUSTOM_PAYLOAD_BYTES;
    }
}
