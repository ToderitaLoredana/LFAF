package LFAF.lab4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// takes the tree from RegexParser and builds a random valid string from it
// can also record each step it takes, which is needed for the bonus part
public class RegexGenerator {

    private final Random random;
    private List<String> generationSteps;
    private boolean tracing;

    public RegexGenerator() {
        this.random = new Random();
    }

    // simple generate - just returns the string, no logging
    public String generate(String regex) {
        tracing = false;
        generationSteps = null;
        RegexParser parser = new RegexParser(regex);
        RegexParser.Node ast = parser.parse();
        return walk(ast, 0);
    }

    // same as generate but also records every decision made during parsing and string building
    // used for the bonus part of the lab
    public String generateWithTrace(String regex, List<String> allSteps) {
        allSteps.clear();

        // first parse and save those steps
        allSteps.add("=== PARSE PHASE ===");
        RegexParser parser = new RegexParser(regex);
        RegexParser.Node ast = parser.parse();
        allSteps.addAll(parser.getParseSteps());

        // then generate and save those steps too
        allSteps.add("");
        allSteps.add("=== GENERATION PHASE ===");
        tracing = true;
        generationSteps = new ArrayList<>();
        String result = walk(ast, 0);
        allSteps.addAll(generationSteps);

        allSteps.add("");
        allSteps.add("=== RESULT: \"" + result + "\" ===");
        return result;
    }

    // recursively walks the tree and builds the output string
    private String walk(RegexParser.Node node, int depth) {
        String pad = "  ".repeat(depth);

        if (node instanceof RegexParser.Literal) {
            char c = ((RegexParser.Literal) node).ch;
            trace(pad + "Emit '" + c + "'");
            return String.valueOf(c);
        }

        if (node instanceof RegexParser.Sequence) {
            trace(pad + "Sequence:");
            StringBuilder sb = new StringBuilder();
            for (RegexParser.Node part : ((RegexParser.Sequence) node).parts) {
                sb.append(walk(part, depth + 1));
            }
            return sb.toString();
        }

        if (node instanceof RegexParser.Alternation) {
            RegexParser.Alternation alt = (RegexParser.Alternation) node;
            int choice = random.nextInt(alt.options.size());
            trace(pad + "Alternation → chose option " + (choice + 1)
                    + "/" + alt.options.size()
                    + " [" + alt.options.get(choice).describe() + "]");
            return walk(alt.options.get(choice), depth + 1);
        }

        if (node instanceof RegexParser.Repetition) {
            RegexParser.Repetition rep = (RegexParser.Repetition) node;
            int count;
            if (rep.min == rep.max) {
                count = rep.min;
            } else {
                count = rep.min + random.nextInt(rep.max - rep.min + 1);
            }
            trace(pad + "Repetition → repeat " + count
                    + " time(s) (range [" + rep.min + ".." + rep.max + "])");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                trace(pad + "  iter " + (i + 1) + "/" + count + ":");
                sb.append(walk(rep.child, depth + 2));
            }
            return sb.toString();
        }

        return "";
    }

    private void trace(String msg) {
        if (tracing && generationSteps != null) {
            generationSteps.add(msg);
        }
    }
}
