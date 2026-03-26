package LFAF.lab4;

import java.util.ArrayList;
import java.util.List;

// Lab 4 - Regular Expressions, Variant 3
// regexes:
//   1. O(P|Q|R)+2(3|4)
//   2. A*B(C|D|E)F(G|H|I)^2
//   3. J+K(L|M|N)*O?(P|Q)^3
public class Main {

    private static final String[] REGEXES = {
        "O(P|Q|R)+2(3|4)",
        "A*B(C|D|E)F(G|H|I)^2",
        "J+K(L|M|N)*O?(P|Q)^3"
    };

    public static void main(String[] args) {
        RegexGenerator gen = new RegexGenerator();

        printBanner("Lab 4 — Regular Expressions  [Variant 3]");
        System.out.println("* unlimited quantifiers capped at " + RegexParser.MAX_REPEAT);
        System.out.println();

        // part a - generate 5 strings per regex
        printBanner("Part (a): Generated strings");
        for (int i = 0; i < REGEXES.length; i++) {
            String regex = REGEXES[i];
            System.out.println("Regex " + (i + 1) + ":  " + regex);
            System.out.print("  Samples: { ");
            List<String> samples = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                samples.add(gen.generate(regex));
            }
            System.out.println(String.join(", ", samples) + " }");
            System.out.println();
        }

        // bonus - show what happens step by step when parsing and generating
        printBanner("Bonus: Processing sequence trace");
        List<String> trace = new ArrayList<>();
        for (int i = 0; i < REGEXES.length; i++) {
            String regex = REGEXES[i];
            System.out.println("┌─ Regex " + (i + 1) + ": " + regex);
            String result = gen.generateWithTrace(regex, trace);
            for (String line : trace) {
                System.out.println("│  " + line);
            }
            System.out.println("└─ Generated string: \"" + result + "\"");
            System.out.println();
        }
    }

    private static void printBanner(String title) {
        String border = "─".repeat(title.length() + 4);
        System.out.println("┌" + border + "┐");
        System.out.println("│  " + title + "  │");
        System.out.println("└" + border + "┘");
    }
}
