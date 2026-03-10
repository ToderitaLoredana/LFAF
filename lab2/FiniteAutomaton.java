package LFAF.lab2;

import java.util.*;

/**
 * Finite Automaton: {Q, Σ, δ, q0, F}
 * Supports NFA simulation, determinism check, NFA→DFA conversion,
 * FA→Grammar conversion, and Graphviz DOT output.
 */
public class FiniteAutomaton {
    private final Set<String> states;                                   // Q
    private final Set<Character> alphabet;                              // Σ
    private final Map<String, Map<Character, Set<String>>> delta;       // δ
    private final String startState;                                    // q0
    private final Set<String> acceptingStates;                          // F

    public FiniteAutomaton(Set<String> states,
                           Set<Character> alphabet,
                           Map<String, Map<Character, Set<String>>> delta,
                           String startState,
                           Set<String> acceptingStates) {
        this.states = new LinkedHashSet<>(states);
        this.alphabet = new LinkedHashSet<>(alphabet);
        this.delta = deepCopy(delta);
        this.startState = startState;
        this.acceptingStates = new LinkedHashSet<>(acceptingStates);
    }

    // ───────────────────────────────────────────────
    //  Factory – Variant 23 NDFA
    // ───────────────────────────────────────────────

    /**
     * Builds the NDFA from Variant 23:
     *   Q = {q0, q1, q2}, Σ = {a, b}, F = {q2}
     *   δ(q0,a) = {q0, q1}   (non-deterministic!)
     *   δ(q1,b) = {q2}
     *   δ(q0,b) = {q0}
     *   δ(q2,b) = {q2}
     *   δ(q1,a) = {q0}
     */
    public static FiniteAutomaton variant23NDFA() {
        Set<String> states = new LinkedHashSet<>(Arrays.asList("q0", "q1", "q2"));
        Set<Character> sigma = new LinkedHashSet<>(Arrays.asList('a', 'b'));
        Set<String> accepting = new LinkedHashSet<>(Collections.singletonList("q2"));

        Map<String, Map<Character, Set<String>>> d = new LinkedHashMap<>();
        for (String q : states) d.put(q, new LinkedHashMap<>());

        // δ(q0, a) = {q0, q1}
        d.get("q0").put('a', new LinkedHashSet<>(Arrays.asList("q0", "q1")));
        // δ(q0, b) = {q0}
        d.get("q0").put('b', new LinkedHashSet<>(Collections.singletonList("q0")));
        // δ(q1, b) = {q2}
        d.get("q1").put('b', new LinkedHashSet<>(Collections.singletonList("q2")));
        // δ(q1, a) = {q0}
        d.get("q1").put('a', new LinkedHashSet<>(Collections.singletonList("q0")));
        // δ(q2, b) = {q2}
        d.get("q2").put('b', new LinkedHashSet<>(Collections.singletonList("q2")));

        return new FiniteAutomaton(states, sigma, d, "q0", accepting);
    }

    // ───────────────────────────────────────────────
    //  1) String membership (NFA simulation)
    // ───────────────────────────────────────────────

    public boolean stringBelongToLanguage(String input) {
        if (input == null) return false;
        Set<String> current = new LinkedHashSet<>();
        current.add(startState);

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (!alphabet.contains(ch)) return false;
            Set<String> next = new LinkedHashSet<>();
            for (String st : current) {
                Map<Character, Set<String>> map = delta.get(st);
                if (map == null) continue;
                Set<String> targets = map.get(ch);
                if (targets != null) next.addAll(targets);
            }
            current = next;
            if (current.isEmpty()) return false;
        }
        for (String st : current) {
            if (acceptingStates.contains(st)) return true;
        }
        return false;
    }

    // ───────────────────────────────────────────────
    //  2) FA → Regular Grammar
    // ───────────────────────────────────────────────

    /**
     * Converts this finite automaton to an equivalent right-linear regular grammar.
     *
     * Algorithm:
     *   - Each state becomes a non-terminal (renaming q0→S, q1→A, q2→B, …).
     *   - For δ(qi, a) containing qj:
     *       • if qj is NOT an accepting state → add production Qi → a Qj
     *       • if qj IS an accepting state     → add Qi → a Qj  AND  Qi → a
     *   - The start state maps to the grammar's start symbol.
     */
    public Grammar toRegularGrammar() {
        // Build a mapping: state name → non-terminal letter
        List<String> stateList = new ArrayList<>(states);
        Map<String, String> stateToNT = new LinkedHashMap<>();
        // Start state always maps to "S"
        stateToNT.put(startState, "S");
        char nextNT = 'A';
        for (String s : stateList) {
            if (s.equals(startState)) continue;
            if (nextNT == 'S') nextNT++; // skip S
            stateToNT.put(s, String.valueOf(nextNT++));
        }

        Set<String> nonTerminals = new LinkedHashSet<>(stateToNT.values());
        Set<Character> terminals  = new LinkedHashSet<>(alphabet);
        Map<String, List<String>> productions = new LinkedHashMap<>();
        for (String nt : nonTerminals) productions.put(nt, new ArrayList<>());

        for (Map.Entry<String, Map<Character, Set<String>>> entry : delta.entrySet()) {
            String from = entry.getKey();
            String fromNT = stateToNT.get(from);
            if (fromNT == null) continue;

            for (Map.Entry<Character, Set<String>> tr : entry.getValue().entrySet()) {
                char symbol = tr.getKey();
                for (String to : tr.getValue()) {
                    String toNT = stateToNT.get(to);
                    if (toNT == null) continue;
                    // Always add A → a B
                    String prod = "" + symbol + toNT;
                    if (!productions.get(fromNT).contains(prod)) {
                        productions.get(fromNT).add(prod);
                    }
                    // If 'to' is accepting, also add A → a
                    if (acceptingStates.contains(to)) {
                        String termProd = String.valueOf(symbol);
                        if (!productions.get(fromNT).contains(termProd)) {
                            productions.get(fromNT).add(termProd);
                        }
                    }
                }
            }
        }

        return new Grammar(nonTerminals, terminals, productions, "S");
    }

    // ───────────────────────────────────────────────
    //  3) Check determinism
    // ───────────────────────────────────────────────

    /**
     * An FA is deterministic (DFA) if for every state and every symbol
     * there is at most one target state.
     */
    public boolean isDeterministic() {
        for (Map.Entry<String, Map<Character, Set<String>>> entry : delta.entrySet()) {
            for (Map.Entry<Character, Set<String>> tr : entry.getValue().entrySet()) {
                if (tr.getValue().size() > 1) return false;
            }
        }
        return true;
    }

    // ───────────────────────────────────────────────
    //  4) NFA → DFA  (subset construction)
    // ───────────────────────────────────────────────

    /**
     * Converts this (possibly non-deterministic) FA to an equivalent DFA
     * using the standard subset construction algorithm.
     */
    public FiniteAutomaton toDFA() {
        if (isDeterministic()) return this; // already a DFA

        // Each DFA state is a set of NFA states
        Map<Set<String>, String> dfaStateNames = new LinkedHashMap<>();
        Map<String, Map<Character, Set<String>>> dfaDelta = new LinkedHashMap<>();
        Set<String> dfaAccepting = new LinkedHashSet<>();

        Set<String> startSet = new LinkedHashSet<>();
        startSet.add(startState);
        String startName = setToStateName(startSet);
        dfaStateNames.put(startSet, startName);

        Queue<Set<String>> worklist = new LinkedList<>();
        worklist.add(startSet);

        while (!worklist.isEmpty()) {
            Set<String> current = worklist.poll();
            String currentName = dfaStateNames.get(current);
            dfaDelta.put(currentName, new LinkedHashMap<>());

            for (char sym : alphabet) {
                Set<String> nextSet = new LinkedHashSet<>();
                for (String nfaState : current) {
                    Map<Character, Set<String>> map = delta.get(nfaState);
                    if (map != null && map.containsKey(sym)) {
                        nextSet.addAll(map.get(sym));
                    }
                }
                if (nextSet.isEmpty()) continue; // dead state (no transition)

                if (!dfaStateNames.containsKey(nextSet)) {
                    String newName = setToStateName(nextSet);
                    dfaStateNames.put(nextSet, newName);
                    worklist.add(nextSet);
                }
                String targetName = dfaStateNames.get(nextSet);
                dfaDelta.get(currentName)
                        .computeIfAbsent(sym, k -> new LinkedHashSet<>())
                        .add(targetName);
            }
        }

        // Determine accepting states: any DFA state whose NFA state-set
        // intersects the original accepting states
        for (Map.Entry<Set<String>, String> e : dfaStateNames.entrySet()) {
            for (String nfaSt : e.getKey()) {
                if (acceptingStates.contains(nfaSt)) {
                    dfaAccepting.add(e.getValue());
                    break;
                }
            }
        }

        Set<String> dfaStates = new LinkedHashSet<>(dfaStateNames.values());
        return new FiniteAutomaton(dfaStates, alphabet, dfaDelta, startName, dfaAccepting);
    }

    /** Creates a readable name for a DFA state from a set of NFA states, e.g. {q0,q1} */
    private String setToStateName(Set<String> stateSet) {
        List<String> sorted = new ArrayList<>(stateSet);
        Collections.sort(sorted);
        return "{" + String.join(",", sorted) + "}";
    }

    // ───────────────────────────────────────────────
    //  5) Graphviz DOT output
    // ───────────────────────────────────────────────

    /**
     * Returns a Graphviz DOT representation of this finite automaton.
     */
    public String toDot(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph \"").append(title).append("\" {\n");
        sb.append("    rankdir=LR;\n");
        sb.append("    size=\"10,5\";\n");
        sb.append("    node [shape = point]; start;\n");

        // Accepting states: double circle
        sb.append("    node [shape = doublecircle];");
        for (String acc : acceptingStates) {
            sb.append(" \"").append(acc).append("\"");
        }
        sb.append(";\n");

        // Regular states
        sb.append("    node [shape = circle];\n");

        // Start arrow
        sb.append("    start -> \"").append(startState).append("\";\n");

        // Transitions
        // Merge transitions with same source & target into one label
        Map<String, Map<String, List<Character>>> edgeLabels = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Character, Set<String>>> entry : delta.entrySet()) {
            String from = entry.getKey();
            for (Map.Entry<Character, Set<String>> tr : entry.getValue().entrySet()) {
                char sym = tr.getKey();
                for (String to : tr.getValue()) {
                    edgeLabels
                        .computeIfAbsent(from, k -> new LinkedHashMap<>())
                        .computeIfAbsent(to, k -> new ArrayList<>())
                        .add(sym);
                }
            }
        }

        for (Map.Entry<String, Map<String, List<Character>>> fromEntry : edgeLabels.entrySet()) {
            String from = fromEntry.getKey();
            for (Map.Entry<String, List<Character>> toEntry : fromEntry.getValue().entrySet()) {
                String to = toEntry.getKey();
                List<Character> syms = toEntry.getValue();
                Collections.sort(syms);
                StringBuilder label = new StringBuilder();
                for (int i = 0; i < syms.size(); i++) {
                    if (i > 0) label.append(",");
                    label.append(syms.get(i));
                }
                sb.append("    \"").append(from).append("\" -> \"").append(to)
                  .append("\" [label=\"").append(label).append("\"];\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    // ───────────────────────────────────────────────
    //  Utility
    // ───────────────────────────────────────────────

    private Map<String, Map<Character, Set<String>>> deepCopy(
            Map<String, Map<Character, Set<String>>> original) {
        Map<String, Map<Character, Set<String>>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Character, Set<String>>> e : original.entrySet()) {
            Map<Character, Set<String>> inner = new LinkedHashMap<>();
            for (Map.Entry<Character, Set<String>> in : e.getValue().entrySet()) {
                inner.put(in.getKey(), new LinkedHashSet<>(in.getValue()));
            }
            copy.put(e.getKey(), inner);
        }
        return copy;
    }

    // Getters
    public Set<String> getStates()          { return Collections.unmodifiableSet(states); }
    public Set<Character> getAlphabet()     { return Collections.unmodifiableSet(alphabet); }
    public String getStartState()           { return startState; }
    public Set<String> getAcceptingStates() { return Collections.unmodifiableSet(acceptingStates); }
    public Map<String, Map<Character, Set<String>>> getDelta() {
        return Collections.unmodifiableMap(delta);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FA {\n");
        sb.append("  Q  = ").append(states).append("\n");
        sb.append("  Σ  = ").append(alphabet).append("\n");
        sb.append("  q0 = ").append(startState).append("\n");
        sb.append("  F  = ").append(acceptingStates).append("\n");
        sb.append("  δ  = {\n");
        for (Map.Entry<String, Map<Character, Set<String>>> e : delta.entrySet()) {
            for (Map.Entry<Character, Set<String>> tr : e.getValue().entrySet()) {
                sb.append("    δ(").append(e.getKey()).append(", ").append(tr.getKey())
                  .append(") = ").append(tr.getValue()).append("\n");
            }
        }
        sb.append("  }\n}");
        return sb.toString();
    }
}
