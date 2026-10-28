package su.nightexpress.excellentcrates.crates.hologram;

import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.api.crate.Crate;
import su.nightexpress.excellentcrates.api.crate.block.position.CratePosition;
import su.nightexpress.excellentcrates.api.crate.block.position.CratePositionRegistry;
import su.nightexpress.excellentcrates.api.crate.component.CrateComponentKeys;
import su.nightexpress.excellentcrates.api.crate.hologram.HologramProvider;
import su.nightexpress.excellentcrates.api.crate.hologram.HologramTextProvider;
import su.nightexpress.excellentcrates.api.crate.hologram.component.HologramComponent;
import su.nightexpress.excellentcrates.api.crate.hologram.component.HologramOffset;
import su.nightexpress.excellentcrates.api.crate.placeholder.CratePlaceholders;
import su.nightexpress.excellentcrates.api.crate.registry.CrateRegistry;
import su.nightexpress.excellentcrates.core.util.RegionTasks;
import su.nightexpress.nightcore.util.LocationUtil;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;

@NullMarked
public class HologramDisplayService {

    private final CratesPlugin          plugin;
    private final HologramProvider      provider;
    private final CrateRegistry         crateRegistry;
    private final CratePositionRegistry cratePositionRegistry;
    private final CratePlaceholders     cratePlaceholders;

    public HologramDisplayService(CratesPlugin plugin,
                                  HologramProvider provider,
                                  CrateRegistry crateRegistry,
                                  CratePositionRegistry cratePositionRegistry,
                                  CratePlaceholders cratePlaceholders) {
        this.plugin = plugin;
        this.provider = provider;
        this.crateRegistry = crateRegistry;
        this.cratePositionRegistry = cratePositionRegistry;
        this.cratePlaceholders = cratePlaceholders;
    }

    public void removeAll() {
        this.provider.removeAll();
    }

    public void removeAll(Crate crate) {
        this.provider.remove(crate);
    }

    public void updateHolograms() {
        this.crateRegistry.values().forEach(this::update);
    }

    public void remake(Crate crate) {
        this.removeAll(crate);
        this.update(crate);
    }

    public void update(Crate crate) {
        HologramComponent hologramComponent = crate.getComponentOrNull(CrateComponentKeys.HOLOGRAM);
        if (hologramComponent == null || !hologramComponent.isEnabled()) {
            return;
        }

        Set<CratePosition> cratePositions = this.cratePositionRegistry.getCratePositions(crate.id());
        if (cratePositions.isEmpty()) return; // Skip if no positions are registered for this crate. The controller will handle the position (un)linking and update the holograms accordingly

        HologramTextProvider textProvider = player -> {
            List<String> text = hologramComponent.getText();
            PlaceholderContext placeholders = PlaceholderContext.builder()
                .apply(this.cratePlaceholders.allPlaceholders(crate, player))
                .andThen(CommonPlaceholders.forPlaceholderAPI(player))
                .build();

            return placeholders.apply(text);
        };

        HologramOffset offset = hologramComponent.getOffset();

        cratePositions.forEach(cratePosition -> {
            World world = Bukkit.getWorld(cratePosition.worldKey().bukkit());
            if (world == null) return; // Skip if world is not loaded, the controller will handle the (un)loading of worlds and update the holograms accordingly

            Location blockLocation = cratePosition.position().toLocation(world);
            Location hologramLocation = LocationUtil.setCenter3D(blockLocation)
                .add(offset.getX(), offset.getY(), offset.getZ());

            // Providers look up nearby players and resolve their placeholders, which the location's region owns.
            RegionTasks.runAt(this.plugin, hologramLocation, () -> {
                // A queued render must not recreate a hologram whose block was removed or disabled meanwhile.
                if (!crate.id().equals(this.cratePositionRegistry.getCrateIdAt(cratePosition))) return;
                if (!(crate.getComponentOrNull(CrateComponentKeys.HOLOGRAM) instanceof HologramComponent current) || !current.isEnabled()) return;

                this.provider.render(crate, cratePosition, hologramLocation, world, textProvider);
            });
        });
    }
}
