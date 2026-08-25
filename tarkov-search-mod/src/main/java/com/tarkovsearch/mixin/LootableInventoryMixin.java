package com.tarkovsearch.mixin;

import com.tarkovsearch.network.ChestRevealPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into the vanilla "roll loot for this container" logic.
 * <p>
 * As of this Minecraft version, that logic lives as a default method
 * (generateLoot) directly on the LootableInventory interface rather than as
 * a method on LootableContainerBlockEntity, so this mixin targets the
 * interface itself (an "interface mixin", supported by Fabric's Mixin fork).
 * <p>
 * IMPORTANT: this mixin never changes what loot is generated, when it is
 * generated, or how many items appear. It only *observes* the moment the
 * loot table is about to be rolled (which vanilla already limits to a
 * single, first-open event per container) and tells the client who
 * triggered it which chest that was, so the client can play a purely
 * visual "searching" animation over its slots. Which individual slots end
 * up occupied is decided purely client-side, from the real (unmodified)
 * item stacks the client receives through normal container syncing.
 */
@Mixin(LootableInventory.class)
public interface LootableInventoryMixin {

    @Shadow
    @Nullable
    RegistryKey<LootTable> getLootTable();

    @Shadow
    BlockPos getPos();

    @Inject(method = "generateLoot", at = @At("HEAD"))
    default void tarkovsearch$notifyReveal(@Nullable PlayerEntity player, CallbackInfo ci) {
        if (this.getLootTable() == null) {
            // No pending loot table -> this call is a no-op in vanilla too
            // (already looted before, or never had a loot table at all).
            return;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        ServerPlayNetworking.send(serverPlayer, new ChestRevealPayload(this.getPos()));
    }
}
