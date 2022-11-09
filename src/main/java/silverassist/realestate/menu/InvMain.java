package silverassist.realestate.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

import static silverassist.realestate.Function.createItem;
import static silverassist.realestate.Function.isOwner;
import static silverassist.realestate.menu.InvFunc.*;

public class InvMain {
    public static void openManageGui(Player p,String type){
        List<String> data = splitType(type);
        if(data.size()==1)return;
        curInv.put(p,type);
        Inventory inv = null;
        switch (data.get(1)){
            case "def":
                inv = Bukkit.createInventory(p,9,"§1§lid:"+data.get(0)+"§1§lの管理ページ");
                inv.setItem(0,createItem(Material.GRASS_BLOCK,"§a§l土地の詳細設定",List.of("§f土地の状態を設定できます"),0));
                inv.setItem(1,createItem(Material.PLAYER_HEAD,"§a§l土地の住人を管理",List.of("§f土地の住人を管理できます"),0));
                if(isOwner(p,data.get(0)))inv.setItem(2,createItem(Material.DIAMOND_BLOCK,"§b§lオーナー権の譲渡",List.of("§fOwner権を譲渡することができます"),0));
        }
        if(inv!=null)p.openInventory(inv);
        else p.sendMessage(type); //デバックメッセージ
        //近日実装
    }


}
