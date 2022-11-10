package silverassist.realestate.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import silverassist.realestate.RealEstate;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

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

        ItemStack item;
        ItemMeta meta;
        List<String> lore;
        FileConfiguration region = RealEstate.region.getConfig();
        FileConfiguration city = RealEstate.city.getConfig();
        FileConfiguration memo = RealEstate.memo.getConfig();
        char[] cood = {'x','z'};
        switch (args[0]){
            //領域指定斧取得
            case "wand":
                item = new ItemStack(Material.DIAMOND_AXE);
                meta = item.getItemMeta();
                meta.setDisplayName(ADMIN_WAND);
                lore = List.of("§f§l開始位置: §6§l左クリックで指定","§f§l終了位置: §6§l右クリックで指定",p.getUniqueId().toString());
                meta.setLore(lore);
                item.setItemMeta(meta);
                p.getInventory().addItem(item);
                break;
            //領域登録
            case "register":
                if(args.length<2)return true;
                if(!isInt(args[1])){
                    sendPrefixMessage(p,"§c§lidは整数で指定してください");
                    return true;
                }
                if(region.get(args[1])!=null){
                    sendPrefixMessage(p,"§c§lそのidの土地は既に存在しています");
                    return true;
                }

                item = p.getInventory().getItemInMainHand();
                meta = item.getItemMeta();
                if(meta==null||!item.getItemMeta().getDisplayName().equals(ADMIN_WAND)){
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
                String[] locS = lore.get(0).replace("§f§l開始位置: ","").split(",");
                String[] locE = lore.get(1).replace("§f§l終了位置: ","").split(",");
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

                region.set(args[1]+".world",locS[0]);
                region.set(args[1]+".start", Arrays.asList(locRegister[0][0],locRegister[0][1],locRegister[0][2]));
                region.set(args[1]+".end", Arrays.asList(locRegister[1][0],locRegister[1][1],locRegister[1][2]));
                for(int i = 0;i<2;i++) {
                    for (int j = Math.round(locRegister[0][i*2] / 100); j <= Math.round(locRegister[1][i*2] / 100); j++) {
                        List<String> list = memo.getStringList(cood[i]+String.valueOf(j));
                        list.add(args[1]);
                        memo.set(cood[i]+String.valueOf(j), list);
                    }
                }
                //configに記録 & 登録通知
                region.set(args[1]+".owner","admin");
                region.set(args[1]+".price",-1);
                region.set(args[1]+".home",p.getLocation());
                region.set(args[1]+".default",0);
                region.set(args[1]+".status","protect");
                sendPrefixMessage(p,"§a指定した保護を§d§lid"+args[1]+"§a§lで登録しました");
                p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                if(args.length<3)region.set(args[1]+".city","");
                else {
                    region.set(args[1]+".city",args[2]);
                    if(city.get(args[2])==null)cityCreate(args[2],city);//都市がなければ自動生成
                    sendPrefixMessage(p,"§did:"+args[1]+"§aの土地を§6"+args[2]+"§aに所属させました");
                }
                break;

            case "delete":
                if(!args[1].matches("-?\\d+")){
                    sendPrefixMessage(p,"§c§lidは整数で指定してください");
                    return true;
                }
                if(region.get(args[1])==null){
                    sendPrefixMessage(p,"§c§lそのidの土地は存在しません");
                    return true;
                }
                if(args[2] == null || !args[2].equals("confirm")){
                    sendPrefixMessage(p,"§c§l本当に§d§lid:"+args[1]+"§c§lの土地を削除する場合は、以下のコマンドを実行してください");
                    sendPrefixMessage(p,"§e/realestate.admin delete "+args[1]+" confirm");
                    return true;
                }
                for(int i = 0;i<2;i++) {
                    for (int j = Math.round(region.getFloatList(args[1] + ".start").get(0) / 100); j <= Math.round(region.getFloatList(args[1] + ".end").get(0) / 100); j++) {
                        List<String> list = memo.getStringList(cood[i]+String.valueOf(j));
                        list.remove(args[1]);
                        memo.set(cood[i]+String.valueOf(j), list);
                    }
                }
                region.set(args[1],null);
                sendPrefixMessage(p,"§6§lid:"+args[1]+"§c§lの土地を削除しました");
                break;


            case "city":
                if(args.length<2)return true;
                switch (args[1]) {
                    case "belong":
                        if (args.length<4) return true;
                        if (region.get(args[2]) == null) {
                            sendPrefixMessage(p, "§cそのidの土地は見つかりません");
                            return true;
                        }
                        region.set(args[2] + ".city", args[3]);
                        if (city.get(args[3]) == null) cityCreate(args[3], city);//都市がなければ自動生成
                        sendPrefixMessage(p, "§did:" + args[2] + "§aの土地を§6" + args[3] + "§aに所属させました");
                        break;
                        //ここに所属させる処理
                    case "list":
                        Set<String> list;
                        if (args.length==2) {
                            list = city.getKeys(false);
                            sendPrefixMessage(p, "§a都市の一覧は次の通りです");
                            list.forEach(name -> sendPrefixMessage(p, "§e" + name));
                            sendPrefixMessage(p, "§6--- 以上 ---");
                        } else {
                            if(city.get(args[2])==null){
                                sendPrefixMessage(p,"§cその名前の都市は存在しません");
                                return true;
                            }

                            //逆引きできないのかな...?
                            list = region.getKeys(false);
                            sendPrefixMessage(p,"§a都市『§d"+args[2]+"§a』に属するidは次の通りです");
                            list.forEach(id ->{
                                if(!region.getString(id+".city").equals(args[2]))return;
                                sendPrefixMessage(p,"§e"+id);
                            });
                            sendPrefixMessage(p,"§6--- 以上 ---");
                        }
                }

            case "reload":
                RealEstate.plugin.reloadConfig();
                RealEstate.region.reloadConfig();
                RealEstate.city.reloadConfig();

        }
        RealEstate.region.saveConfig();
        RealEstate.city.saveConfig();
        RealEstate.memo.saveConfig();
        return true;
    }

    private void cityCreate(String name, FileConfiguration city){
        city.set(name+".maxUser",4);
        city.set(name+".tax",0);
        city.set(name+".taxType",0);//0:なし, 1:毎日, 2:毎月
    }
}
