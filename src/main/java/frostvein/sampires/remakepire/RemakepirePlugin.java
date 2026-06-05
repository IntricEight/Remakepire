package frostvein.sampires.remakepire;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import frostvein.sampires.remakepire.commands.BrigadierCommands;
import frostvein.sampires.remakepire.listeners.BatTransformationListener;
import frostvein.sampires.remakepire.listeners.BeaconConversionListener;
import frostvein.sampires.remakepire.listeners.BeaconTeleportListener;
import frostvein.sampires.remakepire.listeners.BeetrootHarvestListener;
import frostvein.sampires.remakepire.listeners.BeetrootListener;
import frostvein.sampires.remakepire.listeners.BlockListener;
import frostvein.sampires.remakepire.listeners.BloodMoonAttributeListener;
import frostvein.sampires.remakepire.listeners.CombatListener;
import frostvein.sampires.remakepire.listeners.ConfigGuiListener;
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;
import frostvein.sampires.remakepire.listeners.DamageSuppressionListener;
import frostvein.sampires.remakepire.listeners.DeathHandler;
import frostvein.sampires.remakepire.listeners.EndermanRemovalListener;
import frostvein.sampires.remakepire.listeners.ExperienceBottleListener;
import frostvein.sampires.remakepire.listeners.FeedingListener;
import frostvein.sampires.remakepire.listeners.ForcedCureChoiceListener;
import frostvein.sampires.remakepire.listeners.FourthBookRevealListener;
import frostvein.sampires.remakepire.listeners.InitGameListener;
import frostvein.sampires.remakepire.listeners.InteractionListener;
import frostvein.sampires.remakepire.listeners.IronWeaknessListener;
import frostvein.sampires.remakepire.listeners.MovementBoundaryListener;
import frostvein.sampires.remakepire.listeners.MountTeamsListener;
import frostvein.sampires.remakepire.listeners.NoSleepListener;
import frostvein.sampires.remakepire.listeners.PlayerJoinListener;
import frostvein.sampires.remakepire.listeners.ThirstEffectsListener;
import frostvein.sampires.remakepire.listeners.TomeListener;
import frostvein.sampires.remakepire.listeners.TomeVampireRestrictionListener;
import frostvein.sampires.remakepire.listeners.VampireCraftBlocker;
import frostvein.sampires.remakepire.listeners.VampireFallDamageListener;
import frostvein.sampires.remakepire.listeners.WeaponDropRemover;
import frostvein.sampires.remakepire.managers.BatTransformationManager;
import frostvein.sampires.remakepire.managers.BeaconMajorityManager;
import frostvein.sampires.remakepire.managers.BeaconManager;
import frostvein.sampires.remakepire.managers.BeetrootManager;
import frostvein.sampires.remakepire.managers.BloodMoonManager;
import frostvein.sampires.remakepire.managers.ConfigGuiManager;
import frostvein.sampires.remakepire.managers.ConfigManager;
import frostvein.sampires.remakepire.managers.CureBookManager;
import frostvein.sampires.remakepire.managers.EffectManager;
import frostvein.sampires.remakepire.managers.ForcedCureChoiceManager;
import frostvein.sampires.remakepire.managers.HolyWaterEffectManager;
import frostvein.sampires.remakepire.managers.InitGameManager;
import frostvein.sampires.remakepire.managers.MobTeamManager;
import frostvein.sampires.remakepire.managers.PassiveMobSpawningManager;
import frostvein.sampires.remakepire.managers.PermadeathManager;
import frostvein.sampires.remakepire.managers.PlayerChatManager;
import frostvein.sampires.remakepire.managers.SessionManager;
import frostvein.sampires.remakepire.managers.ThirstManager;
import frostvein.sampires.remakepire.managers.TomeDistributionManager;
import frostvein.sampires.remakepire.managers.TomeManager;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;
import frostvein.sampires.remakepire.managers.VampireFeedingManager;
import frostvein.sampires.remakepire.managers.VampireManager;
import frostvein.sampires.remakepire.managers.VampireSireManager;
import frostvein.sampires.remakepire.managers.VampireTexturePackManager;
import frostvein.sampires.remakepire.managers.VampireTrackingManager;
import frostvein.sampires.remakepire.managers.VampireTurningManager;

public final class RemakepirePlugin extends JavaPlugin {
    public static final String WORLD_NAME = "world";
    private ConfigManager configManager;
    private SessionManager sessionManager;
    private VampireManager vampireManager;
    private EffectManager effectManager;
    private DeathHandler deathHandler;
    private BloodMoonManager bloodMoonManager;
    private PlayerChatManager playerChatManager;
    private VampireAbilityManager vampireAbilityManager;
    private IronWeaknessListener ironWeaknessListener;
    private BeaconManager beaconManager;
    private BeetrootManager beetrootManager;
    private MobTeamManager mobTeamManager;
    private BatTransformationManager batTransformationManager;
    private ThirstManager thirstManager;
    private FeedingListener feedingListener;
    private ThirstEffectsListener thirstEffectsListener;
    private BeaconConversionListener beaconConversionListener;
    private BeaconTeleportListener beaconTeleportListener;
    private TomeManager tomeManager;
    private HolyWaterEffectManager holyWaterEffectManager;
    private VampireFeedingManager vampireFeedingManager;
    private BloodMoonAttributeListener bloodMoonAttributeListener;
    private BeaconMajorityManager beaconMajorityManager;
    private TomeVampireRestrictionListener tomeVampireRestrictionListener;
    private TomeDistributionManager tomeDistributionManager;
    private CureBookManager cureBookManager;
    private VampireTexturePackManager vampireTexturePackManager;
    private EndermanRemovalListener endermanRemovalListener;
    private DamageSuppressionListener damageSuppressionListener;
    private VampireTrackingManager vampireTrackingManager;
    private PermadeathManager permadeathManager;
    private PassiveMobSpawningManager passiveMobSpawningManager;
    private VampireTurningManager vampireTurningManager;
    private VampireSireManager sireManager;
    private ForcedCureChoiceManager forcedCureChoiceManager;
    private ConfigGuiManager configGuiManager;
    private InitGameManager initGameManager;
    private CureBookReadingListener cureBookReadingListener;
    private World world;
    private Team castTeam;
    private Team vampireCastTeam;
    private Location vampireRespawnLocation;
    private FileConfiguration textConfig;

    /**
     * Enable the Remakepires plugin on the server.
     */
    public void onEnable() {
        this.saveDefaultConfig();

        // Save the text-config file in the plugin data folder (If it does not already exist)
        saveResource("text-config.yml", false);
        this.loadTextConfig();

        this.configManager = new ConfigManager(this);
        this.world = Bukkit.getWorld(WORLD_NAME);
        this.initializeHumanCastTeam();
        this.initializeVampireCastTeam();
        this.sessionManager = new SessionManager(this);
        this.sessionManager.initializeScoreboard();
        this.sessionManager.startBackgroundTasks();
        this.vampireManager = new VampireManager(this);
        this.thirstManager = new ThirstManager(this);
        this.beaconManager = new BeaconManager(this);
        this.effectManager = new EffectManager(this);
        this.deathHandler = new DeathHandler(this);
        this.bloodMoonManager = new BloodMoonManager(this);
        this.ironWeaknessListener = new IronWeaknessListener(this);
        this.feedingListener = new FeedingListener(this);
        this.thirstEffectsListener = new ThirstEffectsListener(this);
        this.thirstEffectsListener.startTasks();
        this.playerChatManager = new PlayerChatManager(this);
        this.vampireAbilityManager = new VampireAbilityManager(this);
        this.beaconConversionListener = new BeaconConversionListener(this);
        this.beaconTeleportListener = new BeaconTeleportListener(this);
        this.beetrootManager = new BeetrootManager(this);
        this.mobTeamManager = new MobTeamManager(this);
        this.batTransformationManager = new BatTransformationManager(this);
        this.tomeManager = new TomeManager(this);
        this.holyWaterEffectManager = new HolyWaterEffectManager(this);
        this.vampireFeedingManager = new VampireFeedingManager(this);
        this.beaconMajorityManager = new BeaconMajorityManager(this);
        this.tomeDistributionManager = new TomeDistributionManager(this);
        this.cureBookManager = new CureBookManager(this);
        this.vampireTexturePackManager = new VampireTexturePackManager(this);
        this.endermanRemovalListener = new EndermanRemovalListener(this);
        this.damageSuppressionListener = new DamageSuppressionListener(this);
        this.vampireTrackingManager = new VampireTrackingManager(this);
        this.permadeathManager = new PermadeathManager(this);
        this.passiveMobSpawningManager = new PassiveMobSpawningManager(this);
        this.vampireTurningManager = new VampireTurningManager(this);
        this.sireManager = new VampireSireManager(this);
        this.forcedCureChoiceManager = new ForcedCureChoiceManager(this);
        this.configGuiManager = new ConfigGuiManager(this);

        this.initGameManager = new InitGameManager(this);
        this.getServer().getPluginManager().registerEvents(this.damageSuppressionListener, this);
        this.getServer().getPluginManager().registerEvents(this.deathHandler, this);
        this.getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ConfigGuiListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        this.getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        this.getServer().getPluginManager().registerEvents(new VampireCraftBlocker(this), this);
        this.getServer().getPluginManager().registerEvents(this.ironWeaknessListener, this);
        this.getServer().getPluginManager().registerEvents(this.feedingListener, this);
        this.getServer().getPluginManager().registerEvents(this.thirstEffectsListener, this);
        this.getServer().getPluginManager().registerEvents(new NoSleepListener(this), this);
        this.getServer().getPluginManager().registerEvents(this.playerChatManager, this);
        this.getServer().getPluginManager().registerEvents(new VampireFallDamageListener(this.vampireManager), this);
        this.getServer().getPluginManager().registerEvents(this.beaconConversionListener, this);
        this.getServer().getPluginManager().registerEvents(this.beaconTeleportListener, this);
        this.getServer().getPluginManager().registerEvents(new BeetrootListener(this), this);
        this.getServer().getPluginManager().registerEvents(new WeaponDropRemover(this), this);
        this.getServer().getPluginManager().registerEvents(new InteractionListener(this), this);
        this.getServer().getPluginManager().registerEvents(new BatTransformationListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ExperienceBottleListener(this), this);

        this.cureBookReadingListener = new CureBookReadingListener(this);
        this.getServer().getPluginManager().registerEvents(this.cureBookReadingListener, this);
        this.getServer().getPluginManager().registerEvents(new TomeListener(this), this);
        this.getServer().getPluginManager().registerEvents(new BeetrootHarvestListener(this), this);

        this.tomeVampireRestrictionListener = new TomeVampireRestrictionListener(this);
        this.getServer().getPluginManager().registerEvents(this.tomeVampireRestrictionListener, this);
        this.getServer().getPluginManager().registerEvents(this.endermanRemovalListener, this);
        this.getServer().getPluginManager().registerEvents(new MovementBoundaryListener(this), this);
        this.getServer().getPluginManager().registerEvents(new MountTeamsListener(this), this);
        this.getServer().getPluginManager().registerEvents(new FourthBookRevealListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ForcedCureChoiceListener(this), this);
        this.getServer().getPluginManager().registerEvents(new InitGameListener(this), this);
        this.bloodMoonAttributeListener = new BloodMoonAttributeListener(this);
        this.getServer().getPluginManager().registerEvents(this.bloodMoonAttributeListener, this);

        BrigadierCommands brigadierCommands = new BrigadierCommands(this);
        brigadierCommands.registerAll();

        this.initializeDeathScoreboard();
        this.effectManager.startEffectTask();
        this.beaconManager.validateBeacons();
        this.initVampireRespawnLocation();
        this.sessionManager.executeServerCommand("tick freeze");
        this.logInfo("Remakepire Plugin has been enabled!");
    }

    /**
     * Disable the Remakepires plugin on the server.
     */
    public void onDisable() {
        if (this.effectManager != null) {
            this.effectManager.stopEffectTask();
            this.effectManager.shutdown();
        }

        if (this.thirstManager != null) {
            this.thirstManager.shutdown();
        }

        if (this.vampireManager != null) {
            this.vampireManager.shutdown();
        }

        if (this.sessionManager.isSessionActive()) {
            this.sessionManager.pauseSession();
        } else if (this.sessionManager.getSessionState() == 3) {
            this.sessionManager.primeNewSession();
        }

        if (this.vampireAbilityManager != null) {
            this.vampireAbilityManager.shutdown();
        }

        if (this.bloodMoonManager != null) {
            this.bloodMoonManager.shutdown();
        }

        if (this.beaconManager != null) {
            this.beaconManager.shutdown();
        }

        if (this.beaconConversionListener != null) {
            this.beaconConversionListener.shutdown();
        }

        if (this.beetrootManager != null) {
            this.beetrootManager.shutdown();
        }

        if (this.tomeDistributionManager != null) {
            this.tomeDistributionManager.shutdown();
        }

        if (this.passiveMobSpawningManager != null) {
            this.passiveMobSpawningManager.shutdown();
        }

        if (this.mobTeamManager != null) {
            this.mobTeamManager.shutdown();
        }

        if (this.batTransformationManager != null) {
            this.batTransformationManager.shutdown();
        }

        if (this.tomeManager != null) {
            this.tomeManager.shutdown();
        }

        if (this.holyWaterEffectManager != null) {
            this.holyWaterEffectManager.shutdown();
        }

        if (this.vampireFeedingManager != null) {
            this.vampireFeedingManager.shutdown();
        }

        if (this.bloodMoonAttributeListener != null) {
            this.bloodMoonAttributeListener.shutdown();
        }

        if (this.beaconMajorityManager != null) {
            this.beaconMajorityManager.shutdown();
        }

        if (this.vampireTexturePackManager != null) {
            this.vampireTexturePackManager.shutdown();
        }

        if (this.endermanRemovalListener != null) {
            this.endermanRemovalListener.shutdown();
        }

        if (this.vampireTrackingManager != null) {
            this.vampireTrackingManager.shutdown();
        }

        if (this.permadeathManager != null) {
            this.permadeathManager.shutdown();
        }

        if (this.vampireTurningManager != null) {
            this.vampireTurningManager.shutdown();
        }

        if (this.sireManager != null) {
            this.sireManager.shutdown();
        }

        if (this.forcedCureChoiceManager != null) {
            this.forcedCureChoiceManager.shutdown();
        }

        if (this.configGuiManager != null) {
            this.configGuiManager.shutdown();
        }

        this.logInfo("Remakepire Plugin has been disabled!");
    }

    /**
     * Retrieve the text configuration file, used for modifying text readouts such as the contents of cure books, or border messages.
     *
     * @return A file of configuration options.
     */
    public FileConfiguration getTextConfig() {
        return textConfig;
    }

    /**
     * Load the text configuration file into the project resources.
     */
    private void loadTextConfig() {
        File textConfigFile = new File(getDataFolder(), "text-config.yml");

        if (!textConfigFile.exists()) {
            saveResource("text-config.yml", false);
        }

        textConfig = YamlConfiguration.loadConfiguration(textConfigFile);
    }

    /**
     * Log the message to the console. The message may not appear if nonessential logging is disabled.
     *
     * @param message the message being sent to the log.
     */
    public void logInfo(String message) {
        if (!this.getConfigManager().isNonEssentialLoggingDisabled()) {
            this.getLogger().info(message);
        }
    }

    /**
     * Create the human team for players to be assigned to.
     */
    private void initializeHumanCastTeam() {
        try {
            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team existingTeam = mainScoreboard.getTeam("CastTeam");

            if (existingTeam != null) {
                this.castTeam = existingTeam;
                this.logInfo("Found existing CastTeam, updating settings...");
            } else {
                this.castTeam = mainScoreboard.registerNewTeam("CastTeam");
                this.logInfo("Created new CastTeam for name tag management.");
            }

            this.castTeam.setNameTagVisibility(NameTagVisibility.NEVER);
            this.castTeam.setDisplayName("§6Human Team");
            this.castTeam.setCanSeeFriendlyInvisibles(false);

            this.logInfo("CastTeam initialized successfully with hidden name tags.");

        } catch (Exception e) {
            this.getLogger().severe("Failed to initialize CastTeam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create the scoreboard for tracking each player's death counter.
     */
    private void initializeDeathScoreboard() {
        try {
            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String objectiveName = "vsmp_death";
            Objective existingObjective = mainScoreboard.getObjective(objectiveName);

            if (existingObjective != null) {
                String criteria = existingObjective.getCriteria();

                if ("deathCount".equals(criteria)) {
                    this.logInfo("Migrating death scoreboard from 'deathCount' to 'dummy' criteria...");
                    existingObjective.unregister();
                    mainScoreboard.registerNewObjective(objectiveName, "dummy", "Deaths");
                    this.logInfo("Migration complete - death scoreboard now uses 'dummy' criteria.");
                } else {
                    this.logInfo("Found existing death scoreboard objective with correct criteria.");
                }
            } else {
                mainScoreboard.registerNewObjective(objectiveName, "dummy", "Deaths");
                this.logInfo("Created new death scoreboard objective with 'dummy' criteria.");
            }
        } catch (Exception e) {
            this.getLogger().severe("Failed to initialize death scoreboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create the vampire team for players to be assigned to.
     */
    private void initializeVampireCastTeam() {
        try {
            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team existingTeam = mainScoreboard.getTeam("VampireCastTeam");

            if (existingTeam != null) {
                this.vampireCastTeam = existingTeam;
                this.logInfo("Found existing VampireCastTeam, updating settings...");
            } else {
                this.vampireCastTeam = mainScoreboard.registerNewTeam("VampireCastTeam");
                this.logInfo("Created new VampireCastTeam for name tag management.");
            }

            this.vampireCastTeam.setNameTagVisibility(NameTagVisibility.NEVER);
            this.vampireCastTeam.setCanSeeFriendlyInvisibles(false);
            this.vampireCastTeam.setDisplayName("§4Vampire Team");

            this.logInfo("VampireCastTeam initialized successfully with hidden name tags.");

        } catch (Exception e) {
            this.getLogger().severe("Failed to initialize VampireCastTeam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Set up the vampire team's respawn location.
     */
    private void initVampireRespawnLocation() {
        this.vampireRespawnLocation = this.configManager.getVampireRespawnLocation(this.getWorld());
        this.logInfo("Vampire respawn location set to: " + this.vampireRespawnLocation.getBlockX() + ", " + this.vampireRespawnLocation.getBlockY() + ", " + this.vampireRespawnLocation.getBlockZ());
    }

    /**
     * Retrieve the vampire respawn location.
     *
     * @return The {@code Location} where vampires respawn after death.
     */
    public Location getVampireRespawnLocation() {
        return this.vampireRespawnLocation;
    }

    /**
     * Set up the vampire team's respawn location.
     */
    public void reloadVampireRespawnLocation() {
        this.initVampireRespawnLocation();
    }

    /**
     * Retrieve the team of human players.
     *
     * @return The human {@code Team}.
     */
    public Team getCastTeam() {
        return this.castTeam;
    }

    /**
     * Retrieve the team of vampire players.
     *
     * @return The vampire {@code Team}.
     */
    public Team getVampireCastTeam() {
        return this.vampireCastTeam;
    }

    /**
     * Retrieve the game world.
     *
     * @return The world being used by the server.
     */
    public World getWorld() {
        return this.world;
    }

    public BatTransformationManager getBatTransformationManager() {
        return this.batTransformationManager;
    }

    public BeetrootManager getBeetrootManager() {
        return this.beetrootManager;
    }

    public PlayerChatManager getPlayerChatManager() {
        return this.playerChatManager;
    }

    public VampireAbilityManager getVampireAbilityManager() {
        return this.vampireAbilityManager;
    }

    public IronWeaknessListener getIronWeaknessListener() {
        return this.ironWeaknessListener;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public SessionManager getSessionManager() {
        return this.sessionManager;
    }

    public VampireManager getVampireManager() {
        return this.vampireManager;
    }

    public EffectManager getEffectManager() {
        return this.effectManager;
    }

    public DeathHandler getDeathHandler() {
        return this.deathHandler;
    }

    public BeaconManager getBeaconManager() {
        return this.beaconManager;
    }

    public BeaconConversionListener getBeaconConversionListener() {
        return this.beaconConversionListener;
    }

    public BeaconTeleportListener getBeaconTeleportListener() {
        return this.beaconTeleportListener;
    }

    public ThirstManager getThirstManager() {
        return this.thirstManager;
    }

    public FeedingListener getFeedingListener() {
        return this.feedingListener;
    }

    public VampireFeedingManager getVampireFeedingManager() {
        return this.vampireFeedingManager;
    }

    public ThirstEffectsListener getThirstEffectsListener() {
        return this.thirstEffectsListener;
    }

    public TomeManager getTomeManager() {
        return this.tomeManager;
    }

    public HolyWaterEffectManager getHolyWaterEffectManager() {
        return this.holyWaterEffectManager;
    }

    public BloodMoonAttributeListener getBloodMoonAttributeListener() {
        return this.bloodMoonAttributeListener;
    }

    public BloodMoonManager getBloodMoonManager() {
        return this.bloodMoonManager;
    }

    public BeaconMajorityManager getBeaconMajorityManager() {
        return this.beaconMajorityManager;
    }

    public MobTeamManager getMobTeamManager() {
        return this.mobTeamManager;
    }

    public TomeVampireRestrictionListener getTomeVampireRestrictionListener() {
        return this.tomeVampireRestrictionListener;
    }

    public TomeDistributionManager getTomeDistributionManager() {
        return this.tomeDistributionManager;
    }

    public CureBookManager getCureBookManager() {
        return this.cureBookManager;
    }

    public VampireTexturePackManager getVampireTexturePackManager() {
        return this.vampireTexturePackManager;
    }

    public VampireTrackingManager getVampireTrackingManager() {
        return this.vampireTrackingManager;
    }

    public PermadeathManager getPermadeathManager() {
        return this.permadeathManager;
    }

    public PassiveMobSpawningManager getPassiveMobSpawningManager() {
        return this.passiveMobSpawningManager;
    }

    public EndermanRemovalListener getEndermanRemovalListener() {
        return this.endermanRemovalListener;
    }

    public VampireTurningManager getVampireTurningManager() {
        return this.vampireTurningManager;
    }

    public VampireSireManager getSireManager() {
        return this.sireManager;
    }

    public ForcedCureChoiceManager getForcedCureChoiceManager() {
        return this.forcedCureChoiceManager;
    }

    public ConfigGuiManager getConfigGuiManager() {
        return this.configGuiManager;
    }

    public InitGameManager getInitGameManager() {
        return this.initGameManager;
    }

    public CureBookReadingListener getCureBookReadingListener() {
        return this.cureBookReadingListener;
    }
}