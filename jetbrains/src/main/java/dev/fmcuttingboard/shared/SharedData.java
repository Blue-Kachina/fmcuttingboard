package dev.fmcuttingboard.shared;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Loads the platform-independent data files shared with the VS Code extension.
 *
 * The canonical copies live in the repository's {@code shared/data/} folder; the Gradle build packages
 * them into the plugin jar under {@code /shared/}. Both plugins must read these files rather than
 * hardcoding the same knowledge, so the two implementations can't drift apart.
 */
public final class SharedData {

    public static final String FILEMAKER_FUNCTIONS = "filemaker-functions.json";
    public static final String CLIPBOARD_FORMATS = "clipboard-formats.json";

    private SharedData() {}

    /** Reads a shared JSON data file. Throws if it is missing, which indicates a packaging bug. */
    public static @NotNull JsonObject readJson(@NotNull String fileName) {
        String resource = "/shared/" + fileName;
        try (InputStream in = SharedData.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Shared data file missing from plugin: " + resource);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read shared data file: " + resource, e);
        }
    }
}
