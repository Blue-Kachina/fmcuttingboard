package dev.fmcuttingboard.fm;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Phase 3.1 — Core Conversion Logic
 * Converts raw clipboard text into a normalized fmxmlsnippet XML string and
 * a lightweight internal model (FmSnippet).
 */
public class ClipboardToXmlConverter {

    private final FileMakerClipboardParser parser;

    public ClipboardToXmlConverter() {
        this(new DefaultFileMakerClipboardParser());
    }

    public ClipboardToXmlConverter(FileMakerClipboardParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    /**
     * Extracts fmxmlsnippet from clipboard text or throws ConversionException if not found or invalid.
     */
    public FmSnippet convert(String clipboardText) throws ConversionException {
        try {
            return parser.normalizeToXmlText(clipboardText)
                    .map(xml -> {
                        EnumSet<ElementType> types = FmSnippet.detectTypes(xml);
                        return new FmSnippet(xml, types);
                    })
                    .orElseThrow(() -> new ConversionException("Clipboard does not contain a recognizable FileMaker fmxmlsnippet."));
        } catch (ConversionException ce) {
            throw ce;
        } catch (Throwable t) {
            throw new ConversionException("Unexpected error during conversion.", t);
        }
    }

    /**
     * Convenience method returning only the XML string.
     */
    public String convertToXml(String clipboardText) throws ConversionException {
        return convert(clipboardText).getXml();
    }
}
