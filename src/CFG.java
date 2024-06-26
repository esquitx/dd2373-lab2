
import java.util.*;

public class CFG {
	private FG fg = null;
	private DFA dfa = null;

	// Properties
	String startingVariable = "$";
	HashMap<String, List<AppearanceNode>> appearances;
	HashMap<String, Set<Production>> productTable;
	HashMap<String, Integer> generatingTable;

	// Constructor
	// Takes flow graph (FG) and specification (DFA) and builds
	public CFG(FG fg, DFA dfa) {

		this.fg = fg;
		this.dfa = dfa;
		appearances = new HashMap<>();
		productTable = new HashMap<>();
		generatingTable = new HashMap<>();

		computeProduct();

		//
		// printTable();
		// printAppearences();
		//

		int i = emptynessTest();
		System.out.println(
				(i == 1) ? " XXX || SPECIFICATIONS VIOLATED || XXX" : " _/_/_/ || SPECIFICATIONS RESPECTED || _/_/_/");

		// generate counter example !!!
//		if (i == 1) {
//			System.out.println("Generating counterexample...");
//			deleteExperimentalVariables();
//			generateExperimental();
//		}
	}

	public void addAppearence(String key, Production product, int indexInProduction) {
		AppearanceNode node = new AppearanceNode(product, indexInProduction);
		if (!appearances.containsKey(key)) {
			appearances.put(key, new ArrayList<AppearanceNode>());
		}
		appearances.get(key).add(node);
	}

	public void addToProductTable(String key, Production product) {
		if (!productTable.containsKey(key)) {
			productTable.put(key, new HashSet<>());
		}
		productTable.get(key).add(product);
	}

	public void computeProduct() {
		// set dfa to complement dfa
		dfa.complement();

		// update productions to get correct CFG

		// The process followed respects the bullet point layout in the lab handout
		// Every step corresponds to a different bullet point, that, when carried out in
		// order should obtain the desired result

		// 1-> starting
		addStartingProductions();

		// 2-> epsilon
		addEpsilonTransitionsOfFG();

		// 3 -> tripled rules
		addTripledRules();

		// 4 -> return nodes
		addReturnNodeRules();

		// 5 -> DFA rules
		addRulesFromDFA();
	}

	// STEP 1 ->
	// For every final state qi ∈ QF , a production S → [q0 v0 qi ],
	// where v0 is the entry node of the main method.
	private void addStartingProductions() {
		String initial = (String) dfa.getInitialState(); // q0
		Set<String> finals = dfa.getAcceptingStates(); // Qf of DFA

		String entry = fg.getEntryOfMain();

		System.out.println(fg.methodsToNodes);
		System.out.println(entry);

		for (String state : finals) {
			Production prod = new Production();
			prod.count = 1;
			prod.parentVariable = startingVariable;
			prod.production.add("[" + String.join("-", initial, entry, state) + "]");
			this.addToProductTable(startingVariable, prod);
			this.addAppearence(prod.production.get(prod.production.size() - 1), prod, -1);
		}
	}

	// STEP 2 ->
	// Add production for every transfer edge and state sequence
	private void addEpsilonTransitionsOfFG() {
		Set<NodePair<String>> epsPairs = fg.getMethodTransitions("eps");

		// get all pairs of state sequences
		ArrayList<String> stateSequences = this.getStateSequences(false);

		// for each eps transition edge
		for (NodePair<String> pair : epsPairs) {

			// for each q_a,q_b in Q^2
			for (String sequence : stateSequences) {
				String[] seq = sequence.split("-");
				String src = pair.firstNode; // v_i
				String dst = pair.secondNode; // v_j
				String qA = seq[0]; // q_a
				String qB = seq[1]; // q_b
				String head = "[" + String.join("-", qA, src, qB) + "]";

				Production prod = new Production();
				prod.parentVariable = head;
				prod.count = 1;
				prod.production.add("[" + String.join("-", qA, dst, qB) + "]");
				this.addToProductTable(head, prod);
				this.addAppearence(prod.production.get(prod.production.size() - 1), prod, -1);
			}
		}
	}

	// STEP 3 ->
	// For every call edge and state sequence, add tripled rules (production out of
	// entry node)
	private void addTripledRules() {
		// get all Q^4 => q_a,q_b,q_c,q_d
		ArrayList<String> quadSequences = this.getStateSequences(true);

		// get method names and remove eps
		Set<String> methods = fg.edgeTransitions.keySet();

		// methods.remove("eps"); // remove to declutter rules

		// for every call edge m
		for (String method : methods) {

			if (!method.equals("eps")) {
				Set<String> entryNode = fg.getNodes(method, NodeType.ENTRY);
				if (entryNode.size() == 0) {
					break;
				}
				String entry = entryNode.toArray(new String[entryNode.size()])[0]; // v_k
				Set<NodePair<String>> pairs = fg.getMethodTransitions(method); // all transitions on m

				// for every v_i -m-> v_j
				for (NodePair<String> pair : pairs) {
					String src = pair.firstNode; // v_i
					String dst = pair.secondNode; // v_j

					// for every sequence q_A,q_b,q_c,q_d in Q^4
					for (String sequence : quadSequences) {
						// Create and add production
						Production prod = new Production();
						String[] stateSeq = sequence.split("-");

						String head = "[" + String.join("-", stateSeq[0], src, stateSeq[3]) + "]";
						prod.parentVariable = head;
						prod.count = 3;

						prod.production.add("[" + String.join("-", stateSeq[0], method, stateSeq[1]) + "]");
						this.addAppearence(prod.production.get(prod.production.size() - 1), prod, -1);

						prod.production.add("[" + String.join("-", stateSeq[1], entry, stateSeq[2]) + "]");
						this.addAppearence(prod.production.get(prod.production.size() - 1), prod, -1);

						prod.production.add("[" + String.join("-", stateSeq[2], dst, stateSeq[3]) + "]");
						this.addAppearence(prod.production.get(prod.production.size() - 1), prod, -1);

						this.addToProductTable(head, prod);
					}
				}
			}
		}
	}

	// STEP 4 ->
	// For every return node and every state, add production to empty string
	private void addReturnNodeRules() {
		Set<String> methods = fg.methodsToNodes.keySet();
		Set<String> states = dfa.getStates();
		for (String method : methods) {

			if (!method.equals("eps")) {
				// get every return node of method
				Set<String> returnNodes = fg.getNodes(method, NodeType.RET);

				// for every return node v_i
				for (String node : returnNodes) {

					for (String state : states) {
						String head = "[" + String.join("-", state, node, state) + "]";

						Production prod = new Production();
						prod.parentVariable = head;
						prod.production.add("eps");

						this.addToProductTable(head, prod);
					}
				}
			}
		}
	}

	// STEP 5 ->
	// Add production for every transition in DFA
	private void addRulesFromDFA() {
		HashMap<String, HashMap<String, Set<String>>> transitions = dfa.getTransitions();

		// iterator over the states of DFA
		Iterator states = transitions.entrySet().iterator();

		while (states.hasNext()) {
			Map.Entry pair = (Map.Entry) states.next();
			String src = (String) pair.getKey(); // q_i

			// Iterator over transitions from src
			HashMap<String, String> labels = (HashMap<String, String>) pair.getValue();
			Iterator trans = labels.entrySet().iterator();

			while (trans.hasNext()) {
				Map.Entry innerPair = (Map.Entry) trans.next();
				String dst = (String) innerPair.getKey(); // q_j
				Set<String> methods = (Set<String>) innerPair.getValue();

				// iterate over methods, excluding eps that trigger the transition
				for (String method : methods) {
					if (!method.equals("eps")) {
						String head = "[" + String.join("-", src, method, dst) + "]";

						Production prod = new Production();
						prod.production.add(method);
						prod.parentVariable = head;
						this.addToProductTable(head, prod);
					}
				}
			}
		}
	}

	// get either Q^2 or Q^4
	private ArrayList<String> getStateSequences(boolean quadrupled) {
		Set<String> allStates = (Set<String>) dfa.getStates();

		ArrayList<String> sequences = new ArrayList<>();

		for (String first : allStates) {

			for (String second : allStates) {
				// if we need a quad-sequence, Q^4
				if (quadrupled) {
					for (String third : allStates) {

						for (String fourth : allStates) {
							sequences.add(String.join("-", first, second, third, fourth));
						}
					}
				} else {
					sequences.add(String.join("-", first, second));
				}
			}
		}
		return sequences;
	}

	public int emptinessTest() {
		System.out.println("PRINTING TABLEEE");
		printTable();
		return 0;
	}

	public int emptynessTest() {
		// Adjust the appearances table and generating table for the algorithm
		for (String key : productTable.keySet()) {
			// Put 0 to represent ? for every variable
			generatingTable.put(key, 0);

			// If a variable does not appear any where in the production table, create an
			// empty list to avoid null pointer exception
			if (!appearances.containsKey(key)) {
				appearances.put(key, new ArrayList<>());
			}
		}

		LinkedList<String> nodesToVisit = new LinkedList<>();
		// Traverse over every production to find terminating variables
		for (String variable : productTable.keySet()) {
			for (Production product : productTable.get(variable)) {
				if (product.count == 0 && generatingTable.get(variable) == 0) {
					nodesToVisit.add(variable);
					generatingTable.put(variable, 1);
				}
			}
		}

		// Try to find every possible generating variable
		while (nodesToVisit.size() > 0) {
			// Get the variable at the head
			String nodeToVisit = nodesToVisit.remove();

			// System.out.println( nodeToVisit);
			// Get the appearances of that variable
			List<AppearanceNode> appearanceList = appearances.get(nodeToVisit);
			for (AppearanceNode toVisit : appearanceList) {
				toVisit.productionAppeardIn.count--;
				if (toVisit.productionAppeardIn.count == 0
						&& generatingTable.get(toVisit.productionAppeardIn.parentVariable) == 0) {
					nodesToVisit.add(toVisit.productionAppeardIn.parentVariable);
					generatingTable.put(toVisit.productionAppeardIn.parentVariable, 1);
				}
			}
		}

		// Mark every variable as non-generating
		for (String key : generatingTable.keySet()) {
			if (generatingTable.get(key) != 1) {
				generatingTable.put(key, -1);
			}
		}

		return generatingTable.get(startingVariable);
	}

	public void printAppearences() {
		for (String variable : appearances.keySet()) {
			System.out.print(variable + " -> ");
			for (AppearanceNode node : appearances.get(variable)) {
				System.out.print(node.productionAppeardIn.parentVariable + " , ");
			}
			System.out.println();
		}
	}

	public void printTable() {
		for (String parentVariable : productTable.keySet()) {
			System.out.print(parentVariable + " -> ");
			for (Production p : productTable.get(parentVariable)) {
				for (String symbol : p.production) {
					System.out.print(symbol); // + ".");
				}
				// System.out.print( " and count is " + p.count);
				// System.out.print( " and parent node is " + p.parentVariable);
				System.out.print(", ");
			}
			System.out.println();
		}
	}

	public void deleteExperimentalVariables() {
		printTable();
		System.out.println("*******************");
		System.out.println("*******************");

		int count = 0;
		for (String curVariable : productTable.keySet()) {
			ArrayList<Production> productions = new ArrayList<>(productTable.get(curVariable));
			int i = 0;
			while (i < productions.size()) {
				if (productions.get(i).count > 0) {
					count++;
					productTable.get(curVariable).remove(productions.get(i));
				}
				i++;
			}
		}
		System.out.println(count);

		for (String curVariable : generatingTable.keySet()) {
			if (generatingTable.get(curVariable) == -1) {
				productTable.remove(curVariable);
			}
		}

		System.out.println("*******************");
		System.out.println("*******************");
		printTable();
	}

	public void generateExperimental() {
		// Hashtable<Production, Boolean> visited = new Hashtable<>();
		HashSet<Production> visited = new HashSet<>();
		Stack<String> variableStack = new Stack<>();
		StringBuilder toGenerate = new StringBuilder();

		variableStack.push(startingVariable);
		while (variableStack.size() > 0) {
			String curVariable = variableStack.pop();

			// Check if terminal
			if (curVariable.charAt(0) != '[' && curVariable.charAt(0) != '$') // Check if it is a terminal symbol
			{
				// Push the terminating symbol only if its not EPSILON
				if (!curVariable.equals("eps")) {
					toGenerate.append(curVariable);
				}
			} else // Find a new production that is not visited and push it to the stack according
					// to the variable order!
			{
				for (Production p : productTable.get(curVariable)) {
					if (!visited.contains(p)) {
						// mark Production as visited
						visited.add(p);

						ArrayList<String> variablesToVisit = p.production;
						for (int i = variablesToVisit.size() - 1; i >= 0; i--) {
							variableStack.push(variablesToVisit.get(i));
						}
					}
				}
			}
		}
		System.out.println(toGenerate.toString());
	}
}

//class AppearanceNode {
//	Production productionAppeardIn;
//	int index;
//
//	public AppearanceNode(Production production, int indexInProduction) {
//		productionAppeardIn = production;
//		index = indexInProduction;
//	}
//
//	public AppearanceNode(Production production) {
//		productionAppeardIn = production;
//		index = -1;
//	}
//}
//
//class Production {
//	ArrayList<String> production;
//	int count;
//	String parentVariable;
//
//	public Production() {
//		production = new ArrayList<>();
//		count = 0;
//		parentVariable = null;
//	}
//}