package su.nightexpress.excellentcrates.keys.balance.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentcrates.api.key.CrateKey;
import su.nightexpress.excellentcrates.api.key.command.KeyCommand;
import su.nightexpress.excellentcrates.api.key.dispatcher.KeyMessageDispatcher;
import su.nightexpress.excellentcrates.core.SharedPlaceholders;
import su.nightexpress.excellentcrates.keys.balance.KeyBalanceService;
import su.nightexpress.excellentcrates.keys.lang.KeyLang;
import su.nightexpress.excellentcrates.keys.permission.KeyPerms;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.commands.tree.ExecutableNode;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;

@NullMarked
public class KeyGiveAllCommand implements KeyCommand {

    private static final String ARGUMENT_KEY    = "key";
    private static final String ARGUMENT_AMOUNT = "amount";

    private static final String FLAG_SILENT          = "s";
    private static final String FLAG_SILENT_FEEDBACK = "sf";

    private final KeyBalanceService    balanceService;
    private final KeyMessageDispatcher dispatcher;

    public KeyGiveAllCommand(KeyBalanceService balanceService,
                             KeyMessageDispatcher dispatcher) {
        this.balanceService = balanceService;
        this.dispatcher = dispatcher;
    }

    @Override
    public ExecutableNode createCommand() {
        return Commands.literal("giveall", builder -> builder
            .description(KeyLang.COMMAND_GIVE_ALL_DESCRIPTION)
            .permission(KeyPerms.COMMAND_GIVE_ALL)
            .withArguments(
                Arguments.argument(ARGUMENT_KEY, CrateKey.class),
                Arguments.integer(ARGUMENT_AMOUNT, 1)
                    .optional()
                    .suggestions((reader, context) -> List.of("1", "5", "10"))
            )
            .withFlags(FLAG_SILENT, FLAG_SILENT_FEEDBACK)
            .executes(this::run)
        );
    }

    private boolean run(CommandContext context, ParsedArguments arguments) {
        CrateKey key = arguments.get(ARGUMENT_KEY, CrateKey.class);
        int amount = Math.abs(arguments.getInt(ARGUMENT_AMOUNT, 1));
        CommandSender sender = context.getSender();

        AtomicInteger totalGiven = new AtomicInteger();
        List<CompletableFuture<Void>> deliveries = new ArrayList<>();

        for (Player player : Players.getOnline()) {
            if (!player.hasPermission(KeyPerms.COMMAND_GIVE_ALL_INCLUDED)) continue;

            deliveries.add(this.balanceService.addKeyAsync(player, key, amount).thenAccept(result -> {
                if (!result.success()) {
                    result.handleFeedback((locale, actionContext) -> {
                        this.dispatcher.sendBase(sender, key, locale, ctx -> ctx
                            .apply(actionContext)
                            .with(CommonPlaceholders.PLAYER.resolver(player))
                        );
                    });
                    return;
                }

                if (!context.hasFlag(FLAG_SILENT)) {
                    result.handleFeedback((locale, actionContext) -> {
                        this.dispatcher.sendBase(player, key, KeyLang.KEY_GIVE_NOTIFY, ctx -> ctx
                            .apply(actionContext)
                        );
                    });
                }

                totalGiven.addAndGet(amount);
            }));
        }

        if (!context.hasFlag(FLAG_SILENT_FEEDBACK)) {
            CompletableFuture.allOf(deliveries.toArray(new CompletableFuture[0])).whenComplete((ignored, throwable) -> {
                this.dispatcher.sendBase(sender, key, KeyLang.KEY_GIVE_ALL_FEEDBACK, ctx -> ctx
                    .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(amount))
                    .with(SharedPlaceholders.TOTAL, () -> NumberUtil.format(totalGiven.get()))
                );
            });
        }

        return true;
    }
}
