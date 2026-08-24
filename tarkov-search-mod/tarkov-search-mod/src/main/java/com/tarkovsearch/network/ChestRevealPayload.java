package com.tarkovsearch.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Sent Server -> Client right after a lootable container (chest, barrel, etc.)
 * generates its loot for the very first time (i.e. the player who receives this
 * packet is the one who actually "rolled" the loot).
 * <p>
 * It only carries the block position and which inventory slots ended up with an
 * item in them. It never touches what those items actually ARE - that's still
 * 100% vanilla loot generation. The client uses this purely to know which slots
 * to hide behind the magnifying-glass icon and for how long.
 * <p>
 * NOTE: there is no dedicated PacketCodecs.INT_ARRAY in this Minecraft version
 * (only byte/long array helpers exist), so the slot list is encoded as a
 * List<Integer> via PacketCodecs.VAR_INT.collect(PacketCodecs.toList()).
 */
public record ChestRevealPayload(BlockPos pos, List<Integer> slots) implements CustomPayload {

    public static final CustomPayload.Id<ChestRevealPayload> ID =
            new CustomPayload.Id<>(Identifier.of("tarkovsearch", "chest_reveal"));

    public static final PacketCodec<RegistryByteBuf, ChestRevealPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, ChestRevealPayload::pos,
            PacketCodecs.VAR_INT.collect(PacketCodecs.toList()), ChestRevealPayload::slots,
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
