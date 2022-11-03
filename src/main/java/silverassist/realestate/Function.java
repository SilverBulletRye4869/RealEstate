package silverassist.realestate;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Function {
    public static final String PREFIX = "§b§l[§e§lRealEstate§b§l]§r";
    public static String ADMIN_WAND = "§d§l領域指定斧";

    public final static boolean areaCheck(Location loc, List<Double> s, List<Double> e, String w){
        if(!loc.getWorld().getName().equals(w))return false;
        if(loc.getX() < s.get(0) || loc.getX()>e.get(0))return false;
        if(loc.getZ() < s.get(2) || loc.getZ()>e.get(2))return false;
        if(loc.getY() < s.get(1) || loc.getY()>e.get(1))return false;
        return true;
    }

    public static List<String> getGuardList(Location loc){
        FileConfiguration config = RealEstate.plugin.getConfig();
        List<String> guardList = new LinkedList<>();

        //オーダを一番気おつけないといけないところ！
        config.getKeys(false).forEach(path ->{
            if(!areaCheck(loc, config.getDoubleList(path+".start"),config.getDoubleList(path+".end"),config.getString(path+".world")))return;
            guardList.add(path);
        });
        return guardList;
    }

    public static boolean hasPermission(Player p,Location loc,Action type){
        List<String> list = getGuardList(loc);
        if(list.size()==0){
            //ない場合はワールドの設定を見るようにする(近日改良)
        }
        UUID uuid = p.getUniqueId();
        FileConfiguration config = RealEstate.plugin.getConfig();

        AtomicBoolean allow = new AtomicBoolean(true);
        list.forEach(id -> {
            String s = new StringBuilder(Integer.toBinaryString(config.getInt(id+".user."+uuid))).reverse() + "0000000000";
            if(s.charAt(Action.ALL.getNum())=='1')return;
            if(s.charAt(type.getNum())=='0') allow.set(false);
        });
        return allow.get();
    }

    public static void sendPrefixMessage(Player p, String text){
        p.sendMessage(PREFIX+text);
    }

    public static boolean isAdmin(Player p){
        return p.hasPermission("realestate.admin");
    }

    public static void broadCast(String msg){
        RealEstate.plugin.getServer().broadcastMessage(PREFIX+msg);
    }

}
