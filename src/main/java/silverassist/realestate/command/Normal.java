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

import static silverassist.realestate.Function.*;

public class Normal implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))return true;
        Player p = (Player) sender;
        if(args.length<2)return true;

        //=======================================================config読み込み
        FileConfiguration region = RealEstate.getRegionYml().getConfig();
        FileConfiguration config = RealEstate.getInstance().getConfig();
        FileConfiguration city = RealEstate.getCityYml().getConfig();

        Location loc;
        OfflinePlayer target;
        String id = args[1];

        //コマンドの引数によって分離
        switch (args[0]){
            //================================================================================================購入コマンド
            case "buy":
                if(!isInt(id))return true;  //土地のidが不正ならreturn
                if(region.get(id)==null){  //土地が存在しているか
                    sendPrefixMessage(p,"§c土地が存在しません！");
                    return true;
                } else if(!region.get(id+".status").equals("sale")){  //売却状態か
                    sendPrefixMessage(p,"§c現在この土地を購入することはできません");
                    return true;
                }
                int money = Integer.valueOf(region.getString(id+".price"));  //土地の価格を取得
                Economy economy = RealEstate.getVault().getEconomy();  //プレイヤーの所持金を取得
                if(economy.getBalance(p) < money){  //所持金が足りるか
                    sendPrefixMessage(p,"§c所持金がたりません！");
                    return true;
                }

                if(args.length==2 || !args[2].equals("confirm")){  //最終確認
                    sendBuyCheckMessage(p,id);
                    return true;
                }

                economy.withdrawPlayer(p, money);  //代金支払い
                OfflinePlayer owner = getOwner(id);  //旧Owner取得
                if(owner!=null){  //ownerが居れば実行
                    economy.withdrawPlayer(owner,money);  //代金振り込み
                    if(owner.isOnline()) sendPrefixMessage((Player) owner,"§d§lid:"+id+"§a§lの土地が購入されました！");  //オンラインなら変れたことを通知
                }
                region.set(id+".owner",p.getUniqueId().toString());  //新Owner反映
                region.set(id+".status","protect");  //保護状態にする
                sendPrefixMessage(p,"§d§lid:"+id+"§a§lの土地を§d§l"+money+ (config.getString("money_unit")) +"§a§lで購入しました");
                break;

            //==============================================================================================土地へのテレポート
            case "tp":
            case "teleport":
                if(!p.hasPermission("realestate.tp")){
                    sendPrefixMessage(p,"§cあなたは土地にテレポートする権限がありません");
                    return true;
                }
                if(!isInt(id)){
                    sendPrefixMessage(p,"§c土地のidは自然数で入力してください");
                    return true;
                }
                p.teleport(region.getLocation(args[1]+".home"));
                break;
            //===============================================================================================土地の情報
            case "info":
                //近日改良予定
                break;
            //===============================================================================================土地の管理
            case "manage":
                if(!isInt(id)){  //土地のidが不正か？
                    sendPrefixMessage(p,"§c土地のidは整数で入力してください");
                    return true;
                }else if(region.get(id)==null){  //土地が存在しているか？
                    sendPrefixMessage(p,"§c指定したidの土地が見つかりません");
                    return true;
                }
                if(!p.isOp() && region.getString(id+".status").equals("frozen")){  //凍結されていないか(opはスルー)
                    sendPrefixMessage(p,"§cこの土地は凍結されています！");
                    return true;
                }

                //管理内容が指定されていなければホームを開く
                if(args.length<3){
                    if(!isAdmin(p,id)){  //管理権限があるか？
                        sendPrefixMessage(p,"§cあなたはこの土地を管理する権限がありません！");
                        return true;
                    }
                    InvMain.openManageGui(p,"region."+id+".def");  //GUIを開く
                    break;
                }

                //管理内容が指定されている場合
                switch (args[2]){
                    //===============================================================================オーナ権の譲渡
                    case "setowner":
                        if(args.length<4)return true;
                        if(!isOwner(p,id)){  //その土地のownerか？
                            sendPrefixMessage(p,"§cあなたはその土地のオーナではありません");
                            return true;
                        }
                        target = Bukkit.getPlayer(args[3]);
                        if(target ==null){ //ownerがいるか？
                            sendPrefixMessage(p,"§cプレイヤー名が間違っているかオフラインです");
                            return true;
                        }
                        if(args.length==4||!args[4].equals("confirm")){  //最終確認
                            sendPrefixMessage(p,"§c------------------【警告】------------------");
                            sendPrefixMessage(p,"§6本当にオーナ権限を譲渡する場合は次のコマンドを実行してください");
                            sendPrefixMessage(p,"§c/re manage "+id+" setowner "+ args[3] +" confirm");
                            sendSuggestMessage(p,"§d§l[ここをクリックして一部自動入力]","/re manage "+id+" setowner"+ args[3]);
                            return true;
                        }

                        region.set(id+".owner",target.getUniqueId().toString());  //owner変更
                        sendPrefixMessage(p,"§a"+args[3]+"を§did"+id+"§aのオーナーとして登録しました");
                        break;
                    //==============================================================================住人追加
                    case "adduser":
                        if(args.length<4)return true;
                        if(!isAdmin(p,id)){  //管理権限があるか
                            sendPrefixMessage(p,"§cあなたはその土地を管理する権限がありません");
                            return true;
                        }
                        String belongCity = region.getString(id+".city");  //所属している土地を取得
                        int max_user = city.getString(belongCity+".maxUser") != null ? city.getInt(belongCity+".maxUser") : config.getInt("default_maxUser");  //土地の上限人数を取得
                        if(region.get(id+".user")!=null && region.getConfigurationSection(id+".user").getKeys(false).size() >= max_user){  //最大人数かどうか
                            sendPrefixMessage(p,"§cこの土地にはこれ以上プレイヤーを追加することはできません");
                            return true;
                        }
                        target = Bukkit.getPlayer(args[3]);
                        //プレイヤーがいるか
                        if(target==null){
                            sendPrefixMessage(p,"§cプレイヤー名が間違っているかオフラインです");
                            return true;
                        }
                        //既に住人になっているか？
                        if(region.getInt(id+".user."+target.getUniqueId()) !=0){
                            sendPrefixMessage(p,"§cそのプレイヤーは既に登録されています");
                            return true;
                        }
                        region.set(id+".user."+target.getUniqueId(),region.getInt(id+".default"));//土地のデフォルト設定を反映させる
                        sendPrefixMessage(p,"§a"+target.getName()+"を§did:"+id+"§aの土地に登録しました");
                        break;
                    //=================================================================================住人退去
                    case "removeuser":
                        if(args.length<4)return true;
                        if(!isAdmin(p,id)){  //管理権限があるか？
                            sendPrefixMessage(p,"§cあなたはその土地を管理する権限がありません");
                            return true;
                        }
                        target = Bukkit.getOfflinePlayer(args[3]);
                        //入居しているか？
                        if(!region.getConfigurationSection(id+".user").getKeys(false).contains(target.getUniqueId().toString())){
                            sendPrefixMessage(p,"§cその人は入居していません");
                            return true;
                        }
                        //AdminはAdminを退去させられない
                        if(!isOwner(p,id)&&isAdmin(target,id)){
                            sendPrefixMessage(p,"§cあなたはこの人を退去させることはできません");
                            return true;
                        }
                        region.set(id+".user."+target.getUniqueId(),null);  //反映
                        sendPrefixMessage(p,"§a"+target.getName()+"を§did:"+id+"§aの土地から削除しました");
                        break;

                    //=================================================================================権限設定
                    case "setpermission":
                        if(args.length<5)return true;
                        if(!isAdmin(p,id)){  //管理権限があるか？
                            sendPrefixMessage(p,"§cあなたはその土地を管理する権限がありません");
                            return true;
                        }
                        target = Bukkit.getOfflinePlayer(args[3]);
                        //住人かどうか
                        if(region.getString(id+".user."+target.getUniqueId())==null){
                            sendPrefixMessage(p,"§cそのプレイヤーは住人として登録されていません");
                            return true;
                        }
                        //パーミッション値が正常か？
                        if(args[4].length()>10||Integer.parseInt(args[4])<0){
                            sendPrefixMessage(p,"§cパーミッション値が大きすぎまたは負です！");
                            return true;
                        }
                        int perm = Integer.parseInt(args[4]);
                        //Owner以外は管理権限を与えれない
                        if(perm%2==1 && !isOwner(p,id)){
                            sendPrefixMessage(p,"§cあなたはこのパーミッション値にする権限がありません");
                            return true;
                        }
                        region.set(args[1]+".user."+target.getUniqueId(),perm);  //反映
                        sendPrefixMessage(p,"§d"+args[3]+"§aのパーミッション値を§d"+perm+"§aに設定しました");
                        break;

                    //==================================================================================値段設定
                    case "setprice":
                        if(args.length<4)return true;
                        if(!isOwner(p,id)){  //Ownerか？
                            sendPrefixMessage(p,"§cこの土地の値段を設定する権限がありません");
                            return true;
                        }
                        //値段チェック
                        if(args[3].length()>10){
                            sendPrefixMessage(p,"§c値段が高すぎます！");
                            return true;
                        }
                        int price = Integer.parseInt(args[3]);
                        if(price<0){
                            sendPrefixMessage(p,"§c値段は0以上の自然数にする必要があります");
                            return true;
                        }
                        region.set(args[1]+".price",args[3]);  //反映
                        sendPrefixMessage(p,"§aid:"+id+"の土地の値段を§d"+args[3]+"§aに設定しました");
                        break;

                }
        }
        RealEstate.getRegionYml().saveConfig();  //保存
        editRegionSign(id);  //看板更新
        return true;
    }


}
