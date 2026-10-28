package su.nightexpress.excellentcrates.core.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.MenuType;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.nightcore.NightCorePlugin;
import su.nightexpress.nightcore.core.CoreConfig;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.MenuViewer;

@NullMarked
public abstract class RegionSafeObjectMenu<T> extends AbstractObjectMenu<T> {

    protected RegionSafeObjectMenu(NightCorePlugin plugin, MenuType defaultType, String defaultTitle, Class<T> type) {
        super(plugin, defaultType, defaultTitle, type);
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent event) {
        MenuViewer viewer = this.getViewer(player);
        if (viewer == null) return;

        event.setCancelled(true);

        if (!viewer.canClickAgain()) return;

        viewer.setNextClickIn(System.currentTimeMillis() + CoreConfig.MENU_CLICK_COOLDOWN.get());

        this.onClick(viewer.createContext(), event);

        this.plugin.runTask(player, () -> viewer.handleClick(event));
    }
}
