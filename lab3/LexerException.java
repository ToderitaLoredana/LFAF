package LFAF.lab3;

// thrown when the lexer hits something it doesn't recognize
// the line and column fields let us show the user exactly where the problem is
public class LexerException extends RuntimeException {

    public final int line;
    public final int column;

    public LexerException(String message, int line, int column) {
        super(String.format("Lexer error at line %d, column %d: %s", line, column, message));
        this.line   = line;
        this.column = column;
    }
}
