package silverassist.realestate.menu;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvFunc {
    public static Map<Player,String> curInv = new HashMap<>();

    public static List<String> splitType(String type){
        return List.of(type.split("\\."));
    }
}
