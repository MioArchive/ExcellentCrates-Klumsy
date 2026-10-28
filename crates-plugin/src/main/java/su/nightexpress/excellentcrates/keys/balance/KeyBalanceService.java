package su.nightexpress.excellentcrates.keys.balance;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.engine.action.ActionResult;
import su.nightexpress.engine.data.cache.CacheStrategy;
import su.nightexpress.engine.id.Identifier;
import su.nightexpress.excellentcrates.api.CratesPlugin;
import su.nightexpress.excellentcrates.api.key.CrateKey;
import su.nightexpress.excellentcrates.api.key.balance.IKeyBalance;
import su.nightexpress.excellentcrates.core.util.RegionTasks;
import su.nightexpress.excellentcrates.keys.item.KeyItemService;
import su.nightexpress.excellentcrates.keys.lang.KeyLang;
import su.nightexpress.excellentcrates.keys.storage.db.KeyStorageCachedDataService;
import su.nightexpress.excellentcrates.keys.storage.model.StoredKey;
import su.nightexpress.excellentcrates.util.InventoryUtils;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;

@NullMarked
public class KeyBalanceService {

    private final CratesPlugin                plugin;
    private final KeyStorageCachedDataService storageService;
    private final KeyItemService              itemService;

    public KeyBalanceService(CratesPlugin plugin,
                             KeyStorageCachedDataService storageService,
                             KeyItemService itemService) {
        this.plugin = plugin;
        this.storageService = storageService;
        this.itemService = itemService;
    }

    public boolean hasKey(Player player, CrateKey key, int amount) {
        int absAmount = Math.abs(amount);
        return this.countKeys(player, key) >= absAmount;
    }

    public int countKeys(Player player, CrateKey key) {
        if (key.getBase().isVirtual()) {
            return this.countVirtualKeys(player, key);
        }

        return this.countPhysicalKeys(player, key);
    }

    public int countPhysicalKeys(Player player, CrateKey key) {
        return this.countPhysicalKeys(player, key.getId());
    }

    public int countPhysicalKeys(Player player, Identifier keyId) {
        return InventoryUtils.countInventoryAmount(player, itemStack -> this.itemService.isKeyItem(itemStack, keyId));
    }

    public int countVirtualKeys(Player player, CrateKey key) {
        return this.countVirtualKeys(player, key.getId());
    }

    public int countVirtualKeys(Player player, Identifier keyId) {
        StoredKey storedKey = this.storageService.getCached(player.getUniqueId(), keyId).orElse(null);
        return storedKey != null ? storedKey.getVirtualBalance() : 0;
    }

    public int countUnclaimedKeys(Player player, CrateKey key) {
        return this.countUnclaimedKeys(player, key.getId());
    }

    public int countUnclaimedKeys(Player player, Identifier keyId) {
        StoredKey storedKey = this.storageService.getCached(player.getUniqueId(), keyId).orElse(null);
        return storedKey != null ? storedKey.getUnclaimedAmount() : 0;
    }

    public IKeyBalance getKeyBalance(Player player, CrateKey key) {
        int physical = this.countPhysicalKeys(player, key);
        int virtual = this.countVirtualKeys(player, key);
        int unclaimed = this.countUnclaimedKeys(player, key);

        return new KeyBalance(physical, virtual, unclaimed);
    }

    /**
     * Runs {@code online} on the player's region thread, or {@code offline} when the player is not online
     * or leaves before the task runs. Inventories may only be touched from the owning thread.
     */
    private <T> CompletableFuture<T> withPlayer(UUID playerId, Function<Player, T> online, Supplier<CompletableFuture<T>> offline) {
        Player player = Players.getPlayer(playerId);
        if (player == null) {
            return offline.get();
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        RegionTasks.runFor(this.plugin, player,
            () -> {
                try {
                    future.complete(online.apply(player));
                }
                catch (Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            },
            () -> offline.get().whenComplete((result, throwable) -> {
                if (throwable != null) future.completeExceptionally(throwable);
                else future.complete(result);
            })
        );
        return future;
    }

    public CompletableFuture<IKeyBalance> getKeyBalanceAsync(UUID playerId, CrateKey key) {
        return this.withPlayer(playerId,
            player -> this.getKeyBalance(player, key),
            () -> this.getOfflineKeyBalanceAsync(playerId, key)
        );
    }

    private CompletableFuture<IKeyBalance> getOfflineKeyBalanceAsync(UUID playerId, CrateKey key) {
        Identifier keyId = key.getId();

        return this.storageService.loadAndCacheAsync(playerId, keyId, CacheStrategy.TEMPORARY)
            .thenApply(opt -> {
                if (opt.isEmpty()) {
                    return KeyBalance.empty();
                }

                StoredKey storedKey = opt.get();

                int inventory = 0;  // Player is offline, can't count physical keys
                int virtual = storedKey != null ? storedKey.getVirtualBalance() : 0;
                int unclaimed = storedKey != null ? storedKey.getUnclaimedAmount() : 0;

                return new KeyBalance(inventory, virtual, unclaimed);
            });
    }

    public ActionResult redeemKey(Player player, CrateKey key) {
        UUID playerId = player.getUniqueId();
        Identifier keyId = key.getId();

        StoredKey storedKey = this.storageService.getCached(playerId, keyId).orElse(null);
        if (storedKey == null || storedKey.getUnclaimedAmount() <= 0) {
            return ActionResult.fail(KeyLang.KEY_REDEEM_NO_UNCLAIMED);
        }

        int unclaimed = storedKey.getUnclaimedAmount();
        ActionResult result = this.addKey(player, key, unclaimed);
        if (!result.success()) {
            return result;
        }
        // Subtract instead of zeroing so an offline-path delivery landing meanwhile is kept.
        synchronized (storedKey) {
            storedKey.setUnclaimedAmount(Math.max(0, storedKey.getUnclaimedAmount() - unclaimed));
        }
        this.storageService.markDirty(storedKey);

        return ActionResult.ok(KeyLang.KEY_REDEEM_FEEDBACK, ctx -> ctx
            .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(unclaimed))
        );
    }

    public ActionResult addKey(Player player, CrateKey key, int amount) {
        boolean virtual = key.getBase().isVirtual();
        int absAmount = Math.abs(amount);

        if (virtual) {
            Identifier keyId = key.getId();

            StoredKey storedKey = this.storageService.getCachedOrCreate(
                player.getUniqueId(), keyId, CacheStrategy.PERMANENT
            );
            synchronized (storedKey) {
                storedKey.setVirtualBalance(storedKey.getVirtualBalance() + absAmount);
            }
            this.storageService.markDirty(storedKey);
        }
        else {
            ItemStack keyItem = this.itemService.createTaggedItem(key).orElse(null);
            if (keyItem == null) {
                return ActionResult.fail(KeyLang.ERROR_ITEM_CREATION_FAILED);
            }

            int space = InventoryUtils.countInventorySpace(player, keyItem);
            if (space < absAmount) {
                return ActionResult.fail(KeyLang.ERROR_INVENTORY_SPACE);
            }

            keyItem.setAmount(absAmount);
            InventoryUtils.giveItem(player, keyItem);
        }

        return ActionResult.ok(KeyLang.KEY_GIVE_FEEDBACK, ctx -> ctx
            .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(absAmount))
        );
    }

    /**
     * Gives a key to a player asynchronously.
     * If the key is virtual, it will update the player's virtual balance in the database.
     * If the key is physical and the player is online, it will add the key item to their inventory.
     * If the player is offline, it will add the unclaimed amount to their account for later retrieval.
     * 
     * @param playerId the UUID of the player to give the key to
     * @param key      the key to give
     * @param amount   the amount of keys to give (can be negative for removal)
     * @return the result of the action
     */
    public CompletableFuture<ActionResult> giveKeyAsync(UUID playerId, CrateKey key, int amount) {
        if (key.getBase().isVirtual()) {
            return this.storeKeyAsync(playerId, key, amount);
        }

        return this.withPlayer(playerId,
            player -> this.addKey(player, key, amount),
            () -> this.storeKeyAsync(playerId, key, amount)
        );
    }

    /**
     * Adds keys to an online player on their region thread, or stores them as virtual/unclaimed keys if the
     * player leaves before that.
     */
    public CompletableFuture<ActionResult> addKeyAsync(Player player, CrateKey key, int amount) {
        UUID playerId = player.getUniqueId();
        return this.withPlayer(playerId,
            online -> this.addKey(online, key, amount),
            () -> this.storeKeyAsync(playerId, key, amount)
        );
    }

    /** Adds to the stored virtual balance, or to the unclaimed amount for physical keys. */
    private CompletableFuture<ActionResult> storeKeyAsync(UUID playerId, CrateKey key, int amount) {
        int absAmount = Math.abs(amount);
        boolean virtual = key.getBase().isVirtual();

        ActionResult success = ActionResult.ok(KeyLang.KEY_GIVE_FEEDBACK, ctx -> ctx
            .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(absAmount))
        );

        return this.storageService.updateDataAndCreateIfAbsentAsync(playerId, key.getId(), CacheStrategy.TEMPORARY,
            storedKey -> {
                synchronized (storedKey) {
                    if (virtual) {
                        storedKey.setVirtualBalance(storedKey.getVirtualBalance() + absAmount);
                    }
                    else {
                        storedKey.setUnclaimedAmount(storedKey.getUnclaimedAmount() + absAmount);
                    }
                }
                return success;
            });
    }

    public ActionResult removeKey(Player player, CrateKey key, int amount) {
        Identifier keyId = key.getId();
        int absAmount = Math.abs(amount);
        boolean virtual = key.getBase().isVirtual();

        if (virtual) {
            StoredKey storedKey = this.storageService.getCached(player.getUniqueId(), keyId).orElse(null);
            if (storedKey == null || !takeVirtualKeys(storedKey, absAmount)) {
                return ActionResult.fail(KeyLang.KEY_REMOVE_NOT_ENOUGH, ctx -> ctx
                    .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(absAmount))
                );
            }

            this.storageService.markDirty(storedKey);
        }
        else {
            InventoryUtils.removeFromInventory(player,
                itemStack -> this.itemService.isKeyItem(itemStack, keyId),
                absAmount
            );
        }

        return ActionResult.ok(KeyLang.KEY_REMOVE_FEEDBACK, ctx -> ctx
            .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(absAmount))
        );
    }

    public CompletableFuture<ActionResult> removeKeyAsync(UUID playerId, CrateKey key, int amount) {
        Identifier keyId = key.getId();
        int absAmount = Math.abs(amount);
        boolean virtual = key.getBase().isVirtual();

        ActionResult success = ActionResult.ok(KeyLang.KEY_REMOVE_FEEDBACK, ctx -> ctx
            .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(absAmount))
        );

        ActionResult notEnough = ActionResult.fail(KeyLang.KEY_REMOVE_NOT_ENOUGH, ctx -> ctx
            .with(CommonPlaceholders.GENERIC_AMOUNT, () -> NumberUtil.format(absAmount))
        );

        if (virtual) {
            return this.storageService.updateDataIfExistsAsync(playerId, keyId, CacheStrategy.TEMPORARY, storedKey -> {
                return storedKey != null && takeVirtualKeys(storedKey, absAmount) ? success : notEnough;
            });
        }

        return this.withPlayer(playerId,
            player -> this.removeKey(player, key, amount),
            () -> this.storageService.updateDataIfExistsAsync(playerId, keyId, CacheStrategy.TEMPORARY, storedKey -> {
                if (storedKey == null) return notEnough;

                synchronized (storedKey) {
                    if (storedKey.getUnclaimedAmount() < absAmount) return notEnough;

                    storedKey.setUnclaimedAmount(storedKey.getUnclaimedAmount() - absAmount);
                }
                return success;
            })
        );
    }

    // Balances are changed from player, global and database threads; check and subtract as one step.
    private static boolean takeVirtualKeys(StoredKey storedKey, int amount) {
        synchronized (storedKey) {
            if (storedKey.getVirtualBalance() < amount) return false;

            storedKey.setVirtualBalance(storedKey.getVirtualBalance() - amount);
            return true;
        }
    }
}
