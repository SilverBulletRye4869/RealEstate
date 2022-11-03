package silverassist.realestate.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import silverassist.realestate.Action;

import static silverassist.realestate.Function.PREFIX;
import static silverassist.realestate.Function.hasPermission;

public class NormalEvent implements Listener {
    @EventHandler//ブロック破壊
    public void breakBlock(BlockBreakEvent e){
        Player p = e.getPlayer();
        if(p.isOp())return;
        if(hasPermission(p,e.getBlock().getLocation(), Action.BLOCK))return;
        p.sendMessage(PREFIX+"§cこのブロックを触る権限がありません");
        e.setCancelled(true);

    }

    @EventHandler//ブロック設置
    public void placeBlock(BlockPlaceEvent e){
        Player p = e.getPlayer();
        if(p.isOp())return;
        if(hasPermission(p,e.getBlock().getLocation(), Action.BLOCK))return;
        p.sendMessage(PREFIX+"§cこのブロックを触る権限がありません");
        e.setCancelled(true);
    }
}
