package LFAF.lab4;

import java.util.ArrayList;
import java.util.List;

// parses a regex string into a tree structure (AST)
// supports: literals, (A|B|C) groups, and quantifiers *, +, ?, ^n
// i cap * and + at MAX_REPEAT=5 so strings don't get too long
public class RegexParser {

    public static final int MAX_REPEAT = 5;

    // node types for the tree
    public abstract static class Node {
        public abstract String describe();
    }

    // just a single character like 'A' or '3'
    public static class Literal extends Node {
        public final char ch;
        public Literal(char ch) { this.ch = ch; }
        @Override public String describe() { return "Literal('" + ch + "')"; }
    }

    // a list of things one after another
    public static class Sequence extends Node {
        public final List<Node> parts;
        public Sequence(List<Node> parts) { this.parts = parts; }
        @Override public String describe() { return "Sequence[" + parts.size() + "]"; }
    }

    // pick one of several options, like (A|B|C)
    public static class Alternation extends Node {
        public final List<Node> options;
        public Alternation(List<Node> options) { this.options = options; }
        @Override public String describe() { return "Alternation{" + options.size() + " opts}"; }
    }

    // repeat something min..max times
    public static class Repetition extends Node {
        public final Node child;
        public final int min, max;
        public Repetition(Node child, int min, int max) {
            this.child = child;
            this.min = min;
            this.max = max;
        }
        @Override public String describe() {
            return "Repetition(" + child.describe() + ", " + min + ".." + max + ")";
        }
    }

    private final String input;
    private int pos;
    private final List<String> parseSteps = new ArrayList<>();

    public RegexParser(String input) {
        this.input = input;
        this.pos = 0;
    }

    public List<String> getParseSteps() { return parseSteps; }

    public Node parse() {
        parseSteps.add("Parsing regex: \"" + input + "\"");
        Node root = parseSequence();
        parseSteps.add("Parse complete → root: " + root.describe());
        return root;
    }

    // reads characters one by one until hitting ) or |
    private Node parseSequence() {
        List<Node> parts = new ArrayList<>();
        while (pos < input.length()
                && input.charAt(pos) != ')'
                && input.charAt(pos) != '|') {
            parts.add(parsePrimary());
        }
        if (parts.size() == 1) return parts.get(0);
        parseSteps.add("  Sequence of " + parts.size() + " elements");
        return new Sequence(parts);
    }

    // handles the | inside a group, e.g. P|Q|R
    private Node parseAlternation() {
        List<Node> opts = new ArrayList<>();
        opts.add(parseSequence());
        while (pos < input.length() && input.charAt(pos) == '|') {
            pos++;                      // consume '|'
            opts.add(parseSequence());
        }
        if (opts.size() == 1) return opts.get(0);
        StringBuilder sb = new StringBuilder("  Alternation with " + opts.size() + " options: [");
        for (int i = 0; i < opts.size(); i++) {
            sb.append(opts.get(i).describe());
            if (i < opts.size() - 1) sb.append(", ");
        }
        sb.append("]");
        parseSteps.add(sb.toString());
        return new Alternation(opts);
    }

    // parses one "atom" - either a letter or a (group), then checks for a quantifier after it
    private Node parsePrimary() {
        Node base;

        if (pos < input.length() && input.charAt(pos) == '(') {
            pos++; // skip the (
            parseSteps.add("  Entering group at pos " + pos);
            base = parseAlternation();
            if (pos < input.length() && input.charAt(pos) == ')') pos++; // skip the )
            parseSteps.add("  Exiting group → " + base.describe());
        } else {
            char c = input.charAt(pos++);
            parseSteps.add("  Literal '" + c + "'");
            base = new Literal(c);
        }

        // check if there's a quantifier right after
        if (pos < input.length()) {
            char q = input.charAt(pos);
            if (q == '*') {
                pos++;
                parseSteps.add("  Quantifier '*' → repeat 0.." + MAX_REPEAT + " on " + base.describe());
                return new Repetition(base, 0, MAX_REPEAT);
            } else if (q == '+') {
                pos++;
                parseSteps.add("  Quantifier '+' → repeat 1.." + MAX_REPEAT + " on " + base.describe());
                return new Repetition(base, 1, MAX_REPEAT);
            } else if (q == '?') {
                pos++;
                parseSteps.add("  Quantifier '?' → repeat 0..1 on " + base.describe());
                return new Repetition(base, 0, 1);
            } else if (q == '^') {
                pos++; // skip the ^
                int n = 0;
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                    n = n * 10 + (input.charAt(pos++) - '0');
                }
                parseSteps.add("  Quantifier '^" + n + "' → repeat exactly " + n + " on " + base.describe());
                return new Repetition(base, n, n);
            }
        }

        return base;
    }
}
