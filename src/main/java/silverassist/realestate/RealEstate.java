package silverassist.realestate;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import silverassist.realestate.command.Admin;
import silverassist.realestate.command.Normal;
import silverassist.realestate.event.AdminEvent;

public final class RealEstate extends JavaPlugin {
    public static JavaPlugin plugin = null;
    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        this.saveDefaultConfig();
        PluginCommand command = getCommand("realestate");
        if(command!=null)command.setExecutor(new Normal());
        command = getCommand("realestate.admin");
        if(command!=null)command.setExecutor(new Admin());

        plugin.getServer().getPluginManager().registerEvents(new AdminEvent(),this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}