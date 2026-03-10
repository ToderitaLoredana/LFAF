package LFAF.lab3;

// represents a single token with its type, the actual text (lexeme), and line number
// keeping it immutable so nothing can accidentally change a token after it's created
public class Token {

    public final TokenType type;
    public final String    lexeme;
    public final int       line;

    public Token(TokenType type, String lexeme, int line) {
        this.type   = type;
        this.lexeme = lexeme;
        this.line   = line;
    }

    @Override
    public String toString() {
        return String.format("Token(%-18s | %-20s | line %d)", type, "\"" + lexeme + "\"", line);
    }
}
