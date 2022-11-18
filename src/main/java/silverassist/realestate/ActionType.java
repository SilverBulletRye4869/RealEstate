package silverassist.realestate;

public enum ActionType {
    ADMIN(0),  //土地の状態変更と価格変更、管理人任命、オーナー任命を除く全行為が可能。
    ALL(1),  //土地の管理行為を除くすべての行為が可能(主にバニラでできること)
    BLOCK(2),  //ブロックの設置・破壊が可能
    CLICK(3),  //チェスト系ブロック以外に対しての右クリックが可能
    CHEST(4),  //チェスト系ブロックへのアクセスが可能
    PVP(5),  //プレイヤーに対してダメージを与えられる
    PICK_UP(6);  //アイテムの拾い上げが可能

    //-----------------アクションに対応した数値を取得できるように
    private int num;
    ActionType(int i) {
        this.num = i;
    }
    public int getNum() {
        return this.num;
    }
}
