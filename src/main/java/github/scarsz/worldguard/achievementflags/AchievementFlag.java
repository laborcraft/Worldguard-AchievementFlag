package github.scarsz.worldguard.achievementflags;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class AchievementFlag extends JavaPlugin implements Listener {

    private static StringFlag FLAG_ADVANCEMENT_REQUIRED;
    private static StringFlag FLAG_ADVANCEMENT_DENIED;

    private RegionQuery regionQuery;@Override

    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        FLAG_ADVANCEMENT_REQUIRED = registerFlag(registry, "advancement-required");
        FLAG_ADVANCEMENT_DENIED = registerFlag(registry, "advancement-denied");
    }

    @Override
    public void onEnable() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        regionQuery = container.createQuery();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMoveAdvancementRequired(PlayerMoveEvent event) {
        String advancementRaw = regionQuery.queryValue(
                BukkitAdapter.adapt(event.getTo() != null ? event.getTo() : event.getFrom()),
                WorldGuardPlugin.inst().wrapPlayer(event.getPlayer()),
                FLAG_ADVANCEMENT_REQUIRED
        );
        if (advancementRaw != null) {
            Advancement advancement = parseAdvancement(advancementRaw);
            if (advancement != null) {
                if (!event.getPlayer().getAdvancementProgress(advancement).isDone()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMoveAdvancementDenied(PlayerMoveEvent event) {
        String advancementRaw = regionQuery.queryValue(
                BukkitAdapter.adapt(event.getTo() != null ? event.getTo() : event.getFrom()),
                WorldGuardPlugin.inst().wrapPlayer(event.getPlayer()),
                FLAG_ADVANCEMENT_DENIED
        );
        if (advancementRaw != null) {
            Advancement advancement = parseAdvancement(advancementRaw);
            if (advancement != null) {
                if (event.getPlayer().getAdvancementProgress(advancement).isDone()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private <T extends Flag<?>> T registerFlag(FlagRegistry registry, String name) {
        try {
            StringFlag flag = new StringFlag(name);
            registry.register(flag);
            return (T) flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get(name);
            if (existing instanceof StringFlag) {
                return (T) existing;
            }
            return null;
        }
    }

    private Advancement parseAdvancement(String advancement) {
        String[] split = advancement.split(":", 2);
        Plugin plugin = Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .filter(p -> p.getName().equalsIgnoreCase(split[0]))
                .findFirst().orElse(null);

        return Bukkit.getAdvancement(split.length == 1
                ? NamespacedKey.minecraft(split[0])
                : plugin != null ? new NamespacedKey(plugin, split[1]) : NamespacedKey.minecraft(split[1])
        );
    }
}
