package su.nightexpress.excellentcrates.animation.style.pickone.ui;

import org.jspecify.annotations.NullMarked;

import su.nightexpress.engine.ui.menu.MenuKey;
import su.nightexpress.excellentcrates.animation.style.pickone.ui.menu.context.PickOneMenuContext;

@NullMarked
public final class PickOneUIKeys {

    public static final MenuKey<PickOneMenuContext> MENU_PICK = MenuKey.of("animation.pick_one");

    private PickOneUIKeys() {
    }
}
