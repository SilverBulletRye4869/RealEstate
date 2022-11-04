package silverassist.realestate;

public enum ActionType {
    ADMIN(0), //With this, anything, except selling the land, can be done! This authority encompasses all of the following.
    ALL(1), //With this, you can do everything except manage the land
    BLOCK(2), //With this, you can set up or destroy blocks!
    CLICK(3), //With this, you can access the chest-type block
    CHEST(4); //With this, you can perform click operations on all blocks except chest-type blocks

    private int num;
    ActionType(int i) {
        this.num = i;
    }
    public int getNum() {
        return this.num;
    }
}
