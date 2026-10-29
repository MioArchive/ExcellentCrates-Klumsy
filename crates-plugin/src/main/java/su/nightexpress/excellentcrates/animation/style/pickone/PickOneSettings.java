package su.nightexpress.excellentcrates.animation.style.pickone;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record PickOneSettings(String name, int choices, int spinTicks, int lockIntervalTicks) {

    public static PickOneSettings defaultSettings() {
        return new PickOneSettings("Pick One (Default)", 3, 30, 12);
    }
}
