import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

// Data structure to store the flow graph input
public class FG {

	HashMap<String, Set<NodePair<String>>> edgeTransitions;
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

	public void printFG() {
		System.out.print('\n');
		System.out.println("---> NODE CONFIGURATIONS <---");
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

		System.out.println("---> EDGE CONFIGURATIONS <---");
		for (String key : edgeTransitions.keySet()) {
			System.out.println("METHOD " + key);
			for (NodePair pair : edgeTransitions.get(key)) {
				System.out.println("ORIGIN : " + pair.firstNode + " -> DESTINATION : " + pair.secondNode);
			}
			// whitespace
			System.out.println();
		}
	}
}
