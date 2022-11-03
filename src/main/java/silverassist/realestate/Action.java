package silverassist.realestate;

public enum Action {
    ALL(0),//0
    BLOCK(1),//1
    CLICK(2);//2

    private int num;
    Action(int i) {
        this.num = i;
    }
    public int getNum() {
        return this.num;
    }
}
