package silverassist.realestate.event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import static silverassist.realestate.Function.ADMIN_WAND;
import static silverassist.realestate.Function.*;

public class AdminEvent implements Listener {
    @EventHandler
    public void setLocation(PlayerInteractEvent e){
        ItemStack item = e.getItem();
        if(item==null)return;
        if(item.getType() != Material.DIAMOND_AXE)return;
        ItemMeta meta = item.getItemMeta();
        if(meta == null)return;
        if(!meta.getDisplayName().equals(ADMIN_WAND))return;

        e.setCancelled(true);//イベントのキャンセル

        Player p = e.getPlayer();
        List<String> lore = meta.getLore();
        if(lore==null)return;
        if(lore.size()!=3){
            sendPrefixMessage(p,"§c§l斧が正常ではありません。再度斧を取得してください");
            return;
        }

        //不正使用を検知した場合は、削除しておく
        if(!lore.get(2).equals(p.getUniqueId().toString())) {
            e.getPlayer().getInventory().remove(e.getItem());
            return;
        }

        //イベント内容と位置情報を取得
        boolean isLeft;
        Location loc;
        switch (e.getAction()){
            case LEFT_CLICK_BLOCK:
                loc = e.getClickedBlock().getLocation();
                isLeft = true;
                break;

            case RIGHT_CLICK_BLOCK:
                loc = e.getClickedBlock().getLocation();
                isLeft = false;
                break;

            default://右クリ、左クリ以外の場合は終了
                return;
        }

        String locStr = loc.getWorld().getName()+","+loc.getX()+","+loc.getY()+","+loc.getZ();
        if(isLeft){
            lore.set(0,"§f§l開始位置: "+locStr);
            sendPrefixMessage(p,"§a開始位置を登録しました§r§6("+locStr+")");
        }
        else {
            lore.set(1,"§f§l終了位置: "+locStr);
            sendPrefixMessage(p,"§a終了位置を登録しました§r§6("+locStr+")");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
