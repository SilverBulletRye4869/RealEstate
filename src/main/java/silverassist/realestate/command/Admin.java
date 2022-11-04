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
        FileConfiguration config = RealEstate.land.getConfig();
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
                if(config.get(args[1])!=null){
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


                String[] locS = lore.get(0).replace("§f§l開始位置: ","").split(",");
                String[] locE = lore.get(1).replace("§f§l終了位置: ","").split(",");
                double[][] locRegister = new double[2][3];
                for(int i = 0;i<3;i++){
                    double s = Double.parseDouble(locS[i+1]);
                    double e = Double.parseDouble(locE[i+1]);
                    if(s<e){
                        locRegister[0][i] =s;
                        locRegister[1][i] =e;
                    }else{
                        locRegister[0][i] = e;
                        locRegister[1][i] = s;
                    }
                }
                String[] memo = {".start",".end"};
                config.set(args[1]+".world",locS[0]);
                for(int i = 0;i<2;i++){
                    config.set(args[1]+memo[i], Arrays.asList(locRegister[i][0],locRegister[i][1],locRegister[i][2]));
                }
                config.set(args[1]+".owner","admin");
                config.set(args[1]+".price",-1);
                config.set(args[1]+".home",p.getLocation());
                config.set(args[1]+".default",0);

                RealEstate.land.saveConfig();
                sendPrefixMessage(p,"§a指定した保護を§d§lid"+args[1]+"§a§lで登録しました");
                p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                break;

            case "delete":
                if(!args[1].matches("-?\\d+")){
                    sendPrefixMessage(p,"§c§lidは整数で指定してください");
                    return true;
                }
                if(config.get(args[1])==null){
                    sendPrefixMessage(p,"§c§lそのidの土地は存在しません");
                    return true;
                }
                if(args[2] == null || !args[2].equals("confirm")){
                    sendPrefixMessage(p,"§c§l本当に§d§lid:"+args[1]+"§c§lの土地を削除する場合は、以下のコマンドを実行してください");
                    sendPrefixMessage(p,"§e/realestate.admin delete "+args[1]+" confirm");
                    return true;
                }
                config.set(args[1],null);
                sendPrefixMessage(p,"§6§lid:"+args[1]+"§c§lの土地を削除しました");
                break;

            case "reload":
                RealEstate.plugin.reloadConfig();
                RealEstate.land.reloadConfig();

        }
        return true;
    }
}
