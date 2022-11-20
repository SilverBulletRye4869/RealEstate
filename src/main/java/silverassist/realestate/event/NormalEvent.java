package silverassist.realestate.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
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


    //5秒間値を保持しておくことにより軽量化
    private Map<Player,Map<ActionType,Boolean>> ActionFlag = new HashMap<>();
    private boolean isAllow(Player p,Location loc, ActionType action){
        FileConfiguration world =RealEstate.getWorldYml().getConfig();
        String worldUUID = loc.getWorld().getUID().toString();
        if(world.get(worldUUID)==null)return true; //ワールドについて記載がなければ許可
        if(!world.getBoolean(worldUUID+".enabled"))return true; //保護が有効化されてなければ許可

        if(ActionFlag.containsKey(p) && ActionFlag.get(p).containsKey(action)) return ActionFlag.get(p).get(action);
        Boolean allow = hasPermission(p,loc,action);
        ActionFlag.put(p,new HashMap<>(){{put(action,allow);}});
        JavaPlugin plugin = RealEstate.getInstance();
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                ActionFlag.get(p).remove(action);
            }
        },20 * plugin.getConfig().getInt("permission_save_time"));
        return allow;
    }

    //-----------------------------------------------------------------------ブロック破壊
    @EventHandler
    public void breakBlock(BlockBreakEvent e){
        Player p = e.getPlayer();
        Block block = e.getBlock();

        if(isAllow(p,block.getLocation(), ActionType.BLOCK)) {
            if(!(block.getState() instanceof Sign))return;
            Sign sign = (Sign) block.getState();
            if(!sign.getLine(0).contains(PREFIX))return;
            String id = sign.getLine(1).replace("§d§lid: ","");
            FileConfiguration region = RealEstate.getRegionYml().getConfig();
            if(region.get(id)==null)return;
            List<Location> locList =(List<Location>) region.getList(id+".sign");
            locList.remove(e.getBlock().getLocation());
            region.set(id+".sign",locList);
            RealEstate.getRegionYml().saveConfig();
            return;
        }
        sendPrefixMessage(p,"§cこのブロックを壊す権限がありません");
        e.setCancelled(true);

    }
    @EventHandler //空バケツによる採取
    public void bucketCatch(PlayerBucketEmptyEvent e){
        Player p = e.getPlayer();
        if(p.isOp())return;
        if(isAllow(p,e.getBlock().getLocation(),ActionType.BLOCK))return;
        sendPrefixMessage(p,"§cこの液体をすくう権限がありません");
        e.setCancelled(true);
    }

    //------------------------------------------------------------------------ブロック設置
    @EventHandler
    public void placeBlock(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (p.isOp()) return;
        if (isAllow(p, e.getBlock().getLocation(), ActionType.BLOCK)) return;
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
            if (isAllow(p, block.getLocation(), ActionType.CHEST)) return;
        }else{
            if (isAllow(p, block.getLocation(), ActionType.CLICK)) return;
        }
        sendPrefixMessage(p,"§cこのブロックを触る権限がありません");
        e.setCancelled(true);
    }

    //------------------------------------------------------------------------PVP系
    @EventHandler
    public void playerAttackEvent(EntityDamageByEntityEvent e){
        if(!(e.getDamager() instanceof Player && e.getEntity() instanceof  Player))return;
        Player p = (Player) e.getDamager();
        if(isAllow(p,p.getLocation(),ActionType.PVP))return;
        e.setCancelled(true);
        sendPrefixMessage(p,"§cあなたはここでプレイヤーにダメージを与える権限がありません");
    }

    //-------------------------------------------------------------------------アイテムpickUp系
    private Map<Player,Boolean> pickupMessageFlag = new LinkedHashMap<>();
    @EventHandler
    public void itemPickUpEvent(EntityPickupItemEvent e){
        if(!(e.getEntity() instanceof Player))return;
        Player p = (Player) e.getEntity();
        if(isAllow(p,p.getLocation(),ActionType.PICK_UP))return;
        e.setCancelled(true);
        sendPrefixMessage(p,"§cこのエリアのアイテムを拾う権限がありません");
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

        FileConfiguration region = RealEstate.getRegionYml().getConfig();
        if(region.get(id)==null)return;
        List<Location> locList = (region.get(id + ".sign") != null) ? (List<Location>)region.getList(id + ".sign") : new ArrayList<>();

        //看板が無いのに残っているか確認
        new ArrayList<>(locList).forEach(loc -> { //new ArrayListしないとエラー吐く！
                Block block = RealEstate.getInstance().getServer().getWorld(loc.getWorld().getName()).getBlockAt(loc);
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
        RealEstate.getRegionYml().saveConfig();
    }

    //看板クリック
    private void signClickEvent(PlayerInteractEvent e){
        Player p = e.getPlayer();

        Sign sign = (Sign)e .getClickedBlock().getState();
        String[] lines = sign.getLines();
        if(!lines[0].equals(PREFIX))return;
        String id = lines[1].replace("§d§lid: ","");
        FileConfiguration region = RealEstate.getRegionYml().getConfig();
        if(region.get(id)==null)return;

        sendPrefixMessage(p,"§e§l--------------[id:"+id+"の情報]--------------");
        sendPrefixMessage(p,"§6§l所有者: "+lines[2]);
        sendPrefixMessage(p,"§6§lステータス: §a§l"+lines[3]);
        sendPrefixMessage(p,"§6§l価格: §a§l"+region.get(id+".price")+RealEstate.getInstance().getConfig().get("money_unit"));
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
        if(region.getString(id+".status").equals("sale")){
            sendRunCommandMessage(p,"§c§l[この土地を買う！]","/re buy "+id);
        }
    }
}
