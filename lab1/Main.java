package LFAF.lab1;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        Grammar grammar = Grammar.variant23();
        FiniteAutomaton fa = grammar.toFiniteAutomaton();

        System.out.println("Lab 1 — Variant 23 ");
        System.out.println("Generated valid strings (and FA check):");

        for (int i = 0; i < 5; i++) {
            String w = grammar.generateString();
            System.out.println((i + 1) + ". " + w + "  -> " + fa.stringBelongToLanguage(w));
        }

        System.out.println("\nMembership tests:");
        String[] tests = {
                "c",        // false (nu poți porni din S cu c)
                "aac",      // true: S->aB->aC->c
                "abac",     // true: S->aB->bB->aC->c
                "aa",       // false
                "aba",      // false
                "ababc"    
        };

        for (String t : tests) {
            System.out.printf("  %-8s -> %s%n", t, fa.stringBelongToLanguage(t));
        }
    }
}
