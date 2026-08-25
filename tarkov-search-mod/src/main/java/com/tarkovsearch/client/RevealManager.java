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
 * A slot's reveal timer is assigned lazily, the first time the screen asks
 * about it while it actually holds a (real, server-generated) item - this
 * avoids ever needing to know in advance which slots are occupied.
 */
public final class RevealManager {

    private static final int MIN_DELAY_MS = 900;
    private static final int MAX_DELAY_MS = 5500;
    private static final int JITTER_MS = 250;

    private static final Random RANDOM = new Random();

    private static final Map<BlockPos, Map<Integer, Long>> ACTIVE = new HashMap<>();

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
    public static void startReveal(BlockPos pos) {
        ACTIVE.put(pos.toImmutable(), new HashMap<>());
    }

    /**
     * @return true if this occupied slot of this chest should currently be
     * drawn as "still searching" (magnifying glass) instead of showing the
     * real item. Only call this for slots that actually hold an item.
     */
    public static boolean isHidden(BlockPos pos, int slotIndex) {
        Map<Integer, Long> perSlot = ACTIVE.get(pos);
        if (perSlot == null) {
            return false;
        }
        Long revealAt = perSlot.get(slotIndex);
        if (revealAt == null) {
            long delay = MIN_DELAY_MS + RANDOM.nextInt(MAX_DELAY_MS - MIN_DELAY_MS)
                    + RANDOM.nextInt(JITTER_MS);
            perSlot.put(slotIndex, System.currentTimeMillis() + delay);
            return true;
        }
        if (System.currentTimeMillis() >= revealAt) {
            perSlot.remove(slotIndex);
            if (perSlot.isEmpty()) {
                ACTIVE.remove(pos);
            }
            return false;
        }
        return true;
    }
}
