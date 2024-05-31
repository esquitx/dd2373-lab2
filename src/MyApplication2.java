import org.w3c.dom.Node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
        CFG2 cfg;
        // Parse and process the grammar file
        try {
            parseGrammarFile(fg, grammarFilePath);
        } catch (IOException e) {
            System.out.println("Error reading grammar file: " + e.getMessage());
        }

        // Parse and process the specification file
        try {
            specDFA = new DFA<>(specFilePath);
            cfg = new CFG2(fg, specDFA); // this runs the emptiness test
//            cfg.computeProduct();
//            int i = cfg.emptynessTest(); // 1 if the grammar is generating
//            System.out.println(
//                    (i == 1) ? " sXXX || SPECIFICATIONS VIOLATED ($ is generating) || XXX"
//                            : " s_/_/_/ || SPECIFICATIONS RESPECTED ($ is not generating) || _/_/_/");

            System.out.println();
//            cfg.emptinessTest();

////             generate counter example !!!
//             if (i == 1) {
//                 System.out.println("Generating counterexample...");
//                 cfg.deleteExperimentalVariables();
//                 cfg.generateExperimental();
//             }
        } catch (IOException e) {
            System.out.println("Error reading specification file: " + e.getMessage());
        }
    }

    // Method to parse and process the grammar file
    private static void parseGrammarFile(FG fg, String filePath) throws IOException {
        if (filePath.contains("Vote.cfg")) {
            parseGrammarFileWithExceptions(fg, filePath);
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            Set<String> acceptableMethods = new HashSet<>();
            acceptableMethods.add("eps");
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    if ("node".equals(parts[0])) {
                        String type = parts[0];
                        String vertexName = parts[1];
                        String method = parts[2].substring(5, parts[2].length() - 1); // remove the meth( ) around meth(main) for example
                        if (method.startsWith("Vote-"))
                            method = method.substring(5);
                        if (method.startsWith("_"))
                            method = method.substring(1);
                        if (method.endsWith("_"))
                            method = method.substring(0,method.length() - 1);
                        String nodeType = parts.length == 4 ? parts[3] : NodeType.NONE;
                        acceptableMethods.add(method);
                        System.out.println("INSERTING node " + vertexName + " in method " + method + " of type " + nodeType);
                        fg.addNodeType(method, vertexName, nodeType);
//                        System.out.println("Node: " + type + ", " + vertexName + ", " + method + ", " + nodeType);
                    } else if ("edge".equals(parts[0])) {
//                        for (String part : parts)
//                            System.out.println("part " + part);
                        String type = parts[0];
                        String vertexFrom = parts[1];
                        String vertexTo = parts[2];
                        String method = parts[3];
                        if (method.startsWith("Vote-"))
                            method = method.substring(5);
                        if (method.startsWith("_"))
                            method = method.substring(1);
                        if (method.endsWith("_"))
                            method = method.substring(0,method.length() - 1);
//                        System.out.println("GUGUGAGA " + method);
                        System.out.println("FOUND method " + method + " from " + vertexFrom + " to " + vertexTo);
                        if (!acceptableMethods.contains(method))
                            method = "eps";
                        System.out.println("INSERTING method " + method + " from " + vertexFrom + " to " + vertexTo);
                        fg.addNodePair(method, vertexFrom, vertexTo);
//                        System.out.println("Edge: " + type + ", " + vertexFrom + ", " + vertexTo + ", " + method);
                    }
                }
            }
        }
    }

    private static void parseGrammarFileWithExceptions(FG fg, String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            Set<String> acceptableMethods = new HashSet<>();
            acceptableMethods.add("eps");
            System.out.println("READING VOTE.CFG");
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    if ("node".equals(parts[0])) {
                        String type = parts[0];
                        String vertexName = parts[1];
                        int endOfMethodName = parts[2].indexOf(')');
                        String method = parts[2].substring(5, endOfMethodName); // remove the meth( ) around meth(main) for example
                        if (method.startsWith("Vote-"))
                            method = method.substring(5);
                        if (method.startsWith("_"))
                            method = method.substring(1);
                        if (method.endsWith("_"))
                            method = method.substring(0,method.length() - 1);

                        if (parts.length >= 4) {
                            String nodeType;
                            if (parts[parts.length - 1].equals("ret") || parts[parts.length - 1].equals("entry") ) {
                                nodeType = parts[parts.length - 1];
                            }
                            else nodeType = NodeType.NONE;
                            acceptableMethods.add(method);
                            System.out.println("INSERTING node " + vertexName + " in method " + method + " of type " + nodeType);
                            fg.addNodeType(method, vertexName, nodeType);
                        }
//                        System.out.println("Node: " + type + ", " + vertexName + ", " + method + ", " + nodeType);
                    } else if ("edge".equals(parts[0])) {
//                        for (String part : parts)
//                            System.out.println("part " + part);
                        String type = parts[0];
                        String vertexFrom = parts[1];
                        String vertexTo = parts[2];
                        String method = parts[3];
                        if (method.startsWith("Vote-"))
                            method = method.substring(5);
                        if (method.startsWith("_"))
                            method = method.substring(1);
                        if (method.endsWith("_"))
                            method = method.substring(0,method.length() - 1);
                        System.out.println("FOUND method " + method + " from " + vertexFrom + " to " + vertexTo);
//                        System.out.println("GUGUGAGA " + method);
                        if (!acceptableMethods.contains(method))
                            method = "eps";
                        System.out.println("INSERTING method " + method + " from " + vertexFrom + " to " + vertexTo);
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
