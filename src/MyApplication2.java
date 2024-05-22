import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MyApplication2 {
    public static void main(String[] args) {
        // Check if the correct number of arguments is provided
        if (args.length != 2) {
            // Print usage message
            System.err.println("Usage: java Main <grammar-file.cfg> <specification-file.spec>");
            System.exit(1);
            return;
        }

        // Get the file paths from the arguments
        String grammarFilePath = args[0];
        String specFilePath = args[1];

        // Check if the files exist
        File grammarFile = new File(grammarFilePath);
        File specFile = new File(specFilePath);

//        if (!grammarFile.exists() || !specFile.exists()) {
//            System.out.println("Error: One or both of the specified files do not exist.");
//            return;
//        }

        // Proceed with processing the files
        System.out.println("Processing files:");
        System.out.println("Grammar file: " + grammarFilePath);
        System.out.println("Specification file: " + specFilePath);

        // Add your file processing logic here
        // Automaton and so
        DFA<String, String> specDFA;
        FG fg = new FG();
        CFG cfg;
        // Parse and process the grammar file
        try {
            parseGrammarFile(fg, grammarFilePath);
        } catch (IOException e) {
            System.out.println("Error reading grammar file: " + e.getMessage());
        }

        // Parse and process the specification file
        try {
            specDFA = new DFA<>(specFilePath);
            cfg = new CFG(fg, specDFA); // this runs the emptiness test
            cfg.computeProduct();
            int i = cfg.emptynessTest(); // 1 if the grammar is generating
            System.out.println(
                    (i == 1) ? " XXX || SPECIFICATIONS VIOLATED ($ is generating) || XXX"
                            : " _/_/_/ || SPECIFICATIONS RESPECTED ($ is not generating) || _/_/_/");

            System.out.println();
            i = cfg.emptinessTest();
            System.out.println(
                    (i == 1) ? " XXX || SPECIFICATIONS VIOLATED ($ is generating) || XXX"
                            : " _/_/_/ || SPECIFICATIONS RESPECTED ($ is not generating) || _/_/_/");

////             generate counter example !!!
//             if (i == 1) {
//                 System.out.println("Generating counterexample...");
//                 deleteExperimentalVariables();
//                 generateExperimental();
//             }
        } catch (IOException e) {
            System.out.println("Error reading specification file: " + e.getMessage());
        }
    }

    // Method to parse and process the grammar file
    private static void parseGrammarFile(FG fg, String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 3) {
                    if ("node".equals(parts[0])) {
                        String type = parts[0];
                        String vertexName = parts[1];
                        String method = parts[2];
                        String nodeType = parts.length == 4 ? parts[3] : "none";
                        fg.addNodeType(method, vertexName, nodeType);
//                        System.out.println("Node: " + type + ", " + vertexName + ", " + method + ", " + nodeType);
                    } else if ("edge".equals(parts[0])) {
                        String type = parts[0];
                        String vertexFrom = parts[1];
                        String vertexTo = parts[2];
                        String method = parts[3];
                        fg.addNodePair(method, vertexFrom, vertexTo);
//                        System.out.println("Edge: " + type + ", " + vertexFrom + ", " + vertexTo + ", " + method);
                    }
                }
            }
        }
    }

    // Method to parse and process the specification file
//    private static DFA parseSpecFile(DFA dfa, String filePath) throws IOException {
//        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                System.out.println("Spec: " + line);
//            }
//        }
//    }
}
