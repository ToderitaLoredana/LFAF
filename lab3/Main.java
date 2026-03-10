package LFAF.lab3;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Lab 3 - Recipe Lexer ===");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        // keep running until the user decides to quit
        while (true) {
            printMenu();
            System.out.print("Choose option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    runInteractiveMode(scanner);
                    break;
                case "2":
                    runBuiltinDemo();
                    break;
                case "3":
                    System.out.println("Bye!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Unknown option, try again.");
            }
            System.out.println();
        }
    }

    private static void printMenu() {
        System.out.println("1 - Type / paste a recipe and tokenize it");
        System.out.println("2 - Run built-in demo (Spaghetti Carbonara)");
        System.out.println("3 - Exit");
    }

    // reads multi-line input from the console, stops on a blank line
    private static void runInteractiveMode(Scanner scanner) {
        System.out.println();
        System.out.println("Enter your recipe below.");
        System.out.println("Use the DSL format (RECIPE, INGREDIENTS, STEP, etc.).");
        System.out.println("Leave a blank line when you're done.");
        System.out.println();

        StringBuilder sb = new StringBuilder();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isEmpty()) break;
            sb.append(line).append("\n");
        }

        String input = sb.toString().trim();
        if (input.isEmpty()) {
            System.out.println("Nothing entered.");
            return;
        }

        System.out.println();
        tokenizeAndPrint("Your recipe", input);
        printStats("Your recipe", input);
    }

    // the hardcoded example so there's always something to show
    private static void runBuiltinDemo() {
        String source =
            "RECIPE \"Spaghetti Carbonara\" SERVES 4\n" +
            "INGREDIENTS:\n" +
            "  400 g spaghetti\n" +
            "  200 g pancetta\n" +
            "  4 eggs\n" +
            "  100 g Parmesan\n" +
            "  2 cloves garlic\n" +
            "  2 tbsp olive_oil\n" +
            "  salt AND pepper\n" +
            "INSTRUCTIONS:\n" +
            "  STEP 1: BOIL spaghetti FOR 10 min UNTIL cooked\n" +
            "  STEP 2: HEAT olive_oil AT 180 C FOR 2 min\n" +
            "  STEP 3: ADD pancetta AND garlic, FRY FOR 5 min UNTIL golden\n" +
            "  STEP 4: WHISK eggs WITH Parmesan\n" +
            "  STEP 5: DRAIN spaghetti, COMBINE WITH pancetta\n" +
            "  STEP 6: REMOVE FROM heat, POUR egg_mixture OVER pasta\n" +
            "  STEP 7: STIR FOR 1 min UNTIL thick\n" +
            "  STEP 8: SEASON WITH salt AND pepper\n" +
            "END\n";

        System.out.println();
        tokenizeAndPrint("Spaghetti Carbonara", source);
        printStats("Spaghetti Carbonara", source);
        runErrorDemo();
    }

    // prints the token table for a given source string
    private static void tokenizeAndPrint(String title, String source) {
        System.out.println("--- Tokens: " + title + " ---");

        RecipeLexer lexer = new RecipeLexer(source);

        List<Token> tokens;
        try {
            tokens = lexer.tokenize();
        } catch (LexerException e) {
            System.out.println("Lexer error: " + e.getMessage());
            return;
        }

        System.out.printf("  %-4s  %-22s  %-18s  %s%n", "No.", "Lexeme", "Type", "Line");
        System.out.println("  " + "-".repeat(60));

        int i = 1;
        for (Token t : tokens) {
            // skip newline tokens in the output, they just clutter the table
            if (t.type == TokenType.NEWLINE) continue;
            System.out.printf("  %-4d  %-22s  %-18s  %d%n",
                    i++,
                    "\"" + t.lexeme + "\"",
                    t.type,
                    t.line);
        }
        System.out.println();
    }

    // counts how many tokens of each type appeared
    private static void printStats(String title, String source) {
        System.out.println("--- Token type counts: " + title + " ---");

        RecipeLexer lexer = new RecipeLexer(source);
        List<Token> tokens;
        try {
            tokens = lexer.tokenize();
        } catch (LexerException e) {
            return; // error already shown in tokenizeAndPrint
        }

        Map<TokenType, Integer> counts = new TreeMap<>();
        for (Token t : tokens) {
            if (t.type == TokenType.NEWLINE || t.type == TokenType.EOF) continue;
            counts.merge(t.type, 1, Integer::sum);
        }

        counts.forEach((type, cnt) ->
                System.out.printf("  %-20s : %d%n", type, cnt));
        System.out.println();
    }

    // shows what happens when the input has an invalid character
    private static void runErrorDemo() {
        System.out.println("--- Error handling demo ---");

        String badSource =
            "RECIPE \"Broken Dish\" SERVES 2\n" +
            "INGREDIENTS:\n" +
            "  300 g chicken @@@\n" + // @@@ is not a valid token
            "END\n";

        System.out.println("Input:");
        System.out.println(badSource);

        try {
            new RecipeLexer(badSource).tokenize();
        } catch (LexerException e) {
            System.out.println("Caught error -> " + e.getMessage());
        }
        System.out.println();
    }
}
