package silverassist.realestate;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class Vault{
    private JavaPlugin plugin;  //PluginInstanceを格納しておく奴
    static final Logger log = Logger.getLogger("Minecraft");  //Log登録
    private static Economy econ = null;  //お金をいじるのに必要な奴を入れておくところ

    //コンストラクタ
    public Vault(JavaPlugin plugin){
        this.plugin = plugin;
    }

    //お金系のシステム起動
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
    }  //お金をいじるのに必要な奴
}
