package dev.fmcuttingboard.clipboard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

/**
 * Pure byte-level encoding and decoding of FileMaker clipboard payloads (no IDE or OS dependencies).
 *
 * The rules are specified in {@code shared/data/clipboard-formats.json} and
 * {@code docs/FileMaker-Native-Clipboard-Analysis.md}, and pinned by the shared golden fixtures in
 * {@code shared/fixtures/}. The VS Code extension has a 1:1 TypeScript port; keep the two in step.
 */
public final class FmClipboardCodec {

    private static final String OPEN_TAG = "<fmxmlsnippet";
    private static final String CLOSE_TAG = "</fmxmlsnippet>";

    private FmClipboardCodec() {}

    /** Converts CRLF and lone CR to LF. */
    public static @NotNull String normalizeToLfNewlines(@Nullable String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    /** The bytes for a FileMaker custom format (e.g. Mac-XMSS): newlines normalized, then length-prefixed UTF-8. */
    public static byte @NotNull [] encodeCustomFormatPayload(@Nullable String text) {
        return utf8LengthPrefixedNoBom(normalizeToLfNewlines(text));
    }

    /** [4-byte little-endian payload length] + [UTF-8 payload without BOM], no trailing NUL. */
    public static byte @NotNull [] utf8LengthPrefixedNoBom(@Nullable String s) {
        byte[] payload = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        int len = payload.length;
        byte[] out = new byte[4 + len];
        out[0] = (byte) (len & 0xFF);
        out[1] = (byte) ((len >>> 8) & 0xFF);
        out[2] = (byte) ((len >>> 16) & 0xFF);
        out[3] = (byte) ((len >>> 24) & 0xFF);
        System.arraycopy(payload, 0, out, 4, len);
        return out;
    }

    /** CF_UNICODETEXT bytes: UTF-16LE without BOM, followed by a two-byte NUL terminator. */
    public static byte @NotNull [] utf16leNullTerminated(@Nullable String s) {
        byte[] data = (s == null ? "" : s).getBytes(StandardCharsets.UTF_16LE);
        byte[] out = new byte[data.length + 2];
        System.arraycopy(data, 0, out, 0, data.length);
        return out;
    }

    /**
     * Decodes clipboard bytes of unknown encoding: honours a UTF-8/UTF-16 BOM, otherwise guesses UTF-16
     * from the distribution of zero bytes, otherwise UTF-8. Embedded NULs are removed.
     */
    public static @NotNull String decodeBytesWithBomHeuristics(byte @Nullable [] bytes) {
        if (bytes == null || bytes.length == 0) return "";

        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return stripNulls(new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8));
        }
        if (bytes.length >= 2) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            if (b0 == 0xFE && b1 == 0xFF) {
                return stripNulls(new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE));
            }
            if (b0 == 0xFF && b1 == 0xFE) {
                return stripNulls(new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE));
            }
        }

        // No BOM: 10% or more zero bytes, concentrated on one parity, suggests UTF-16
        int zerosEven = 0, zerosOdd = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                if ((i & 1) == 0) zerosEven++; else zerosOdd++;
            }
        }
        int threshold = Math.max(2, bytes.length / 10);
        if (zerosOdd > zerosEven && zerosOdd >= threshold) {
            return stripNulls(new String(bytes, StandardCharsets.UTF_16LE));
        } else if (zerosEven > zerosOdd && zerosEven >= threshold) {
            return stripNulls(new String(bytes, StandardCharsets.UTF_16BE));
        }

        return stripNulls(new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Locates {@code <fmxmlsnippet … </fmxmlsnippet>} (first opening tag through last closing tag) directly
     * in raw bytes, trying UTF-8 first, then UTF-16LE, then UTF-16BE. Returns the trimmed snippet, or
     * null if none is found. UTF-16 matches must start on an even byte offset, so one encoding is never
     * mistaken for the other.
     */
    public static @Nullable String extractFmxmlFromBytes(byte @Nullable [] bytes) {
        if (bytes == null || bytes.length == 0) return null;

        byte[] startUtf8 = OPEN_TAG.getBytes(StandardCharsets.US_ASCII);
        byte[] endUtf8 = CLOSE_TAG.getBytes(StandardCharsets.US_ASCII);
        int start = indexOf(bytes, startUtf8, 1);
        if (start >= 0) {
            int end = lastIndexOf(bytes, endUtf8, 1);
            if (end >= start) {
                int endPos = end + endUtf8.length;
                return new String(bytes, start, endPos - start, StandardCharsets.UTF_8).trim();
            }
        }

        String le = extractUtf16(bytes, true);
        if (le != null) return le;
        return extractUtf16(bytes, false);
    }

    private static @Nullable String extractUtf16(byte[] bytes, boolean littleEndian) {
        java.nio.charset.Charset cs = littleEndian ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_16BE;
        byte[] startPattern = OPEN_TAG.getBytes(cs);
        byte[] endPattern = CLOSE_TAG.getBytes(cs);
        int start = indexOf(bytes, startPattern, 2);
        if (start < 0) return null;
        int end = lastIndexOf(bytes, endPattern, 2);
        if (end < start) return null;
        int endPos = end + endPattern.length;
        return stripNulls(new String(bytes, start, endPos - start, cs)).trim();
    }

    public static @NotNull String stripNulls(@Nullable String s) {
        return s == null ? "" : s.replace("\u0000", "");
    }

    /** First match at an offset divisible by {@code alignment}, or -1. */
    private static int indexOf(byte[] data, byte[] pattern, int alignment) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i += alignment) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    /** Last match at an offset divisible by {@code alignment}, or -1. */
    private static int lastIndexOf(byte[] data, byte[] pattern, int alignment) {
        int i = data.length - pattern.length;
        i -= i % alignment;
        outer:
        for (; i >= 0; i -= alignment) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
