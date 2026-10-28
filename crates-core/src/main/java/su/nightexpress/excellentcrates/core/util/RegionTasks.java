package su.nightexpress.excellentcrates.core.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.nightcore.NightCorePlugin;

@NullMarked
public final class RegionTasks {

    public static void runFor(NightCorePlugin plugin, Entity entity, Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
        }
        else {
            plugin.runTask(entity, task);
        }
    }

    public static void runFor(NightCorePlugin plugin, Entity entity, Runnable task, Runnable onRemoved) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
            return;
        }

        if (entity.getScheduler().run(plugin, scheduled -> task.run(), onRemoved) == null) {
            onRemoved.run();
        }
    }

    public static void runAt(NightCorePlugin plugin, Location location, Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(location)) {
            task.run();
        }
        else {
            plugin.runTask(location, task);
        }
    }

    public static void runGlobal(NightCorePlugin plugin, Runnable task) {
        if (Bukkit.isGlobalTickThread()) {
            task.run();
        }
        else {
            plugin.runTask(task);
        }
    }
}
