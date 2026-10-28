package su.nightexpress.excellentcrates.animation.session;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.engine.component.BaseController;
import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.api.animation.AnimationInstance;
import su.nightexpress.excellentcrates.core.util.RegionTasks;

@NullMarked
public class AnimationSessionController extends BaseController {

    private final AnimationSessionManager sessionManager;

    public AnimationSessionController(CratesPlugin plugin, AnimationSessionManager sessionManager) {
        super(plugin);
        this.sessionManager = sessionManager;
    }

    @Override
    protected void onControllerReload() {

    }

    @Override
    protected void onControllerShutdown() {
        this.stopAllSessions();
    }

    @Override
    protected void onControllerStart() {
        this.runInstanceTickTask();
    }

    private void runInstanceTickTask() {
        this.addTickTask(this::tickSessions, 1);
    }

    private void tickSessions() {
        this.sessionManager.purgeStoppedSessions();

        for (AnimationSessionManager.Session session : this.sessionManager.getSessions()) {
            RegionTasks.runAt(this.plugin, session.location(), () -> tickSession(session.instance()));
        }
    }

    private static void tickSession(AnimationInstance instance) {
        if (instance.isRunning() && !instance.tick()) {
            instance.stop();
        }
    }

    private void stopSession(UUID playerId) {
        AnimationSessionManager.Session session = this.sessionManager.removeSession(playerId);
        if (session == null) return;

        RegionTasks.runAt(this.plugin, session.location(), session.instance()::stop);
    }

    private void stopAllSessions() {
        for (AnimationSessionManager.Session session : this.sessionManager.getSessions()) {
            RegionTasks.runAt(this.plugin, session.location(), session.instance()::stop);
        }

        this.sessionManager.purgeStoppedSessions();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        this.stopSession(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        this.stopSession(playerId);
    }
}
