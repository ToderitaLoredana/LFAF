package LFAF.lab3;

/*
 * All the token types the lexer can produce.
 * I grouped them by category to make it easier to read.
 *
 * The recipe format looks like:
 *   RECIPE "name" SERVES number
 *   INGREDIENTS: ...
 *   INSTRUCTIONS:
 *     STEP number: ACTION ingredient FOR duration AT temperature UNTIL condition
 *   END
 */
public enum TokenType {

    // structure keywords
    RECIPE,
    SERVES,
    INGREDIENTS,
    INSTRUCTIONS,
    STEP,
    END,

    // modifier keywords
    FOR,            // duration clause
    AT,             // temperature clause
    UNTIL,          // condition clause
    AND,            // joining ingredients or steps
    WITH,

    // semantic categories - separating these from IDENTIFIER makes the parser's life easier
    ACTION,         // cooking verb: mix, bake, boil, fry, etc.
    CONDITION,      // done-state adjective: golden, soft, thick, etc.

    // literals
    NUMBER,         // integer, decimal, or fraction like 1/2
    STRING_LITERAL, // anything in quotes, used for recipe names

    // measurement/time/temperature units
    UNIT,           // g, kg, tsp, tbsp, min, C, F, etc.

    // ingredient names and other free-text words
    IDENTIFIER,

    // punctuation
    COLON,
    COMMA,
    SEMICOLON,
    SLASH,
    LPAREN,
    RPAREN,

    // control tokens
    NEWLINE,        // kept as a token because it separates steps
    EOF
}
