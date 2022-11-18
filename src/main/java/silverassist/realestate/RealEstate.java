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
    private static JavaPlugin plugin = null;  //このプラグインのインスタンスを格納しておくところ
    private static CustomConfig region = null;  //土地の情報を登録しておくyml
    private static CustomConfig city = null;  //都市の情報を登録しておくyml
    private static CustomConfig memo = null;  //処理量削減のためのyml (悲しいメモリを添えて)
    private static CustomConfig world =null;  //ワールドデータを登録しておくyml
    private static Vault vault = null;  //VaultAPIを使う単に必要な奴
    
    @Override
    public void onEnable() {

        //================================================================VaultPlugin起動
        vault = new Vault(this);
        if (!vault.setupEconomy() ) {
            vault.log.severe(String.format("[%s] プラグイン「Vault」が見つかりません！", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        //================================================================インスタンスを格納
        plugin = this;
        //===============================================================yml登録
        region = new CustomConfig(this, "regions.yml");
        city = new CustomConfig(this,"city.yml");
        memo = new CustomConfig(this,"memo.yml");
        world = new CustomConfig(this,"world.yml");

        this.saveDefaultConfig();
        region.saveDefaultConfig();
        city.saveDefaultConfig();
        memo.saveDefaultConfig();
        world.saveDefaultConfig();

        //==============================================================コマンド登録
        PluginCommand command = getCommand("realestate");
        if(command!=null)command.setExecutor(new Normal());
        command = getCommand("realestate.admin");
        if(command!=null)command.setExecutor(new Admin());

        //===============================================================イベント登録
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new AdminEvent(),this);
        pm.registerEvents(new NormalEvent(),this);
        pm.registerEvents(new InvClick(),this);
    }

    @Override
    public void onDisable() {
        //=======================================================yml全保存
        this.saveConfig();
        if(region!=null)region.saveConfig();
        if(city!=null)city.saveConfig();
        if(memo!=null)memo.saveConfig();
        if(world!=null)world.saveConfig();
    }

    //Instanceを取得
    public static JavaPlugin getInstance(){
        return plugin;
    }

    //Region.ymlを取得
    public static CustomConfig getRegionYml(){
        return region;
    }

    //memo.ymlを取得
    public static CustomConfig getMemoYml(){
        return memo;
    }

    //city.ymlを取得
    public static CustomConfig getCityYml(){
        return city;
    }

    //world.ymlを取得
    public static CustomConfig getWorldYml(){
        return world;
    }

    //VaultAPIを使うのに必要な奴を取得
    public static Vault getVault(){
        return vault;
    }
}
