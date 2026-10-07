package dev.fmcuttingboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.fmcuttingboard.clipboard.ClipboardFormats;
import dev.fmcuttingboard.clipboard.FmClipboardCodec;
import dev.fmcuttingboard.clipboard.SnippetType;
import dev.fmcuttingboard.fm.ConversionException;
import dev.fmcuttingboard.fm.DefaultFileMakerClipboardParser;
import dev.fmcuttingboard.fm.DefaultXmlToClipboardConverter;
import dev.fmcuttingboard.fm.FmSnippet;
import dev.fmcuttingboard.fm.FmXmlParser;
import dev.fmcuttingboard.fm.ParsedSnippet;
import dev.fmcuttingboard.fs.ProjectFiles;
import dev.fmcuttingboard.language.FileMakerCalculationLexerAdapter;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Runs the shared golden fixtures (repo-root shared/fixtures/, on the test classpath via build.gradle.kts).
 * The VS Code extension runs the same files; shared/fixtures/README.md defines what each section means.
 */
public class SharedGoldenFixturesTest {

    private static final HexFormat HEX = HexFormat.of().withUpperCase();

    private static Path fixturesRoot() throws Exception {
        URL url = SharedGoldenFixturesTest.class.getClassLoader().getResource("golden/cases.json");
        assertNotNull(url, "shared/fixtures is not on the test classpath");
        return Path.of(url.toURI()).getParent().getParent();
    }

    private static JsonObject readJson(Path p) throws Exception {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String sha256(byte[] b) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
    }

    private static void assertBytes(JsonObject expected, byte[] actual, String what) throws Exception {
        assertEquals(expected.get("length").getAsInt(), actual.length, what + " length");
        assertEquals(expected.get("headHex").getAsString(),
                HEX.formatHex(actual, 0, Math.min(16, actual.length)), what + " first bytes");
        assertEquals(expected.get("sha256").getAsString(), sha256(actual), what + " sha256");
    }

    private static String crlf(String lf) {
        return lf.replace("\n", "\r\n");
    }

    /** Text from the first {@code <fmxmlsnippet} through the end of the last {@code </fmxmlsnippet>}. */
    private static String snippetOf(String text) {
        int start = text.indexOf("<fmxmlsnippet");
        int end = text.lastIndexOf("</fmxmlsnippet>") + "</fmxmlsnippet>".length();
        return text.substring(start, end);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(a);
        out.writeBytes(b);
        return out.toByteArray();
    }

    private static byte[] buildVariant(String variant, String text) {
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        byte[] le = text.getBytes(StandardCharsets.UTF_16LE);
        byte[] be = text.getBytes(StandardCharsets.UTF_16BE);
        return switch (variant) {
            case "utf8" -> utf8;
            case "utf8-bom" -> concat(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, utf8);
            case "utf16le" -> le;
            case "utf16le-bom" -> concat(new byte[]{(byte) 0xFF, (byte) 0xFE}, le);
            case "utf16le-nul-terminated" -> concat(le, new byte[2]);
            case "utf16be" -> be;
            case "utf16be-bom" -> concat(new byte[]{(byte) 0xFE, (byte) 0xFF}, be);
            case "custom-format-payload" -> FmClipboardCodec.encodeCustomFormatPayload(text);
            default -> throw new IllegalArgumentException("Unknown byte variant in cases.json: " + variant);
        };
    }

    private static byte[] inputBytes(JsonObject c) {
        if (c.has("hex")) return HexFormat.of().parseHex(c.get("hex").getAsString());
        String text = c.get("text").getAsString();
        return switch (c.get("encoding").getAsString()) {
            case "utf8" -> text.getBytes(StandardCharsets.UTF_8);
            case "utf16le" -> text.getBytes(StandardCharsets.UTF_16LE);
            case "utf16be" -> text.getBytes(StandardCharsets.UTF_16BE);
            default -> throw new IllegalArgumentException("Unknown encoding in cases.json: " + c.get("encoding"));
        };
    }

    private static String expectedOrNull(JsonObject c) {
        JsonElement e = c.get("expect");
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    @TestFactory
    Stream<DynamicTest> snippetFixtures() throws Exception {
        Path root = fixturesRoot();
        JsonObject golden = readJson(root.resolve("golden/snippets.generated.json"));
        JsonObject cases = readJson(root.resolve("golden/cases.json"));
        List<DynamicTest> tests = new ArrayList<>();

        for (JsonElement ce : golden.getAsJsonArray("cases")) {
            JsonObject c = ce.getAsJsonObject();
            String id = c.get("id").getAsString();
            JsonObject expect = c.getAsJsonObject("expect");
            String text = Files.readString(root.resolve(c.get("fixture").getAsString()), StandardCharsets.UTF_8);

            tests.add(dynamicTest(id + ": detection and format names", () -> {
                SnippetType type = ClipboardFormats.detectSnippetType(text);
                assertEquals(expect.get("snippetType").getAsString(), type.name());
                List<String> formats = new ArrayList<>();
                expect.getAsJsonArray("windowsFormats").forEach(f -> formats.add(f.getAsString()));
                assertEquals(formats, ClipboardFormats.windowsFormatNames(type));
            }));
            tests.add(dynamicTest(id + ": custom format payload (LF and CRLF input)", () -> {
                assertBytes(expect.getAsJsonObject("customFormatPayload"), FmClipboardCodec.encodeCustomFormatPayload(text), "LF");
                assertBytes(expect.getAsJsonObject("customFormatPayload"), FmClipboardCodec.encodeCustomFormatPayload(crlf(text)), "CRLF");
            }));
            tests.add(dynamicTest(id + ": CF_UNICODETEXT bytes", () -> {
                JsonObject ut = expect.getAsJsonObject("unicodeText");
                assertBytes(ut.getAsJsonObject("lf"), FmClipboardCodec.utf16leNullTerminated(text), "LF");
                assertBytes(ut.getAsJsonObject("crlf"), FmClipboardCodec.utf16leNullTerminated(crlf(text)), "CRLF");
            }));

            for (JsonElement ve : cases.getAsJsonObject("byteVariants").getAsJsonArray("variants")) {
                JsonObject v = ve.getAsJsonObject();
                String variant = v.get("id").getAsString();
                for (String endings : List.of("lf", "crlf")) {
                    String input = endings.equals("lf") ? text : crlf(text);
                    tests.add(dynamicTest(id + ": " + variant + " (" + endings + ")", () -> {
                        byte[] bytes = buildVariant(variant, input);
                        if (v.get("decode").getAsString().equals("text")) {
                            assertEquals(input, FmClipboardCodec.decodeBytesWithBomHeuristics(bytes), "decode");
                        }
                        String expectedSnippet = switch (v.get("extract").getAsString()) {
                            case "snippet" -> snippetOf(input);
                            case "lf-snippet" -> snippetOf(FmClipboardCodec.normalizeToLfNewlines(input));
                            default -> throw new IllegalArgumentException("Unknown extract expectation: " + v.get("extract"));
                        };
                        assertEquals(expectedSnippet, FmClipboardCodec.extractFmxmlFromBytes(bytes), "extract");
                    }));
                }
            }
        }
        return tests.stream();
    }

    @TestFactory
    Stream<DynamicTest> handWrittenCases() throws Exception {
        JsonObject cases = readJson(fixturesRoot().resolve("golden/cases.json"));
        List<DynamicTest> tests = new ArrayList<>();
        DefaultFileMakerClipboardParser parser = new DefaultFileMakerClipboardParser();

        for (JsonElement e : cases.getAsJsonArray("detection")) {
            JsonObject c = e.getAsJsonObject();
            tests.add(dynamicTest("detection: " + c.get("id").getAsString(), () ->
                    assertEquals(c.get("expect").getAsString(),
                            ClipboardFormats.detectSnippetType(c.get("text").getAsString()).name())));
        }
        for (JsonElement e : cases.getAsJsonArray("normalizeToXmlText")) {
            JsonObject c = e.getAsJsonObject();
            tests.add(dynamicTest("normalizeToXmlText: " + c.get("id").getAsString(), () ->
                    assertEquals(expectedOrNull(c), parser.normalizeToXmlText(c.get("input").getAsString()).orElse(null))));
        }
        for (JsonElement e : cases.getAsJsonArray("decodeBytesWithBomHeuristics")) {
            JsonObject c = e.getAsJsonObject();
            tests.add(dynamicTest("decode: " + c.get("id").getAsString(), () ->
                    assertEquals(expectedOrNull(c), FmClipboardCodec.decodeBytesWithBomHeuristics(inputBytes(c)))));
        }
        for (JsonElement e : cases.getAsJsonArray("extractFmxmlFromBytes")) {
            JsonObject c = e.getAsJsonObject();
            tests.add(dynamicTest("extract: " + c.get("id").getAsString(), () ->
                    assertEquals(expectedOrNull(c), FmClipboardCodec.extractFmxmlFromBytes(inputBytes(c)))));
        }
        for (JsonElement e : cases.getAsJsonArray("parseSnippet")) {
            JsonObject c = e.getAsJsonObject();
            tests.add(dynamicTest("parseSnippet: " + c.get("id").getAsString(), () -> assertParseCase(c)));
        }
        for (JsonElement e : cases.getAsJsonArray("fmSnippetDetectTypes")) {
            JsonObject c = e.getAsJsonObject();
            tests.add(dynamicTest("fmSnippetDetectTypes: " + c.get("id").getAsString(), () ->
                    assertEquals(strings(c, "expect"),
                            FmSnippet.detectTypes(c.get("xml").getAsString()).stream().map(Enum::name).toList())));
        }
        for (JsonElement e : cases.getAsJsonArray("lexer")) {
            JsonObject c = e.getAsJsonObject();
            tests.add(dynamicTest("lexer: " + c.get("id").getAsString(), () ->
                    assertEquals(strings(c, "tokens"), lexerTokens(c.get("calc").getAsString()))));
        }
        for (JsonElement e : cases.getAsJsonArray("fileNaming")) {
            JsonObject c = e.getAsJsonObject();
            tests.add(dynamicTest("fileNaming: " + c.get("id").getAsString(), () -> {
                String pattern = c.get("pattern").isJsonNull() ? null : c.get("pattern").getAsString();
                String base = ProjectFiles.resolveFileName(pattern, c.get("extension").getAsString(), c.get("nowMillis").getAsLong());
                List<String> existing = strings(c, "existing");
                assertEquals(c.get("expect").getAsString(), ProjectFiles.uniqueFileName(base, existing::contains));
            }));
        }
        return tests.stream();
    }

    /** Non-whitespace tokens as TYPE:text, with consecutive BLOCK_COMMENT tokens merged (cases.json "lexer"). */
    private static List<String> lexerTokens(String calc) {
        Lexer lexer = new FileMakerCalculationLexerAdapter();
        lexer.start(calc);
        List<String> types = new ArrayList<>();
        List<StringBuilder> texts = new ArrayList<>();
        for (IElementType t = lexer.getTokenType(); t != null; lexer.advance(), t = lexer.getTokenType()) {
            if (t == FileMakerCalculationTokenType.WHITE_SPACE) continue;
            String type = t.toString();
            String text = calc.substring(lexer.getTokenStart(), lexer.getTokenEnd());
            int last = types.size() - 1;
            if (type.equals("BLOCK_COMMENT") && last >= 0 && types.get(last).equals("BLOCK_COMMENT")) {
                texts.get(last).append(text);
            } else {
                types.add(type);
                texts.add(new StringBuilder(text));
            }
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) out.add(types.get(i) + ":" + texts.get(i));
        return out;
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        o.getAsJsonArray(key).forEach(x -> out.add(x.getAsString()));
        return out;
    }

    private static String stringOrNull(JsonObject o, String key) {
        return o.get(key).isJsonNull() ? null : o.get(key).getAsString();
    }

    private static void assertParseCase(JsonObject c) throws Exception {
        String xml = c.get("xml").getAsString();
        if (c.has("error")) {
            ConversionException ex = assertThrows(ConversionException.class, () -> new FmXmlParser().parse(xml));
            assertEquals(c.get("error").getAsString(), ex.getMessage());
            return;
        }
        ParsedSnippet model = new FmXmlParser().parse(xml);
        JsonObject expect = c.getAsJsonObject("expect");
        assertEquals(strings(expect, "elementTypes"), model.getElementTypes().stream().map(Enum::name).toList(), "elementTypes");
        assertEquals(stringOrNull(expect, "version"), model.getVersion(), "version");
        assertEquals(stringOrNull(expect, "typeHint"), model.getTypeHint(), "typeHint");
        assertEquals(strings(expect, "fieldNames"), model.getFieldNames(), "fieldNames");
        assertEquals(strings(expect, "layoutNames"), model.getLayoutNames(), "layoutNames");
        assertEquals(strings(expect, "scriptNames"), model.getScriptNames(), "scriptNames");

        DefaultXmlToClipboardConverter converter = new DefaultXmlToClipboardConverter();
        if (c.has("payloadError")) {
            ConversionException ex = assertThrows(ConversionException.class, () -> converter.convertToClipboardPayload(xml));
            assertEquals(c.get("payloadError").getAsString(), ex.getMessage());
        } else {
            assertEquals(c.get("payload").getAsString(), converter.convertToClipboardPayload(xml));
        }
    }

    /** Raw captures from real FileMaker: our encoding must reproduce FileMaker's bytes exactly. */
    @TestFactory
    Stream<DynamicTest> rawFileMakerCaptures() throws Exception {
        Path clipboardDir = fixturesRoot().resolve("clipboard");
        List<DynamicTest> tests = new ArrayList<>();
        if (!Files.isDirectory(clipboardDir)) return tests.stream();
        List<Path> captures;
        try (Stream<Path> s = Files.walk(clipboardDir)) {
            captures = s.filter(p -> p.getFileName().toString().equals("capture.json")).sorted().toList();
        }
        for (Path captureJson : captures) {
            Path dir = captureJson.getParent();
            String typeFolder = clipboardDir.relativize(dir).getName(0).toString();
            JsonObject capture = readJson(captureJson);
            for (JsonElement fe : capture.getAsJsonArray("formats")) {
                JsonObject f = fe.getAsJsonObject();
                String name = f.get("name").getAsString();
                Path bin = dir.resolve(f.get("file").getAsString());
                if (!ClipboardFormats.isFileMakerWindowsFormat(name) || !Files.exists(bin)) continue;
                tests.add(dynamicTest(clipboardDir.relativize(dir) + ": " + name, () -> {
                    byte[] raw = Files.readAllBytes(bin);
                    assertTrue(raw.length >= 4, "payload shorter than its length prefix");
                    int declared = (raw[0] & 0xFF) | (raw[1] & 0xFF) << 8 | (raw[2] & 0xFF) << 16 | (raw[3] & 0xFF) << 24;
                    assertEquals(raw.length - 4, declared, "length prefix");
                    String xml = FmClipboardCodec.extractFmxmlFromBytes(raw);
                    assertNotNull(xml, "no fmxmlsnippet found");
                    assertEquals(folderType(typeFolder), ClipboardFormats.detectSnippetType(xml).name(), "snippet type");
                    String payload = new String(raw, 4, raw.length - 4, StandardCharsets.UTF_8);
                    assertArrayEquals(raw, FmClipboardCodec.encodeCustomFormatPayload(payload), "re-encoded bytes differ from FileMaker's");
                }));
            }
        }
        return tests.stream();
    }

    private static String folderType(String folder) {
        return switch (folder) {
            case "script" -> "SCRIPT";
            case "script-steps" -> "SCRIPT_STEPS";
            case "fields" -> "FIELD_DEFINITION";
            case "tables" -> "TABLE_DEFINITION";
            case "layout-objects" -> "LAYOUT_OBJECTS";
            case "custom-functions" -> "CUSTOM_FUNCTION";
            case "value-lists" -> "VALUE_LIST";
            default -> throw new IllegalArgumentException("Unknown capture folder: " + folder);
        };
    }
}
