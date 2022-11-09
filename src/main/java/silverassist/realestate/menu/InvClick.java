package silverassist.realestate.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.List;

import static silverassist.realestate.menu.InvFunc.*;
import static silverassist.realestate.menu.InvMain.openManageGui;
import static silverassist.realestate.Function.*;

public class InvClick implements Listener {


    @EventHandler
    public void ClickEvent(InventoryClickEvent e){
        Player p = (Player)e.getWhoClicked();
        if(!curInv.containsKey(p))return;
        List<String> type = splitType(curInv.get(p));
        if(type.size()<2)return;
        String id = type.get(0);
        switch (type.get(1)){
            case "def":
                switch (e.getSlot()){
                    case 0:
                        openManageGui(p,id+".admin.def");
                        break;
                    case 1:
                        openManageGui(p,id+".admin.person.def");
                        break;
                    case 2:
                        sendPrefixMessage(p,"§e§l土地のOwnerを変更するには次のコマンドを実行してください");
                        sendPrefixMessage(p,"§a§l/re manage setowner "+id+" <次期オーナーのMCID> confirm");
                        sendSuggestMessage(p,"§d§l[ここをクリックで自動入力]","/re manage setowner "+id+" ");
                        break;
                }
        }
        e.setCancelled(true);
        p.closeInventory();
    }

    @EventHandler
    public void CloseEvent(InventoryCloseEvent e){
        curInv.remove((Player)e.getPlayer());
    }
}
