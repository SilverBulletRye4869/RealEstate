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
    //==============================================================================領域指定斧使用Event
    public void setLocation(PlayerInteractEvent e){
        ItemStack item = e.getItem();
        if(item==null)return;
        if(item.getType() != Material.DIAMOND_AXE)return;  //ダイヤの斧か
        ItemMeta meta = item.getItemMeta();
        if(meta == null)return;
        if(!meta.getDisplayName().equals(ADMIN_WAND))return;  //名前が等しいか

        e.setCancelled(true);//イベントのキャンセル

        Player p = e.getPlayer();
        //=================================================================斧が正常なものか判定
        List<String> lore = meta.getLore();
        if(lore==null)return;
        if(lore.size()!=3){
            sendPrefixMessage(p,"§c§l斧が正常ではありません。再度斧を取得してください");
            return;
        }
        //不正使用を検知した場合は、削除しておく
        if(!lore.get(2).equals(p.getUniqueId().toString())) {  //loreの3行目には斧取得者のuuidが格納されている
            e.getPlayer().getInventory().remove(e.getItem());
            return;
        }

        //イベント内容と位置情報を取得
        boolean isLeft;  //左クリックか？
        Location loc;  //クリックした場所
        switch (e.getAction()){
            case LEFT_CLICK_BLOCK:  //左クリック
                isLeft = true;
                break;

            case RIGHT_CLICK_BLOCK:  //右クリック

                isLeft = false;
                break;

            default://右クリ、左クリ以外の場合は終了
                return;
        }
        loc = e.getClickedBlock().getLocation();  //クリックした位置を取得

        String locStr = loc.getWorld().getName()+","+loc.getX()+","+loc.getY()+","+loc.getZ();  //クリックした場所をStrに強引変換
        if(isLeft){  //左クリックなら開始位置として登録
            lore.set(0,"§f§l開始位置: "+locStr);
            sendPrefixMessage(p,"§a開始位置を登録しました§r§6("+locStr+")");
        }
        else {  //右クリックなら開始位置として登録
            lore.set(1,"§f§l終了位置: "+locStr);
            sendPrefixMessage(p,"§a終了位置を登録しました§r§6("+locStr+")");
        }

        meta.setLore(lore);  //loreをセット
        item.setItemMeta(meta);  //itemに反映
    }
}
