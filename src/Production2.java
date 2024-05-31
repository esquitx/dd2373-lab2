public class Production2 {
    Node headNode;
    Node[] productionBody;
    int count;

    public Production2(Node headNode, Node[] productionBody, int count) {
        this.headNode = headNode;
        this.productionBody = productionBody;
        this.count = count;
    }

    public void decrementCountBy(int delta) {
        count -= delta;
        if (count < 0) {
            System.err.println("COUNT OF NODE DECREMENTED BELOW ZERO");
            System.exit(1);
        }
        if (count == 0)
            this.headNode.status = Status.GENERATING;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(headNode.name).append(" -> ");
        for (Node node : productionBody) {
            sb.append(" ").append(node.name);
        }
        sb.append(", COUNT = ").append(count);
        return  sb.toString();
    }
}