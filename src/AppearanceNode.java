class AppearanceNode {
    Production productionAppearedIn;
    int index;

    public AppearanceNode(Production production, int indexInProduction) {
        productionAppearedIn = production;
        index = indexInProduction;
    }

    public AppearanceNode(Production production) {
        productionAppearedIn = production;
        index = -1;
    }
}
