package silverassist.realestate.command;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import silverassist.realestate.menu.InvMain;
import silverassist.realestate.RealEstate;

import java.util.UUID;

import static silverassist.realestate.Function.*;
import static silverassist.realestate.RealEstate.vault;

public class Normal implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))return true;
        Player p = (Player) sender;
        if(args.length==0)return true;
        FileConfiguration region = RealEstate.region.getConfig();
        FileConfiguration config = RealEstate.plugin.getConfig();
        FileConfiguration city = RealEstate.city.getConfig();

        Location loc;
        OfflinePlayer target;
        switch (args[0]){
            case "buy":
                if(args.length==1)return true;
                if(!isInt(args[1]))return true;
                String id = args[1];

                if(region.get(id)==null){
                    sendPrefixMessage(p,"§c土地が存在しません！");
                    return true;
                }
                if(!region.get(id+".status").equals("sale")){
                    sendPrefixMessage(p,"§c現在この土地を購入することはできません");
                    return true;
                }
                int money = region.getInt(id+".price");
                Economy economy = vault.getEconomy();
                if(economy.getBalance(p) < money){
                    sendPrefixMessage(p,"§c所持金がたりません！");
                    return true;
                }

                if(args.length==2 || !args[2].equals("confirm")){
                    sendBuyCheckMessage(p,args[1],true);
                    return true;
                }

                economy.withdrawPlayer(p, money);

                OfflinePlayer owner = getOwner(id);
                if(owner!=null){
                    target = owner;
                    economy.withdrawPlayer(target,money);
                    if(target.isOnline()){
                        sendPrefixMessage((Player) target,"§d§lid:"+id+"§a§lの土地が購入されました！");
                    }
                }
                region.set(id+".owner",p.getUniqueId().toString());
                sendPrefixMessage(p,"§d§lid:"+id+"§a§lの土地を§d§l"+money+ (config.get("money_unit")) +"§a§lで購入しました");
                break;

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
                p.teleport(region.getLocation(args[1]+".home"));
                break;
            //-----------------------------------------------------土地の情報 (看板クリック時もここへ飛ばされる)
            case "info":
                //近日改良予定
                break;
            case "manage":
                if(args.length<2)return true;
                else if(!isInt(args[1])){
                    sendPrefixMessage(p,"§c土地のidは整数で入力してください");
                    return true;
                }else if((region.get(args[1])==null)){
                    sendPrefixMessage(p,"§c指定したidの土地が見つかりません");
                    return true;
                }
                if(!p.isOp() && region.getString(args[1]+".status").equals("frozen")){
                    sendPrefixMessage(p,"§cこの土地は凍結されています！");
                    return true;
                }
                if(args.length<3){
                    if(!isAdmin(p,args[1])){
                        sendPrefixMessage(p,"§cあなたはこの土地を管理する権限がありません！");
                        return true;
                    }
                    InvMain.openManageGui(p,args[1]+".def");
                    break;
                }

                switch (args[2]){
                    //-----------------------------------------------------------------オーナ権の譲渡
                    case "setowner":
                        if(args.length<4)return true;
                        if(!isOwner(p,args[1])){
                            sendPrefixMessage(p,"§cあなたはその土地のオーナではありません");
                            return true;
                        }
                        target = Bukkit.getPlayer(args[3]);
                        if(target ==null){
                            sendPrefixMessage(p,"§cプレイヤー名が間違っているかオフラインです");
                            return true;
                        }
                        if(args.length==4||!args[4].equals("confirm")){
                            sendPrefixMessage(p,"§c--------【警告】--------");
                            sendPrefixMessage(p,"§6本当にオーナ権限を譲渡する場合は次のコマンドを実行してください");
                            sendPrefixMessage(p,"§c/re manage setowner "+args[1]+" "+ args[3] +" confirm");
                            return true;
                        }
                        region.set(args[1]+".owner",target.getUniqueId().toString());
                        sendPrefixMessage(p,"§a"+args[3]+"を§did"+args[1]+"§aのオーナーとして登録しました");
                        break;
                    //------------------------------------------------------------------住人追加
                    case "adduser":
                        if(args.length<4)return true;
                        if(!isAdmin(p,args[1])){
                            sendPrefixMessage(p,"§cあなたはその土地を管理する権限がありません");
                            return true;
                        }
                        String belongCity = region.getString(args[1]+".city");
                        int max_user = city.getString(belongCity+".maxUser") != null ? city.getInt(belongCity+".maxUser") : config.getInt("default_maxUser");
                        if(region.get(args[1]+".user")!=null && region.getConfigurationSection(args[1]+".user").getKeys(false).size() >= max_user){
                            sendPrefixMessage(p,"§cこの土地にはこれ以上プレイヤーを追加することはできません");
                            return true;
                        }
                        target = Bukkit.getPlayer(args[3]);
                        if(target==null){
                            sendPrefixMessage(p,"§cプレイヤー名が間違っているかオフラインです");
                            return true;
                        }
                        if(region.getInt(args[1]+".user."+target.getUniqueId()) !=0){
                            sendPrefixMessage(p,"§cそのプレイヤーは既に登録されています");
                            return true;
                        }
                        region.set(args[1]+".user."+target.getUniqueId(),0);
                        sendPrefixMessage(p,"§a"+target.getName()+"を§did:"+args[1]+"§aの土地に登録しました");
                        break;
                    //-------------------------------------------------------------------住人退去
                    case "removeuser":
                        if(args.length<4)return true;
                        if(!isAdmin(p,args[1])){
                            sendPrefixMessage(p,"§cあなたはその土地を管理する権限がありません");
                            return true;
                        }
                        target = Bukkit.getOfflinePlayer(args[3]);
                        if(!region.getConfigurationSection(args[1]+".user").getKeys(false).contains(target.getUniqueId().toString())){
                            sendPrefixMessage(p,"§cその人は入居していません");
                            return true;
                        }
                        if(!isOwner(p,args[1])&&isAdmin(target,args[1])){
                            sendPrefixMessage(p,"§cあなたはこの人を退去させることはできません");
                            return true;
                        }
                        region.set(args[1]+".user."+target.getUniqueId(),null);
                        sendPrefixMessage(p,"§a"+target.getName()+"を§did:"+args[1]+"§aの土地から削除しました");
                        break;

                    //-------------------------------------------------------------------権限設定
                    case "setpermission":
                        if(args.length<5)return true;
                        if(!isAdmin(p,args[1])){
                            sendPrefixMessage(p,"§cあなたはその土地を管理する権限がありません");
                            return true;
                        }
                        target = Bukkit.getOfflinePlayer(args[3]);
                        if(region.getString(args[1]+".user."+target.getUniqueId())==null){
                            sendPrefixMessage(p,"§cそのプレイヤーは住人として登録されていません");
                            return true;
                        }
                        if(args[4].length()>10||Integer.parseInt(args[4])<0){
                            sendPrefixMessage(p,"§cパーミッション値が大きすぎまたは負です！");
                            return true;
                        }
                        int perm = Integer.parseInt(args[4]);
                        if(perm%2==1 && !isOwner(p,args[1])){
                            sendPrefixMessage(p,"§cあなたはこのパーミッション値にする権限がありません");
                            return true;
                        }
                        region.set(args[1]+".user."+target.getUniqueId(),perm);
                        sendPrefixMessage(p,"§d"+args[3]+"§aのパーミッション値を§d"+perm+"§aに設定しました");
                        break;

                    //----------------------------------------------------------------------値段設定
                    case "setprice":
                        if(args.length<4)return true;
                        if(!isOwner(p,args[1])){
                            sendPrefixMessage(p,"§cこの土地の値段を設定する権限がありません");
                            return true;
                        }
                        if(args[3].length()>10){
                            sendPrefixMessage(p,"§c値段が高すぎます！");
                            return true;
                        }
                        int price = Integer.parseInt(args[3]);
                        if(price<0){
                            sendPrefixMessage(p,"§c値段は0以上の自然数にする必要があります");
                            return true;
                        }
                        region.set(args[1]+".price",args[3]);
                        sendPrefixMessage(p,"§aid:"+args[1]+"の土地の値段を§d"+args[3]+"§aに設定しました");
                        break;

                }
        }
        RealEstate.region.saveConfig();
        return true;
    }


}
