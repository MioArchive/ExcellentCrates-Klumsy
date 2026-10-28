package su.nightexpress.excellentcrates.animation.style.pickone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import su.nightexpress.engine.ui.CoreUIService;
import su.nightexpress.excellentcrates.animation.style.pickone.ui.PickOneUIKeys;
import su.nightexpress.excellentcrates.animation.style.pickone.ui.menu.context.PickOneMenuContext;
import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.api.animation.AnimationContext;
import su.nightexpress.excellentcrates.api.reward.Reward;
import su.nightexpress.excellentcrates.core.animation.BaseAnimationInstance;
import su.nightexpress.excellentcrates.core.util.RegionTasks;
import su.nightexpress.excellentcrates.util.WeightedRandomSelector;
import su.nightexpress.nightcore.util.Randomizer;

@NullMarked
public class PickOneAnimation extends BaseAnimationInstance {

    private final CratesPlugin       plugin;
    private final CoreUIService      coreUI;
    private final PickOneMenuContext menuContext;

    private volatile @Nullable Reward picked;
    private volatile boolean          menuOpen;

    public PickOneAnimation(AnimationContext context,
                            Runnable onComplete,
                            PickOneSettings settings,
                            CratesPlugin plugin,
                            CoreUIService coreUI) {
        super(context, onComplete);
        this.plugin = plugin;
        this.coreUI = coreUI;
        this.menuContext = new PickOneMenuContext(
            this.rollChoices(settings.choices()), this.rewardPreview, this::pick, this::onMenuClose
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
        if (picked == null) return true;

        this.reward.setGranted(picked);
        return false;
    }

    private void openMenu() {
        if (!this.isRunning() || this.picked != null) return;

        this.menuOpen = this.coreUI.openMenu(this.player, PickOneUIKeys.MENU_PICK, this.menuContext).success();

        if (!this.menuOpen) {
            this.picked = this.reward.getGranted();
        }
    }

    private void closeMenu() {
        if (this.menuOpen) {
            this.player.closeInventory();
        }
    }

    private void pick(Reward reward) {
        if (this.picked != null) return;

        this.picked = reward;
        RegionTasks.runFor(this.plugin, this.player, this::closeMenu);
    }

    private void onMenuClose() {
        this.menuOpen = false;
        this.plugin.runTask(this.player, this::openMenu);
    }
}
