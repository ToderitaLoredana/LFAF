# Lab 3 — Lexer & Scanner

### Course: Formal Languages & Finite Automata
### Author: Tacu Loredana
### Variant: 23

---

## Theory

**Lexical analysis** (also called *scanning* or *tokenization*) is the first phase of a compiler or interpreter. Its job is to read a raw stream of characters and group them into meaningful chunks called **lexemes**, then label each chunk with a **token type** that describes the category of the lexeme (e.g. keyword, number, identifier).

The key distinction:
- A **lexeme** is the actual character sequence found in the source (e.g. `BOIL`, `"Spaghetti Carbonara"`, `400`).
- A **token** pairs a lexeme with its type plus any useful metadata (line number, column).

A lexer is typically implemented as a finite automaton: each character read triggers a transition between internal states, and a final (accepting) state produces a token. Regular expressions are the theoretical foundation — every token type can be described by a regular expression, which in turn corresponds to a deterministic finite automaton (DFA).

---

## Objectives

1. Understand what lexical analysis is.
2. Get familiar with the inner workings of a lexer / scanner / tokenizer.
3. Implement a sample lexer and demonstrate how it works.

---

## Domain choice — Cooking Recipes

Instead of the canonical calculator example, a **cooking recipe** was chosen as the domain. A recipe DSL is richer than arithmetic: it has keywords for recipe structure (`RECIPE`, `SERVES`, `INGREDIENTS`, `INSTRUCTIONS`, `STEP`, `END`), control-flow words (`FOR`, `AT`, `UNTIL`), a vocabulary of action verbs (`BOIL`, `FRY`, `WHISK`, …), condition adjectives (`golden`, `tender`, `thick`, …), measurement units (`g`, `kg`, `tsp`, `min`, `C`, …), numeric literals (integers, decimals, and fractions like `1/2`), quoted string literals for recipe names, and punctuation.

Sample recipe in the DSL:

```
RECIPE "Spaghetti Carbonara" SERVES 4
INGREDIENTS:
  400 g spaghetti
  200 g pancetta
  4 eggs
  100 g Parmesan
  2 tbsp olive_oil
  salt AND pepper
INSTRUCTIONS:
  STEP 1: BOIL spaghetti FOR 10 min UNTIL cooked
  STEP 2: HEAT olive_oil AT 180 C FOR 2 min
  STEP 3: ADD pancetta AND garlic, FRY FOR 5 min UNTIL golden
  ...
END
```

---

## Implementation Description

The implementation consists of four Java source files in `lab3/`:

| File | Role |
|------|------|
| `TokenType.java` | Enum of all recognised token categories |
| `Token.java` | Immutable value object holding `(type, lexeme, line)` |
| `LexerException.java` | Runtime exception with line/column info |
| `RecipeLexer.java` | Hand-written single-pass lexer |
| `Main.java` | Driver: two full recipes + error demo |

### TokenType enum

```java
public enum TokenType {
    // Keywords
    RECIPE, SERVES, INGREDIENTS, INSTRUCTIONS,
    STEP, FOR, AT, UNTIL, END, AND, WITH,
    // Semantic categories
    ACTION,          // cooking verbs: BOIL, FRY, WHISK ...
    CONDITION,       // done-state adjectives: golden, soft, thick ...
    // Literals
    NUMBER,          // integers, decimals, fractions  e.g. 2, 3.5, 1/2
    STRING_LITERAL,  // quoted text                   e.g. "Pasta Carbonara"
    UNIT,            // measurement units              e.g. g, kg, tsp, min, C
    IDENTIFIER,      // ingredient names, free text
    // Punctuation
    COLON, COMMA, SEMICOLON, SLASH, LPAREN, RPAREN,
    // Control
    NEWLINE, EOF
}
```

Having dedicated `ACTION` and `CONDITION` types (not just IDENTIFIER) makes the token stream directly useful for a downstream parser — it immediately knows whether a word is a verb commanding an action or an adjective describing a desired state.

### Token class

```java
public class Token {
    public final TokenType type;
    public final String    lexeme;
    public final int       line;
}
```

Immutable; `toString()` produces a nicely aligned debug string:
```
Token(ACTION             | "BOIL"               | line 11)
```

### RecipeLexer — core algorithm

The lexer is a **hand-written, single-pass, character-by-character scanner**. It maintains three pieces of state:

| Field | Meaning |
|-------|---------|
| `pos` | Current index into the source string |
| `line` | Current 1-based line number |
| `col` | Current 1-based column number (for error messages) |

The public entry point is `tokenize()`:

```java
public List<Token> tokenize() {
    List<Token> tokens = new ArrayList<>();
    while (!isAtEnd()) {
        skipWhitespace();          // skip spaces, tabs, \r
        if (isAtEnd()) break;
        Token t = nextToken();     // produce one token
        if (t != null) tokens.add(t);
    }
    tokens.add(new Token(TokenType.EOF, "", line));
    return tokens;
}
```

`nextToken()` dispatches on the current character:

```
\n            → NEWLINE token
#             → skip to end of line (comment)
: , ; / ( )   → single-character punctuation tokens
" or '        → scanStringLiteral()
0-9           → scanNumber()
letter / _ / °→ scanWord()
anything else → throw LexerException
```

#### Number scanning

```java
private Token scanNumber() {
    // collect digits
    while (isDigit(peek())) sb.append(advance());
    // optional decimal part
    if (peek() == '.' && isDigit(peekNext())) { ... }
    // optional fraction  1/2, 3/4
    if (peek() == '/' && isDigit(peekNext())) { ... }
    return new Token(TokenType.NUMBER, sb.toString(), startLine);
}
```

Fractions like `1/2` and `3/4` are consumed as a single NUMBER token — convenient for recipe quantities. If the `/` is not followed by a digit it is left for the SLASH token rule.

#### Word scanning and classification

After collecting a maximal sequence of alphanumeric/underscore characters, the lexeme is looked up in four ordered sets:

```
1. KEYWORDS   → specific keyword TokenType (RECIPE, STEP, FOR, …)
2. ACTIONS    → TokenType.ACTION     (BOIL, FRY, WHISK, COOK, …)
3. CONDITIONS → TokenType.CONDITION  (golden, tender, thick, …)
4. UNITS      → TokenType.UNIT       (g, kg, tsp, min, C, …)
5. otherwise  → TokenType.IDENTIFIER
```

Unit matching is case-insensitive (stored lower-case in the set) so both `C` and `c` map to UNIT. Keyword/action/condition matching is done by converting the lexeme to upper-case first, so recipes can be written in any casing.

#### Error handling

Any character that does not start a recognised pattern causes a `LexerException`:

```java
throw new LexerException("Unexpected character '" + c + "'", line, col);
```

The exception carries the exact line and column so the user gets precise feedback:
```
Lexer error at line 3, column 17: Unexpected character '@'
```

---

## Running the Lexer

Compile and run from the project root:

```bash
javac -d out lab3/TokenType.java lab3/LexerException.java \
             lab3/Token.java lab3/RecipeLexer.java lab3/Main.java
java -cp out LFAF.lab3.Main
```

### Output excerpt — Spaghetti Carbonara (first 12 tokens)

```
No.   Lexeme                Type                Line
──────────────────────────────────────────────────────────────────
1     "RECIPE"              RECIPE              1
2     "Spaghetti Carbonara" STRING_LITERAL      1
3     "SERVES"              SERVES              1
4     "4"                   NUMBER              1
5     "INGREDIENTS"         INGREDIENTS         2
6     ":"                   COLON               2
7     "400"                 NUMBER              3
8     "g"                   UNIT                3
9     "spaghetti"           IDENTIFIER          3
10    "200"                 NUMBER              4
11    "g"                   UNIT                4
12    "pancetta"            IDENTIFIER          4
```

### Token-type statistics (Spaghetti Carbonara)

| Token type | Count |
|-----------|-------|
| ACTION | 12 |
| AND | 3 |
| AT | 1 |
| COLON | 10 |
| COMMA | 3 |
| CONDITION | 3 |
| END | 1 |
| FOR | 4 |
| IDENTIFIER | 23 |
| INGREDIENTS | 1 |
| INSTRUCTIONS | 1 |
| NUMBER | 20 |
| RECIPE | 1 |
| SERVES | 1 |
| STEP | 8 |
| STRING_LITERAL | 1 |
| UNIT | 9 |
| UNTIL | 3 |
| WITH | 3 |

### Error demo

```
Input: RECIPE "Broken Dish" SERVES 2
INGREDIENTS:
  300 g chicken @@@
END

Caught LexerException:
  Lexer error at line 3, column 17: Unexpected character '@'
```

---

## Conclusions

- A **lexer** transforms a flat character stream into a structured sequence of typed tokens, making subsequent parsing far easier.
- Token type design is domain-driven: separating `ACTION` verbs and `CONDITION` adjectives from generic `IDENTIFIER`s gives the token stream semantic richness.
- The lexer is implemented as a **hand-written DFA**: each scanning function (scanNumber, scanWord, scanStringLiteral) corresponds to a small DFA that accepts a regular language.
- **Error location** (line + column) is tracked at the character level, which is essential for user-friendly diagnostics.
- The approach scales naturally: adding a new keyword (e.g. `OPTIONAL`) or unit (e.g. `pinch`) requires only a one-line addition to the appropriate lookup set.

---

## References

1. [LLVM Tutorial — My First Language Frontend](https://llvm.org/docs/tutorial/MyFirstLanguageFrontend/LangImpl01.html)
2. [Lexical analysis — Wikipedia](https://en.wikipedia.org/wiki/Lexical_analysis)
