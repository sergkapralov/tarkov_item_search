package com.tarkovsearch.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Sent Server -> Client right after a lootable container (chest, barrel, etc.)
 * generates its loot for the very first time (i.e. the player who receives this
 * packet is the one who actually "rolled" the loot).
 * <p>
 * It only carries the block position - it never touches what the loot
 * actually IS. The client decides which individual slots to hide purely
 * from the real (unmodified) item stacks it receives through normal
 * container syncing, so this payload just marks "this chest is now in its
 * first-open reveal window".
 */
public record ChestRevealPayload(BlockPos pos) implements CustomPayload {

    public static final CustomPayload.Id<ChestRevealPayload> ID =
            new CustomPayload.Id<>(Identifier.of("tarkovsearch", "chest_reveal"));

    public static final PacketCodec<RegistryByteBuf, ChestRevealPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, ChestRevealPayload::pos,
            ChestRevealPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /** Call once from the mod's common entrypoint to register this payload type. */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
