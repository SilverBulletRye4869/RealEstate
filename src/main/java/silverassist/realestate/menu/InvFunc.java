package silverassist.realestate.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import silverassist.realestate.ActionType;
import silverassist.realestate.RealEstate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static silverassist.realestate.Function.createItem;

public class InvFunc {
    //現在開いてるRealEstate系Invを格納しておく奴
    static Map<Player,String> curInv = new HashMap<>();
    //ワールド保護のtrue, falseアイテム
    static final ItemStack ENABLED_TRUE_ITEM = createItem(Material.GREEN_CONCRETE, "§a保護有効化中",List.of("§fこのワールドに属する","§f全ての保護が§6有効化§fさ","§fれています"));
    static final ItemStack ENABLED_FALSE_ITEM = createItem(Material.RED_CONCRETE, "§c保護無効化中",List.of("§fこのワールドに属する","§f全ての保護が§6無効化§fさ","§fれています"));

    //Invの種類を分割するやつ
    static List<String> splitType(String type){
        return List.of(type.split("\\."));
    }




    /*****************************************************
     * クリックしたスロットを元に、権限を書き換えるメソッド
     * @param e : InventoryClickEvent
     * @param ymlFile : 書き換えるymlファイル
     * @param path : ymlのパス
     */
    static final int START_PERM_PLACE = 10; //権限設定用板ガラスが一番初めに登場する位置
    static void SetPermission(InventoryClickEvent e, FileConfiguration ymlFile, String path){
        int slot = e.getSlot();
        if(slot<START_PERM_PLACE || slot>START_PERM_PLACE+ActionType.values().length-1)return;  //関係ないとこをクリックしたときは終了
        int permPlace = slot - START_PERM_PLACE;  //権限ナンバー取得
        int permNum = ymlFile.getInt(path);
        //書き換え
        if(e.getCurrentItem().getType()== Material.LIME_STAINED_GLASS_PANE){
            permNum -= Math.pow(2,permPlace);
            e.getInventory().setItem(slot,createItem(Material.RED_STAINED_GLASS_PANE,"§c§l拒否",null));
        }else{
            permNum += Math.pow(2,permPlace);
            e.getInventory().setItem(slot,createItem(Material.LIME_STAINED_GLASS_PANE,"§a§l許可",null));
        }
        //ymlにセット
        ymlFile.set(path,permNum);
        return;
    }

    /****************************************************
     * 基礎のGUI作るメソッド
     * @param p : プレイヤー
     * @param row : 段数
     * @param invName : インベントリの名前
     * @return : 作ったインベントリ
     */
    static Inventory createDefaultGui(Player p,int row, String invName){
        Inventory inv = Bukkit.createInventory(p, row*9, invName);
        for(int i = 0;i<row;i++)setItemPlus(inv,createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§r",null), new int[]{0+9*i, 1+9*i, 7+9*i, 8+9*i});
        return inv;
    }


    /**********************************************************
     * 権限系のデフォルトGUIセット
     * @param inv : セットするInventory
     * @param id : 土地のid
     * @param targetUUID : 権限変更の対象(nullの場合はデフォルト権限)
     */
    static void setDefaultPermissionGui(Inventory inv,String id, String targetUUID){
        inv.setItem(1,createItem(Material.GOLD_BLOCK,"§6§l管理(Admin)権限",null));
        inv.setItem(2,createItem(Material.IRON_BLOCK,"§7§lフル活動権限 §c§l(拒否推奨)",List.of("§f土地の管理行為を除く","§fすべての行動が可能です")));
        inv.setItem(3,createItem(Material.GRASS_BLOCK,"§a§lブロック権限 §c§l(拒否推奨)",List.of("§fブロックの設置･破壊が可能です")));
        inv.setItem(4,createItem(Material.OAK_DOOR,"§e§l右クリック権限 §c§l(拒否推奨)",List.of("§fチェスト系ブロック以外の","§fブロックに対して右クリック","§fを実行できます")));
        inv.setItem(5,createItem(Material.CHEST,"§e§lチェストアクセス権限 §c§l(拒否推奨)",List.of("§fチェスト系ブロックへの","§fアクセスが可能です")));
        inv.setItem(6,createItem(Material.DIAMOND_SWORD,"§b§lPVP権限",List.of("§f他プレイヤに対して攻撃","§fをすることができます。")));
        inv.setItem(7,createItem(Material.HOPPER,"§1§lPICKUP権限",List.of("§f保護エリア内で落ちている","§fアイテムを拾うことがで","§fきます。")));

        int permNum;
        if(targetUUID != null) {  //対象プレイヤーが記載されいるときは土地の権限を見る
            FileConfiguration region = RealEstate.getRegionYml().getConfig();
            if (targetUUID.equals("default")) permNum = region.getInt(id + ".default");
            else permNum = region.getInt( id + ".user." + targetUUID);
        }else{  //対象プレイヤーが記載されいるときはワールドの権限を見る
            FileConfiguration world = RealEstate.getWorldYml().getConfig();
            permNum = world.getInt(id+".permission");
        }
        String perm = new StringBuilder(Integer.toBinaryString(permNum)).reverse() + "0000000000";  //パーミッション値を2進数で取得
        //各権限が許可されているか否か
        for(int i = 0; i< ActionType.values().length; i++){
            if(perm.charAt(i)=='1')inv.setItem(START_PERM_PLACE+i,createItem(Material.LIME_STAINED_GLASS_PANE,"§a§l許可",null));
            else inv.setItem(START_PERM_PLACE+i,createItem(Material.RED_STAINED_GLASS_PANE,"§c§l拒否",null));
        }
        return;
    }

    //一気にアイテムをセット
    static void setItemPlus(Inventory inv,ItemStack item,int... index){
        for(int i:index)inv.setItem(i,item);
    }
}
