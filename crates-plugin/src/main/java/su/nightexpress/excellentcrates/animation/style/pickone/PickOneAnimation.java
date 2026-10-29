package su.nightexpress.excellentcrates.animation.style.pickone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Sound;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import su.nightexpress.engine.ui.CoreUIService;
import su.nightexpress.excellentcrates.animation.style.pickone.ui.PickOneUIKeys;
import su.nightexpress.excellentcrates.animation.style.pickone.ui.menu.PickOneMenu;
import su.nightexpress.excellentcrates.animation.style.pickone.ui.menu.context.PickOneMenuContext;
import su.nightexpress.excellentcrates.animation.style.pickone.ui.menu.context.PickOneMenuContext.Board;
import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.api.animation.AnimationContext;
import su.nightexpress.excellentcrates.api.reward.Reward;
import su.nightexpress.excellentcrates.core.animation.BaseAnimationInstance;
import su.nightexpress.excellentcrates.core.util.RegionTasks;
import su.nightexpress.excellentcrates.util.WeightedRandomSelector;
import su.nightexpress.nightcore.util.Randomizer;
import su.nightexpress.nightcore.util.sound.VanillaSound;

@NullMarked
public class PickOneAnimation extends BaseAnimationInstance {

    private static final VanillaSound SPIN_SOUND = VanillaSound.of(Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.5f);
    private static final VanillaSound LOCK_SOUND = VanillaSound.of(Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f);

    private final CratesPlugin       plugin;
    private final CoreUIService      coreUI;
    private final PickOneSettings    settings;
    private final List<Reward>       choices;
    private final PickOneMenuContext menuContext;

    private @Nullable Board board;
    private int             revealTick;

    private volatile @Nullable Reward picked;

    public PickOneAnimation(AnimationContext context,
                            Runnable onComplete,
                            PickOneSettings settings,
                            CratesPlugin plugin,
                            CoreUIService coreUI) {
        super(context, onComplete);
        this.plugin = plugin;
        this.coreUI = coreUI;
        this.settings = settings;
        this.choices = this.rollChoices(settings.choices());
        this.menuContext = new PickOneMenuContext(
            this.choices, this.rewardPreview, this::onMenuShow, this::pick, this::onMenuClose
        );
    }

    private List<Reward> rollChoices(int amount) {
        Reward granted = this.reward.getGranted();

        List<Reward> pool = new ArrayList<>(this.availableRewards);
        pool.removeIf(other -> other.getWeight() <= 0D || other.id().equals(granted.id()));

        List<Reward> choices = new ArrayList<>();
        choices.add(granted);

        WeightedRandomSelector selector = new WeightedRandomSelector(Randomizer.getSource());
        while (choices.size() < amount && !pool.isEmpty()) {
            Reward choice = selector.selectItem(pool, Reward::getWeight);
            pool.remove(choice);
            choices.add(choice);
        }

        Collections.shuffle(choices, Randomizer.getSource());
        return List.copyOf(choices);
    }

    @Override
    protected void onStart() {
        RegionTasks.runFor(this.plugin, this.player, this::openMenu);
    }

    @Override
    protected void onStop() {
        RegionTasks.runFor(this.plugin, this.player, this::closeMenu);
    }

    @Override
    protected boolean onTick() {
        Reward picked = this.picked;
        if (picked == null) {
            RegionTasks.runFor(this.plugin, this.player, this::tickReveal);
            return true;
        }

        this.reward.setGranted(picked);
        return false;
    }

    private void tickReveal() {
        Board board = this.board;
        if (board == null || this.isRevealed()) return;

        int[] slots = board.slots();
        for (int index = 0; index < Math.min(slots.length, this.choices.size()); index++) {
            int lockTick = this.lockTick(index);
            Reward shown = this.revealTick < lockTick ? Randomizer.pick(this.availableRewards) : this.choices.get(index);

            board.inventory().setItem(slots[index], PickOneMenu.createIcon(this.rewardPreview, shown).getItemStack());

            if (this.revealTick == lockTick) {
                LOCK_SOUND.play(this.player);
            }
        }

        SPIN_SOUND.play(this.player);
        this.revealTick++;
    }

    private int lockTick(int index) {
        return this.settings.spinTicks() + index * Math.max(0, this.settings.lockIntervalTicks());
    }

    private boolean isRevealed() {
        return this.revealTick > this.lockTick(this.choices.size() - 1);
    }

    private void openMenu() {
        if (!this.isRunning() || this.picked != null) return;

        if (!this.coreUI.openMenu(this.player, PickOneUIKeys.MENU_PICK, this.menuContext).success()) {
            this.picked = this.reward.getGranted();
        }
    }

    private void closeMenu() {
        if (this.board != null) {
            this.player.closeInventory();
        }
    }

    private boolean pick(Reward reward) {
        if (this.picked != null || !this.isRevealed()) return false;

        this.picked = reward;
        RegionTasks.runFor(this.plugin, this.player, this::closeMenu);
        return true;
    }

    private void onMenuShow(Board board) {
        this.board = board;
        // Covers the real choices the menu has just drawn before the player can see them.
        this.tickReveal();
    }

    private void onMenuClose() {
        this.board = null;
        this.plugin.runTask(this.player, this::openMenu);
    }
}
