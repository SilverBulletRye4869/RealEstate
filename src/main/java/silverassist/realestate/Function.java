package silverassist.realestate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Function {
    public static final String PREFIX = "§b§l[§e§lRealEstate§b§l]§r";
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
    private static boolean areaCheck(Location loc, List<Float> s, List<Float> e, String w){
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
        FileConfiguration config = RealEstate.region.getConfig();

        AtomicBoolean allow = new AtomicBoolean(true);
        list.forEach(id -> {
            int perm = config.getInt(id+".user."+uuid);
            if(perm == 0)perm = config.getInt(id+".default");
            String s = new StringBuilder(Integer.toBinaryString(perm)).reverse() + "0000000000";
            if(isAdmin(p,id)||s.charAt(ActionType.ALL.getNum())=='1')return;
            if(s.charAt(type.getNum())=='0') allow.set(false);
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
        if(lore.size()>0)meta.setLore(lore);
        if(model.length>0)meta.setCustomModelData(model[0]);
        item.setItemMeta(meta);
        return item;
    }


}
