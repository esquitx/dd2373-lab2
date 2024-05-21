class AppearanceNode {
    Production productionAppeardIn;
    int index;

    public AppearanceNode(Production production, int indexInProduction) {
        productionAppeardIn = production;
        index = indexInProduction;
    }

    public AppearanceNode(Production production) {
        productionAppeardIn = production;
        index = -1;
    }
}
