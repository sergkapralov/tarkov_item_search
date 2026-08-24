package com.tarkovsearch.client;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Purely client-side, purely visual state. Holds, per chest position, which
 * inventory slots are still "hidden" behind the magnifying-glass icon and
 * when each of them should reveal.
 * <p>
 * This never touches the real ItemStacks - the actual items are already in
 * the container's inventory as normal. We're just deciding whether the
 * screen should draw the real icon yet or the search icon instead.
 */
public final class RevealManager {

    /** Minimum / maximum delay (ms) before a hidden slot reveals its item. */
    private static final int MIN_DELAY_MS = 900;
    private static final int MAX_DELAY_MS = 5500;
    /** Extra random stagger so two slots essentially never reveal on the same tick. */
    private static final int JITTER_MS = 250;

    private static final Random RANDOM = new Random();

    private static final Map<BlockPos, Map<Integer, Long>> HIDDEN_UNTIL = new HashMap<>();

    private RevealManager() {
    }

    /**
     * The block position of the last block the local player right-clicked.
     * Chest/barrel/shulker screens don't carry their world position with
     * them, so we cache it here the moment the player interacts, and read
     * it back while the resulting screen is open. Set by
     * {@code ClientPlayerInteractionManagerMixin}.
     */
    public static volatile BlockPos lastInteractedPos;

    /** Called when the server tells us a chest just rolled its loot for the first time. */
    public static void startReveal(BlockPos pos, int[] slots) {
        Map<Integer, Long> perSlot = new HashMap<>();
        long now = System.currentTimeMillis();
        for (int slot : slots) {
            long delay = MIN_DELAY_MS + RANDOM.nextInt(MAX_DELAY_MS - MIN_DELAY_MS)
                    + RANDOM.nextInt(JITTER_MS);
            perSlot.put(slot, now + delay);
        }
        HIDDEN_UNTIL.put(pos.toImmutable(), perSlot);
    }

    /**
     * @return true if this slot of this chest should currently be drawn as
     * "still searching" (magnifying glass) instead of showing the real item.
     */
    public static boolean isHidden(BlockPos pos, int slotIndex) {
        Map<Integer, Long> perSlot = HIDDEN_UNTIL.get(pos);
        if (perSlot == null) {
            return false;
        }
        Long revealAt = perSlot.get(slotIndex);
        if (revealAt == null) {
            return false;
        }
        if (System.currentTimeMillis() >= revealAt) {
            perSlot.remove(slotIndex);
            if (perSlot.isEmpty()) {
                HIDDEN_UNTIL.remove(pos);
            }
            return false;
        }
        return true;
    }

    /** True if this position currently has any hidden slot at all (cheap early-out check). */
    public static boolean hasActiveReveal(BlockPos pos) {
        return HIDDEN_UNTIL.containsKey(pos);
    }
}
