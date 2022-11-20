package silverassist.realestate.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import silverassist.realestate.RealEstate;
import silverassist.realestate.menu.InvMain;

import java.util.*;

import static silverassist.realestate.Function.*;

public class Admin implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))return true;
        Player p = (Player) sender;
        if(!p.isOp())return true;
        if(args.length == 0){
            //ヘルプ
            return true;
        }

        //アイテムデータ格納しておく奴
        ItemStack item;
        ItemMeta meta;
        List<String> lore;

        //=======================================================yml取得
        FileConfiguration region = RealEstate.getRegionYml().getConfig();
        FileConfiguration city = RealEstate.getCityYml().getConfig();
        FileConfiguration memo = RealEstate.getMemoYml().getConfig();
        FileConfiguration config = RealEstate.getInstance().getConfig();

        char[] cood = {'x','z'};  //memoに使うやつ
        switch (args[0]){
            //========================================================================================領域指定斧取得
            case "wand":
                item = new ItemStack(Material.DIAMOND_AXE);
                meta = item.getItemMeta();
                meta.setDisplayName(ADMIN_WAND);
                lore = List.of("§f§l開始位置: §6§l左クリックで指定","§f§l終了位置: §6§l右クリックで指定",p.getUniqueId().toString());
                meta.setLore(lore);
                item.setItemMeta(meta);
                p.getInventory().addItem(item);  //Give
                break;
            //========================================================================================領域登録
            case "register":
                if(args.length<2)return true;
                if(!isInt(args[1])){  //idが不正でないか？
                    sendPrefixMessage(p,"§c§lidは整数で指定してください");
                    return true;
                }
                if(region.get(args[1])!=null){  //土地が存在していないか？
                    sendPrefixMessage(p,"§c§lそのidの土地は既に存在しています");
                    return true;
                }

                item = p.getInventory().getItemInMainHand();  //手持ちを検知
                meta = item.getItemMeta();  //metaを取得
                if(meta==null||!item.getItemMeta().getDisplayName().equals(ADMIN_WAND)){  //領域指定斧を持っているか
                    sendPrefixMessage(p,"§d§l領域指定斧をメインハンドに持って実行してください");
                    return true;
                }

                //指定されているか
                lore = meta.getLore();
                if(lore.get(1).contains("§6§l左クリックで指定")){
                    sendPrefixMessage(p,"§d§l開始位置が指定されていません");
                    return true;
                }
                if(lore.get(2).contains("§6§l右クリックで指定")){
                    sendPrefixMessage(p,"§d§l終了位置が指定されていません");
                    return true;
                }

                //各座標の最少位置、最大位置を登録
                String[] locS = lore.get(0).replace("§f§l開始位置: ","").split(",");  //最少位置
                String[] locE = lore.get(1).replace("§f§l終了位置: ","").split(",");  //最大位置
                float[][] locRegister = new float[2][3];
                for(int i = 0;i<3;i++){
                    float s = Float.parseFloat(locS[i+1]);
                    float e = Float.parseFloat(locE[i+1]);
                    if(s<e){
                        locRegister[0][i] =s;
                        locRegister[1][i] =e;
                    }else{
                        locRegister[0][i] = e;
                        locRegister[1][i] = s;
                    }
                }

                region.set(args[1]+".world",locS[0]);  //土地のあるワールドを保存
                region.set(args[1]+".start", Arrays.asList(locRegister[0][0],locRegister[0][1],locRegister[0][2]));  //開始位置(最少位置)を保存
                region.set(args[1]+".end", Arrays.asList(locRegister[1][0],locRegister[1][1],locRegister[1][2]));  //終了位置(最大位置)を保存
                for(int i = 0;i<2;i++) {  //処理量削減のためのmemo生成(100マス単位で区切って保存)
                    for (int j = Math.round(locRegister[0][i*2] / 100); j <= Math.round(locRegister[1][i*2] / 100); j++) {
                        List<String> list = memo.getStringList(cood[i]+String.valueOf(j));
                        list.add(args[1]);
                        memo.set(cood[i]+String.valueOf(j), list);
                    }
                }
                //configに記録 & 登録通知
                region.set(args[1]+".owner","admin");  //ownerは運営
                region.set(args[1]+".price",0);  //とりあえず、0円
                region.set(args[1]+".home",p.getLocation());  //コマンド実行位置をhomeに設定
                region.set(args[1]+".default",0);  //デフォルト権限
                region.set(args[1]+".status","protect");  //ステータス
                region.set(args[1]+".sign", new ArrayList<>());  //看板保存用
                sendPrefixMessage(p,"§a指定した保護を§d§lid"+args[1]+"§a§lで登録しました");
                p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));  //メインハンドを空気に置換
                if(args.length<3)region.set(args[1]+".city","");  //都市が入力されてなければ無所属
                else {  //入力されてれば所属させる
                    region.set(args[1]+".city",args[2]);  //都市保存
                    if(city.get(args[2])==null)cityCreate(args[2],city,1000000);//都市がなければ自動生成
                    region.set(args[1]+".price",RealEstate.getCityYml().getConfig().getInt(args[2]+".defaultPrice"));  //その都市のデフォルト価格を反映
                    sendPrefixMessage(p,"§did:"+args[1]+"§aの土地を§6"+args[2]+"§aに所属させました");
                }
                break;

            //====================================================================================================領域削除
            case "delete":
                if(args.length==1)return true;
                if(!args[1].matches("-?\\d+")){  //idが正しいか？
                    sendPrefixMessage(p,"§c§lidは整数で指定してください");
                    return true;
                }
                if(region.get(args[1])==null){  //土地が存在するか？
                    sendPrefixMessage(p,"§c§lそのidの土地は存在しません");
                    return true;
                }
                if(args.length==2 || !args[2].equals("confirm")){  //最終確認
                    sendPrefixMessage(p,"§c§l本当に§d§lid:"+args[1]+"§c§lの土地を削除する場合は、以下のコマンドを実行してください");
                    sendPrefixMessage(p,"§e§l/realestate.admin delete "+args[1]+" confirm");
                    sendSuggestMessage(p,"§d§l[ここをクリックして一部自動入力]","/realestate.admin delete "+args[1]);
                    return true;
                }
                for(int i = 0;i<2;i++) {  //memoも消す
                    for (int j = Math.round(region.getFloatList(args[1] + ".start").get(0) / 100); j <= Math.round(region.getFloatList(args[1] + ".end").get(0) / 100); j++) {
                        List<String> list = memo.getStringList(cood[i]+String.valueOf(j));
                        list.remove(args[1]);
                        memo.set(cood[i]+String.valueOf(j), list);
                    }
                }
                List<Location> signLocationList = (List<Location>) region.getList(args[1]+".sign");  //看板一覧取得
                //看板も消す
                signLocationList.forEach(signLocation -> {
                    Block block = signLocation.getBlock();
                    if(block==null || block.getState()==null)return;
                    BlockState state = block.getState();
                    if(!(state instanceof Sign))return;
                    for(int i=0;i<4;i++)((Sign)state).setLine(i,"");
                    state.update();
                });
                region.set(args[1],null);  //ymlから完全消去
                sendPrefixMessage(p,"§6§lid:"+args[1]+"§c§lの土地を削除しました");
                break;

            //=================================================================================================都市管理
            case "city":
                if(args.length<2)return true;
                switch (args[1]) {
                    //=================================================================================都市に所属させる
                    case "belong":
                        if (args.length<4) return true;
                        if (region.get(args[2]) == null) {  //土地が存在するか
                            sendPrefixMessage(p, "§cそのidの土地は見つかりません");
                            return true;
                        }
                        region.set(args[2] + ".city", args[3]);  //反映
                        if (city.get(args[3]) == null) cityCreate(args[3], city, 1000000);//都市がなければ自動生成
                        sendPrefixMessage(p, "§did:" + args[2] + "§aの土地を§6" + args[3] + "§aに所属させました");
                        break;
                    //==================================================================================都市一覧表示
                    case "list":
                        Set<String> list;  //都市一覧保存しておく奴
                        if (args.length==2) {
                            list = city.getKeys(false);  //都市名を全取得
                            sendPrefixMessage(p, "§a都市の一覧は次の通りです");
                            list.forEach(name -> sendPrefixMessage(p, "§e" + name));  //全出力
                            sendPrefixMessage(p, "§6--- 以上 ---");
                        } else {
                            if(city.get(args[2])==null){  //歳が存在するか？
                                sendPrefixMessage(p,"§cその名前の都市は存在しません");
                                return true;
                            }

                            //逆引きできないのかな...? (双方向じゃないので全探索)
                            list = region.getKeys(false);
                            sendPrefixMessage(p,"§a都市『§d"+args[2]+"§a』に属するidは次の通りです");
                            list.forEach(id ->{
                                if(!region.getString(id+".city").equals(args[2]))return;  //所属している都市が異なればreturn
                                sendPrefixMessage(p,"§e"+id);
                            });
                            sendPrefixMessage(p,"§6--- 以上 ---");
                        }
                        break;
                    //===================================================================================都市を管理
                    case "manage":
                        if(args.length < 5)return true;
                        if(city.get(args[2])==null){  //都市が存在するか？
                            sendPrefixMessage(p,"§cその都市は見つかりません");
                            return true;
                        }
                        //何を設定するか？
                        switch (args[3]){
                            //===============================================デフォルト価格
                            case "setdefaultprice":
                                city.set(args[2]+".defaultPrice",Integer.parseInt(args[4]));  //反映
                                sendPrefixMessage(p,"§a都市『§d"+args[2]+"§a』のデフォルト価格を§d"+args[4]+config.get("money_unit")+"§aに設定しました");
                                break;
                            //================================================各土地の最大人数
                            case "setmaxuser":
                                city.set(args[2]+".maxUser",Integer.parseInt(args[4]));
                                sendPrefixMessage(p,"§a都市『§d"+args[2]+"§a』のデフォルト上限人数を§d"+args[4]+"§a人に設定しました");
                                break;
                        }
                }
                break;

            //=======================================================================================================ワールド管理
            case "world":
                if(args.length==1)return true;
                //何をするか？
                switch (args[1]){
                    //=======================================================================ワールド管理
                    case "manage":
                        if(args.length==2)return true;
                        if(Bukkit.getWorld(args[2]) == null){  //ワールドが存在するか？
                            sendPrefixMessage(p, "§cワールドが見つかりません！");
                            return true;
                        }
                        InvMain.openManageGui(p,"world."+args[2]+".def");  //GuiOpen

                    //=======================================================================ワールド一覧取得
                    case "list":
                        sendPrefixMessage(p,"§e-----------[ワールド一覧]-----------");
                        Bukkit.getWorlds().forEach(world -> sendRunCommandMessage(p,"§a§lName: §d§l"+world.getName(),"/re.a world manage "+world.getName()));
                        sendPrefixMessage(p,"§e---------------------------------");
                        break;
                }
                break;


            //======================================================================================================yml再読み込み
            case "reload":
                RealEstate.getInstance().reloadConfig();
                RealEstate.getRegionYml().reloadConfig();
                RealEstate.getCityYml().reloadConfig();
                RealEstate.getMemoYml().reloadConfig();
                RealEstate.getWorldYml().reloadConfig();

        }

        //保存
        RealEstate.getRegionYml().saveConfig();
        RealEstate.getCityYml().saveConfig();
        RealEstate.getMemoYml().saveConfig();
        return true;
    }

    private void cityCreate(String name, FileConfiguration city, int defaultPrice){
        city.set(name+".maxUser",4);  //最初の上限人数は4人
        //いつか実装したい
        //city.set(name+".tax",0);
        //city.set(name+".taxType",0);//0:なし, 1:毎日, 2:毎月
        city.set(name+".defaultPrice", defaultPrice);  //デフォルト価格を反映
        RealEstate.getCityYml().saveConfig();  //保存
    }
}
