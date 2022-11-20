package silverassist.realestate.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import silverassist.realestate.RealEstate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static silverassist.realestate.Function.*;
import static silverassist.realestate.menu.InvFunc.*;

public class InvMain {
    public static void openManageGui(Player p,String type){
        List<String> types = splitType(type);
        if(types.size()<3)return;
        curInv.put(p,type);  //GUIタイプ文を分離

        Inventory inv = null;  //開くInv
        int slot;  //slot番号保管用

        FileConfiguration config = RealEstate.getInstance().getConfig();  //config

        String id = types.get(1);  //id取得

        switch (types.get(0)) {  //対象物によって分離(region, world, city)
            case "region":
                //==================================================================================================================土地関係
                FileConfiguration region = RealEstate.getRegionYml().getConfig();  //土地の情報
                switch (types.get(2)) {
                    //==========================================================================================ホームメニュー
                    case "def":
                        inv = Bukkit.createInventory(p, 9, "§1§lid:" + id + "§1§lの管理ページ");
                        inv.setItem(0, createItem(Material.GRASS_BLOCK, "§a§l土地の詳細設定", List.of("§f土地の情報を設定できます"), 0));
                        inv.setItem(1, createItem(Material.PLAYER_HEAD, "§a§l土地の住人を管理", List.of("§f土地の住人を管理できます"), 0));
                        if (isOwner(p, types.get(0)))  //ownerのみ実施
                            inv.setItem(2, createItem(Material.DIAMOND_BLOCK, "§b§lオーナー権の譲渡", List.of("§fOwner権を譲渡することができます"), 0));
                        break;
                    //==========================================================================================詳細管理メニュー
                    case "admin":
                        if (types.size() == 3) return;
                        //詳細管理の内容によって分離
                        switch (types.get(3)) {
                            //========================================================================詳細管理のホーム
                            case "def":
                                inv = Bukkit.createInventory(p, 9, "§1§lid:" + id + "§1§lの詳細管理ページ");
                                inv.setItem(0, createItem(Material.ENDER_PEARL, "§a§l土地のテレポート位置を設定", List.of("§e現在地を土地のテレポート地点にします", "§c(土地の範囲外には設定できません)")));
                                inv.setItem(1, createItem(Material.COMPARATOR, "§c§lデフォルトの権限を設定", List.of("§f全プレイヤーの権限を設定します")));
                                if (isOwner(p, types.get(0))) {  //Ownerのみ
                                    inv.setItem(4, createItem(Material.GRAY_STAINED_GLASS_PANE, "§r", null));
                                    String status = region.getString(id + ".status");  //土地のステータス
                                    inv.setItem(5, createItem(Material.PAPER, "§b§l土地の状態を設定", List.of("§f土地の状態を設定できます", "§6現在の状態: §a§l" + status)));
                                    inv.setItem(6, createItem(Material.GOLD_INGOT, "§6§l土地の値段を設定", List.of("§f土地の値段を設定できます", "§6現在の価格: §c§l" + region.getString(id + ".price") + config.get("money_unit"))));
                                }
                                break;
                            //========================================================================土地の権限管理
                            case "permission":
                                if (types.size() == 4) return;
                                String t4 = types.get(4);  //uuidか"def"が入ってる
                                switch (t4) {
                                    //=================================================入居者一覧表示
                                    case "def":
                                        Set<String> uuidList = region.getConfigurationSection(id + ".user") != null ? region.getConfigurationSection(id + ".user").getKeys(false) : new HashSet<>();  //入居者リスト取得
                                        int size = Math.min(54, (((uuidList.size() - 1) / 9) + 1) * 9); //人数に合わせて大きさを調整(54人まで対応)
                                        inv = Bukkit.createInventory(p, size, "§1§lid:" + id + "§1§lの住人管理");
                                        slot = 0;
                                        for (String uuidStr : uuidList) {  //全てのuuidで実行
                                            UUID u = UUID.fromString(uuidStr);  //uuidに変換
                                            if(p.getUniqueId().equals(u))continue;  //自分を除く  //--デバック時アウト
                                            ItemStack head = createItem(Material.PLAYER_HEAD, "§a§l" + Bukkit.getOfflinePlayer(u).getName(), List.of("§fuuid: " + u));  //頭生成
                                            SkullMeta meta = (SkullMeta) head.getItemMeta();  //頭の内容をいじれるように
                                            meta.setOwningPlayer(Bukkit.getOfflinePlayer(u));  //プレイヤー情報投入
                                            head.setItemMeta(meta);  //アイテムに反映
                                            inv.setItem(slot, head);  //Invにセット
                                            slot++;
                                        }
                                        break;

                                    //=================================================ユーザー管理
                                    default:
                                        //t4はuuidが格納されているとして扱う
                                        inv = createDefaultGui(p, 2, "§1§lid:" + id + "の「" + getPlayer(t4).getName() + "」の権限設定");
                                        setDefaultPermissionGui(inv, id, t4);  //基礎GUI

                                        if (!isOwner(p, id)) inv.setItem(START_PERM_PLACE, createItem(Material.BARRIER, "§c§l変更権限なし", null));
                                        inv.setItem(17, createItem(Material.LAVA_BUCKET, "§c§lユーザーを退去", List.of("§6ここをクリックして、", "§6ユーザーを退去させる")));

                                        //AdminはAdminを退去させられない
                                        if (isOwner(p, id)) break;
                                        if (!isAdmin(getPlayer(t4), id)) break;
                                        inv.setItem(17, createItem(Material.BARRIER, "§c§l退去権限なし", null));
                                }
                                break;

                            //=====================================================================土地のデフォルト権限設定
                            case "defaultPermission":
                                inv = createDefaultGui(p, 2, "§1§lid:" + id + "のデフォルト権限設定");
                                setDefaultPermissionGui(inv, id, "default");
                                inv.setItem(START_PERM_PLACE, createItem(Material.BARRIER, "§c§l変更権限なし", List.of("§fこの権限はデフォルト", "§fでは付与できません")));
                                break;

                            //=====================================================================土地のステータス設定
                            case "status":
                                inv = createDefaultGui(p, p.isOp() ? 2 : 1, "§1§lid:" + id + "のステータス設定");
                                setItemPlus(inv, createItem(Material.GOLD_INGOT, "§c§l販売中にする", List.of("§f土地が売りに出されます。", "§6土地が購入されると、オーナー", "§6権限が自動的に譲渡されます")), 0, 1);
                                setItemPlus(inv, createItem(Material.TRIPWIRE_HOOK, "§6§l保護状態にする §a§l(推奨)", List.of("§f土地が保護されます")), 3, 4, 5);
                                setItemPlus(inv, createItem(Material.RED_STAINED_GLASS_PANE, "§6§l保護を無効化する §c§l(非推奨)", List.of("§f全プレイヤーが全行動をする", "§fことができます。")), 7, 8);
                                //opのみ凍結ボタン追加
                                if (p.isOp()) setItemPlus(inv, createItem(Material.BLUE_ICE, "§c§l凍結する", List.of("§f土地を凍結すると、オーナー", "§fも土地を触れなくなります")), 9, 10, 11, 12, 13, 14, 15, 16, 17);
                                break;
                        }
                }
                break;

            //=======================================================================================================ワールド管理
            case "world":
                FileConfiguration world = RealEstate.getWorldYml().getConfig();  //worldデータ取得
                String worldUUID = Bukkit.getWorld(id).getUID().toString();  //ワールド名 -> UUID -> 文字列に変換
                switch (types.get(2)){
                    //=================================================================================ワールド管理のホーム
                    case "def":
                        //configに無ければ生成
                        if(world.get(worldUUID)==null) {
                            world.set(worldUUID + ".permission", 64);  //拾い上げ権限のみ許可
                            world.set(worldUUID + ".enabled", false);  //無効状態で生成
                            RealEstate.getWorldYml().saveConfig();  //保存
                        }
                        inv = Bukkit.createInventory(p,9,"§1ワールド「"+id+"」の管理");
                        inv.setItem(0, createItem(Material.COMPARATOR,"§6§lワールドのデフォルト権限を設定", List.of("§fそのワールドのどこにも","§f属していない土地の権限","§fを設定します。")));
                        boolean enabled = world.getBoolean(worldUUID+".enabled");
                        //有効化無効化ボタン
                        if(enabled)inv.setItem(1,ENABLED_TRUE_ITEM);
                        else inv.setItem(1,ENABLED_FALSE_ITEM);
                        break;
                    //=================================================================================ワールドの権限設定
                    case "permission":
                        inv = createDefaultGui(p,2,"§1ワールド『"+id+"』の権限設定");
                        setDefaultPermissionGui(inv, worldUUID,null);
                        inv.setItem(START_PERM_PLACE, createItem(Material.BARRIER, "§c§l変更不可", List.of("§fこの権限はワールドに対", "§fして、付与できません")));

                }

        }
        if(inv!=null)p.openInventory(inv);  //Invが作られてたら開く
        else p.sendMessage(type); //デバックメッセージ
        //近日実装
    }


}
