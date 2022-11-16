package silverassist.realestate;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import silverassist.realestate.command.Admin;
import silverassist.realestate.command.Normal;
import silverassist.realestate.event.AdminEvent;
import silverassist.realestate.event.NormalEvent;
import silverassist.realestate.menu.InvClick;

public final class RealEstate extends JavaPlugin {
    public static JavaPlugin plugin = null;
    public static CustomConfig region = null;
    public static CustomConfig city = null;
    public static CustomConfig memo = null;
    public static CustomConfig world =null;
    public static Vault vault = null;
    
    @Override
    public void onEnable() {
        vault = new Vault(this);
        if (!vault.setupEconomy() ) {
            vault.log.severe(String.format("[%s] プラグイン「Vault」が見つかりません！", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // Plugin startup logic
        plugin = this;
        region = new CustomConfig(this, "regions.yml");
        city = new CustomConfig(this,"city.yml");
        memo = new CustomConfig(this,"memo.yml");
        world = new CustomConfig(this,"world.yml");

        this.saveDefaultConfig();
        region.saveDefaultConfig();
        city.saveDefaultConfig();
        memo.saveDefaultConfig();
        world.saveDefaultConfig();

        PluginCommand command = getCommand("realestate");
        if(command!=null)command.setExecutor(new Normal());
        command = getCommand("realestate.admin");
        if(command!=null)command.setExecutor(new Admin());


        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new AdminEvent(),this);
        pm.registerEvents(new NormalEvent(),this);
        pm.registerEvents(new InvClick(),this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.saveConfig();
        if(region!=null)region.saveConfig();
        if(city!=null)city.saveConfig();
        if(memo!=null)memo.saveConfig();
        if(world!=null)world.saveConfig();
    }
}
