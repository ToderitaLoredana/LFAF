package LFAF.lab1;

import java.util.*;


public class FiniteAutomaton {
    private final Set<String> states;                          // Q
    private final Set<Character> alphabet;                     // Sigma
    private final Map<String, Map<Character, Set<String>>> delta; // δ
    private final String startState;                           // q0
    private final Set<String> acceptingStates;                 // F

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
        validate();
    }

    private void validate() {
        if (!states.contains(startState)) {
            throw new IllegalArgumentException("Start state must be in Q.");
        }
        if (!states.containsAll(acceptingStates)) {
            throw new IllegalArgumentException("Accepting states must be in Q.");
        }
        for (Map.Entry<String, Map<Character, Set<String>>> e : delta.entrySet()) {
            String from = e.getKey();
            if (!states.contains(from)) throw new IllegalArgumentException("Delta has unknown state: " + from);

            for (Map.Entry<Character, Set<String>> tr : e.getValue().entrySet()) {
                char sym = tr.getKey();
                if (!alphabet.contains(sym)) throw new IllegalArgumentException("Symbol not in Sigma: " + sym);

                for (String to : tr.getValue()) {
                    if (!states.contains(to)) throw new IllegalArgumentException("Delta leads to unknown state: " + to);
                }
            }
        }
    }

    private Map<String, Map<Character, Set<String>>> deepCopy(Map<String, Map<Character, Set<String>>> original) {
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

    
    public boolean stringBelongToLanguage(final String inputString) {
        if (inputString == null) return false;

        Set<String> current = new LinkedHashSet<>();
        current.add(startState);

        for (int i = 0; i < inputString.length(); i++) {
            char ch = inputString.charAt(i);
            if (!alphabet.contains(ch)) return false; // caracter invalid

            Set<String> next = new LinkedHashSet<>();
            for (String state : current) {
                Map<Character, Set<String>> map = delta.get(state);
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

    public Set<String> getStates() { return Collections.unmodifiableSet(states); }
    public Set<Character> getAlphabet() { return Collections.unmodifiableSet(alphabet); }
    public String getStartState() { return startState; }
    public Set<String> getAcceptingStates() { return Collections.unmodifiableSet(acceptingStates); }
    public Map<String, Map<Character, Set<String>>> getDelta() { return Collections.unmodifiableMap(delta); }
}
