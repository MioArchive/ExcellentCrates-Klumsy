package su.nightexpress.excellentcrates.effect.instance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.api.crate.block.position.CratePosition;
import su.nightexpress.excellentcrates.api.effect.EffectInstance;
import su.nightexpress.excellentcrates.api.effect.EffectRegistry;
import su.nightexpress.excellentcrates.core.util.RegionTasks;

@NullMarked
public class EffectInstanceService {

    private final CratesPlugin   plugin;
    private final EffectRegistry effectRegistry;

    private final Map<CratePosition, EffectInstance> activeEffects = new ConcurrentHashMap<>();

    public EffectInstanceService(CratesPlugin plugin, EffectRegistry effectRegistry) {
        this.plugin = plugin;
        this.effectRegistry = effectRegistry;
    }

    public void tick() {
        this.activeEffects.forEach((position, instance) -> {
            World world = Bukkit.getWorld(position.worldKey().bukkit());
            if (world == null) return;

            RegionTasks.runAt(this.plugin, position.position().toLocation(world), () -> instance.tick(this.effectRegistry));
        });
    }

    public void registerInstance(CratePosition position, EffectInstance instance) {
        this.activeEffects.put(position, instance);
    }

    public void unregisterInstance(CratePosition position) {
        this.activeEffects.remove(position);
    }
}
