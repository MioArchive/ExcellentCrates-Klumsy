package su.nightexpress.excellentcrates.animation.style.pickone;

import org.jspecify.annotations.NullMarked;

import su.nightexpress.engine.id.Identifier;
import su.nightexpress.engine.ui.CoreUIService;
import su.nightexpress.excellentcrates.animation.style.pickone.codec.PickOneSettingsCodec;
import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.api.animation.AnimationProvider;
import su.nightexpress.nightcore.bridge.key.AdaptedKey;
import su.nightexpress.nightcore.config.FileConfig;

@NullMarked
public class PickOneProvider implements AnimationProvider<PickOneProfile> {

    private static final Identifier ID = new Identifier("pick_one");

    private final CratesPlugin  plugin;
    private final CoreUIService coreUI;

    public PickOneProvider(CratesPlugin plugin, CoreUIService coreUI) {
        this.plugin = plugin;
        this.coreUI = coreUI;
    }

    @Override
    public PickOneProfile readProfile(AdaptedKey key, FileConfig config) {
        PickOneSettings settings = config.getOrSet("settings",
            PickOneSettingsCodec.INSTANCE,
            PickOneSettings.defaultSettings()
        );

        return new PickOneProfile(key, settings, this.plugin, this.coreUI);
    }

    @Override
    public void writeDefaultProfile(AdaptedKey key, FileConfig config) {
        config.set("settings", PickOneSettings.defaultSettings());
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
