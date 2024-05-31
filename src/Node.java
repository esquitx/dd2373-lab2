import java.util.ArrayList;

public class Node { // {name, [Node1, Node2, ...], [{headNode1, idx1}, {headNode2, idx1}, ...], count}
    String name;
    //		ArrayList<Node[]> productions; // all productions from this node to other nodes
    ArrayList<Production2> productions;
    ArrayList<Production2> appearances; // all appearances of the node
    Status status;

    public Node(String name) {
        this.name = name;
        this.productions = new ArrayList<>();
        this.appearances = new ArrayList<>();
        this.status = Status.NON_DETERMINED;
    }

    // the method addProduction is the crux of this implementation
    public void addProduction(Node[] dstNodes) { // this method adds a production srcNode -> dstNode, adds appearance for dstNode, increments count for srcNode (if necessary)
        Production2 newProduction = new Production2(this, dstNodes, dstNodes.length);
        this.productions.add(newProduction);
        for (Node dstNode : dstNodes) {
            dstNode.appearances.add(newProduction);
        }
    }

    public Production2 addProduction(Node dstNode) { // this method adds a production srcNode -> dstNode, adds appearance for dstNode, increments count for srcNode (if necessary)
        Node[] dstNodes = new Node[]{dstNode};
        Production2 newProduction = new Production2(this, dstNodes, 1);
        this.productions.add(newProduction);
        dstNode.appearances.add(newProduction);
//			if (generatingTable.get(dstNode.name).status == Status.NON_DETERMINED) // in case it is known to be generating, we do not increment
//				count += 1;
        return this.productions.get(this.productions.size() - 1);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(" -> ");
        for (Production2 production : productions) {
            for (Node dstNode : production.productionBody) {
                sb.append(dstNode.name);
            }
            sb.append(" count = ").append(production.count).append(" | "); // concatenation of these
        }
        return sb.toString();
    }
}
