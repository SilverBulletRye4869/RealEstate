package silverassist.realestate;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import silverassist.realestate.command.Admin;
import silverassist.realestate.command.Normal;
import silverassist.realestate.event.AdminEvent;
import silverassist.realestate.event.NormalEvent;

public final class RealEstate extends JavaPlugin {
    public static JavaPlugin plugin = null;
    public static CustomConfig region = null;
    public static CustomConfig city = null;
    public static CustomConfig memo = null;
    
    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        region = new CustomConfig(this, "regions.yml");
        city = new CustomConfig(this,"city.yml");
        memo = new CustomConfig(this,"memo.yml");

        this.saveDefaultConfig();
        region.saveDefaultConfig();
        city.saveDefaultConfig();
        memo.saveDefaultConfig();

        PluginCommand command = getCommand("realestate");
        if(command!=null)command.setExecutor(new Normal());
        command = getCommand("realestate.admin");
        if(command!=null)command.setExecutor(new Admin());


        plugin.getServer().getPluginManager().registerEvents(new AdminEvent(),this);
        plugin.getServer().getPluginManager().registerEvents(new NormalEvent(),this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.saveConfig();
        region.saveConfig();
        city.saveConfig();
        memo.saveConfig();
    }
}
