package dev.fmcuttingboard.language;

import com.google.gson.JsonElement;
import com.intellij.lexer.FlexAdapter;
import com.intellij.psi.tree.IElementType;
import dev.fmcuttingboard.shared.SharedData;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Wraps the generic JFlex lexer and classifies plain identifiers the same way the VS Code grammar does:
 * <ul>
 *   <li>a name followed by {@code (} is a function call ({@link FileMakerCalculationTokenType#KEYWORD_FUNCTION}),
 *       whether or not it is a known built-in (custom functions look the same);</li>
 *   <li>a name inside {@code Get ( … )} is a {@link FileMakerCalculationTokenType#GET_CONSTANT};</li>
 *   <li>a name listed in shared/data/fm-calc-catalogue.json constants (True, JSONString, Bold, …) is a
 *       {@link FileMakerCalculationTokenType#CONSTANT}.</li>
 * </ul>
 * All checks are case-insensitive, as in FileMaker. The classification depends only on the buffer, so the lexer
 * stays restartable for incremental highlighting.
 */
public class FileMakerCalculationLexerAdapter extends FlexAdapter {

    private static final class Constants {
        static final Set<String> NAMES = load();

        private static Set<String> load() {
            Set<String> names = new HashSet<>();
            for (JsonElement e : SharedData.readJson(SharedData.CALC_CATALOGUE).getAsJsonArray("constants")) {
                names.add(e.getAsJsonObject().get("name").getAsString().toLowerCase(Locale.ROOT));
            }
            return names;
        }
    }

    private int classifiedStart = -1;
    private IElementType classifiedType;

    public FileMakerCalculationLexerAdapter() {
        // The JFlex-generated lexer uses the standard idea-flex.skeleton which expects a Reader in the ctor
        super(new _FileMakerCalculationLexer((java.io.Reader) null));
    }

    @Override
    public IElementType getTokenType() {
        IElementType type = super.getTokenType();
        if (type != FileMakerCalculationTokenType.IDENTIFIER) return type;
        int start = getTokenStart();
        if (start != classifiedStart) {
            classifiedStart = start;
            classifiedType = classify(getBufferSequence(), start, getTokenEnd());
        }
        return classifiedType;
    }

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
        classifiedStart = -1;
        super.start(buffer, startOffset, endOffset, initialState);
    }

    static IElementType classify(CharSequence buffer, int start, int end) {
        if (buffer.charAt(start) == '$') return FileMakerCalculationTokenType.IDENTIFIER;

        int next = end;
        while (next < buffer.length() && Character.isWhitespace(buffer.charAt(next))) next++;
        if (next < buffer.length() && buffer.charAt(next) == '(') return FileMakerCalculationTokenType.KEYWORD_FUNCTION;

        if (isInsideGetCall(buffer, start)) return FileMakerCalculationTokenType.GET_CONSTANT;

        String name = buffer.subSequence(start, end).toString().toLowerCase(Locale.ROOT);
        if (Constants.NAMES.contains(name)) return FileMakerCalculationTokenType.CONSTANT;
        return FileMakerCalculationTokenType.IDENTIFIER;
    }

    /** True when the name at {@code start} is preceded by {@code Get (} (any case, any spacing). */
    private static boolean isInsideGetCall(CharSequence buffer, int start) {
        int i = start - 1;
        while (i >= 0 && Character.isWhitespace(buffer.charAt(i))) i--;
        if (i < 0 || buffer.charAt(i) != '(') return false;
        i--;
        while (i >= 0 && Character.isWhitespace(buffer.charAt(i))) i--;
        int wordEnd = i + 1;
        while (i >= 0 && (Character.isLetterOrDigit(buffer.charAt(i)) || buffer.charAt(i) == '_' || buffer.charAt(i) == '.')) i--;
        return "get".equalsIgnoreCase(buffer.subSequence(i + 1, wordEnd).toString());
    }
}
