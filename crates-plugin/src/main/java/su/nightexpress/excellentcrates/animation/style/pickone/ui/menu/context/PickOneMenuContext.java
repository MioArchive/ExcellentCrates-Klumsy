package su.nightexpress.excellentcrates.animation.style.pickone.ui.menu.context;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentcrates.api.reward.Reward;
import su.nightexpress.excellentcrates.api.reward.preview.RewardPreviewAPI;

@NullMarked
public record PickOneMenuContext(List<Reward> choices, RewardPreviewAPI rewardPreview, Consumer<Reward> onPick, Runnable onClose) {

}
