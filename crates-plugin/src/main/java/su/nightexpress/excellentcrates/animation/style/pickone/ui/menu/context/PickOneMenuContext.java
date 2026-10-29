package su.nightexpress.excellentcrates.animation.style.pickone.ui.menu.context;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentcrates.api.reward.Reward;
import su.nightexpress.excellentcrates.api.reward.preview.RewardPreviewAPI;

@NullMarked
public record PickOneMenuContext(List<Reward> choices,
                                 RewardPreviewAPI rewardPreview,
                                 Consumer<Board> onShow,
                                 Predicate<Reward> onPick,
                                 Runnable onClose) {

    public record Board(Inventory inventory, int[] slots) {

    }
}
