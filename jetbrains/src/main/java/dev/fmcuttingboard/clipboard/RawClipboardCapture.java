package dev.fmcuttingboard.clipboard;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Writes a raw clipboard capture to disk: one {@code .bin} file per clipboard format plus a
 * {@code capture.json} index. Captures become golden fixtures under {@code shared/fixtures/clipboard/};
 * see {@code shared/fixtures/README.md}.
 */
public final class RawClipboardCapture {

    /** One clipboard format's raw bytes. {@code name} is null for unregistered, non-standard formats. */
    public record FormatBytes(int id, @Nullable String name, byte @NotNull [] bytes) {}

    private static final Map<Integer, String> STANDARD_FORMAT_NAMES = Map.ofEntries(
            Map.entry(1, "CF_TEXT"),
            Map.entry(2, "CF_BITMAP"),
            Map.entry(7, "CF_OEMTEXT"),
            Map.entry(8, "CF_DIB"),
            Map.entry(13, "CF_UNICODETEXT"),
            Map.entry(16, "CF_LOCALE"),
            Map.entry(17, "CF_DIBV5"));

    private RawClipboardCapture() {}

    /**
     * Writes {@code formats} into a new folder {@code capture-<epochMillis>} under {@code parentDir}.
     * Returns the folder.
     */
    public static @NotNull Path write(@NotNull Path parentDir, @NotNull List<FormatBytes> formats,
                                      @NotNull Instant capturedAt, @NotNull String source) throws IOException {
        Path dir = parentDir.resolve("capture-" + capturedAt.toEpochMilli());
        Files.createDirectories(dir);

        JsonArray entries = new JsonArray();
        SnippetType detected = SnippetType.UNKNOWN;
        Set<String> usedFileNames = new HashSet<>();
        for (FormatBytes f : formats) {
            String displayName = displayName(f);
            String fileName = uniqueFileName(sanitize(displayName) + ".bin", usedFileNames);
            Files.write(dir.resolve(fileName), f.bytes());

            JsonObject e = new JsonObject();
            e.addProperty("name", displayName);
            e.addProperty("id", f.id());
            e.addProperty("size", f.bytes().length);
            e.addProperty("file", fileName);
            boolean fileMakerFormat = ClipboardFormats.isFileMakerWindowsFormat(f.name());
            e.addProperty("fileMakerFormat", fileMakerFormat);
            entries.add(e);

            if (fileMakerFormat && detected == SnippetType.UNKNOWN) {
                detected = ClipboardFormats.detectSnippetType(FmClipboardCodec.extractFmxmlFromBytes(f.bytes()));
            }
        }

        JsonObject root = new JsonObject();
        root.addProperty("capturedAt", capturedAt.toString());
        root.addProperty("source", source);
        root.addProperty("os", System.getProperty("os.name", "") + " " + System.getProperty("os.version", ""));
        root.addProperty("fileMakerVersion", "");
        root.addProperty("detectedSnippetType", detected.name());
        root.addProperty("notes", "Fill in fileMakerVersion and what was copied before adding this to shared/fixtures/clipboard/.");
        root.add("formats", entries);
        String json = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root) + "\n";
        Files.writeString(dir.resolve("capture.json"), json, StandardCharsets.UTF_8);
        return dir;
    }

    static @NotNull String displayName(@NotNull FormatBytes f) {
        if (f.name() != null && !f.name().isBlank()) return f.name();
        return STANDARD_FORMAT_NAMES.getOrDefault(f.id(), "format-" + f.id());
    }

    static @NotNull String sanitize(@NotNull String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String uniqueFileName(String candidate, Set<String> used) {
        String name = candidate;
        int n = 2;
        while (!used.add(name.toLowerCase())) {
            name = candidate.replaceFirst("\\.bin$", "-" + n++ + ".bin");
        }
        return name;
    }
}
