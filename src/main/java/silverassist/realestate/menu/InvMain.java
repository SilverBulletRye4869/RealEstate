package silverassist.realestate.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import silverassist.realestate.RealEstate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static silverassist.realestate.Function.*;
import static silverassist.realestate.menu.InvFunc.*;

public class InvMain {
    public static void openManageGui(Player p,String type){
        List<String> types = splitType(type);
        if(types.size()==1)return;
        curInv.put(p,type);

        Inventory inv = null;
        int slot;
        FileConfiguration region = RealEstate.region.getConfig();
        FileConfiguration config = RealEstate.plugin.getConfig();
        String id = types.get(0);

        switch (types.get(1)){
            case "def":
                inv = Bukkit.createInventory(p,9,"§1§lid:"+id+"§1§lの管理ページ");
                inv.setItem(0,createItem(Material.GRASS_BLOCK,"§a§l土地の詳細設定",List.of("§f土地の情報を設定できます"),0));
                inv.setItem(1,createItem(Material.PLAYER_HEAD,"§a§l土地の住人を管理",List.of("§f土地の住人を管理できます"),0));
                if(isOwner(p,types.get(0)))inv.setItem(2,createItem(Material.DIAMOND_BLOCK,"§b§lオーナー権の譲渡",List.of("§fOwner権を譲渡することができます"),0));
                break;
            case "admin":
                if(types.size()==2)return;
                switch (types.get(2)){
                    case "def":
                        inv = Bukkit.createInventory(p,9,"§1§lid:"+id+"§1§lの詳細管理ページ");
                        inv.setItem(0,createItem(Material.ENDER_PEARL,"§a§l土地のテレポート位置を設定",List.of("§e現在地を土地のテレポート地点にします","§c(土地の範囲外には設定できません)")));
                        inv.setItem(1,createItem(Material.COMPARATOR,"§c§lデフォルトの権限を設定", List.of("§f全プレイヤーの権限を設定します")));
                        if(isOwner(p,types.get(0))){
                            inv.setItem(4,createItem(Material.GRAY_STAINED_GLASS_PANE,"§r",null));
                            String status = region.getString(id+".status");
                            if(status.equals("frozen"))inv.setItem(5,createItem(Material.PAPER,"§b§l土地の状態を設定",List.of("§f土地の状態を設定できます","§6現在の状態: §a§l"+status)));
                            inv.setItem(6,createItem(Material.GOLD_INGOT,"§6§l土地の値段を設定",List.of("§f土地の値段を設定できます","§6現在の価格: §c§l" + region.getString(id+".price") + config.get("money_unit"))));
                        }
                        break;
                    case "person":
                        if(types.size()==3)return;
                        String t3 = types.get(3);
                        switch (t3){
                            case "def":
                                Set<String> uuidList = region.getConfigurationSection(id + ".user").getKeys(false);
                                int size = Math.min(54, (((uuidList.size()-1)/9)+1) * 9); //人数に合わせて大きさを調整(54人まで対応)
                                inv = Bukkit.createInventory(p,size,"§1§lid:"+id+"§1§lの住人管理");
                                slot = 0;
                                for(String uuidStr : uuidList){
                                    UUID u = UUID.fromString(uuidStr);
                                    //if(p.getUniqueId().equals(u))continue; //--デバック時アウト
                                    ItemStack head = createItem(Material.PLAYER_HEAD,"§a§l"+Bukkit.getOfflinePlayer(u).getName(),List.of("§fuuid: "+u));
                                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(u));
                                    head.setItemMeta(meta);
                                    inv.setItem(slot,head);
                                    slot++;
                                }
                                break;
                            default:
                                //t3はuuidが格納されているとして扱う
                                inv = createDefaultGui(p,2,"§1§lid:"+id+"の「"+getPlayer(t3).getName()+"」の権限設定");
                                inv = setDefaultPermissionGui(inv,id, t3);

                                if(!isOwner(p,id))inv.setItem(START_PERM_PLACE,createItem(Material.BARRIER,"§c§l変更権限なし",null));
                                inv.setItem(17,createItem(Material.LAVA_BUCKET,"§c§lユーザーを退去",List.of("§6ここをクリックして、","§6ユーザーを退去させる")));

                                //AdminはAdminを退去させられない
                                if(isOwner(p,id))break;
                                if(!isAdmin(getPlayer(t3),id))break;
                                inv.setItem(17,createItem(Material.BARRIER,"§c§l退去権限なし",null));
                        }
                        break;

                    case "defaultPermission":
                        inv = createDefaultGui(p,2,"§1§lid:"+id+"のデフォルト権限設定");
                        inv = setDefaultPermissionGui(inv,id,"default");
                        inv.setItem(START_PERM_PLACE,createItem(Material.BARRIER,"§c§l変更権限なし",List.of("§fこの権限をデフォルトに","§fすることは、できません")));
                        break;

                    case "status":
                        inv = createDefaultGui(p,p.isOp() ? 2 : 1,"§1§lid:"+id+"のステータス設定");
                        inv = setItemPlus(inv,createItem(Material.GOLD_INGOT,"§c§l販売中にする",List.of("§f土地が売りに出されます。","§6土地が購入されると、オーナー","§6権限が自動的に譲渡されます")),0,1);
                        inv = setItemPlus(inv,createItem(Material.TRIPWIRE_HOOK,"§6§l保護状態にする §a§l(推奨)",List.of("§f土地が保護されます")),3,4,5);
                        inv = setItemPlus(inv,createItem(Material.RED_STAINED_GLASS_PANE,"§6§l保護を無効化する §c§l(非推奨)",List.of("§f全プレイヤーが全行動をする","§fことができます。")),7,8);
                        if(p.isOp())inv = setItemPlus(inv,createItem(Material.BLUE_ICE, "§c§l凍結する", List.of("§f土地を凍結すると、オーナー","§fも土地を触れなくなります")),9,10,11,12,13,14,15,16,17);
                        break;
                }
        }
        if(inv!=null)p.openInventory(inv);
        else p.sendMessage(type); //デバックメッセージ
        //近日実装
    }


}
