package silverassist.realestate.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import silverassist.realestate.RealEstate;

import static silverassist.realestate.Function.*;

public class Normal implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))return true;
        Player p = (Player) sender;
        if(args.length==0){
            openMergeGui(p);
            return true;
        }
        FileConfiguration config = RealEstate.land.getConfig();

        Location loc;
        Player target;
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
                if(args.length<4){
                    //ヘルプ
                    sendPrefixMessage(p,"§c引数がたりません");
                    return true;
                }
                if(!isInt(args[2])){
                    sendPrefixMessage(p,"§c土地のidは整数で入力してください");
                    return true;
                }

                switch (args[1]){
                    //-----------------------------------------------------------------オーナ権の譲渡
                    case "setowner":
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
                    case "adduser":
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
                        sendPrefixMessage(p,"§a"+target.getDisplayName()+"を§did:"+args[2]+"§aの土地に登録しました");
                        break;

                }


        }
        RealEstate.land.saveConfig();
        return true;
    }

    private void openMergeGui(Player p){
        //近日実装
    }
}
