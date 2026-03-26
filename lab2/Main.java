package LFAF.lab2;

import java.io.*;

public class Main {
    public static void main(String[] args) throws Exception {

        System.out.println("   Lab 2 — Variant 23                        ");
        System.out.println("   Determinism, NDFA→DFA, Chomsky Hierarchy   ");
        System.out.println("\n");

        // ── Task 2a: Chomsky hierarchy classification (grammar from Lab 1) ──
        Grammar grammar = Grammar.variant23();
        System.out.println("── Task 2a: Chomsky Classification (Lab 1 Grammar) ──");
        System.out.println(grammar);
        System.out.println("Classification: " + grammar.classifyChomskyHierarchy());
        System.out.println();

        // ── Task 3: Variant 23 NDFA ──
        FiniteAutomaton ndfa = FiniteAutomaton.variant23NDFA();
        System.out.println("── NDFA (Variant 23) ──");
        System.out.println(ndfa);
        System.out.println();

        // ── Task 3a: FA → Regular Grammar ──
        System.out.println("── Task 3a: NDFA → Regular Grammar ──");
        Grammar rgFromFA = ndfa.toRegularGrammar();
        System.out.println(rgFromFA);
        System.out.println("Classification: " + rgFromFA.classifyChomskyHierarchy());
        System.out.println();

        // ── Task 3b: Determinism check ──
        System.out.println("── Task 3b: Determinism Check ──");
        System.out.println("Is NDFA deterministic? " + ndfa.isDeterministic());
        System.out.println();

        // ── Task 3c: NDFA → DFA ──
        System.out.println("── Task 3c: NDFA → DFA (Subset Construction) ──");
        FiniteAutomaton dfa = ndfa.toDFA();
        System.out.println(dfa);
        System.out.println("Is DFA deterministic? " + dfa.isDeterministic());
        System.out.println();

        // ── Verify: both accept the same strings ──
        System.out.println("── Verification: NDFA vs DFA on test strings ──");
        String[] tests = {"ab", "aab", "abb", "aabb", "abab", "b", "a", "ba", "bab", "aabbb"};
        System.out.printf("  %-10s  %-6s  %-6s%n", "String", "NDFA", "DFA");
        System.out.println("  " + "-".repeat(28));
        for (String t : tests) {
            boolean ndfaResult = ndfa.stringBelongToLanguage(t);
            boolean dfaResult  = dfa.stringBelongToLanguage(t);
            System.out.printf("  %-10s  %-6s  %-6s%n", t, ndfaResult, dfaResult);
        }
        System.out.println();

        // ── Task 3d: Graphviz DOT output ──
        System.out.println("── Task 3d: Graphviz DOT Output ──");

        String ndfaDot = ndfa.toDot("NDFA_Variant23");
        String dfaDot  = dfa.toDot("DFA_Variant23");

        // Write DOT files
        String basePath = "LFAF/lab2/";
        writeFile(basePath + "ndfa.dot", ndfaDot);
        writeFile(basePath + "dfa.dot",  dfaDot);
        System.out.println("DOT files written: ndfa.dot, dfa.dot");

        // Try to render PNGs using Graphviz (if installed)
        boolean graphvizAvailable = renderDot(basePath + "ndfa.dot", basePath + "ndfa.png");
        if (graphvizAvailable) {
            renderDot(basePath + "dfa.dot", basePath + "dfa.png");
            System.out.println("PNG files generated: ndfa.png, dfa.png");
        } else {
            System.out.println("Graphviz (dot) not found. To render the graphs, install Graphviz and run:");
            System.out.println("  dot -Tpng " + basePath + "ndfa.dot -o " + basePath + "ndfa.png");
            System.out.println("  dot -Tpng " + basePath + "dfa.dot  -o " + basePath + "dfa.png");
        }

        System.out.println("\n── NDFA DOT ──");
        System.out.println(ndfaDot);
        System.out.println("── DFA DOT ──");
        System.out.println(dfaDot);
    }

    private static void writeFile(String path, String content) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.print(content);
        }
    }

    private static boolean renderDot(String dotFile, String pngFile) {
        try {
            Process p = new ProcessBuilder("dot", "-Tpng", dotFile, "-o", pngFile)
                    .redirectErrorStream(true)
                    .start();
            int exit = p.waitFor();
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
