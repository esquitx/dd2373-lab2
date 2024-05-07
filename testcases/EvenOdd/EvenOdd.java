class EvenOdd {
    private int number;

    public void init(int number) {
        this.number = number;
    }

    public boolean even() {
        if (number == 0) {
            return true;
        } else {
            number--;
            return odd();
        }
    }

    public boolean odd() {
        if (number == 0) {
            return false;
        } else {
            number--;
            return even();
        }
    }

    public static void main(String[] args) {
        EvenOdd eo = new EvenOdd();
        for (int i=0; i < args.length; i++) {
            eo.init(Integer.parseInt(args[i]));
            if (eo.even()) {
                System.out.println(args[i] + " is even!"); 
            } else {
                System.out.println(args[i] + " is odd!"); 
            }
        }
    }
}
