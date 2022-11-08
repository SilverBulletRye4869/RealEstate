package silverassist.realestate.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import silverassist.realestate.RealEstate;

import java.util.List;

import static silverassist.realestate.Function.*;

public class Normal implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))return true;
        Player p = (Player) sender;
        if(args.length==0)return true;
        FileConfiguration config = RealEstate.region.getConfig();

        Location loc;
        OfflinePlayer target;
        switch (args[0]){
            //------------------------------------------------------土地へのテレポート
            case "tp":
            case "teleport":
                if(!p.hasPermission("realestate.tp")){
                    sendPrefixMessage(p,"§cあなたは土地にテレポートする権限がありません");
                    return true;
                }
                if(args.length<1)return true;
                if(!isInt(args[1])){
                    sendPrefixMessage(p,"§c土地のidは自然数で入力してください");
                    return true;
                }
                p.teleport(config.getLocation(args[1]+".home"));
                break;
            //-----------------------------------------------------土地の情報 (看板クリック時もここへ飛ばされる)
            case "info":
                //近日改良予定
                break;
            case "manage":
                if(args.length>2 &&!isInt(args[2])){
                    sendPrefixMessage(p,"§c土地のidは整数で入力してください");
                    return true;
                }else if(args.length<2)return true;

                switch (args[1]){
                    //-----------------------------------------------------------------オーナ権の譲渡
                    case "setowner":
                        if(args.length<5)return true;
                        if(!isOwner(p,args[2])){
                            sendPrefixMessage(p,"§cあなたはその土地のオーナではありません");
                            return true;
                        }
                        target = Bukkit.getPlayer(args[3]);
                        if(target ==null){
                            sendPrefixMessage(p,"§cプレイヤー名が間違っているかオフラインです");
                            return true;
                        }
                        if(args[4]==null||!args[4].equals("confirm")){
                            sendPrefixMessage(p,"§c--------【警告】--------");
                            sendPrefixMessage(p,"§6本当にオーナ権限を譲渡する場合は次のコマンドを実行してください");
                            sendPrefixMessage(p,"§c/re manage setowner "+args[2]+" "+ args[3] +" confirm");
                            return true;
                        }
                        config.set(args[2]+".owner",target.getUniqueId().toString());
                        sendPrefixMessage(p,PREFIX+"§a"+args[3]+"を§did"+args[2]+"§aのオーナーとして登録しました");
                        break;
                    //------------------------------------------------------------------住人追加
                    case "adduser":
                        if(args.length<4)return true;
                        if(!isAdmin(p,args[2])){
                            sendPrefixMessage(p,"§cあなたはその土地を管理する権限がありません");
                            return true;
                        }
                        target = Bukkit.getPlayer(args[3]);
                        if(target==null){
                            sendPrefixMessage(p,"§cプレイヤー名が間違っているかオフラインです");
                            return true;
                        }
                        if(config.getInt(args[2]+".user."+target.getUniqueId()) !=0){
                            sendPrefixMessage(p,"§cそのプレイヤーは既に登録されています");
                            return true;
                        }
                        config.set(args[2]+".user."+target.getUniqueId(),0);
                        sendPrefixMessage(p,"§a"+target.getName()+"を§did:"+args[2]+"§aの土地に登録しました");
                        break;
                    //-------------------------------------------------------------------権限設定
                    case "setpermission":
                        if(args.length<5)return true;
                        if(!isAdmin(p,args[2])){
                            sendPrefixMessage(p,"§cあなたはその土地を管理する権限がありません");
                            return true;
                        }
                        target = Bukkit.getOfflinePlayer(args[3]);
                        if(config.getString(args[2]+".user."+target.getUniqueId())==null){
                            sendPrefixMessage(p,"§cそのプレイヤーは住人として登録されていません");
                            return true;
                        }
                        if(args[4].length()>10||Integer.parseInt(args[4])<0){
                            sendPrefixMessage(p,"§cパーミッション値が大きすぎまたは負です！");
                            return true;
                        }
                        int perm = Integer.parseInt(args[4]);
                        if(perm%2==1 && !isOwner(p,args[2])){
                            sendPrefixMessage(p,"§cあなたはこのパーミッション値にする権限がありません");
                            return true;
                        }
                        config.set(args[2]+".user."+target.getUniqueId(),perm);
                        sendPrefixMessage(p,"§d"+args[3]+"§aのパーミッション値を§d"+perm+"§aに設定しました");
                        break;
                    default:
                        if(!isInt(args[1]))return true;
                        if(!isAdmin(p,args[1]))return true;
                        openMergeGui(p,args[1]+".def");
                }
        }
        RealEstate.region.saveConfig();
        return true;
    }

    private void openMergeGui(Player p, String type){
        List<String> data = List.of(type.split("\\."));
        if(data.size()==1)return;
        Inventory inv = null;
        switch (data.get(1)){
            case "def":
                inv = Bukkit.createInventory(p,54,"§1§lid:"+data.get(0)+"§1§lの管理ページ");
                inv.setItem(0,createItem(Material.GRASS_BLOCK,"§a§l土地の詳細設定",List.of("§f土地の状態を設定できます"),0));
                inv.setItem(1,createItem(Material.PLAYER_HEAD,"§a§l土地の住人を管理",List.of("§f土地の住人を管理できます"),0));
                if(isOwner(p,data.get(0)))inv.setItem(2,createItem(Material.DIAMOND_BLOCK,"§b§lオーナー権の譲渡",List.of("§fOwner権を譲渡することができます"),0));
        }
        if(inv!=null)p.openInventory(inv);
        //近日実装
    }
}
