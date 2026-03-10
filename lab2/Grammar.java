package LFAF.lab2;

import java.util.*;


public class Grammar {
    private final Set<String> nonTerminals;
    private final Set<Character> terminals;
    private final String startSymbol;
    private final Map<String, List<String>> productions;
    private final Random random = new Random();

    public Grammar(Set<String> nonTerminals,
                   Set<Character> terminals,
                   Map<String, List<String>> productions,
                   String startSymbol) {
        this.nonTerminals = new LinkedHashSet<>(nonTerminals);
        this.terminals = new LinkedHashSet<>(terminals);
        this.productions = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : productions.entrySet()) {
            this.productions.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        this.startSymbol = startSymbol;
    }

    public static Grammar variant23() {
        Set<String> vn = new LinkedHashSet<>(Arrays.asList("S", "B", "C"));
        Set<Character> vt = new LinkedHashSet<>(Arrays.asList('a', 'b', 'c'));

        Map<String, List<String>> p = new LinkedHashMap<>();
        p.put("S", Arrays.asList("aB"));
        p.put("B", Arrays.asList("aC", "bB"));
        p.put("C", Arrays.asList("bB", "c", "aS"));

        return new Grammar(vn, vt, p, "S");
    }

    //  Chomsky Hierarchy Classification

    public String classifyChomskyHierarchy() {
        if (isType3()) return "Type 3 – Regular Grammar";
        if (isType2()) return "Type 2 – Context-Free Grammar";
        if (isType1()) return "Type 1 – Context-Sensitive Grammar";
        return "Type 0 – Unrestricted (Recursively Enumerable) Grammar";
    }

    private boolean isType3() {
        // Check right-linear: every production A → aB or A → a
        boolean rightLinear = true;
        // Check left-linear:  every production A → Ba or A → a
        boolean leftLinear = true;

        for (Map.Entry<String, List<String>> entry : productions.entrySet()) {
            String lhs = entry.getKey();
            if (lhs.length() != 1 || !nonTerminals.contains(lhs)) {
                return false; // LHS must be a single non-terminal
            }
            for (String rhs : entry.getValue()) {
                if (rhs.isEmpty()) {
                    // ε-production: allowed only if LHS is start symbol and S doesn't appear on RHS
                    if (!lhs.equals(startSymbol) || startSymbolOnRHS()) {
                        rightLinear = false;
                        leftLinear = false;
                    }
                    continue;
                }
                // Check right-linear form
                if (!isRightLinearRHS(rhs)) rightLinear = false;
                // Check left-linear form
                if (!isLeftLinearRHS(rhs))  leftLinear = false;
            }
        }
        return rightLinear || leftLinear;
    }

    /** A → a  or  A → aB  (terminal then optional non-terminal) */
    private boolean isRightLinearRHS(String rhs) {
        if (rhs.length() == 1) {
            return terminals.contains(rhs.charAt(0));
        }
        if (rhs.length() == 2) {
            return terminals.contains(rhs.charAt(0))
                    && nonTerminals.contains(String.valueOf(rhs.charAt(1)));
        }
        return false;
    }

    /** A → a  or  A → Ba  (optional non-terminal then terminal) */
    private boolean isLeftLinearRHS(String rhs) {
        if (rhs.length() == 1) {
            return terminals.contains(rhs.charAt(0));
        }
        if (rhs.length() == 2) {
            return nonTerminals.contains(String.valueOf(rhs.charAt(0)))
                    && terminals.contains(rhs.charAt(1));
        }
        return false;
    }

    private boolean startSymbolOnRHS() {
        for (List<String> rhsList : productions.values()) {
            for (String rhs : rhsList) {
                if (rhs.contains(startSymbol)) return true;
            }
        }
        return false;
    }

    private boolean isType2() {
        // Every LHS is a single non-terminal
        for (String lhs : productions.keySet()) {
            if (lhs.length() != 1 || !nonTerminals.contains(lhs)) {
                return false;
            }
        }
        return true;
    }

    private boolean isType1() {
        // |LHS| <= |RHS| for every production (except possibly S → ε)
        for (Map.Entry<String, List<String>> entry : productions.entrySet()) {
            String lhs = entry.getKey();
            for (String rhs : entry.getValue()) {
                if (rhs.isEmpty()) {
                    if (!lhs.equals(startSymbol) || startSymbolOnRHS()) return false;
                } else {
                    if (lhs.length() > rhs.length()) return false;
                }
            }
        }
        return true;
    }

    // ───────────────────────────────────────────────
    //  String generation (from Lab 1)
    // ───────────────────────────────────────────────

    public String generateString() {
        return generateString(30);
    }

    public String generateString(int maxSteps) {
        for (int attempt = 0; attempt < 50; attempt++) {
            String current = startSymbol;
            int steps = 0;
            while (containsAnyNonTerminal(current)) {
                if (steps++ > maxSteps) break;
                int idx = indexOfFirstNonTerminal(current);
                String nt = String.valueOf(current.charAt(idx));
                List<String> rhsOptions = productions.get(nt);
                if (rhsOptions == null || rhsOptions.isEmpty()) break;
                String chosen;
                if ("C".equals(nt)) {
                    chosen = weightedChoice(rhsOptions, Map.of("c", 3));
                } else {
                    chosen = rhsOptions.get(random.nextInt(rhsOptions.size()));
                }
                current = current.substring(0, idx) + chosen + current.substring(idx + 1);
            }
            if (!containsAnyNonTerminal(current)) return current;
        }
        throw new IllegalStateException("Failed to generate a valid string.");
    }

    private String weightedChoice(List<String> options, Map<String, Integer> extraWeights) {
        int total = 0;
        for (String opt : options) total += extraWeights.getOrDefault(opt, 1);
        int r = random.nextInt(total);
        int acc = 0;
        for (String opt : options) {
            acc += extraWeights.getOrDefault(opt, 1);
            if (r < acc) return opt;
        }
        return options.get(options.size() - 1);
    }

    private boolean containsAnyNonTerminal(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (nonTerminals.contains(String.valueOf(s.charAt(i)))) return true;
        }
        return false;
    }

    private int indexOfFirstNonTerminal(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (nonTerminals.contains(String.valueOf(s.charAt(i)))) return i;
        }
        return -1;
    }

    // ───────────────────────────────────────────────
    //  Grammar → Finite Automaton (from Lab 1)
    // ───────────────────────────────────────────────

    public FiniteAutomaton toFiniteAutomaton() {
        String FINAL = "FINAL";
        Set<String> states = new LinkedHashSet<>(nonTerminals);
        states.add(FINAL);
        Set<String> accepting = new LinkedHashSet<>();
        accepting.add(FINAL);
        Map<String, Map<Character, Set<String>>> delta = new LinkedHashMap<>();
        for (String q : states) delta.put(q, new LinkedHashMap<>());

        for (Map.Entry<String, List<String>> e : productions.entrySet()) {
            String from = e.getKey();
            for (String rhs : e.getValue()) {
                char symbol = rhs.charAt(0);
                String to = (rhs.length() == 2) ? String.valueOf(rhs.charAt(1)) : FINAL;
                delta.get(from)
                        .computeIfAbsent(symbol, k -> new LinkedHashSet<>())
                        .add(to);
            }
        }
        return new FiniteAutomaton(states, terminals, delta, startSymbol, accepting);
    }

    // ───────────────────────────────────────────────
    //  Getters
    // ───────────────────────────────────────────────

    public Set<String> getNonTerminals()       { return Collections.unmodifiableSet(nonTerminals); }
    public Set<Character> getTerminals()       { return Collections.unmodifiableSet(terminals); }
    public String getStartSymbol()             { return startSymbol; }
    public Map<String, List<String>> getProductions() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : productions.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Grammar {\n");
        sb.append("  Vn = ").append(nonTerminals).append("\n");
        sb.append("  Vt = ").append(terminals).append("\n");
        sb.append("  S  = ").append(startSymbol).append("\n");
        sb.append("  P  = {\n");
        for (Map.Entry<String, List<String>> e : productions.entrySet()) {
            sb.append("    ").append(e.getKey()).append(" -> ")
              .append(String.join(" | ", e.getValue())).append("\n");
        }
        sb.append("  }\n}");
        return sb.toString();
    }
}
