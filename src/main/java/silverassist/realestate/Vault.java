package silverassist.realestate;


import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class Vault{
    private JavaPlugin plugin;
    static final Logger log = Logger.getLogger("Minecraft");
    private static Economy econ = null;

    public Vault(JavaPlugin plugin){
        this.plugin = plugin;
    }

    boolean setupEconomy() {
        if (this.plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = this.plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }


    public static Economy getEconomy() {
        return econ;
    }

}
