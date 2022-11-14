package silverassist.realestate.event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import silverassist.realestate.ActionType;
import silverassist.realestate.RealEstate;

import java.util.*;
import java.util.regex.Pattern;

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
        Block block = e.getBlock();
        if(hasPermission(p,block.getLocation(), ActionType.BLOCK)) {
            if(!(block.getState() instanceof Sign))return;
            Sign sign = (Sign) block.getState();
            if(!sign.getLine(0).contains(PREFIX))return;
            String id = sign.getLine(1).replace("§d§lid: ","");
            FileConfiguration region = RealEstate.region.getConfig();
            if(region.get(id)==null)return;
            List<Location> locList =(List<Location>) region.getList(id+".sign");
            locList.remove(e.getBlock().getLocation());
            region.set(id+".sign",locList);
            RealEstate.region.saveConfig();
            return;
        }
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
    //通常クリック
    public void interactEvent(PlayerInteractEvent e){
        if(e.getAction()!= Action.RIGHT_CLICK_BLOCK)return;
        if(!e.hasBlock())return;
        if(e.getClickedBlock().getState() instanceof Sign)signClickEvent(e);
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



    //-------------------------------------------------------------------土地看板
    private final Pattern REGION_SIGN = Pattern.compile("id:\\d*");

    @EventHandler
    //看板製作
    public void signPlaceEvent(SignChangeEvent e){
        String[] line = e.getLines();
        if(line.length==0)return;
        if(!REGION_SIGN.matcher(line[0]).matches())return;
        String id = line[0].replace("id:","");

        FileConfiguration region = RealEstate.region.getConfig();
        if(region.get(id)==null)return;
        List<Location> locList = new ArrayList<>((List<Location>)region.getList(id + ".sign"));

        //看板が無いのに残っているか確認
        new ArrayList<>(locList).forEach(loc -> { //new ArrayListしないとエラー吐く！
                Block block = RealEstate.plugin.getServer().getWorld(loc.getWorld().getName()).getBlockAt(loc);
                if(block==null)return;
                BlockState state = block.getState();
                if (state instanceof Sign && ((Sign) state).getLine(0).equals(PREFIX)) return;
                locList.remove(loc);
        });
        Player p = e.getPlayer();
        if(!isAdmin(p,id)){
            sendPrefixMessage(p,"§c管理権限を持っていない土地の看板は置けません！");
            return;
        }
        if(locList.size()>2){
            sendPrefixMessage(p,"§c土地の看板は3つまでしか認識されません");
            return;
        }
        setRegionSign((Sign) e.getBlock().getState(), id);
        Location loc = e.getBlock().getLocation();
        if(!locList.contains(loc))locList.add(e.getBlock().getLocation());
        region.set(id+".sign",locList);
        RealEstate.region.saveConfig();
    }

    //看板クリック
    private void signClickEvent(PlayerInteractEvent e){
        Player p = e.getPlayer();

        Sign sign = (Sign)e .getClickedBlock().getState();
        String[] lines = sign.getLines();
        if(!lines[0].equals(PREFIX))return;
        String id = lines[1].replace("§d§lid: ","");
        FileConfiguration region = RealEstate.region.getConfig();
        if(region.get(id)==null)return;

        sendPrefixMessage(p,"§e§l-------[id:"+id+"の情報]-------");
        OfflinePlayer owner = getOwner(id);
        sendPrefixMessage(p,"§6§l所有者: "+lines[2]);
        sendPrefixMessage(p,"§6§lステータス: §a§l"+lines[3]);
        sendPrefixMessage(p,"§6§l価格: §a§l"+region.get(id+".price")+RealEstate.plugin.getConfig().get("money_unit"));
        Set<String> members = new HashSet<>();
        if(region.getConfigurationSection(id+".user") != null)members = region.getConfigurationSection(id+".user").getKeys(false);
        sendPrefixMessage(p,"§6§l住人の数: §a§l"+members.size()+"人");
        if(p.isOp()){
            Iterator members_iterator = members.iterator();
            int cnt = 1;
            while (members_iterator.hasNext()){
                String target = members_iterator.next().toString();
                sendPrefixMessage(p,"§c§l住人("+cnt+"): §a§l"+getPlayer(target).getName()+"§d§l["+region.get(id+".user."+target)+"]");
                cnt++;
            }
        }
        sendPrefixMessage(p,"§e§l--------------------------------------");
    }
}
