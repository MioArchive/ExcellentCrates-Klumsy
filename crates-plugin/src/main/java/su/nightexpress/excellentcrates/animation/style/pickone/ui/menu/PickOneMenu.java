package su.nightexpress.excellentcrates.animation.style.pickone.ui.menu;

import java.util.List;
import java.util.stream.IntStream;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentcrates.animation.style.pickone.ui.menu.context.PickOneMenuContext;
import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.api.common.preview.NameAndLore;
import su.nightexpress.excellentcrates.api.reward.Reward;
import su.nightexpress.excellentcrates.api.reward.preview.RewardPreviewAPI;
import su.nightexpress.nightcore.bridge.wrap.NightSound;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.codec.ConfigCodecs;
import su.nightexpress.nightcore.configuration.property.ConfigProperty;
import su.nightexpress.nightcore.ui.inventory.action.ActionContext;
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.item.populator.SlotPattern;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.sound.VanillaSound;

@NullMarked
public class PickOneMenu extends AbstractObjectMenu<PickOneMenuContext> {

    private static final ConfigProperty<SlotPattern> CHOICE_SLOTS = ConfigProperty.of(
        ConfigCodecs.SLOT_PATTERN,
        "reward.choice-slots",
        new SlotPattern()
            .with(1, 13)
            .with(2, 11, 15)
            .with(3, 11, 13, 15)
            .with(4, 10, 12, 14, 16)
            .with(5, 11, 12, 13, 14, 15)
            .with(6, 10, 11, 12, 14, 15, 16)
            .with(7, 10, 11, 12, 13, 14, 15, 16),
        ""
    );

    private static final ConfigProperty<NightSound> PICK_SOUND = ConfigProperty.of(
        ConfigCodecs.NIGHT_SOUND,
        "reward.pick-sound",
        VanillaSound.of(Sound.ENTITY_PLAYER_LEVELUP),
        ""
    );

    private SlotPattern choiceSlots = CHOICE_SLOTS.getDefaultValue();
    private NightSound  pickSound   = PICK_SOUND.getDefaultValue();

    public PickOneMenu(CratesPlugin plugin) {
        super(plugin, MenuType.GENERIC_9X3, "Choose your reward", PickOneMenuContext.class);
    }

    @Override
    public void defineDefaultLayout() {
        this.addBackgroundItem(Material.GRAY_STAINED_GLASS_PANE, IntStream.range(0, 27).toArray());
    }

    @Override
    protected void onLoad(FileConfig config) {
        this.choiceSlots = config.getOrSet(CHOICE_SLOTS);
        this.pickSound = config.getOrSet(PICK_SOUND);
    }

    @Override
    public void onPrepare(ViewerContext context, InventoryView view, Inventory inventory, List<MenuItem> items) {
        PickOneMenuContext menuContext = this.getObject(context);
        RewardPreviewAPI preview = menuContext.rewardPreview();
        List<Reward> choices = menuContext.choices();
        int[] slots = this.choiceSlots.getSlots(choices.size());

        for (int index = 0; index < Math.min(slots.length, choices.size()); index++) {
            Reward reward = choices.get(index);
            NameAndLore info = preview.getDisplayInfo(reward);

            items.add(MenuItem.custom()
                .defaultState(ItemState.builder()
                    .icon(preview.createPreviewIcon(reward).setDisplayName(info.name()).setLore(info.lore()))
                    .action(ctx -> this.onPickClick(ctx, reward))
                    .build()
                )
                .slots(slots[index])
                .build()
            );
        }
    }

    private void onPickClick(ActionContext context, Reward reward) {
        this.getObject(context).onPick().accept(reward);
        this.pickSound.play(context.getPlayer());
    }

    @Override
    protected void onClose(ViewerContext context, InventoryCloseEvent event) {
        this.getObject(context).onClose().run();
    }

    @Override
    protected void onClick(ViewerContext context, InventoryClickEvent event) {

    }

    @Override
    protected void onDrag(ViewerContext context, InventoryDragEvent event) {

    }

    @Override
    public void registerActions() {

    }

    @Override
    public void registerConditions() {

    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
