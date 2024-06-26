import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

// Data structure to store the flow graph input
public class FG {

	HashMap<String, Set<NodePair<String>>> edgeTransitions = new HashMap<>();
	HashMap<String, HashMap<String, Set<String>>> methodsToNodes;

	public FG() {
		// Create HashMaps linking transitions to actions and nodes to methods
		edgeTransitions = new HashMap<>();
		methodsToNodes = new HashMap<>();
	}

	public void addNodePair(String methodName, String firstNode, String secondNode) {
		NodePair<String> pair = new NodePair<>(firstNode, secondNode);

		if (!edgeTransitions.containsKey(methodName)) {
			edgeTransitions.put(methodName, new HashSet<>());
		}
		edgeTransitions.get(methodName).add(pair);
	}

	public void addNodeType(String methodName, String node, String type) {

		// if not included, add
		if (!methodsToNodes.containsKey(methodName)) {
			methodsToNodes.put(methodName, new HashMap<>());
		}

		if (!methodsToNodes.get(methodName).containsKey(type)) {
			methodsToNodes.get(methodName).put(type, new HashSet<>());
		}

		methodsToNodes.get(methodName).get(type).add(node);
	}

	public Set<String> getNodes(String methodName, String type) {

		// DEBUGGING
		// Get type
		// System.out.println( methodName);
		// System.out.println( type);
		// printFG();

		if (!methodsToNodes.containsKey(methodName)) {
			return new HashSet<String>();
		}

		Set<String> nodes = methodsToNodes.get(methodName).get(type);

		return nodes;
	}

	public Set<String> getNodesOfType(String type) {
		Set<String> nodes = new HashSet<>();
		for (String method : methodsToNodes.keySet()) {
			nodes.addAll(methodsToNodes.get(method).get(type));
		}
		return nodes;
	}

	public String getEntryOfMain() {
		Set<String> methods = methodsToNodes.keySet();
		String mainMethodFullName = "";
		mainMethodFullName = methods.stream()
				.filter(method -> method.contains("main"))
				.collect(Collectors.toSet())
				.iterator()
				.next();
		return methodsToNodes.get(mainMethodFullName).get(NodeType.ENTRY).iterator().next();
	}

	public Set<NodePair<String>> getMethodTransitions(String name) {
		Set<NodePair<String>> nodePairs = edgeTransitions.get(name);

		// DEBUGGING
		// for ( NodePair p : nodePairs)
		// {
		// System.out.println( "From " + p.firstNode + " -> to -> " + p.secondNode);
		// }
		//

		return nodePairs;
	}

	public Set<String> getAllNodesFromMethod(String method) {
		Set<String> nodes = new HashSet<>();
		for (String nodeType : methodsToNodes.get(method).keySet()) {
			nodes.addAll(methodsToNodes.get(method).get(nodeType));
		}
		return nodes;
	}

	public void printFG() {
		System.out.print('\n');
		System.out.println("---> NODE CONFIGURATIONS <---\n");
		for (String key : methodsToNodes.keySet()) {
			System.out.println("METHOD : " + key);
			for (String type : methodsToNodes.get(key).keySet()) {
				System.out.println("NODE TYPE : " + type);
				for (String node : methodsToNodes.get(key).get(type)) {
					System.out.println("CONTENT : " + node);
				}
			}
			System.out.println();
		}

		System.out.println("---> EDGE CONFIGURATIONS <---\n");

		for (String key : edgeTransitions.keySet()) {
			System.out.println("METHOD " + key);
			for (NodePair pair : edgeTransitions.get(key)) {
				System.out.println("TRANSITION : " + pair.firstNode + " -> " + pair.secondNode);
			}
			// whitespace
			System.out.println();
		}
	}

	public void printFG2() {
		// iterate through the methods and generate a .gv file for each
		for (String method : methodsToNodes.keySet()) {
			System.out.println("METHOD: " + method);
			System.out.println("digraph finite_state_machine {");
			System.out.println("\trankdir=LR");
			System.out.println("\tsize=\"100,100\"");
			System.out.println("\tnode [shape = point]; point_q0");
			System.out.print("\tnode [shape = doublecircle]; ");
			for (String node : methodsToNodes.get(method).get(NodeType.RET))
				System.out.print(node + "; ");
			System.out.println();
			System.out.println("\tnode [shape=circle];");
			System.out.println("\tpoint_q0 -> " + methodsToNodes.get(method).get(NodeType.ENTRY).iterator().next() + ";");
			Set<String> allNodes = getAllNodesFromMethod(method);
//			System.out.println("Method is "+ method + " nodes: " + allNodes);
			for (String transitionMethod : edgeTransitions.keySet()) {
				for (NodePair edge : getMethodTransitions(transitionMethod)) {
					if (allNodes.contains(edge.firstNode) && allNodes.contains(edge.secondNode)) {
						System.out.println("\t" + edge.firstNode + " -> " + edge.secondNode + " [ label = \"" + transitionMethod + "\" ];");
					}
				}
			}
			System.out.println("}");
			System.out.println();
		}
	}

	public void ss() {
		Iterator<String> it = methodsToNodes.keySet().iterator();
		String firstMethod = methodsToNodes.keySet().iterator().next();
	}
	// public void printFG_ADAM() {
	//
	// // Iterate over each method and print a graph for each method separately
	// for (String method : methodsToNodes.keySet()) {
	// String acc = "";
	// for (String type : methodsToNodes.get(method).keySet())
	// if (type.equals(NodeType.RET))
	// methodsToNodes.get(method).get(NodeType.RET)
	// acc += stateStringRep(q) + " ";
	//
	// System.out.println("digraph finite_state_machine {");
	// System.out.println(" rankdir=LR;");
	// System.out.println(" size=\"10,10\";");
	// System.out.println(" node [shape = box]; " + stateStringRep(initial) + ";");
	// // TODO - set to valid shape
	// System.out.println(" node [shape = doublecircle]; " + acc + ";");
	// System.out.println(" node [shape = circle];");
	//
	// for (State src : trans.keySet()) {
	// for (State dst : trans.get(src).keySet()) {
	// Set<Sym> syms = trans.get(src).get(dst);
	// System.out.println(
	// stateStringRep(src) + " -> " +
	// stateStringRep(dst) +
	// " [ label = \"" + symsStringRep(syms) + "\" ];");
	// }
	// }
	// System.out.println("}");
	// }
	// }
}
