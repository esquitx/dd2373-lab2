
import java.sql.SQLOutput;
import java.util.*;

public class CFG2 {
	private FG fg = null;
	private DFA dfa = null;

	// Properties
	String startingVariable = "$";
	HashMap<String, Set<Production>> productTable;
	HashMap<String, GeneratingTableEntry> generatingTable; // name -> {Node, Status}

	enum Status {GENERATING, NOT_GENERATING, NON_DETERMINED}

	private class GeneratingTableEntry { // {Node, Status}
		Node node;
		Status status;

		public GeneratingTableEntry(Node node, Status status) {
			this.node = node;
			this.status = status;
		}
	}

	public void printAppearances() {
		System.out.println("PRINTING APPEARANCES:");
		for (String name : generatingTable.keySet()) {
			Node node = generatingTableEntry(name).node;
			System.out.print(node.name + " appears in: ");
			for (Appearance appearance : node.appearances)
				System.out.print(appearance.toString() + "; ");
			System.out.println();
		}
		System.out.println();
	}

	public GeneratingTableEntry generatingTableEntry(String name) { // returns an entry or creates one if non-existent
		if (!generatingTable.containsKey(name))
			generatingTable.put(name, new GeneratingTableEntry(new Node(name), Status.NON_DETERMINED));
		return generatingTable.get(name);
	}

	public GeneratingTableEntry generatingTableEntry(String name, Status status) { // returns an entry or creates one if non-existent
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
	private class Node { // {name, [Node1, Node2, ...], [{headNode1, idx1}, {headNode2, idx1}, ...], count}
		String name;
		ArrayList<Node> productions; // all productions from this node to other nodes
		ArrayList<Appearance> appearances; // all appearances of the node
		int count;


		public Node(String name) {
			this.name = name;
			this.productions = new ArrayList<>();
			this.appearances = new ArrayList<>();
			this.count = 0;
		}

		// the method addProduction is the crux of this implementation
		public void addProduction(Node dstNode) { // this method adds a production srcNode -> dstNode, adds appearance for dstNode, increments count for srcNode (if necessary)
			this.productions.add(dstNode);
			dstNode.appearances.add(new Appearance(this, productions.size() - 1));
//			if (generatingTable.get(dstNode.name).status == Status.NON_DETERMINED) // in case it is known to be generating, we do not increment
//				count += 1;
			count += 1;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(name);
			sb.append(" -> [");
			for (Node dstNode : productions)
				sb.append(dstNode.name);
			sb.append("], count = ");
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
		productTable = new HashMap<>();
		generatingTable = new HashMap<>();

		// print dfa spec and flow graphs
		dfa.printGV();
		fg.printFG2();
		
		computeProduct();

		// perform emptiness test and print the verdict
		Status startingSymbolStatus = emptinessTest();
		System.out.println("The starting symbol $ is " + startingSymbolStatus);
		if (startingSymbolStatus == Status.GENERATING)
			System.out.println("Specification is therefore VIOLATED");
		else
			System.out.println("Specification is therefore ACCEPTED");
		System.out.println();

		// if spec is violated, print a counter example generating derivation
		if (generatingTableEntry(startingVariable).status == Status.GENERATING)
			counterExample();
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
					System.out.println("ASDASODHOIA");
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

						String srcName = "[" + String.join("-", stateSeq[0], src, stateSeq[3]) + "]";
						Node srcNode = generatingTableEntry(srcName).node;

						String[] dstNames = new String[3];
						String[] dstNodes = new String[3];

						dstNames[0] = "[" + String.join("-", stateSeq[0], method, stateSeq[1]) + "]";
						dstNames[1] = "[" + String.join("-", stateSeq[1], entry, stateSeq[2]) + "]";
						dstNames[2] = "[" + String.join("-", stateSeq[2], dst, stateSeq[3]) + "]";

						for (String dstName : dstNames) {
							Node dstNode = generatingTableEntry(dstName).node;
							srcNode.addProduction(dstNode);
						}
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

	public Status emptinessTest() {
//		printGeneratingTable();
//		printAppearances();
		Queue<Node> toVisit = new LinkedList<>();
		Set<Node> visited = new HashSet<>();

		// add terminal symbols to the queue
		for (String nodeName : generatingTable.keySet())
			if (generatingTableEntry(nodeName).status == Status.GENERATING)
				toVisit.add(generatingTableEntry(nodeName).node);
//		for (String nodeName : generatingTable.keySet())
//			if (generatingTableEntry(nodeName).status == Status.NON_DETERMINED && generatingTableEntry(nodeName).node.count == 0) {
//				generatingTableEntry(nodeName, Status.NOT_GENERATING);
//				toVisit.add(generatingTableEntry(nodeName).node);
//			}

		// pop from the queue, decrement counter, update status (if generating) and append appearances to the queue
		while (!toVisit.isEmpty()) {
			Node currentNode = toVisit.remove();
			visited.add(currentNode);
//			System.out.println("JUST POPPED: " + currentNode.toString());
			for (Appearance appearance : currentNode.appearances) {
				if (generatingTableEntry(appearance.headNode.name).status == Status.NON_DETERMINED)
					appearance.headNode.count -= 1;
				if (appearance.headNode.count == 0)
					generatingTableEntry(appearance.headNode.name, Status.GENERATING);
				if (!visited.contains(appearance.headNode)) {
					toVisit.add(appearance.headNode);
				}
			}
		}
		for (String name : generatingTable.keySet())
			if (generatingTableEntry(name).status == Status.NON_DETERMINED)
				generatingTableEntry(name, Status.NOT_GENERATING);
		printGeneratingTable();
		return generatingTableEntry(startingVariable).status;
	}

	public void printGeneratingTable() {
		System.out.println("GENERATING TABLE:");
		for (String name : generatingTable.keySet()) {
			System.out.println(generatingTableEntry(name).status + " " + generatingTableEntry(name).node.toString());
		}
		System.out.println();
	}

	public void counterExample() {
		// find all terminal symbols (the ones we want to generate)
		ArrayList<String> terminals = new ArrayList<>();
		for (String name : generatingTable.keySet())
			if (!name.equals("$") && !name.startsWith("[") && !name.equals("eps"))
				terminals.add(name);

//		System.out.println("TERMINALS: " + Arrays.toString(terminals.toArray()));

		// go over each terminal symbol and propagate backwards until starting symbol $ is reached
		// build the path with StringBuilder along the way
		// once the starting symbol is reached, it means a counter example is found
		// lastly print the counterExample

		for (String terminalName : terminals) {
			// setup
			StringBuilder inversePath = new StringBuilder();
			Set<Node> visited = new HashSet<>();
			Queue<Node> toVisit = new LinkedList<>();

			// get the node
			Node terminalNode = generatingTableEntry(terminalName).node;
			toVisit.add(terminalNode);
			visited.add(terminalNode);

			// iterate through its appearances BFS
			while (!toVisit.isEmpty()) {
				Node node = toVisit.poll();
//				System.out.println(node.name);
				inversePath.append(" <- ").append(node.name);
//				System.out.println(sb);

				// inverse path found - now work from start and only keep the visited ones
				if (node.name.equals(startingVariable)) {

					// inner setup
					StringBuilder path = new StringBuilder();
					Set<Node> innerVisited = new HashSet<>();
					Queue<Node> innerToVisit = new LinkedList<>();
					innerVisited.add(generatingTableEntry(startingVariable).node);
					innerToVisit.add(generatingTableEntry(startingVariable).node);

					// go forward, each time check if the state is in inversePath
					while (!innerToVisit.isEmpty()) {
						Node cur = innerToVisit.poll();
						path.append(cur.name);

						// terminating condition
						if (cur.name.equals(terminalName)) {
							System.out.println("COUNTER EXAMPLE DERIVATION: " + path);
							return;
						}

						// append only the production which is in inversePath
						path.append(" -> ");
						for (Node production : cur.productions) {
							if (inversePath.indexOf(production.name) != -1 && !innerVisited.contains(production)) {
								innerToVisit.add(production);
								innerVisited.add(production);
							}
						}
					}
				}

//				System.out.println("DSDSD: " + node.name + " appears in " + Arrays.toString(generatingTableEntry("[q0-a-q1]").node.appearances.toArray()));

				for (Appearance appearance : node.appearances) {
					if (!visited.contains(appearance.headNode)) {
						visited.add(appearance.headNode);
						toVisit.add(appearance.headNode);
					}
				}

			}

		}

//		printGeneratingTable();
	}
}
