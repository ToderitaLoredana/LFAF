package LFAF.lab3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/*
 * This is the main lexer class for the recipe DSL I designed for lab 3.
 * It reads the input character by character and groups them into tokens.
 * The idea is similar to how a compiler front-end works - you split the
 * raw text into meaningful pieces before trying to understand the structure.
 *
 * The grammar I'm targeting (informally):
 *   recipe      -> RECIPE "name" SERVES number
 *                  INGREDIENTS: ingredient+
 *                  INSTRUCTIONS: step+
 *                  END
 *   ingredient  -> number unit identifier
 *   step        -> STEP number: action ingredient [FOR number unit] [AT number unit] [UNTIL condition]
 *
 * The lexer doesn't care about grammar structure though, that would be the parser's job.
 */
public class RecipeLexer {

    // keywords that have a special meaning in the recipe language
    private static final Set<String> KEYWORDS = Set.of(
            "RECIPE", "SERVES", "INGREDIENTS", "INSTRUCTIONS",
            "STEP", "FOR", "AT", "UNTIL", "END", "AND", "WITH"
    );

    // cooking verbs - these become ACTION tokens so the parser knows it's a command
    private static final Set<String> ACTIONS = Set.of(
            "MIX", "BAKE", "BOIL", "CHOP", "FRY", "HEAT", "STIR",
            "SIMMER", "ADD", "REMOVE", "POUR", "WHISK", "SLICE",
            "GRATE", "PEEL", "MINCE", "SAUTE", "ROAST", "STEAM",
            "BLEND", "FOLD", "KNEAD", "MARINATE", "SEASON", "STRAIN",
            "DRAIN", "COMBINE", "COAT", "TOSS", "COVER", "REST",
            "BRING", "REDUCE", "DEGLAZE", "DICE", "CRUSH", "COOK"
    );

    // words that describe a done state, used after UNTIL
    private static final Set<String> CONDITIONS = Set.of(
            "GOLDEN", "DONE", "SOFT", "CRISPY", "TENDER", "THICK",
            "SMOOTH", "BUBBLY", "TRANSLUCENT", "CARAMELIZED",
            "COOKED", "MELTED", "DISSOLVED", "ABSORBED", "HEATED"
    );

    // measurement and time units, stored lowercase so matching is case-insensitive
    private static final Set<String> UNITS = Set.of(
            "g", "kg", "ml", "l", "tsp", "tbsp", "cup", "cups",
            "oz", "lb", "lbs", "min", "mins", "sec", "secs",
            "h", "hrs", "c", "f", "°c", "°f"
    );

    private final String source;
    private int pos  = 0;  // where we are in the string
    private int line = 1;  // current line number (1-based)
    private int col  = 1;  // current column, only used for error messages

    // constructor just stores the source string, everything else is lazy
    public RecipeLexer(String source) {
        this.source = source;
    }

    // goes through the whole source and returns all tokens at once
    // the last token in the list will always be EOF
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (!isAtEnd()) {
            skipWhitespace();
            if (isAtEnd()) break;

            Token t = nextToken();
            if (t != null) tokens.add(t);
        }

        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    private Token nextToken() {
        char c = peek();

        // newlines are actual tokens because they separate steps
        if (c == '\n') {
            int startLine = line;
            advance();
            return new Token(TokenType.NEWLINE, "\\n", startLine);
        }

        // lines starting with # are comments, just skip them
        if (c == '#') {
            while (!isAtEnd() && peek() != '\n') advance();
            return null;
        }

        // single-character punctuation
        switch (c) {
            case ':': advance(); return new Token(TokenType.COLON,     ":", line);
            case ',': advance(); return new Token(TokenType.COMMA,     ",", line);
            case ';': advance(); return new Token(TokenType.SEMICOLON, ";", line);
            case '/': advance(); return new Token(TokenType.SLASH,     "/", line);
            case '(': advance(); return new Token(TokenType.LPAREN,    "(", line);
            case ')': advance(); return new Token(TokenType.RPAREN,    ")", line);
        }

        // quoted string - used for the recipe name
        if (c == '"' || c == '\'' || c == '\u201C' || c == '\u201D') {
            return scanStringLiteral();
        }

        // numbers can be integers, decimals, or fractions like 1/2
        if (Character.isDigit(c)) {
            return scanNumber();
        }

        // anything starting with a letter could be a keyword, action, condition, unit or identifier
        if (Character.isLetter(c) || c == '_' || c == '°') {
            return scanWord();
        }

        throw new LexerException("Unexpected character '" + c + "'", line, col);
    }

    /** Scans a quoted string; supports " and ' as delimiters. */
    private Token scanStringLiteral() {
        int startLine = line;
        char open = advance();
        char close = (open == '\u201C') ? '\u201D' : open; // handle smart quotes too
        StringBuilder sb = new StringBuilder();

        while (!isAtEnd() && peek() != close) {
            if (peek() == '\n') {
                throw new LexerException("Unterminated string literal", startLine, col);
            }
            sb.append(advance());
        }

        if (isAtEnd()) {
            throw new LexerException("Unterminated string literal", startLine, col);
        }
        advance(); // consume the closing quote

        return new Token(TokenType.STRING_LITERAL, sb.toString(), startLine);
    }

    /** Scans an integer or decimal number. */
    private Token scanNumber() {
        int startLine = line;
        StringBuilder sb = new StringBuilder();

        while (!isAtEnd() && Character.isDigit(peek())) sb.append(advance());

        // decimal part
        if (!isAtEnd() && peek() == '.' && isDigit(peekNext())) {
            sb.append(advance());
            while (!isAtEnd() && Character.isDigit(peek())) sb.append(advance());
        }

        // fraction like 1/2 or 3/4 - treat the whole thing as one number token
        if (!isAtEnd() && peek() == '/') {
            char slash = advance();
            if (!isAtEnd() && Character.isDigit(peek())) {
                sb.append(slash);
                while (!isAtEnd() && Character.isDigit(peek())) sb.append(advance());
            } else {
                pos--; // not a fraction, backtrack so '/' becomes a SLASH token
            }
        }

        return new Token(TokenType.NUMBER, sb.toString(), startLine);
    }

    /*
     * Scans a word and figures out what type it is.
     * The priority order matters: keywords first, then action verbs,
     * then condition words, then units, and everything else is just an identifier.
     * This way "FOR" doesn't accidentally become an identifier.
     */
    private Token scanWord() {
        int startLine = line;
        StringBuilder sb = new StringBuilder();

        // °C and °F should be one token
        if (peek() == '°') {
            sb.append(advance());
        }

        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(advance());
        }

        String raw   = sb.toString();
        String upper = raw.toUpperCase();
        String lower = raw.toLowerCase();

        if (KEYWORDS.contains(upper))    return new Token(keywordType(upper), raw, startLine);
        if (ACTIONS.contains(upper))     return new Token(TokenType.ACTION,   raw, startLine);
        if (CONDITIONS.contains(upper))  return new Token(TokenType.CONDITION, raw, startLine);
        if (UNITS.contains(lower))       return new Token(TokenType.UNIT,      raw, startLine);

        return new Token(TokenType.IDENTIFIER, raw, startLine);
    }

    /** Maps a keyword string to its specific TokenType. */
    private static TokenType keywordType(String upper) {
        switch (upper) {
            case "RECIPE":       return TokenType.RECIPE;
            case "SERVES":       return TokenType.SERVES;
            case "INGREDIENTS":  return TokenType.INGREDIENTS;
            case "INSTRUCTIONS": return TokenType.INSTRUCTIONS;
            case "STEP":         return TokenType.STEP;
            case "FOR":          return TokenType.FOR;
            case "AT":           return TokenType.AT;
            case "UNTIL":        return TokenType.UNTIL;
            case "END":          return TokenType.END;
            case "AND":          return TokenType.AND;
            case "WITH":         return TokenType.WITH;
            default:             return TokenType.IDENTIFIER;
        }
    }

    // skip spaces and tabs but NOT newlines - those are meaningful tokens
    private void skipWhitespace() {
        while (!isAtEnd() && (peek() == ' ' || peek() == '\t' || peek() == '\r')) {
            advance();
        }
    }

    private boolean isAtEnd() { return pos >= source.length(); }

    private char peek() { return isAtEnd() ? '\0' : source.charAt(pos); }

    private char peekNext() {
        return (pos + 1 >= source.length()) ? '\0' : source.charAt(pos + 1);
    }

    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') { line++; col = 1; } else { col++; }
        return c;
    }

    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
}
