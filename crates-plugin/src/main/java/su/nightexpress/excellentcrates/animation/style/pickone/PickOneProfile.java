package su.nightexpress.excellentcrates.animation.style.pickone;

import org.jspecify.annotations.NullMarked;

import su.nightexpress.engine.ui.CoreUIService;
import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.api.animation.AnimationContext;
import su.nightexpress.excellentcrates.api.animation.AnimationInstance;
import su.nightexpress.excellentcrates.core.animation.BaseAnimationProfile;
import su.nightexpress.nightcore.bridge.key.AdaptedKey;

@NullMarked
public class PickOneProfile extends BaseAnimationProfile {

    private final PickOneSettings settings;
    private final CratesPlugin    plugin;
    private final CoreUIService   coreUI;

    public PickOneProfile(AdaptedKey key, PickOneSettings settings, CratesPlugin plugin, CoreUIService coreUI) {
        super(key);
        this.settings = settings;
        this.plugin = plugin;
        this.coreUI = coreUI;
    }

    @Override
    public AnimationInstance createInstance(AnimationContext context, Runnable onComplete) {
        return new PickOneAnimation(context, onComplete, this.settings, this.plugin, this.coreUI);
    }

    @Override
    public String getName() {
        return this.settings.name();
    }
}
