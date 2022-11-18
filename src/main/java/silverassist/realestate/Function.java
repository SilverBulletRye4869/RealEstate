package silverassist.realestate;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Function {
    public static final String PREFIX = "§b§l[§e§lRealEstate§b§l]";  //prefix
    public static String ADMIN_WAND = "§d§l領域指定斧";  //領域指定斧のName

    //---------------------------------------------------------------------------------------------- 保護土地系関数
    /*****************************************************************
     * 指定した座標が保護の範囲内にあるか判定する
     * @param loc : 調査対象の座標
     * @param s : 保護の各最小座標
     * @param e : 保護の各最大座標
     * @param w : ワールド名
     * @return : 範囲内にあればtrue, そうでなければfalse
     */
    public static boolean areaCheck(Location loc, List<Float> s, List<Float> e, String w){
        if(!loc.getWorld().getName().equals(w))return false;  //ワールドが異なればfalseを返す
        if(loc.getX() < s.get(0) || loc.getX()>e.get(0))return false;  //xが範囲外ならfalseを返す
        if(loc.getZ() < s.get(2) || loc.getZ()>e.get(2))return false;  //yが範囲外ならfalseを返す
        if(loc.getY() < s.get(1) || loc.getY()>e.get(1))return false;  //が範囲外ならfalseを返す
        return true; //全部範囲内ならtrueを返す
    }


    /****************************************************
     * 指定した座標が属している保護の一覧を取得
     * @param loc : 調査対象の座標
     * @return : 属している保護の一覧
     */
    private static List<String> getGuardList(Location loc){
        FileConfiguration region = RealEstate.getRegionYml().getConfig();  //土地のデータ
        FileConfiguration memo = RealEstate.getMemoYml().getConfig();  //memoデータ
        List<String> guardList = new LinkedList<>();  //その位置に属している土地のリストを格納しておく奴

        //オーダを一番気おつけないといけないところ!
        List<String> x = memo.getStringList("x"+Math.round(loc.getX()/100));  //x座標とmemoから可能性のある土地一覧を取得
        memo.getStringList("z"+Math.round(loc.getZ()/100)).forEach(path ->{  //z座標とmemoから可能性のある土地一覧を取得してそれぞれに対して実行
            if(!x.contains(path))return;  //xのリストと照合してなければ終了
            if(!areaCheck(loc, region.getFloatList(path+".start"),region.getFloatList(path+".end"),region.getString(path+".world")))return;  //可能性があれば詳細探索
            guardList.add(path);  //属していればリストに加える
        });
        return guardList;
    }


    /******************************************************
     * プレイヤーと座標からその行動を実施する権限があるか
     * @param p : 対象プレイヤー
     * @param loc : 対象の場所
     * @param type : 実施行為
     * @return : 権限があればtrue, 無ければfalse
     */
    public static boolean hasPermission(Player p, Location loc, ActionType type){
        if(p.isOp())return true;  //opならかならずtrue
        List<String> list = getGuardList(loc);  //位置から属する保護のリストを取得

        if(list.size()==0){//ない場合はワールドの設定を見るようにする
            FileConfiguration world = RealEstate.getWorldYml().getConfig();  //worldYmlを読み込み
            int permNum = world.getInt(loc.getWorld().getUID()+".permission");  //そのワールドのパーミッション値を取得
            String perm = new StringBuilder(Integer.toBinaryString(permNum)).reverse() + "0000000000";  //2進数に分解
            return perm.charAt(ActionType.ALL.getNum())=='1';  //分解したものの権限に対応している場所を見て判断(1:権限あり, 0:権限なし)
        }

        UUID uuid = p.getUniqueId();  //uuid取得
        FileConfiguration region = RealEstate.getRegionYml().getConfig();  //土地のyml取得

        AtomicBoolean allow = new AtomicBoolean(true);  //デフォルトをtrueとして処理
        //属するすべての保護に対して実施
        list.forEach(id -> {
            String status = region.getString(id+".status");  //その土地のステータスを取得
            if(status.equals("free"))return; //フリーな土地ならスルー
            if(status.equals("frozen"))allow.set(false); //凍結された土地ならfalseにする

            int permNum = region.getInt(id+".user."+uuid);  //idとuuidからそのプレイヤーのパーミッション値を取得
            if(permNum == 0)permNum = region.getInt(id+".default");  //プレイヤーが見つからない　あるいは　全拒否　ならその土地のデフォルトの権限を取得
            String perm = new StringBuilder(Integer.toBinaryString(permNum)).reverse() + "0000000000";  //パーミッション値を2進数に分解
            if(isAdmin(p,id)||perm.charAt(ActionType.ALL.getNum())=='1')return; //2進数の対応する場所を見て、管理権限orフル活動権限があればスルー
            if(perm.charAt(type.getNum())=='0') allow.set(false); //各行動の権限がなければfalseにする
        });

        //属するすべての土地で許可されている場合にのみtrueを返す
        return allow.get();
    }


    /***************************************************
     * そのプレイヤーがオーナーかどうか判定(OPの場合もtrue)
     * @param p : 対象プレイヤー
     * @param id : 土地のid
     * @return : Ownerであればtrue, そうでないならfalse
     */
    public static boolean isOwner(OfflinePlayer p, String id){
        if(p.isOp())return true;  //opならtrue
        String owner = RealEstate.getRegionYml().getConfig().getString(id+".owner");  //ownerのuuidを取得
        if(owner.equals("admin"))return false;  //ownerが運営ならfalseを返す
        return RealEstate.getRegionYml().getConfig().get(id+".owner").equals(p.getUniqueId().toString());  //対象プレイヤーのuuidとownerのuuidを比較
    }


    /*********************************************************
     * そのプレイヤーがその土地のAdminか(owner, opの場合もtrue)
     * @param p : 対象プレイヤー
     * @param id : 土地のid
     * @return : Adminであればtrue, そうでなければfalse
     */
    public static boolean isAdmin(OfflinePlayer p, String id){
        int permNum = RealEstate.getRegionYml().getConfig().getInt(id+".user."+p.getUniqueId());  //対象プレイヤーのパーミッション値を取得
        return permNum%2==1||isOwner(p,id);  //パーミッション値から管理権限があるか判断する　（owner判定の場合もtrueを返す）
    }

    //---------------------------------------------------------------------------------------------------------土地関数
    /*****************************
     * 本当に土地を買うかの確認msg(コマンド自動実行を添えて)
     * @param p : 対象プレイヤー
     * @param id : 土地のid
     */
        public static void sendBuyCheckMessage(Player p,String id){
        FileConfiguration region = RealEstate.getRegionYml().getConfig();
        FileConfiguration config = RealEstate.getInstance().getConfig();

        //================================================================メッセージ送信
        sendPrefixMessage(p,"§6§l---------- 最終確認 ----------");
        sendPrefixMessage(p,"§c§l購入予定土地 §d§lid:" + id);
        sendPrefixMessage(p,"§c§l値段: §d§l" + region.get(id+".price") + config.get("money_unit"));
        //Ownerの情報エリア
        OfflinePlayer owner = getOwner(id);
        if(owner == null)sendPrefixMessage(p,"§c§l所有者: §d§l運営");
        else{
            String name = owner.getName();
            sendPrefixMessage(p, "§c§l所有者: §d§l"+name);
        }
        sendPrefixMessage(p,"§6§l-------------------------------");


        sendRunCommandMessage(p,"§c§l[本当に購入する場合はここをクリック！]","/re buy "+id +" confirm");  //ﾗﾝコマンドメッセージ送信

    }

    /******************************************
     * 土地を凍結させる関数
     * @param id : 対象の土地
     */
    public static void frozenRegion(String id){
        FileConfiguration region = RealEstate.getRegionYml().getConfig();  //土地のyml取得
        region.set(id+".status","frozen");
        RealEstate.getRegionYml().saveConfig();
    }

    //--------------------------------------------------------------------------------------------------看板関数
    public static final Map<String,String> REGION_STATUS = Map.of("sale","販売中","protect","保護中","free","保護なし","frozen","§c§l凍結中");

    /******************************************************
     * 看板に土地の情報を書き込む
     * @param sign : 対象看板のステータス
     * @param id : 土地のid
     */
    public static void setRegionSign(Sign sign, String id){
        sign.setLine(0,PREFIX);
        sign.setLine(1, "§d§lid: "+id);
        FileConfiguration region = RealEstate.getRegionYml().getConfig();  //土地のyml取得
        //Ownerエリア
        OfflinePlayer owner = getOwner(id);
        if(owner==null)sign.setLine(2,"§a§l運営");
        else sign.setLine(2,"§a§l"+owner.getName());
        String status = region.getString(id+".status");
        if(REGION_STATUS.containsKey(status))sign.setLine(3,"§6§l"+REGION_STATUS.get(status));  //土地の状態を変換し記入

        //遅延がないと、看板設置時に反映されない時アリ
        Bukkit.getScheduler().runTaskLater(RealEstate.getInstance(), new Runnable() {
            @Override
            public void run() {
                sign.update(true);
            }
        },1);
    }

    /**************************************
     * 指定したidの土地看板を全編集
     * @param id : 対象の土地
     */
    public static void editRegionSign(String id){
        FileConfiguration region = RealEstate.getRegionYml().getConfig();
        if(region.get(id)==null)return;  //土地が見つからなければfalse
        List<Location> signLocationList = (List<Location>) region.getList(id+".sign ");  //看板の位置リストを取得

        //看板の位置リストをコピーして全実行
        new ArrayList<>(signLocationList).forEach(signLocation -> {
                Block block = signLocation.getBlock();
                if(block !=null){  //blockがあるか
                    BlockState state = block.getState();
                    if(state instanceof Sign){  //blockが看板であるか
                        Sign sign = (Sign) state;
                        if(sign.getLine(0).equals(PREFIX)){  //土地看板であるか？
                            setRegionSign(sign, id);  //土地看板なら編集関数に飛ばす
                            return;
                        }
                    }
                }
                signLocationList.remove(signLocation);  //土地看板じゃなければ土地看板リストから削除
        });
        region.set(id+".sign",signLocationList);  //土地看板リストを更新
        RealEstate.getRegionYml().saveConfig();
    }


    //-----------------------------------------------------------------一般関数
    //Prefix付きのメッセージに変更
    public static void sendPrefixMessage(Player p, String text){
        p.sendMessage(PREFIX+text);
    }

    //全体メッセージ
    public static void broadCast(String msg){
        RealEstate.getInstance().getServer().broadcastMessage(PREFIX+msg);
    }

    //整数かどうか判定
    public static boolean isInt(String s){
        return s.matches("-?\\d+");
    }

    //アイテムを作成
    public static ItemStack createItem(Material m,String name,List<String> lore,int... model){
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        if(name!=null)meta.setDisplayName(name);
        if(lore!=null&&lore.size()>0)meta.setLore(lore);
        if(model.length>0)meta.setCustomModelData(model[0]);
        item.setItemMeta(meta);
        return item;
    }

    //サジェストメッセージ送信
    public static void sendSuggestMessage(Player p, String text, String command){
        TextComponent msg = new TextComponent(PREFIX + text);
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,command));
        p.spigot().sendMessage(msg);
    }

    //ﾗﾝコマンドメッセージを送信
    public static void sendRunCommandMessage(Player p, String text, String command){
        TextComponent msg = new TextComponent(PREFIX + text);
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        p.spigot().sendMessage(msg);
    }

    //オーナー取得
    public static OfflinePlayer getOwner(String id){
        FileConfiguration region =  RealEstate.getRegionYml().getConfig();
        if(region.get(id)==null)return null;
        String owner = region.getString(id+".owner");
        if(owner.equals("admin"))return null;
        return getPlayer(owner);
    }

    //プレイヤー取得byString
    public static OfflinePlayer getPlayer(String uuidStr){
        return Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
    }



}
