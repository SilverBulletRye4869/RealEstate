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
        //==================================基本処理
        Player p = (Player)e.getWhoClicked();
        if(!curInv.containsKey(p))return;
        e.setCancelled(true);

        //========================================================================処理対象外処理
        if(e.getClickedInventory()==null || e.getClickedInventory().getType()!=InventoryType.CHEST)return;
        ItemStack item = e.getCurrentItem();
        if(item ==null)return;
        if(item.getType()== Material.AIR|| item.getType()==Material.BARRIER)return;

        //=====================================基礎処理
        List<String> types = splitType(curInv.get(p));
        int slot = e.getSlot();  //クリックしたスロット
        if(types.size()<3)return;
        String id = types.get(1);  //土地のid or ワールドName
        String nextInv = null;  //次に開くInv
        boolean CloseCancel = false;  //処理の後にInvを閉じないか？

        switch (types.get(0)) {
            //---------------------------------------------------------------------------------------------------土地設定GUI
            case "region":
                FileConfiguration region = RealEstate.getRegionYml().getConfig();
                switch (types.get(2)) {
                    //----------------------------------------------------------------------初期MENU
                    case "def":
                        switch (slot) {
                            //詳細管理画面に飛ばす
                            case 0:
                                nextInv = "region." + id + ".admin.def";
                                break;
                            //入居者一覧画面に飛ばす
                            case 1:
                                nextInv = "region." + id + ".admin.permission.def";
                                break;
                            //オーナー設定
                            case 2:
                                sendPrefixMessage(p, "§e§l土地のOwnerを変更するには次のコマンドを実行してください");
                                sendPrefixMessage(p, "§a§l/re manage " + id + " setowner <MCID> confirm");
                                sendSuggestMessage(p, "§d§l[ここをクリックで自動入力]", "/re manage " + id + " setowner ");
                                break;
                        }
                        break;
                    //-----------------------------------------------------------------------詳細管理MENU
                    case "admin":
                        if (types.size() == 3) return;
                        switch (types.get(3)) {
                            //------------------------------------------------詳細管理のホーム
                            case "def":
                                switch (slot) {
                                    //土地のhome設定
                                    case 0:
                                        Location loc = p.getLocation();
                                        if (!(areaCheck(loc, region.getFloatList(id + ".start"), region.getFloatList(id + ".end"), region.getString(id + ".world")) || p.isOp())) {
                                            sendPrefixMessage(p, "§c土地の範囲外にhomeをセットすることはできません");
                                            return;
                                        }
                                        region.set(id + ".home", p.getLocation());
                                        sendPrefixMessage(p,"§a現在地をhomeとして登録しました");
                                        break;
                                    //デフォルト権限の設定画面に飛ばす
                                    case 1:
                                        nextInv = "region." + id + ".admin.defaultPermission";
                                        break;
                                    //ステータスの設定画面に飛ばす
                                    case 5:
                                        nextInv = "region." + id + ".admin.status";
                                        break;
                                    //土地価値設定
                                    case 6:
                                        sendPrefixMessage(p, "§e§l土地の値段を変更するには次のコマンドを実行してください");
                                        sendPrefixMessage(p, "§a§l/re manage " + id + " setprice <値段>");
                                        sendSuggestMessage(p, "§d§l[ここをクリックで自動入力]", "/re manage " + id + " setprice ");
                                        break;

                                }
                                break;

                            //-------------------------------------------------入居者権限設定
                            case "permission":
                                if (types.size() == 4) return;
                                String t4 = types.get(4);
                                switch (t4) {
                                    //クリックしたプレイヤーの権限設定画面に飛ばす
                                    case "def":
                                        ItemMeta meta = e.getCurrentItem().getItemMeta();
                                        if (meta == null) return;
                                        if (meta.getLore() == null) return;
                                        UUID uuid = UUID.fromString(meta.getLore().get(0).replace("§fuuid: ", ""));
                                        nextInv = "region."+id + ".admin.permission." + uuid;
                                        break;
                                    //ユーザの権限を設定
                                    default:
                                        if (slot == 17) {
                                            region.set(id + ".user." + t4, null);
                                            sendPrefixMessage(p, "§a" + Bukkit.getOfflinePlayer(UUID.fromString(t4)).getName() + "を§did:" + id + "§aの土地から削除しました");
                                            break;
                                        }
                                        SetPermission(e, region, id + ".user." + t4);
                                        CloseCancel = true;

                                }
                                break;

                            //その土地のデフォルト権限の設定
                            case "defaultPermission":
                                SetPermission(e, region, id + ".default");
                                CloseCancel = true;
                                break;

                            //----------------------------------------------------------------------------土地のステータス設定
                            case "status":
                                switch (slot) {
                                    case 0:
                                    case 1:
                                        region.set(id + ".status", "sale");
                                        sendPrefixMessage(p, "§a§l土地の状態を「§c§l販売中§a§l」に設定しました");
                                        break;
                                    case 3:
                                    case 4:
                                    case 5:
                                        region.set(id + ".status", "protect");
                                        sendPrefixMessage(p, "§a§l土地の状態を「§6§l保護中§a§l」に設定しました");
                                        break;
                                    case 7:
                                    case 8:
                                        region.set(id + ".status", "free");
                                        sendPrefixMessage(p, "§a§l土地の状態を「§c§l保護無効中§a§l」に設定しました");
                                        break;
                                    default:
                                        if (slot < 9 || slot > 17 || !p.isOp()) break;
                                        region.set(id + ".status", "frozen");
                                        sendPrefixMessage(p, "§a§l土地の状態を「§c§l凍結中§a§l」に設定しました");
                                }
                                break;
                        }
                        break;

                }
                RealEstate.getRegionYml().saveConfig();  //保存
                break;

            //-----------------------------------------------------------------------------------------------ワールド設定GUI
            case "world":
                if(!p.isOp())return;
                FileConfiguration world = RealEstate.getWorldYml().getConfig();
                switch (types.get(2)){
                    //----------------------------------------------------------------------デフォルト画面
                    case "def":
                        switch (slot){
                            //権限設定画面に飛ばす
                            case 0:
                                nextInv = "world."+id+".permission";
                                break;
                            //そのワールドの保護の有効/無効設定
                            case 1:
                                UUID worldUUID = Bukkit.getWorld(id).getUID();
                                Boolean enabled = world.getBoolean(worldUUID+".enabled");
                                if(enabled)e.getInventory().setItem(1,ENABLED_FALSE_ITEM);
                                else e.getInventory().setItem(1,ENABLED_TRUE_ITEM);
                                world.set(worldUUID+".enabled", !enabled);
                                CloseCancel = true;
                        }
                        break;

                    //そのワールドの無保護エリアでの活動権限
                    case "permission":
                        SetPermission(e,world,Bukkit.getWorld(id).getUID()+".permission");
                        CloseCancel = true;

                    break;
                }
                RealEstate.getWorldYml().saveConfig();  //保存
                break;
        }

        editRegionSign(id);  //土地看板更新
        if(!CloseCancel)p.closeInventory();  //CloseCancelがfalseならInvを閉じる
        if(nextInv!=null)openManageGui(p,nextInv);  //次のInvが設定されていたら開く
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
