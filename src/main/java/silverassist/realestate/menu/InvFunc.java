package silverassist.realestate.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static silverassist.realestate.Function.createItem;

public class InvFunc {
    static Map<Player,String> curInv = new HashMap<>();

    static List<String> splitType(String type){
        return List.of(type.split("\\."));
    }

    static void SetPermission(InventoryClickEvent e, FileConfiguration region, String path){
        int slot = e.getSlot();
        if(slot<11||slot>15)return;
        int permPlace = slot - 11;
        int permNum = region.getInt(path);
        if(e.getCurrentItem().getType()== Material.LIME_STAINED_GLASS_PANE){
            permNum -= Math.pow(2,permPlace);
            e.getInventory().setItem(slot,createItem(Material.RED_STAINED_GLASS_PANE,"§c§l拒否",null));
        }else{
            permNum += Math.pow(2,permPlace);
            e.getInventory().setItem(slot,createItem(Material.LIME_STAINED_GLASS_PANE,"§a§l許可",null));
        }
        region.set(path,permNum);
        return;
    }

    static Inventory createDefaultGui(Player p,int row, String invName){
        Inventory inv = Bukkit.createInventory(p, row*9, invName);
        for(int i = 0;i<row;i++)inv = setItemPlus(inv,createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,"§r",null), new int[]{0+9*i, 1+9*i, 7+9*i, 8+9*i});
        return inv;
    }

    static Inventory setItemPlus(Inventory inv,ItemStack item,int... index){
        for(int i:index)inv.setItem(i,item);
        return inv;
    }
}
