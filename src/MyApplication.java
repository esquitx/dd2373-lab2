import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class MyApplication {

	public static void main(String[] args) throws FileNotFoundException {

		// Usage: uncomment file you want to test

		//
		System.out.println("<^----------------------------^>");
		System.out.println("|-> R*U*N*N*I*N*G**T*E*S*T*S <-|");
		System.out.println("<------------------------------>");

		// TESTS
		// -----------------------

		// Automaton and so
		DFA<String, String> specDFA;
		FG fg;
		CFG cfg;

		// Paths
		String cfgPath;
		String specPath;

		// // MyElevatorTest
		// // |||||||||||||||||||||||||
		// System.out.println("\n ---> MyElevatorTest <--- \n");

		// // valid
		// System.out.println("Processing valid.cfg file...");
		// cfgPath = getPath("MyElevatorTest/valid.cfg");
		// fg = readFlowGraphConfig(cfgPath);
		// // fg.printFG();
		// //

		// invalid
		// System.out.println("Processing invalid.cfg file...");
		// cfgPath = getPath("MyElevatorTest/invalid.cfg");
		// fg = readFlowGraphConfig(cfgPath);
		// fg.printFG();
		// //
		//
		// // myElevator.spec
		// System.out.println("Processing myElevator.spec file...");
		// specPath = getPath("MyElevatorTest/myElevator.spec");
		// specDFA = new DFA<String, String>(specPath);
		// specDFA.saveGV();
		// specDFA.printGV(); //

		// |||||||||||||||||||||||||

		// // Simple
		// // |||||||||||||||||||||||||
		// System.out.println("\n ---> Simple TEST <--- \n");

		//
		// System.out.println("Processing simple.cfg file...");
		// cfgPath = getPath("Simple/simple.cfg");
		// fg = readFlowGraphConfig(
		// cfgPath);
		// fg.printFG();
		//

		// simple.spec
		// System.out.println("Processing simple.spec file...");
		// specPath = getPath("Simple/simple.spec");
		// specDFA = new DFA<String, String>(
		// specPath);
		// specDFA.saveGV();
		// specDFA.printGV();

		// EvenOdd
		// |||||||||||||||||||||||||
		// System.out.println("\n ---> EvenOdd TEST <--- \n");

		//
		// System.out.println("Processing EvenOdd.cfg file...");
		// cfgPath = getPath("EvenOdd/EvenOdd.cfg");
		// fg = readFlowGraphConfig(cfgPath);
		// fg.printFG();
		//

		// // //
		// System.out.println(" !! testing EvenOdd1a !!");
		// System.out.println("Processing EvenOdd1a.spec file...");
		// specPath = getPath("EvenOdd/EvenOdd1a.spec");
		// specDFA = new DFA<String, String>(
		// specPath);
		// specDFA.printGV();
		// specDFA.saveGV();
		// // //

		//
		// System.out.println(" !! testing EvenOdd1b !!");
		// System.out.println("Processing EvenOdd1b.spec file...");
		// specPath = getPath("EvenOdd/EvenOdd1b.spec");
		// specDFA = new DFA<String, String>(specPath);
		// specDFA.printGV();
		// specDFA.saveGV();
		//

		// // |||||||||||||||||||||||||||||||||

		// // Vote
		// // |||||||||||||||||||||||||||||||||

		// ---------------------------------
		// System.out.println("\n ---> Vote TEST <--- \n");
		// ---------------------------------

		// Vote.cfg
		System.out.println("Processing Vote.cfg file...");
		cfgPath = getPath("Vote/Vote_ne.cfg");
		fg = readFlowGraphConfig(cfgPath);
		fg.printFG();
		//

		// Vote_clean.cfg
		// System.out.println("Processing Vote_clean.cfg file...");
		// cfgPath = getPath("Vote/Vote_clean.cfg");
		// fg = readFlowGraphConfig(cfgPath);
		// fg.printFG();
		//

		// Vote_new.cfg
		// System.out.println("Processing Vote_ne.cfg file...");
		// cfgPath = getPath("Vote/Vote_ne.cfg");
		// fg = readFlowGraphConfig(cfgPath);
		// fg.printFG();
		//

		// Vote_v.spec
		// System.out.println("Processing Vote_v.spec file...");
		// specPath = getPath("Vote/Vote_v.spec");
		// specDFA = new DFA<String, String>(specPath);
		// specDFA.saveGV();
		// specDFA.printGV();
		//

		// Vote_gv.spec
		System.out.println("Processing Vote_gv.spec file...");
		specPath = getPath("Vote/Vote_gv.spec");
		specDFA = new DFA<String, String>(specPath);
		specDFA.saveGV();
		specDFA.printGV();
		//

		// // cfg
		cfg = new CFG(fg, specDFA);

		System.exit(0);
	}

	public static FG readFlowGraphConfig(String filePath) {

		// fg
		FG fg = new FG();

		try {
			Scanner scan = new Scanner(new File(filePath));
			String[] arguments = new String[4];

			while (scan.hasNextLine()) {
				// Read line
				String line = scan.nextLine();

				// System.out.println(line);
				// Set pointers for arguments and index
				int curArg = 0;
				int prev = 0;

				// iterate over characters in line
				for (int i = 0; i < line.length(); i++) {

					if (line.charAt(i) == ' ') {
						while (line.charAt(i + 1) == ' ') {
							i++;
						}
						// if character is empty space, change argument and skip
						arguments[curArg++] = line.substring(prev, i);
						prev = i + 1;

					} else if (i + 1 == line.length()) {
						// otherwise, read word
						arguments[curArg] = line.substring(prev, i + 1);
					}
				}

				// process NODE ->
				if (arguments[0].equals("node")) {
					arguments[2] = getMethod(arguments[2]);
					// set type to if not indicated
					if (arguments[3] == null) {
						arguments[3] = NodeType.NONE;
					}

					fg.addNodeType(arguments[2], arguments[1], arguments[3]);
				} else {
					fg.addNodePair(arguments[3], arguments[1], arguments[2]);
				}

				// reset arguments
				arguments[0] = null;
				arguments[1] = null;
				arguments[2] = null;
				arguments[3] = null;
				// ------------------

			}

			scan.close();
		} catch (IOException e) {
			System.out.println("404 - Input file selected NOT FOUND: " + e.getMessage());
			System.exit(0);
		}

		return fg;
	}

	// returns method name given method defintiion
	public static String getMethod(String method) {
		return method.substring(5, method.length() - 1);
	}

	public static String getPath(String pathBranch) {

		// Get path
		Path current = Paths.get("testcases");
		String pathPrefix = current.toAbsolutePath().toString().replace("\\", "/");
		String filePath = pathPrefix + "/" + pathBranch;

		return filePath;

	}

}
