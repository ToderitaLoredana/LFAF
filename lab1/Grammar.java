package LFAF.lab1;

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
        validate();
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

    private void validate() {
        if (!nonTerminals.contains(startSymbol)) {
            throw new IllegalArgumentException("Start symbol must be in Vn.");
        }
        for (Map.Entry<String, List<String>> e : productions.entrySet()) {
            String lhs = e.getKey();
            if (!nonTerminals.contains(lhs)) {
                throw new IllegalArgumentException("LHS must be a nonterminal: " + lhs);
            }
            for (String rhs : e.getValue()) {
                if (rhs == null || rhs.isEmpty()) {
                    throw new IllegalArgumentException("Empty production is not allowed.");
                }
                char t = rhs.charAt(0);
                if (!terminals.contains(t)) {
                    throw new IllegalArgumentException("RHS must start with a terminal: " + rhs);
                }
                if (rhs.length() == 2) {
                    String nt = String.valueOf(rhs.charAt(1));
                    if (!nonTerminals.contains(nt)) {
                        throw new IllegalArgumentException("RHS nonterminal not in Vn: " + rhs);
                    }
                } else if (rhs.length() != 1) {
                    throw new IllegalArgumentException("Only RHS length 1 or 2 supported: " + rhs);
                }
            }
        }
    }

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

            if (!containsAnyNonTerminal(current)) {
                return current;
            }
        }
        throw new IllegalStateException("Failed to generate a valid string. Increase maxSteps/attempts.");
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

    public Set<String> getNonTerminals() { return Collections.unmodifiableSet(nonTerminals); }
    public Set<Character> getTerminals() { return Collections.unmodifiableSet(terminals); }
    public String getStartSymbol() { return startSymbol; }
    public Map<String, List<String>> getProductions() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : productions.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
