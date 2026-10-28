package su.nightexpress.excellentcrates.animation.session;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import su.nightexpress.excellentcrates.api.animation.AnimationInstance;

@NullMarked
public class AnimationSessionManager {

    public record Session(AnimationInstance instance, Location location) {
    }

    private final Map<UUID, Session> sessions;

    public AnimationSessionManager() {
        this.sessions = new ConcurrentHashMap<>();
    }

    public void purgeStoppedSessions() {
        this.sessions.values().removeIf(session -> !session.instance().isRunning());
    }

    public @Nullable Session removeSession(UUID playerId) {
        return this.sessions.remove(playerId);
    }

    public void registerSession(UUID playerId, AnimationInstance instance, Location location) {
        this.sessions.put(playerId, new Session(instance, location));
    }

    public void unregisterSession(UUID playerId) {
        this.sessions.remove(playerId);
    }

    public @Nullable AnimationInstance getSession(UUID playerId) {
        Session session = this.sessions.get(playerId);
        return session == null ? null : session.instance();
    }

    public boolean hasSession(UUID playerId) {
        Session session = this.sessions.get(playerId);
        return session != null && session.instance().isRunning();
    }

    public Set<Session> getSessions() {
        return Set.copyOf(this.sessions.values());
    }
}
