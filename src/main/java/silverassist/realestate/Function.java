package silverassist.realestate;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Function {
    public static final String PREFIX = "§b§l[§e§lRealEstate§b§l]";
    public static String ADMIN_WAND = "§d§l領域指定斧";

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
        if(!loc.getWorld().getName().equals(w))return false;
        if(loc.getX() < s.get(0) || loc.getX()>e.get(0))return false;
        if(loc.getZ() < s.get(2) || loc.getZ()>e.get(2))return false;
        if(loc.getY() < s.get(1) || loc.getY()>e.get(1))return false;
        return true;
    }


    /****************************************************
     * 指定した座標が属している保護の一覧を取得
     * @param loc : 調査対象の座標
     * @return : 属している保護の一覧
     */
    private static List<String> getGuardList(Location loc){
        FileConfiguration region = RealEstate.region.getConfig();
        FileConfiguration memo = RealEstate.memo.getConfig();
        List<String> guardList = new LinkedList<>();

        //オーダを一番気おつけないといけないところ!
        List<String> x = memo.getStringList("x"+Math.round(loc.getX()/100));
        memo.getStringList("z"+Math.round(loc.getZ()/100)).forEach(path ->{
            if(!x.contains(path))return;
            if(!areaCheck(loc, region.getFloatList(path+".start"),region.getFloatList(path+".end"),region.getString(path+".world")))return;
            guardList.add(path);
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
        List<String> list = getGuardList(loc);
        if(list.size()==0){
            //ない場合はワールドの設定を見るようにする(近日改良)
        }
        UUID uuid = p.getUniqueId();
        FileConfiguration region = RealEstate.region.getConfig();

        AtomicBoolean allow = new AtomicBoolean(true);
        list.forEach(id -> {
            String status = region.getString(id+".status");
            if(status.equals("free"))return; //フリーな土地ならスルー
            if(status.equals("frozen") && !p.isOp())allow.set(false); //凍結された土地ならop以外falseにする

            int permNum = region.getInt(id+".user."+uuid);
            if(permNum == 0)permNum = region.getInt(id+".default");
            String perm = new StringBuilder(Integer.toBinaryString(permNum)).reverse() + "0000000000";
            if(isAdmin(p,id)||perm.charAt(ActionType.ALL.getNum())=='1')return; //管理権限orフル活動権限があればスルー
            if(perm.charAt(type.getNum())=='0') allow.set(false); //各行動の権限がなければfalseにする
        });
        return allow.get();
    }


    /***************************************************
     * そのプレイヤーがオーナーかどうか判定(OPの場合もtrue)
     * @param p : 対象プレイヤー
     * @param id : 土地のid
     * @return : Ownerであればtrue, そうでないならfalse
     */
    public static boolean isOwner(OfflinePlayer p, String id){
        if(p.isOp())return true;
        String owner = RealEstate.region.getConfig().getString(id+".owner");
        if(owner.equals("admin"))return false;
        return RealEstate.region.getConfig().get(id+".owner").equals(p.getUniqueId().toString());
    }


    /*********************************************************
     * そのプレイヤーがその土地のAdminか(owner, opの場合もtrue)
     * @param p : 対象プレイヤー
     * @param id : 土地のid
     * @return : Adminであればtrue, そうでなければfalse
     */
    public static boolean isAdmin(OfflinePlayer p, String id){
        String s = new StringBuilder(Integer.toBinaryString(RealEstate.region.getConfig().getInt(id+".user."+p.getUniqueId()))).reverse() + "0000000000";
        return s.charAt(ActionType.ADMIN.getNum())=='1'||isOwner(p,id);
    }

    //-----------------------------------------------------------------土地関数
    /*****************************
     * 本当に土地を買うかの確認msg(コマンド自動実行を添えて)
     * @param p : 対象プレイヤー
     * @param id : 土地のid
     * @param lastCheck : 最終確認か否か
     */
    public static void sendBuyCheckMessage(Player p,String id,boolean lastCheck){
        FileConfiguration region = RealEstate.region.getConfig();
        FileConfiguration config = RealEstate.plugin.getConfig();
        String color = lastCheck ? "§c§l" : "§f§l";

        sendPrefixMessage(p,"§6§l---------- " + (lastCheck?"最終":"購入") + "確認 ----------");
        sendPrefixMessage(p,color + "購入予定土地 §d§lid:" + id);
        sendPrefixMessage(p,color + "値段: §d§l" + region.get(id+".price") + config.get("money_unit"));
        String owner = region.getString(id+".owner");
        if(owner.equals("admin"))sendPrefixMessage(p,color + "所有者: §d§l運営");
        else{
            String name = Bukkit.getOfflinePlayer(owner).getName();
            sendPrefixMessage(p, color + "所有者: §d§l"+name);
        }
        sendPrefixMessage(p,"§6§l-------------------------------");

        sendRunCommandMessage(p,"§c§l[本当に購入する場合はここをクリック！]","/re buy "+id + (lastCheck ? "　confirm" : ""));

    }

    /******************************************
     * 土地を凍結させる関数
     * @param id : 対象の土地
     */
    public static void frozenRegion(String id){
        FileConfiguration region = RealEstate.region.getConfig();
        region.set(id+".status","frozen");
        RealEstate.region.saveConfig();
    }

    //------------------------------------------------------------------看板関数
    public static final Map<String,String> REGION_STATUS = Map.of("sale","販売中","protect","保護中","free","保護なし","frozen","§c§l凍結中");
    public static void setRegionSign(Sign sign, String id){
        sign.setLine(0,PREFIX);
        sign.setLine(1, "§d§lid: "+id);
        FileConfiguration region = RealEstate.region.getConfig();

        OfflinePlayer owner = getOwner(id);
        if(owner==null)sign.setLine(2,"§a§l運営");
        else sign.setLine(2,"§a§l"+owner.getName());
        String status = region.getString(id+".status");
        if(REGION_STATUS.containsKey(status))sign.setLine(3,"§6§l"+REGION_STATUS.get(status));

        Bukkit.getScheduler().runTaskLater(RealEstate.plugin, new Runnable() {
            @Override
            public void run() {
                sign.update(true);
            }
        },1);
    }


    //-----------------------------------------------------------------一般関数
    //Prefix付きのメッセージに変更
    public static void sendPrefixMessage(Player p, String text){
        p.sendMessage(PREFIX+text);
    }

    //全体メッセージ
    public static void broadCast(String msg){
        RealEstate.plugin.getServer().broadcastMessage(PREFIX+msg);
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
        FileConfiguration region =  RealEstate.region.getConfig();
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
