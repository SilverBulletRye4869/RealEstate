package silverassist.realestate;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Function {
    public static final String PREFIX = "§b§l[§e§lRealEstate§b§l]§r";
    public static String ADMIN_WAND = "§d§l領域指定斧";

    public static boolean areaCheck(Location loc, double[] s, double[] e, World w){
        if(loc.getWorld()!=w)return false;
        if(loc.getX() < s[0] || loc.getX()>e[0])return false;
        if(loc.getZ() < s[2] || loc.getZ()>e[2])return false;
        if(loc.getY() < s[1] || loc.getZ()>e[1])return false;
        return true;
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
