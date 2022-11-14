package silverassist.realestate.menu;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import silverassist.realestate.RealEstate;

import java.util.List;
import java.util.UUID;

import static silverassist.realestate.menu.InvFunc.*;
import static silverassist.realestate.menu.InvMain.openManageGui;
import static silverassist.realestate.Function.*;

public class InvClick implements Listener {
    @EventHandler
    public void ClickEvent(InventoryClickEvent e){
        Player p = (Player)e.getWhoClicked();
        if(!curInv.containsKey(p))return;
        e.setCancelled(true);

        if(e.getClickedInventory()==null || e.getClickedInventory().getType()!=InventoryType.CHEST)return;
        ItemStack item = e.getCurrentItem();
        if(item ==null)return;
        if(item.getType()== Material.AIR|| item.getType()==Material.BARRIER)return;

        List<String> types = splitType(curInv.get(p));
        int slot = e.getSlot();
        FileConfiguration region = RealEstate.region.getConfig();

        if(types.size()<2)return;
        String id = types.get(0);
        String nextInv = null;
        boolean CloseCancel = false;

        switch (types.get(1)){
            case "def":
                switch (slot){
                    case 0:
                        nextInv=id+".admin.def";
                        break;
                    case 1:
                        nextInv=id+".admin.person.def";
                        break;
                    case 2:
                        sendPrefixMessage(p,"§e§l土地のOwnerを変更するには次のコマンドを実行してください");
                        sendPrefixMessage(p,"§a§l/re manage "+id+" setowner <MCID> confirm");
                        sendSuggestMessage(p,"§d§l[ここをクリックで自動入力]","/re manage "+id+" setowner ");
                        break;
                }
                break;
            case "admin":
                if(types.size()==2)return;
                switch (types.get(2)){
                    case "def":
                        switch (slot){
                            case 0:
                                Location loc = p.getLocation();
                                if(!(areaCheck(loc, region.getFloatList(id+".start"),region.getFloatList(id+".end"),region.getString(id+".world"))  ||  p.isOp()  )){
                                    sendPrefixMessage(p,"§c土地の範囲外にhomeをセットすることはできません");
                                    return;
                                }
                                region.set(id+".home",p.getLocation());
                                break;
                            case 1:
                                nextInv = id+".admin.defaultPermission";
                                break;
                            case 5:
                                nextInv = id+".admin.status";
                                break;
                            case 6:
                                sendPrefixMessage(p,"§e§l土地の値段を変更するには次のコマンドを実行してください");
                                sendPrefixMessage(p,"§a§l/re manage "+id+" setprice <値段>");
                                sendSuggestMessage(p,"§d§l[ここをクリックで自動入力]","/re manage "+id+" setprice ");
                                break;

                        }
                        break;
                    case "person":
                        if(types.size()==3)return;
                        String t3 = types.get(3);
                        switch (t3){
                            case "def":
                                ItemMeta meta = e.getCurrentItem().getItemMeta();
                                if(meta==null)return;
                                if(meta.getLore() == null)return;
                                UUID uuid = UUID.fromString(meta.getLore().get(0).replace("§fuuid: ",""));
                                nextInv = id+".admin.person."+uuid;
                                break;
                            default:
                                if(slot == 17){
                                    region.set(id+".user."+t3,null);
                                    sendPrefixMessage(p,"§a"+Bukkit.getOfflinePlayer(UUID.fromString(t3)).getName()+"を§did:"+id+"§aの土地から削除しました");
                                    break;
                                }
                                SetPermission(e,region,id+".user."+t3);
                                CloseCancel=true;

                        }
                        break;

                    case "defaultPermission":
                        InvFunc.SetPermission(e,region,id+".default");
                        CloseCancel=true;
                        break;

                    case "status":
                        switch (slot){
                            case 0:
                            case 1:
                                region.set(id+".status","sale");
                                sendPrefixMessage(p,"§a§l土地の状態を「§c§l販売中§a§l」に設定しました");
                                break;
                            case 3:
                            case 4:
                            case 5:
                                region.set(id+".status","protect");
                                sendPrefixMessage(p,"§a§l土地の状態を「§6§l保護中§a§l」に設定しました");
                                break;
                            case 7:
                            case 8:
                                region.set(id+".status","free");
                                sendPrefixMessage(p,"§a§l土地の状態を「§c§l保護無効中§a§l」に設定しました");
                                break;
                            default:
                                if(slot<9 || slot>17 || !p.isOp())break;
                                region.set(id+".status","frozen");
                                sendPrefixMessage(p,"§a§l土地の状態を「§c§l凍結中§a§l」に設定しました");
                        }
                        break;
                }
                break;

        }


        RealEstate.region.saveConfig();
        if(!CloseCancel)p.closeInventory();
        if(nextInv!=null)openManageGui(p,nextInv);
    }

    @EventHandler
    public void CloseEvent(InventoryCloseEvent e){
        curInv.remove((Player)e.getPlayer());
    }
    @EventHandler
    public void JoinEvent(PlayerJoinEvent e){
        curInv.remove(e.getPlayer());
    }


}
