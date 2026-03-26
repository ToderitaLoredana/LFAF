# Lab 4 — Regular Expressions

### Course: Formal Languages & Finite Automata
### Author: Tacu Loredana
### Variant: 3

----

## Theory

A **regular expression** (regex) is a compact, symbolic notation for describing sets of strings that belong to a regular language. Regular expressions are built from an alphabet Σ using three primitive operations:

- **Concatenation** — placing two expressions next to each other means their strings appear in sequence.
- **Alternation** (union) — `A|B` denotes strings that match either A or B.
- **Kleene star** — `A*` denotes zero or more repetitions of A.

Extended operators like `+` (one or more), `?` (zero or one), and `^n` (exactly n) are derived from these primitives and are standard in practical regex engines.

Regular expressions are equivalent in expressive power to regular grammars and finite automata: every regular expression defines a regular language and vice versa. They are widely used in text processing, lexical analysis (the subject of Lab 3), protocol validation, and many other areas.

---

## Objectives

1. Understand what regular expressions are and what they are used for.
2. For **Variant 3**, implement a program that:
   - **(a)** Dynamically interprets a given regular expression and generates valid strings that match it (no hard-coding of the regex structure).
   - **(b)** Caps indefinite quantifiers (`*`, `+`) at 5 repetitions to keep output manageable.
   - **(c) [Bonus]** Prints a step-by-step trace of how each regex is parsed and how each string is generated.

---

## Variant 3 — Regular Expressions

| # | Regular expression | Example valid strings |
|---|-------------------|-----------------------|
| 1 | `O(P\|Q\|R)+2(3\|4)` | OPP23, OQQQQ24, ORPQR24, … |
| 2 | `A*B(C\|D\|E)F(G\|H\|I)^2` | AAABCFGG, ABDFHI, BCFHH, … |
| 3 | `J+K(L\|M\|N)*O?(P\|Q)^3` | JJKLOPPP, JKNQQQ, JJJKLLMOQPQ, … |

Interpretation of each expression:

1. **`O(P|Q|R)+2(3|4)`**  — literal `O`, followed by **one or more** of {P, Q, R} (capped at 5), then literal `2`, then **one of** {3, 4}.
2. **`A*B(C|D|E)F(G|H|I)^2`** — **zero or more** A's (capped at 5), then B, then **one of** {C, D, E}, then F, then exactly **two** characters each chosen from {G, H, I}.
3. **`J+K(L|M|N)*O?(P|Q)^3`** — **one or more** J's (capped at 5), then K, then **zero or more** of {L, M, N} (capped at 5), then an **optional** O, then exactly **three** characters each chosen from {P, Q}.

---

## Implementation

### Files

| File | Purpose |
|------|---------|
| `RegexParser.java` | Recursive-descent parser: converts a regex string into an AST; records parse trace |
| `RegexGenerator.java` | AST walker: generates a random valid string; records generation trace |
| `Main.java` | Entry point: runs parts (a) and (b)/(bonus) |

---

### RegexParser — AST node types

The AST has four node types, all inner static classes of `RegexParser`:

| Node class | Meaning |
|------------|---------|
| `Literal` | A single character to emit verbatim |
| `Sequence` | Concatenate all child nodes in order |
| `Alternation` | Pick one child uniformly at random |
| `Repetition` | Repeat child node between `min` and `max` times |

```java
public static class Literal    extends Node { public final char ch; … }
public static class Sequence   extends Node { public final List<Node> parts; … }
public static class Alternation extends Node { public final List<Node> options; … }
public static class Repetition extends Node { public final Node child; public final int min, max; … }
```

---

### RegexParser — the parser

A three-function recursive-descent parser converts the raw regex string into an AST in a single left-to-right pass. The grammar it recognises is:

```
sequence    → primary*          (stops at ')' or '|')
alternation → sequence ('|' sequence)*    (only called inside a group)
primary     → char | '(' alternation ')'
            followed by optional quantifier: * | + | ? | ^<int>
```

`parse()` is the public entry point; it calls `parseSequence()`:

```java
public Node parse() {
    parseSteps.add("Parsing regex: \"" + input + "\"");
    Node root = parseSequence();
    parseSteps.add("Parse complete → root: " + root.describe());
    return root;
}
```

`parsePrimary()` reads either a literal character or a parenthesised group, then checks whether the next character is a quantifier (`*`, `+`, `?`, `^n`) and wraps the node in a `Repetition` accordingly:

```java
private Node parsePrimary() {
    Node base;
    if (pos < input.length() && input.charAt(pos) == '(') {
        pos++;                                 // consume '('
        base = parseAlternation();
        if (pos < input.length() && input.charAt(pos) == ')') pos++;
    } else {
        base = new Literal(input.charAt(pos++));
    }

    if (pos < input.length()) {
        char q = input.charAt(pos);
        if      (q == '*') { pos++; return new Repetition(base, 0, MAX_REPEAT); }
        else if (q == '+') { pos++; return new Repetition(base, 1, MAX_REPEAT); }
        else if (q == '?') { pos++; return new Repetition(base, 0, 1); }
        else if (q == '^') {
            pos++;
            int n = 0;
            while (pos < input.length() && Character.isDigit(input.charAt(pos)))
                n = n * 10 + (input.charAt(pos++) - '0');
            return new Repetition(base, n, n);
        }
    }
    return base;
}
```

The constant `MAX_REPEAT = 5` caps all open-ended quantifiers.

---

### RegexGenerator — the walker

`RegexGenerator` recursively walks the AST and builds the output string. At each `Alternation` node it picks one child with `random.nextInt(options.size())`; at each `Repetition` node it picks a count in `[min, max]`.

```java
private String walk(RegexParser.Node node, int depth) {
    if (node instanceof RegexParser.Literal) {
        char c = ((RegexParser.Literal) node).ch;
        trace(pad + "Emit '" + c + "'");
        return String.valueOf(c);
    }
    if (node instanceof RegexParser.Sequence) {
        StringBuilder sb = new StringBuilder();
        for (RegexParser.Node part : ((RegexParser.Sequence) node).parts)
            sb.append(walk(part, depth + 1));
        return sb.toString();
    }
    if (node instanceof RegexParser.Alternation) {
        RegexParser.Alternation alt = (RegexParser.Alternation) node;
        int choice = random.nextInt(alt.options.size());
        return walk(alt.options.get(choice), depth + 1);
    }
    if (node instanceof RegexParser.Repetition) {
        RegexParser.Repetition rep = (RegexParser.Repetition) node;
        int count = (rep.min == rep.max) ? rep.min
                  : rep.min + random.nextInt(rep.max - rep.min + 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++)
            sb.append(walk(rep.child, depth + 2));
        return sb.toString();
    }
    return "";
}
```

The `generateWithTrace()` method first calls the parser (collecting parse steps) and then calls `walk()` with tracing enabled (collecting generation steps), so the full pipeline is visible for the bonus output.

---

### Main — entry point

```java
// five generated strings per regex
for (String regex : REGEXES) {
    for (int j = 0; j < 5; j++) System.out.println(gen.generate(regex));
}

// bonus: full trace for one generation per regex
List<String> trace = new ArrayList<>();
for (String regex : REGEXES) {
    String result = gen.generateWithTrace(regex, trace);
    trace.forEach(System.out::println);
}
```

---

## How to compile and run

From the repository root (`LFAF_Labs/`):

```bash
# Compile
javac LFAF/lab4/*.java

# Run
java LFAF.lab4.Main
```

---

## Program output (verified run)

```
┌────────────────────────────────────────────┐
│  Lab 4 — Regular Expressions  [Variant 3]  │
└────────────────────────────────────────────┘
* unlimited quantifiers capped at 5

┌───────────────────────────────┐
│  Part (a): Generated strings  │
└───────────────────────────────┘
Regex 1:  O(P|Q|R)+2(3|4)
  Samples: { OPPRR24, OQR23, OQRP24, OPP24, OQ23 }

Regex 2:  A*B(C|D|E)F(G|H|I)^2
  Samples: { AAAAABDFHH, ABEFII, AAAAABDFII, AABEFHI, AAAAABCFIG }

Regex 3:  J+K(L|M|N)*O?(P|Q)^3
  Samples: { JJJJJKNMMMMOPQP, JJKNOQQP, JJJJKLOPQQ, JJKNMNMOPQP, JJJJKNLMMQPQ }

┌────────────────────────────────────┐
│  Bonus: Processing sequence trace  │
└────────────────────────────────────┘

[Regex 1 trace — parse phase]
  Parsing regex: "O(P|Q|R)+2(3|4)"
  Literal 'O'
  Entering group at pos 2
  Literal 'P' / Literal 'Q' / Literal 'R'
  Alternation with 3 options: [Literal('P'), Literal('Q'), Literal('R')]
  Exiting group → Alternation{3 opts}
  Quantifier '+' → repeat 1..5 on Alternation{3 opts}
  Literal '2'
  Entering group at pos 11
  Literal '3' / Literal '4'
  Alternation with 2 options: [Literal('3'), Literal('4')]
  Exiting group → Alternation{2 opts}
  Sequence of 4 elements
  Parse complete → root: Sequence[4]

[Regex 1 trace — generation phase]
  Sequence:
    Emit 'O'
    Repetition → repeat 4 time(s) (range [1..5])
      iter 1/4: Alternation → chose option 3/3  → Emit 'R'
      iter 2/4: Alternation → chose option 2/3  → Emit 'Q'
      iter 3/4: Alternation → chose option 3/3  → Emit 'R'
      iter 4/4: Alternation → chose option 1/3  → Emit 'P'
    Emit '2'
    Alternation → chose option 1/2 → Emit '3'
  Result: "ORQRP23"
```

*(Full trace output visible when running the program — output above is condensed for readability.)*

---

## Conclusions

1. **Dynamic interpretation** — the implementation never hard-codes the structure of any specific regex. The same parser and generator handle all three expressions, as well as any other regex written using the supported syntax.

2. **Recursive-descent parsing** is a natural fit for the nested structure of regular expressions. The grammar is unambiguous and the parser is straightforward to reason about.

3. **AST-based generation** separates concerns cleanly: the parser builds a tree, and the generator walks it. This makes it easy to test each phase independently and to extend the system (e.g. add a new quantifier) without touching the other phase.

4. **Capping repetitions** at `MAX_REPEAT = 5` is a simple and transparent policy. The constant is defined in one place and propagated automatically to all `*` and `+` quantifiers.

5. **Bonus trace** shows that the processing pipeline has two distinct phases — parsing (structure discovery) and generation (random-choice traversal) — which helps in understanding how a regex is interpreted at runtime.

---

## References

1. Hopcroft, J. E., Motwani, R., & Ullman, J. D. (2006). *Introduction to Automata Theory, Languages, and Computation* (3rd ed.). Pearson.
2. Sipser, M. (2012). *Introduction to the Theory of Computation* (3rd ed.). Cengage Learning.
3. Course materials — Formal Languages & Finite Automata, TUM.
