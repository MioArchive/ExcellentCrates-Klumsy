package su.nightexpress.excellentcrates.animation.style;

import org.jspecify.annotations.NullMarked;

import su.nightexpress.engine.bootstrap.context.NamedBootstrapContext;
import su.nightexpress.engine.id.Identifier;
import su.nightexpress.engine.ui.CoreUIService;
import su.nightexpress.excellentcrates.animation.style.csgo.CSGOAnimationProvider;
import su.nightexpress.excellentcrates.animation.style.csgo.CSGOAnimationSettings;
import su.nightexpress.excellentcrates.animation.style.csgo.codec.CSGOAnimationSettingsCodec;
import su.nightexpress.excellentcrates.animation.style.pickone.PickOneProvider;
import su.nightexpress.excellentcrates.animation.style.pickone.PickOneSettings;
import su.nightexpress.excellentcrates.animation.style.pickone.codec.PickOneSettingsCodec;
import su.nightexpress.excellentcrates.animation.style.pickone.ui.controller.PickOneMenuRegistrar;
import su.nightexpress.excellentcrates.animation.style.simpleroll.SimpleRollProvider;
import su.nightexpress.excellentcrates.animation.style.simpleroll.SimpleRollSettings;
import su.nightexpress.excellentcrates.animation.style.simpleroll.codec.SimpleRollSettingsCodec;
import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.api.animation.AnimationRegistry;
import su.nightexpress.nightcore.configuration.codec.ConfigCodecs;

@NullMarked
public class AnimationTypeBootstrapContext extends NamedBootstrapContext {

    private static final Identifier ID   = new Identifier("animation.type");
    private static final String     NAME = "Animation Types";

    public AnimationTypeBootstrapContext(CratesPlugin plugin, CoreUIService coreUI, AnimationRegistry animationRegistry) {
        super(ID, NAME);

        ConfigCodecs.register(SimpleRollSettings.class, SimpleRollSettingsCodec.INSTANCE);
        ConfigCodecs.register(CSGOAnimationSettings.class, CSGOAnimationSettingsCodec.INSTANCE);
        ConfigCodecs.register(PickOneSettings.class, PickOneSettingsCodec.INSTANCE);

        animationRegistry.registerProvider(new SimpleRollProvider());
        animationRegistry.registerProvider(new CSGOAnimationProvider());
        animationRegistry.registerProvider(new PickOneProvider(plugin, coreUI));

        this.addComponent(new PickOneMenuRegistrar(plugin, coreUI));
    }
}
