package su.nightexpress.excellentcrates.animation.style.pickone.ui.controller;

import org.jspecify.annotations.NullMarked;

import su.nightexpress.engine.component.BasePluginComponent;
import su.nightexpress.engine.ui.CoreUIService;
import su.nightexpress.excellentcrates.animation.style.pickone.ui.PickOneUIKeys;
import su.nightexpress.excellentcrates.animation.style.pickone.ui.menu.PickOneMenu;
import su.nightexpress.excellentcrates.api.CratesPlugin;

@NullMarked
public class PickOneMenuRegistrar extends BasePluginComponent {

    private final CratesPlugin  plugin;
    private final CoreUIService coreUI;

    public PickOneMenuRegistrar(CratesPlugin plugin, CoreUIService coreUI) {
        super();
        this.plugin = plugin;
        this.coreUI = coreUI;
    }

    @Override
    protected void onReload() {
        this.shutdown();
        this.start();
    }

    @Override
    protected void onShutdown() {
        this.coreUI.unregisterMenu(PickOneUIKeys.MENU_PICK);
    }

    @Override
    protected void onStart() {
        PickOneMenu menu = new PickOneMenu(this.plugin);
        menu.load(this.plugin.menuPath().resolve("animation_pick_one.yml"));

        this.coreUI.registerMenu(PickOneUIKeys.MENU_PICK, menu);
    }
}
