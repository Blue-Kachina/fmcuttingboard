package dev.fmcuttingboard.language.format;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import dev.fmcuttingboard.language.FileMakerCalculationElementType;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import dev.fmcuttingboard.language.validation.FileMakerCalculationAnnotator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The .fmcalc formatter (Claris spacing, pretty-printed). A line-for-line port of vscode/src/core/CalcFormatter.ts;
 * both are pinned by shared/fixtures/golden/cases.json "formatting", and the rules are documented in
 * docs/fmcalc-formatting.md. Change them together.
 */
public final class FmCalcFormatter {

    /**
     * @param indentUnit one level of indentation: "\t" or a run of spaces
     * @param indentWidth columns one level occupies (tab size for "\t")
     * @param maxWidth a call that fits within this many columns stays on one line
     * @param doNotIndentTopLetVariables leave the variables of the outermost Let ( [ … ] ; … ) flush with the Let
     */
    public record Options(String indentUnit, int indentWidth, int maxWidth, boolean doNotIndentTopLetVariables) {}

    /** A token: element type plus offsets. */
    record Tok(IElementType type, int start, int end) {}

    /** A composite PSI node reduced to what the formatter needs (mirrors CalcNode in the TypeScript port). */
    static final class Node {
        final String type;
        final int start;
        final int end;
        final List<Node> children = new ArrayList<>();
        final List<Tok> leaves = new ArrayList<>();

        Node(String type, int start, int end) {
            this.type = type;
            this.start = start;
            this.end = end;
        }
    }

    private FmCalcFormatter() {}

    /** The formatted text, or null when the calculation is not well-formed enough to format safely. */
    public static @Nullable String format(@NotNull PsiFile file, @NotNull Options options) {
        String text = file.getText();
        if (FileMakerCalculationAnnotator.firstLexicalProblem(text) != null) return null;
        List<Tok> tokens = new ArrayList<>();
        collectLeaves(file.getNode(), tokens);
        Node root = toNode(file.getNode(), "FILE");
        if (!isWellFormed(tokens, root)) return null;
        if (root.children.isEmpty()) return null;
        return new Formatter(text, tokens, options, root.children.get(0), false).run(text.endsWith("\n"));
    }

    // ----- PSI → Node -----

    private static boolean isTrivia(IElementType t) {
        return t == FileMakerCalculationTokenType.WHITE_SPACE || isComment(t);
    }

    static boolean isComment(IElementType t) {
        return t == FileMakerCalculationTokenType.LINE_COMMENT || t == FileMakerCalculationTokenType.BLOCK_COMMENT;
    }

    private static void collectLeaves(ASTNode node, List<Tok> out) {
        ASTNode child = node.getFirstChildNode();
        if (child == null) {
            if (node.getTextLength() > 0) out.add(new Tok(node.getElementType(), node.getStartOffset(), node.getStartOffset() + node.getTextLength()));
            return;
        }
        for (; child != null; child = child.getTreeNext()) collectLeaves(child, out);
    }

    private static Node toNode(ASTNode ast, String type) {
        Node node = new Node(type, ast.getStartOffset(), ast.getStartOffset() + ast.getTextLength());
        for (ASTNode child = ast.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType t = child.getElementType();
            if (t instanceof FileMakerCalculationElementType) {
                node.children.add(toNode(child, t.toString()));
            } else if (child.getFirstChildNode() == null && !isTrivia(t) && child.getTextLength() > 0) {
                node.leaves.add(new Tok(t, child.getStartOffset(), child.getStartOffset() + child.getTextLength()));
            }
        }
        return node;
    }

    // ----- well-formedness (same rules as the TypeScript isWellFormed) -----

    private static final Map<String, Set<String>> ALLOWED_LEAVES = Map.of(
            "FUNCTION_CALL", Set.of("KEYWORD_FUNCTION", "IDENTIFIER", "LPAREN", "RPAREN"),
            "ARG_LIST", Set.of("OPERATOR"),
            "ARGUMENT", Set.of(),
            "PAREN_EXPRESSION", Set.of("LPAREN", "RPAREN"),
            "BRACKET_LIST", Set.of("LBRACKET", "RBRACKET", "OPERATOR"),
            "REPETITION_EXPRESSION", Set.of(),
            "BINARY_EXPRESSION", Set.of("OPERATOR", "KEYWORD_LOGICAL"),
            "UNARY_EXPRESSION", Set.of("OPERATOR", "KEYWORD_LOGICAL"),
            "IDENTIFIER_EXPRESSION", Set.of("IDENTIFIER", "KEYWORD_FUNCTION"),
            "LITERAL", Set.of("NUMBER", "STRING", "CONSTANT", "GET_CONSTANT", "FIELD_REFERENCE", "QUOTED_NAME", "PARAGRAPH_MARK"));

    private static boolean isWellFormed(List<Tok> tokens, Node root) {
        int depth = 0;
        for (Tok t : tokens) {
            IElementType type = t.type();
            if (type == FileMakerCalculationTokenType.LPAREN || type == FileMakerCalculationTokenType.LBRACKET) depth++;
            else if (type == FileMakerCalculationTokenType.RPAREN || type == FileMakerCalculationTokenType.RBRACKET) depth--;
            else if (type == com.intellij.psi.TokenType.BAD_CHARACTER || type == FileMakerCalculationTokenType.LBRACE
                    || type == FileMakerCalculationTokenType.RBRACE) return false;
        }
        if (depth != 0) return false;
        if (root.children.size() != 1 || !root.leaves.isEmpty()) return false;
        return !hasStrayLeaves(root.children.get(0));
    }

    private static boolean hasStrayLeaves(Node node) {
        Set<String> ok = ALLOWED_LEAVES.get(node.type);
        if (ok == null) return true;
        for (Tok t : node.leaves) {
            if (!ok.contains(t.type().toString())) return true;
            if ((node.type.equals("ARG_LIST") || node.type.equals("BRACKET_LIST"))
                    && t.type() == FileMakerCalculationTokenType.OPERATOR && t.end() - t.start() != 1) return true;
        }
        for (Node c : node.children) if (hasStrayLeaves(c)) return true;
        return false;
    }

    // ----- the formatter -----

    private static final class Formatter {
        private final String text;
        private final List<Tok> tokens;
        private final Options options;
        private final Node root;
        private final boolean flat;

        private final Map<Integer, List<Tok>> leading = new HashMap<>();
        private final Map<Integer, List<Tok>> trailing = new HashMap<>();
        private final List<Tok> endComments = new ArrayList<>();

        private final List<String> lines = new ArrayList<>();
        private StringBuilder line = new StringBuilder();
        private int lineIndent = 0;
        private boolean pendingNewline = false;

        Formatter(String text, List<Tok> tokens, Options options, Node root, boolean flat) {
            this.text = text;
            this.tokens = tokens;
            this.options = options;
            this.root = root;
            this.flat = flat;
            attachComments();
        }

        String run(boolean endsWithNewline) {
            startLine(0);
            render(root, 0);
            for (Tok c : endComments) {
                newline(0);
                write(text(c));
            }
            flushLine();
            String out = String.join("\n", lines);
            return endsWithNewline ? out + "\n" : out;
        }

        // ----- comments -----

        private void attachComments() {
            Tok prevCode = null;
            boolean newlineSincePrev = false;
            List<Tok> pendingLeading = new ArrayList<>();
            for (Tok t : tokens) {
                if (t.type() == FileMakerCalculationTokenType.WHITE_SPACE) {
                    if (text(t).contains("\n")) newlineSincePrev = true;
                    continue;
                }
                if (isComment(t.type())) {
                    if (prevCode != null && !newlineSincePrev && pendingLeading.isEmpty()) {
                        trailing.computeIfAbsent(prevCode.start(), k -> new ArrayList<>()).add(t);
                        if (t.type() == FileMakerCalculationTokenType.LINE_COMMENT) newlineSincePrev = true;
                    } else {
                        pendingLeading.add(t);
                    }
                    continue;
                }
                if (!pendingLeading.isEmpty()) {
                    leading.put(t.start(), new ArrayList<>(pendingLeading));
                    pendingLeading.clear();
                }
                prevCode = t;
                newlineSincePrev = false;
            }
            endComments.addAll(pendingLeading);
        }

        private boolean hasComments(Node node) {
            for (Tok t : tokens) {
                if (isComment(t.type()) && t.start() >= node.start && t.start() < node.end) return true;
            }
            return false;
        }

        // ----- writer -----

        private String text(Tok t) {
            return text.substring(t.start(), t.end());
        }

        private void startLine(int level) {
            line = new StringBuilder(options.indentUnit().repeat(level));
            lineIndent = level;
        }

        private void flushLine() {
            lines.add(line.toString().replaceAll("[ \\t]+$", ""));
        }

        private boolean atLineStart() {
            return line.length() == options.indentUnit().length() * lineIndent;
        }

        private void newline(int level) {
            flushLine();
            startLine(level);
            pendingNewline = false;
        }

        private int column() {
            return lineIndent * options.indentWidth() + (line.length() - options.indentUnit().length() * lineIndent);
        }

        private void write(String s) {
            line.append(s);
        }

        private void space() {
            if (!atLineStart() && line.charAt(line.length() - 1) != ' ') write(" ");
        }

        /** Writes a token with its comments; {@code level} is used for any line break the comments need. */
        private void token(Tok t, int level) {
            for (Tok c : leading.getOrDefault(t.start(), List.of())) {
                if (!atLineStart()) newline(level);
                write(text(c));
                newline(level);
            }
            if (pendingNewline) newline(level);
            write(text(t));
            for (Tok c : trailing.getOrDefault(t.start(), List.of())) {
                write(" " + text(c));
                if (c.type() == FileMakerCalculationTokenType.LINE_COMMENT) pendingNewline = true;
            }
        }

        // ----- layout decisions -----

        private boolean forcesBreak(Node node) {
            if (hasComments(node)) return true;
            return forcesBreakVisit(node);
        }

        private boolean forcesBreakVisit(Node n) {
            if (n.type.equals("FUNCTION_CALL")) {
                String name = callName(n).toLowerCase(Locale.ROOT);
                List<Node> args = args(n);
                if (name.equals("while")) return true;
                if (name.equals("case") && args.size() >= 4) return true;
                if (name.equals("let") && letBindings(n).size() >= 2) return true;
            }
            for (Node c : n.children) if (forcesBreakVisit(c)) return true;
            return false;
        }

        private boolean fits(Node node) {
            if (flat) return true;
            if (forcesBreak(node)) return false;
            Formatter measure = new Formatter(text, tokens, options, node, true);
            measure.startLine(0);
            measure.render(node, 0);
            return column() + measure.line.length() <= options.maxWidth();
        }

        // ----- structure helpers -----

        private String callName(Node call) {
            return text(call.leaves.get(0));
        }

        private List<Node> args(Node call) {
            for (Node c : call.children) if (c.type.equals("ARG_LIST")) return c.children;
            return List.of();
        }

        private List<Tok> separators(@Nullable Node container) {
            List<Tok> out = new ArrayList<>();
            if (container == null) return out;
            for (Tok t : container.leaves) if (t.type() == FileMakerCalculationTokenType.OPERATOR) out.add(t);
            return out;
        }

        private @Nullable Node argList(Node call) {
            for (Node c : call.children) if (c.type.equals("ARG_LIST")) return c;
            return null;
        }

        private List<Node> letBindings(Node call) {
            List<Node> args = args(call);
            if (args.isEmpty() || args.get(0).children.isEmpty()) return List.of();
            Node first = args.get(0).children.get(0);
            return first.type.equals("BRACKET_LIST") ? first.children : List.of();
        }

        // ----- rendering -----

        private void render(Node node, int level) {
            switch (node.type) {
                case "ARGUMENT" -> {
                    if (!node.children.isEmpty()) render(node.children.get(0), level);
                }
                case "LITERAL", "IDENTIFIER_EXPRESSION" -> token(node.leaves.get(0), level);
                case "UNARY_EXPRESSION" -> {
                    Tok op = node.leaves.get(0);
                    token(op, level);
                    if (op.type() == FileMakerCalculationTokenType.KEYWORD_LOGICAL) space();
                    render(node.children.get(0), level);
                }
                case "BINARY_EXPRESSION" -> {
                    render(node.children.get(0), level);
                    space();
                    token(node.leaves.get(0), level);
                    space();
                    render(node.children.get(1), level);
                }
                case "PAREN_EXPRESSION" -> {
                    token(node.leaves.get(0), level);
                    space();
                    if (!node.children.isEmpty()) render(node.children.get(0), level);
                    space();
                    token(node.leaves.get(1), level);
                }
                case "REPETITION_EXPRESSION" -> {
                    render(node.children.get(0), level);
                    for (Node list : node.children.subList(1, node.children.size())) renderCompactList(list, level);
                }
                case "BRACKET_LIST" -> renderList(node, level);
                case "FUNCTION_CALL" -> renderCall(node, level);
                default -> throw new IllegalStateException("Unexpected node " + node.type);
            }
        }

        /** Field[2]: no spaces */
        private void renderCompactList(Node list, int level) {
            Tok open = list.leaves.get(0);
            Tok close = null;
            for (int i = 1; i < list.leaves.size(); i++) {
                if (list.leaves.get(i).type() == FileMakerCalculationTokenType.RBRACKET) { close = list.leaves.get(i); break; }
            }
            List<Tok> seps = separators(list);
            token(open, level);
            for (int i = 0; i < list.children.size(); i++) {
                render(list.children.get(i), level);
                if (i < seps.size()) {
                    space();
                    token(seps.get(i), level);
                    space();
                }
            }
            token(close, level);
        }

        /** [ a ; b ] on one line if it fits, else one item per line. */
        private void renderList(Node list, int level) {
            int itemLevel = level + 1;
            Tok open = list.leaves.get(0);
            Tok close = list.leaves.get(list.leaves.size() - 1);
            List<Tok> seps = separators(list);
            token(open, level);
            if (list.children.isEmpty()) {
                token(close, level);
                return;
            }
            if (fits(list)) {
                for (int i = 0; i < list.children.size(); i++) {
                    space();
                    render(list.children.get(i), level);
                    if (i < seps.size()) {
                        space();
                        token(seps.get(i), level);
                    }
                }
                space();
                token(close, level);
                return;
            }
            for (int i = 0; i < list.children.size(); i++) {
                newline(itemLevel);
                render(list.children.get(i), itemLevel);
                if (i < seps.size()) {
                    space();
                    token(seps.get(i), itemLevel);
                }
            }
            newline(level);
            token(close, level);
        }

        private void renderCall(Node call, int level) {
            Tok nameToken = call.leaves.get(0);
            Tok open = call.leaves.get(1);
            Tok close = call.leaves.size() > 2 ? call.leaves.get(2) : null;
            for (int i = 2; i < call.leaves.size(); i++) {
                if (call.leaves.get(i).type() == FileMakerCalculationTokenType.RPAREN) { close = call.leaves.get(i); break; }
            }
            List<Node> args = args(call);
            List<Tok> seps = separators(argList(call));
            String name = callName(call).toLowerCase(Locale.ROOT);

            token(nameToken, level);
            space();
            token(open, level);
            if (args.isEmpty() || (args.size() == 1 && args.get(0).children.isEmpty())) {
                token(close, level);
                return;
            }

            if (name.equals("let") && letBindings(call).size() >= 2 && args.size() == 2) {
                renderLet(call, args, seps, close, level);
            } else if (name.equals("case") && args.size() >= 2 && (args.size() >= 4 || !fits(call))) {
                renderCase(args, seps, close, level);
            } else if (!name.equals("while") && fits(call)) {
                for (int i = 0; i < args.size(); i++) {
                    space();
                    render(args.get(i), level);
                    if (i < seps.size()) {
                        space();
                        token(seps.get(i), level);
                    }
                }
                space();
                token(close, level);
            } else {
                for (int i = 0; i < args.size(); i++) {
                    newline(level + 1);
                    render(args.get(i), level + 1);
                    if (i < seps.size()) {
                        space();
                        token(seps.get(i), level + 1);
                    }
                }
                newline(level);
                token(close, level);
            }
        }

        /** Let ( [ / variables / ] ; / calculation / ) */
        private void renderLet(Node call, List<Node> args, List<Tok> seps, Tok close, int level) {
            Node list = args.get(0).children.get(0);
            boolean isTop = call == root;
            int itemLevel = isTop && options.doNotIndentTopLetVariables() ? level : level + 1;
            space();
            Tok listOpen = list.leaves.get(0);
            Tok listClose = list.leaves.get(list.leaves.size() - 1);
            List<Tok> listSeps = separators(list);
            token(listOpen, level);
            for (int i = 0; i < list.children.size(); i++) {
                newline(itemLevel);
                render(list.children.get(i), itemLevel);
                if (i < listSeps.size()) {
                    space();
                    token(listSeps.get(i), itemLevel);
                }
            }
            newline(level);
            token(listClose, level);
            space();
            token(seps.get(0), level);
            newline(level + 1);
            render(args.get(1), level + 1);
            newline(level);
            token(close, level);
        }

        /** Case ( / test1 ; result1 ; / test2 ; result2 ; / default / ) */
        private void renderCase(List<Node> args, List<Tok> seps, Tok close, int level) {
            for (int i = 0; i < args.size(); i++) {
                if (i % 2 == 0) newline(level + 1);
                else space();
                render(args.get(i), level + 1);
                if (i < seps.size()) {
                    space();
                    token(seps.get(i), level + 1);
                }
            }
            newline(level);
            token(close, level);
        }
    }
}
