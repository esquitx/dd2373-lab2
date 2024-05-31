
import java.sql.Array;
import java.sql.SQLOutput;
import java.util.*;

public class CFG2 {
	private FG fg = null;
	private DFA dfa = null;

	// Properties
	String startingVariable = "$";
	HashMap<String, GeneratingTableEntry> generatingTable; // name -> {Node, Status}

	enum Status {
		GENERATING, NOT_GENERATING, NON_DETERMINED
	}

	private class GeneratingTableEntry { // {Node, Status}
		Node node;
		Status status;

		public GeneratingTableEntry(Node node, Status status) {
			this.node = node;
			this.status = status;
		}
	}

	// public void printAppearances() {
	// System.out.println("PRINTING APPEARANCES:");
	// for (String name : generatingTable.keySet()) {
	// Node node = generatingTableEntry(name).node;
	// System.out.print(node.name + " appears in: ");
	// for (Appearance appearance : node.appearances)
	// System.out.print(appearance.toString() + "; ");
	// System.out.println();
	// }
	// System.out.println();
	// }

	public GeneratingTableEntry generatingTableEntry(String name) { // returns an entry or creates one if non-existent
		if (!generatingTable.containsKey(name))
			generatingTable.put(name, new GeneratingTableEntry(new Node(name), Status.NON_DETERMINED));
		return generatingTable.get(name);
	}

	public GeneratingTableEntry generatingTableEntry(String name, Status status) { // returns an entry or creates one if
																					// non-existent
		generatingTableEntry(name).status = status;
		return generatingTable.get(name);
	}

	private class Appearance { // {headNode, index}
		Node headNode;
		int index;

		public Appearance(Node headNode, int index) {
			this.headNode = headNode;
			this.index = index;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("[node=")
					.append(headNode.name)
					.append(" idx=")
					.append(index);
			return sb.toString();
		}
	}

	private class Production {
		Node headNode;
		Node[] productionBody;
		int count;

		public Production(Node headNode, Node[] productionBody, int count) {
			this.headNode = headNode;
			this.productionBody = productionBody;
			this.count = count;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(headNode.name).append(" -> ");
			for (Node node : productionBody) {
				sb.append(" ").append(node.name);
			}
			sb.append(", COUNT = ").append(count);
			return sb.toString();
		}
	}

	private class Node { // {name, [Node1, Node2, ...], [{headNode1, idx1}, {headNode2, idx1}, ...],
							// count}
		String name;
		// ArrayList<Node[]> productions; // all productions from this node to other
		// nodes
		ArrayList<Production> productions;
		ArrayList<Production> appearances; // all appearances of the node
		int count;

		public Node(String name) {
			this.name = name;
			this.productions = new ArrayList<>();
			this.appearances = new ArrayList<>();
			this.count = 0;
		}

		// the method addProduction is the crux of this implementation
		public void addProduction(Node[] dstNodes) { // this method adds a production srcNode -> dstNode, adds
														// appearance for dstNode, increments count for srcNode (if
														// necessary)
			Production newProduction = new Production(this, dstNodes, dstNodes.length);
			this.productions.add(newProduction);
			for (Node dstNode : dstNodes) {
				dstNode.appearances.add(newProduction);
			}
			count += 1;
		}

		public void addProduction(Node dstNode) { // this method adds a production srcNode -> dstNode, adds appearance
													// for dstNode, increments count for srcNode (if necessary)
			Node[] dstNodes = new Node[] { dstNode };
			Production newProduction = new Production(this, dstNodes, 1);
			this.productions.add(newProduction);
			dstNode.appearances.add(newProduction);
			// if (generatingTable.get(dstNode.name).status == Status.NON_DETERMINED) // in
			// case it is known to be generating, we do not increment
			// count += 1;
			count += 1;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(name);
			sb.append(" -> ");
			for (Production production : productions) {
				for (Node dstNode : production.productionBody) {
					sb.append(dstNode.name);
				}
				sb.append(" count = ").append(production.count).append(" | "); // concatenation of these
			}
			sb.append(", varCount = ");
			sb.append(count);
			return sb.toString();
		}
	}

	// Constructor
	// Takes flow graph (FG) and specification (DFA) and builds
	public CFG2(FG fg, DFA dfa) {

		// init
		this.fg = fg;
		this.dfa = dfa;
		generatingTable = new HashMap<>();

		// print dfa spec and flow graphs
		// dfa.printGV();
		// fg.printFG2();

		computeProduct();

		// perform emptiness test and print the verdict
		Status startingSymbolStatus = emptinessTest2();
		System.out.println("The starting symbol $ is " + startingSymbolStatus);
		if (startingSymbolStatus == Status.GENERATING)
			System.out.println("Specification is therefore VIOLATED");
		else
			System.out.println("Specification is therefore ACCEPTED");
		System.out.println();

		// if spec is violated, print a counter example generating derivation
		if (generatingTableEntry(startingVariable).status == Status.GENERATING)
			counterExample2();
		// generateExperimentalAdam();
		System.out.println();

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

		// CUSTOM 6 -> remove all variables which do not produce anything and all
		// productions in which they appear
		removeUnreachable();
	}

	// STEP 1 ->
	// For every final state qi ∈ QF , a production S → [q0 v0 qi ],
	// where v0 is the entry node of the main method.
	private void addStartingProductions() {
		String initial = (String) dfa.getInitialState(); // q0
		Set<String> finals = dfa.getAcceptingStates(); // Qf of DFA

		String entry = fg.getEntryOfMain();

		// System.out.println(fg.methodsToNodes);
		// System.out.println(entry);

		String startSymbol = "$";
		Node startNode = generatingTableEntry(startSymbol).node;

		for (String state : finals) {
			String name = "[" + String.join("-", initial, entry, state) + "]";
			Node dstNode = generatingTableEntry(name).node;
			startNode.addProduction(dstNode);
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

				String srcName = "[" + String.join("-", qA, src, qB) + "]";
				Node srcNode = generatingTableEntry(srcName).node;

				String dstName = "[" + String.join("-", qA, dst, qB) + "]";
				Node dstNode = generatingTableEntry(dstName).node;

				srcNode.addProduction(dstNode);
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
					// System.out.println("ASDASODHOIA");
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
						String[] stateSeq = sequence.split("-");

						String srcName = "[" + String.join("-", stateSeq[0], src, stateSeq[3]) + "]";
						Node srcNode = generatingTableEntry(srcName).node;

						String[] dstNames = new String[3];
						Node[] dstNodes = new Node[3];

						dstNames[0] = "[" + String.join("-", stateSeq[0], method, stateSeq[1]) + "]";
						dstNames[1] = "[" + String.join("-", stateSeq[1], entry, stateSeq[2]) + "]";
						dstNames[2] = "[" + String.join("-", stateSeq[2], dst, stateSeq[3]) + "]";

						for (int i = 0; i < dstNames.length; i++) {
							dstNodes[i] = generatingTableEntry(dstNames[i]).node;
						}
						srcNode.addProduction(dstNodes);
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
						String srcName = "[" + String.join("-", state, node, state) + "]";
						Node srcNode = generatingTableEntry(srcName).node;
						String dstName = "eps";
						Node dstNode = generatingTableEntry(dstName, Status.NOT_GENERATING).node;
						srcNode.addProduction(dstNode);
						srcNode.count -= 1;
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
						String srcName = "[" + String.join("-", src, method, dst) + "]";
						Node srcNode = generatingTableEntry(srcName).node;
						String dstName = method;
						Node dstNode = generatingTableEntry(dstName, Status.GENERATING).node;
						srcNode.addProduction(dstNode);
					}
				}
			}
		}
	}

	// STEP 6 ->
	private void removeUnreachable() {
		// find all terminal symbols (the ones we want to generate)
		ArrayList<String> terminals = new ArrayList<>();
		for (String name : generatingTable.keySet())
			if (!name.equals("$") && !name.startsWith("[")) // allow eps
				terminals.add(name);

		// find the unreachable states
		Queue<String> unreachable = new LinkedList<>();
		Set<String> visited = new HashSet<>();
		for (String variable : generatingTable.keySet()) {
			if (variable.equals("[q1-c34m1p3-q1]")) {
				System.out.println("[q1-c34m1p3-q1] is here");
				System.out.println(generatingTableEntry("[q1-c34m1p3-q1]").node);
			}
			if (!terminals.contains(variable) && generatingTableEntry(variable).node.productions.size() == 0) {
				unreachable.add(variable);
				visited.add(variable);
			}
		}

		System.out.println("MISTAKE " + unreachable.contains("[q1-c34m1p3-q1]"));
		while (!unreachable.isEmpty()) {
			String variable = unreachable.poll();
			// remove all appearances of the unreachable node
			for (Production appearance : generatingTableEntry(variable).node.appearances) {
				appearance.headNode.productions.remove(appearance);
				// if the headNode becomes unreachable, add it to the queue
				if (appearance.headNode.productions.size() == 0 && !visited.contains(appearance.headNode.name)) {
					// unreachable.add(appearance.headNode.name); // these two should be uncommented
					// visited.add(appearance.headNode.name);
					System.out.println("ADDING " + appearance.headNode.name);
				}
			}

			// remove the unreachable node
			generatingTable.remove(variable);
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

	public Status emptinessTest2() {
		Queue<Node> toVisit = new LinkedList<>();
		Set<Node> visited = new HashSet<>();

		// add terminal symbols to the queue
		for (String nodeName : generatingTable.keySet())
			if (generatingTableEntry(nodeName).status == Status.GENERATING)
				toVisit.add(generatingTableEntry(nodeName).node);

		// loop until all generating variables are identified
		while (!toVisit.isEmpty()) {
			Node currentNode = toVisit.remove();
			visited.add(currentNode);
			// System.out.println("POPPED: " + currentNode);
			for (Production appearance : currentNode.appearances) {
				if (generatingTableEntry(appearance.headNode.name).status == Status.NON_DETERMINED) {
					appearance.count = 0; // we can set it to 0 instead of incrementing because we know if one variable
											// in production body is generating, the others can lead to epsilon, so
											// showing one variable is generating also shows the whole body is
											// generating because of the construction
					generatingTableEntry(appearance.headNode.name, Status.GENERATING);
					// if (!toVisit.peek().equals(appearance.headNode)) { // alternative way of
					// adding
					if (!visited.contains(appearance.headNode)) {
						visited.add(appearance.headNode);
						toVisit.add(appearance.headNode);
					}
				}
			}
		}

		// mark all non-generating nodes as non-generating
		for (String name : generatingTable.keySet())
			if (generatingTableEntry(name).status == Status.NON_DETERMINED)
				generatingTableEntry(name, Status.NOT_GENERATING);

		printGeneratingTable();
		return generatingTableEntry(startingVariable).status;
	}

	public void counterExample2() {
		ArrayList<String> step = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		Queue<String> toVisit = new LinkedList<>();

		// find all terminal symbols (the ones we want to generate)
		ArrayList<String> terminals = new ArrayList<>();
		for (String name : generatingTable.keySet())
			// if (!name.equals("$") && !name.startsWith("[") && !name.equals("eps"))
			if (!name.equals("$") && !name.startsWith("[")) // allow derivations to terminate on eps
				terminals.add(name);

		toVisit.add(startingVariable);
		visited.add(startingVariable);
		step.add(startingVariable);

		StringBuilder derivation = new StringBuilder();
		boolean generatingPhase = true;

		while (!toVisit.isEmpty()) {
			String cur = toVisit.poll();

			// System.out.println("cur = " + cur + " step = " + step + " toVisit = " +
			// toVisit); // TO DEBUG
			int idxToReplace = step.indexOf(cur);
			if (idxToReplace == -1)
				break;
			step.remove(idxToReplace);

			String[] generatingBody = new String[0];
			for (Production production : generatingTableEntry(cur).node.productions) {
				// boolean allVisited = true;
				// for (Node variable : production.productionBody)
				// if (!visited.contains(variable.name)) {
				// allVisited = false;
				// break;
				// }
				//
				// if (allVisited)
				// continue;

				if (production.count == 0) {
					// stringify production body & add nextVariable to queue
					generatingBody = new String[production.productionBody.length];
					for (int i = 0; i < production.productionBody.length; i++) {
						generatingBody[i] = production.productionBody[i].name;
					}
					if (generatingPhase)
						break;
				}

				if (production.count != 0) {
					// stringify production body & add nextVariable to queue
					generatingBody = new String[production.productionBody.length];
					for (int i = 0; i < production.productionBody.length; i++) {
						generatingBody[i] = production.productionBody[i].name;
					}
					if (!generatingPhase)
						break;
				}
			}
			for (int i = generatingBody.length - 1; i >= 0; i--) { // SCBDD -> AcCBDD for production S -> Ac
				step.add(idxToReplace, generatingBody[i]);
			}

			for (String variable : step) {
				derivation.append(variable).append(" ");
			}

			// find left-most-variable
			for (String variable : step) {
				if (!terminals.contains(variable)) {
					toVisit.add(variable);
					visited.add(variable);
					break;
				} else {
					generatingPhase = false;
				}
			}

			derivation.append("-> ");
		}
		System.out.println(derivation.substring(0, derivation.length() - 4));
		// System.out.println(derivation);
	}

	// public Status emptinessTest() {
	// System.out.println("KAKAKAKA " +
	// generatingTableEntry(startingVariable).node.count);
	// // printGeneratingTable();
	// // printAppearances();
	// Queue<Node> toVisit = new LinkedList<>();
	// Set<Node> visited = new HashSet<>();

	// // add terminal symbols to the queue
	// for (String nodeName : generatingTable.keySet())
	// if (generatingTableEntry(nodeName).status == Status.GENERATING)
	// toVisit.add(generatingTableEntry(nodeName).node);
	// // for (String nodeName : generatingTable.keySet())
	// // if (generatingTableEntry(nodeName).status == Status.NON_DETERMINED &&
	// // generatingTableEntry(nodeName).node.count == 0) {
	// // generatingTableEntry(nodeName, Status.NOT_GENERATING);
	// // toVisit.add(generatingTableEntry(nodeName).node);
	// // }

	// // pop from the queue, decrement counter, update status (if generating) and
	// // append appearances to the queue
	// while (!toVisit.isEmpty()) {
	// Node currentNode = toVisit.remove();
	// visited.add(currentNode);
	// // System.out.println("JUST POPPED: " + currentNode.toString());
	// // System.out.println("SS " + currentNode);
	// for (Prodduction appearance : currentNode.appearances) { // TODO check this
	// if (generatingTableEntry(appearance.headNode.name).status ==
	// Status.NON_DETERMINED)
	// appearance.headNode.count -= 1;
	// if (appearance.headNode.count == 0) {
	// // if (appearance.headNode.name.equals("$"))
	// // System.out.println("LASTPROD " + currentNode);
	// generatingTableEntry(appearance.headNode.name, Status.GENERATING);
	// }
	// if (!visited.contains(appearance.headNode)) {
	// toVisit.add(appearance.headNode);
	// visited.add(appearance.headNode);
	// }
	// }
	// }

	// // // look at all non-visited nodes
	// // for (String name : generatingTable.keySet())
	// // if (generatingTableEntry(name).status == Status.NON_DETERMINED)
	// // if (!visited.contains(generatingTableEntry(name).node))
	// // toVisit.add(generatingTableEntry(name).node);
	// // System.out.println("TOVISIT2 TEST: " + toVisit.size());
	// //
	// // // mark all nodes which appear in a production where head is generating as
	// // generating
	// // for (String name : generatingTable.keySet()){
	// // Node currentNode = generatingTableEntry(name).node;
	// //// System.out.println("JUST POPPED: " + currentNode.toString());
	// // for (Appearance appearance : currentNode.appearances) {
	// // if (visited.contains(currentNode))
	// // continue;
	// // if (generatingTableEntry(appearance.headNode.name).status ==
	// // Status.GENERATING) {
	// // generatingTableEntry(currentNode.name, Status.GENERATING);
	// // System.out.println("MARKED");
	// // toVisit.poll();
	// // }
	// // }
	// // }

	// // mark all non-generating nodes as non-generating
	// for (String name : generatingTable.keySet())
	// if (generatingTableEntry(name).status == Status.NON_DETERMINED)
	// generatingTableEntry(name, Status.NOT_GENERATING);

	// // generatingTableEntry(name, Status.NOT_GENERATING);
	// printGeneratingTable();
	// return generatingTableEntry(startingVariable).status;
	// }

	public void printGeneratingTable() {
		System.out.println("GENERATING TABLE:");
		for (String name : generatingTable.keySet()) {
			System.out.println(generatingTableEntry(name).status + " " + generatingTableEntry(name).node.toString());
		}
		System.out.println();
	}

	// public void counterExample() {
	// System.out.println("counterExample() under reconstruction");
	// // find all terminal symbols (the ones we want to generate)
	// ArrayList<String> terminals = new ArrayList<>();
	// for (String name : generatingTable.keySet())
	// if (!name.equals("$") && !name.startsWith("[") && !name.equals("eps"))
	// terminals.add(name);

	// // System.out.println("TERMINALS: " + Arrays.toString(terminals.toArray()));

	// // go over each terminal symbol and propagate backwards until starting symbol
	// $
	// // is reached
	// // build the path with StringBuilder along the way
	// // once the starting symbol is reached, it means a counter example is found
	// // lastly print the counterExample

	// for (String terminalName : terminals) {
	// // setup
	// StringBuilder inversePath = new StringBuilder();
	// Set<Node> visited = new HashSet<>();
	// Queue<Node> toVisit = new LinkedList<>();

	// // get the node
	// Node terminalNode = generatingTableEntry(terminalName).node;
	// toVisit.add(terminalNode);
	// visited.add(terminalNode);

	// // iterate through its appearances BFS
	// while (!toVisit.isEmpty()) {
	// Node node = toVisit.poll();
	// // System.out.println(node.name);
	// inversePath.append(" <- ").append(node.name);
	// // System.out.println(sb);

	// // inverse path found - now work from start and only keep the visited ones
	// if (node.name.equals(startingVariable)) {

	// // inner setup
	// StringBuilder path = new StringBuilder();
	// Set<Node[]> innerVisited = new HashSet<>();
	// Queue<Node[]> innerToVisit = new LinkedList<>();
	// innerVisited.add(new Node[] { generatingTableEntry(startingVariable).node });
	// innerToVisit.add(new Node[] { generatingTableEntry(startingVariable).node });

	// // go forward, each time check if the state is in inversePath
	// while (!innerToVisit.isEmpty()) {
	// Node[] cur = innerToVisit.poll();
	// path.append(cur.name);

	// // terminating condition
	// if (cur.name.equals(terminalName)) {
	// System.out.println("COUNTER EXAMPLE DERIVATION: " + path);
	// return;
	// }

	// // append only the production which is in inversePath
	// path.append(" -> ");
	// for (Node[] dstNodes : cur.productions) {
	// if (inversePath.indexOf(production.name) != -1 &&
	// !innerVisited.contains(production)) {
	// innerToVisit.add(production);
	// innerVisited.add(production);
	// }
	// }
	// }
	// }

	// // System.out.println("DSDSD: " + node.name + " appears in " +
	// //
	// Arrays.toString(generatingTableEntry("[q0-a-q1]").node.appearances.toArray()));

	// for (Appearance appearance : node.appearances) {
	// if (!visited.contains(appearance.headNode)) {
	// visited.add(appearance.headNode);
	// toVisit.add(appearance.headNode);
	// }
	// }

	// }

	// }

	// printGeneratingTable();
	// }

	// public void generateExperimental() {
	// System.out.println("THIS GENERATION DOES NOT SEEM TO DO ITS JOB:");
	// // Hashtable<Production, Boolean> visited = new Hashtable<>();
	// HashSet<Node[]> visited = new HashSet<>();
	// Stack<String> variableStack = new Stack<>();
	// StringBuilder toGenerate = new StringBuilder();

	// ArrayList<String> terminals = new ArrayList<>();
	// for (String name : generatingTable.keySet())
	// if (!name.equals("$") && !name.startsWith("[") && !name.equals("eps"))
	// terminals.add(name);

	// variableStack.push(startingVariable);
	// while (variableStack.size() > 0) {
	// String curVariable = variableStack.pop();
	// // Check if terminal
	// if (terminals.contains(curVariable)) // Check if it is a terminal symbol
	// {
	// toGenerate.append(curVariable);
	// // Find a new production that is not visited and push it to the stack
	// according
	// // to the variable order!
	// } else {
	// for (Node[] productionBodies :
	// generatingTableEntry(curVariable).node.productions) {
	// boolean isProductionGenerating = false;
	// for (Node productionBody : productionBodies)
	// if (generatingTableEntry(productionBody.name).status == Status.GENERATING)
	// isProductionGenerating = true;
	// if (isProductionGenerating) {
	// for (Node productionBody : productionBodies)
	// System.out.print(productionBody.name + " ");
	// System.out.print(" -> ");
	// }
	// if (!visited.contains(productionBodies)) {
	// // mark Production as visited
	// visited.add(productionBodies);

	// ArrayList<String> variablesToVisit = new ArrayList<>();
	// for (Node productionBody : productionBodies)
	// variablesToVisit.add(productionBody.name);
	// for (int i = variablesToVisit.size() - 1; i >= 0; i--) {
	// variableStack.push(variablesToVisit.get(i));
	// }
	// }
	// }
	// }
	// }
	// System.out.println(toGenerate.toString());
	// }

	// public boolean isContainsEpsTransition(String variable) {
	// Node node = generatingTableEntry(variable).node;
	// if (node.productions == null || node.productions.size() == 0)
	// return true;
	// for (Node[] productionBody : node.productions) {
	// for (Node productionBodyNode : productionBody)
	// if (productionBodyNode.)
	// }
	// return false;
	// }

	// public void generateExperimentalAdam() {
	// System.out.println("THIS GENERATION DOES NOT SEEM TO DO ITS JOB:");
	// // Hashtable<Production, Boolean> visited = new Hashtable<>();
	// HashSet<Node[]> visited = new HashSet<>();
	// Stack<String> variableStack = new Stack<>();
	// Stack<ArrayList<String>> stepStack = new Stack<>();
	// StringBuilder toGenerate = new StringBuilder();
	// ArrayList<ArrayList<Node>> derivations = new ArrayList<>();

	// ArrayList<String> terminals = new ArrayList<>();
	// for (String name : generatingTable.keySet())
	// if (!name.equals("$") && !name.startsWith("[") && !name.equals("eps"))
	// terminals.add(name);

	// // 1. GENERATING PHASE
	// boolean terminalFound = false;
	// variableStack.push(startingVariable);
	// ArrayList<String> initialStep = new ArrayList<>();
	// initialStep.add(startingVariable);
	// stepStack.add(initialStep);
	// while (true) {
	// ArrayList<String> step = stepStack.pop();
	// System.out.print(step + " -> ");

	// // choose the first generating variable as curVariable
	// String curVariable = "";
	// for (String variable : step)
	// if (generatingTableEntry(variable).status == Status.GENERATING) {
	// curVariable = variable;
	// break;
	// }
	// if (curVariable.equals("")) {
	// System.err.println("No generating vairable in the generating phase");
	// System.exit(1);
	// }

	// // Check if curVariable is a terminal - exit condition
	// if (terminals.contains(curVariable))
	// break;

	// // // if the curVariable is NOT generating, transform it into eps transition
	// // (DFS)
	// // if (generatingTableEntry(curVariable).status == Status.NOT_GENERATING) {
	// // HashSet<String> visitedNG = new HashSet<>(); // visited NOT GENERATING
	// // Stack<String> variableStackNG = new Stack<>();
	// // Stack<ArrayList<String>> stepStackNG = new Stack<>();
	// //
	// // stepStackNG.add(step);
	// // variableStackNG.add(curVariable);
	// // visitedNG.add(curVariable);
	// //
	// // while (true) {
	// //
	// // }
	// // }

	// // GENERATING PHASE algorithm steps:
	// // 1. find productionBody of curVariable which is generating
	// // 2. add the leftMost variable in the productionBody to the stack
	// // 3. Build a string with all the variables in the productionBody
	// // 4. Replace curVariable in the step with the newly built productionBody
	// string
	// // 5. push the new step into stepStack

	// // 1. find productionBody of curVariable which is generating
	// String nextVariable = "";
	// Node[] productionBodyToInsert = new Node[0];
	// System.out.println("curVar" + curVariable);
	// System.out.println("curVarStatus " +
	// generatingTableEntry(curVariable).status);
	// System.out.println("curVarProdLen " +
	// generatingTableEntry(curVariable).node.productions.size());
	// for (Node[] productionBody :
	// generatingTableEntry(curVariable).node.productions) { // productions =
	// // production bodies
	// // exp: S -> SS | A | Ab
	// // here SS, A & Ab are
	// // the production bodies
	// // of S
	// for (Node variable : productionBody) {
	// // System.out.println("var" + variable);
	// System.out.println("SSs " + generatingTableEntry(variable.name).node.name + "
	// status = "
	// + generatingTableEntry(variable.name).status);
	// if (generatingTableEntry(variable.name).status == Status.GENERATING) {
	// System.out.println("FOUND" + variable.name);
	// productionBodyToInsert = productionBody;
	// nextVariable = variable.name;
	// }
	// }
	// if (!nextVariable.equals(""))
	// break;
	// }

	// // 2. add the leftMost generating variable in the productionBody to the stack
	// variableStack.add(nextVariable);

	// // 3. Replace curVariable in the step with the newly built productionBody
	// string
	// int insertIndex = step.indexOf(curVariable);
	// step.remove(insertIndex);
	// for (int i = productionBodyToInsert.length - 1; i >= 0; i--) {
	// System.out.println(productionBodyToInsert[i].name);
	// step.add(insertIndex, productionBodyToInsert[i].name);
	// }

	// // 4. push the new step into stepStack
	// stepStack.add(step);

	// // // 1. find productionBody which is generating
	// //
	// // for (Node variable : productionBody) {
	// // if (generatingTableEntry(variable.name).status == Status.GENERATING)
	// //
	// // }
	// //
	// // if (!visited.contains(productionBodies)) {
	// // // mark Production as visited
	// // visited.add(productionBodies);
	// //
	// // ArrayList<String> variablesToVisit = new ArrayList<>();
	// // for (Node productionBody : productionBodies)
	// // variablesToVisit.add(productionBody.name);
	// // for (int i = variablesToVisit.size() - 1; i >= 0; i--) {
	// // variableStack.push(variablesToVisit.get(i));
	// // }
	// // }
	// // }

	// }

	// // 2. removing non-terminals phase
	// System.out.println(toGenerate.toString());
	// }

	// public class Derivation {
	// ArrayList<ArrayList<String>> steps;
	// ArrayList<String> terminals;
	// boolean containsTerminal;
	// HashMap<String, Integer> visitedProductionBodyIndex;

	// public Derivation(ArrayList<String> terminals) {
	// steps = new ArrayList<>();
	// this.terminals = terminals;
	// containsTerminal = false;
	// visitedProductionBodyIndex = new HashMap<>();
	// }

	// public void addStep(ArrayList<String> step) {
	// steps.add(step);
	// }

	// public void addStep(String productionBody) {
	// ArrayList<String> step = new ArrayList<>();
	// step.add(productionBody);
	// steps.add(step);
	// }

	// public void replaceLeftMostVariable(String[] step) {
	// ArrayList<String> lastStep = steps.get(steps.size() - 1);
	// ArrayList<String> nextStep = (ArrayList<String>) lastStep.clone();

	// for (int i = 0; i < lastStep.size(); i++) {
	// String productionBody = lastStep.get(i);
	// if (!terminals.contains(productionBody))
	// nextStep.add();
	// }
	// }

	// public boolean isContainsTerminal() {
	// return this.containsTerminal;
	// }

	// public String getLeftMostVariable() {
	// String[] lastStep = steps.get(steps.size() - 1);

	// for (String productionBody : lastStep) {
	// if (!terminals.contains(productionBody))
	// return productionBody;
	// }

	// return "NO VARIABLE";
	// }

	// @Override
	// public String toString() {
	// StringBuilder sb = new StringBuilder();
	// for (String[] step : steps) {
	// for (String productionBody : step)
	// sb.append(productionBody).append(" ");
	// sb.append("-> ");
	// }
	// sb.replace(sb.length() - 4, sb.length(), "");
	// return sb.toString();
	// }
	// }

	// public void counter() {
	// // 1. BFS by executing all productionBodies and keeping track of previous
	// derivations until a terminal is created
	// // 2. DFS on each left-most terminal to transform it into epsilon
	// StringBuilder lmd = new StringBuilder(); // left most derivation
	// ArrayList<Node> nodesVisited = new ArrayList<>();
	// Queue<Node> nodesToVisit = new LinkedList<>();
	// ArrayList<Node[]> productionsVisited = new ArrayList<>();
	// ArrayList<ArrayList<String>> derivation = new ArrayList<>();

	// ArrayList<String> terminals = new ArrayList<>();
	// for (String name : generatingTable.keySet())
	// if (!name.equals("$") && !name.startsWith("[") && !name.equals("eps"))
	// terminals.add(name);

	// // Derivation derivation = new Derivation(terminals);
	// ArrayList<String> initialStep = new ArrayList<>();
	// initialStep.add(startingVariable);
	// derivation.add(initialStep);

	// nodesVisited.add(generatingTableEntry(startingVariable).node);
	// nodesToVisit.add(generatingTableEntry(startingVariable).node);

	// while (true) {
	// ArrayList<String> nextSteps =
	// }

	// while (!derivation.getLeftMostVariable().equals("NO VARIABLE")) {
	// String cur = derivation.getLeftMostVariable();
	// String[]

	// if (generatingTableEntry(cur.name).status == Status.NOT_GENERATING)
	// continue;
	// for (Node[] productionBody : cur.productions) {
	// if
	// }
	// }
	// }
	// }
}