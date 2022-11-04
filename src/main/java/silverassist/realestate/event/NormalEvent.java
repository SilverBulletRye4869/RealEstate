package silverassist.realestate.event;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import silverassist.realestate.ActionType;

import java.util.List;

import static silverassist.realestate.Function.PREFIX;
import static silverassist.realestate.Function.*;

public class NormalEvent implements Listener {
    //チェスト系ブロック
    public final List<Material> chestBlock = List.of(
            Material.BARREL,
            Material.CHEST,
            Material.DISPENSER,
            Material.DROPPER,
            Material.FURNACE,
            Material.HOPPER,
            Material.SHULKER_BOX,
            Material.TRAPPED_CHEST
    );


    //-----------------------------------------------------------------------ブロック破壊
    @EventHandler
    public void breakBlock(BlockBreakEvent e){
        Player p = e.getPlayer();
        if(p.isOp())return;
        if(hasPermission(p,e.getBlock().getLocation(), ActionType.BLOCK))return;
        sendPrefixMessage(p,"§cこのブロックを壊す権限がありません");
        e.setCancelled(true);

    }
    @EventHandler //空バケツによる採取
    public void bucketCatch(PlayerBucketEmptyEvent e){
        Player p = e.getPlayer();
        if(p.isOp())return;
        if(hasPermission(p,e.getBlock().getLocation(),ActionType.BLOCK))return;
        sendPrefixMessage(p,"§cこの液体をすくう権限がありません");
        e.setCancelled(true);
    }

    //------------------------------------------------------------------------ブロック設置
    @EventHandler
    public void placeBlock(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (p.isOp()) return;
        if (hasPermission(p, e.getBlock().getLocation(), ActionType.BLOCK)) return;
        sendPrefixMessage(p,"§cここにブロックを設置する権限がありません");
        e.setCancelled(true);
    }

    //------------------------------------------------------------------------クリック系
    @EventHandler
    public void interactEvent(PlayerInteractEvent e){
        if(e.getAction()!= Action.RIGHT_CLICK_BLOCK)return;
        if(!e.hasBlock())return;

        Player p = e.getPlayer();
        if(p.isOp())return;
        Block block = e.getClickedBlock();
        if(this.chestBlock.contains(block.getType())) {
            if (hasPermission(p, block.getLocation(), ActionType.CHEST)) return;
        }else{
            if (hasPermission(p, block.getLocation(), ActionType.CLICK)) return;
        }
        sendPrefixMessage(p,"§cこのブロックを触る権限がありません");
        e.setCancelled(true);
    }


}
