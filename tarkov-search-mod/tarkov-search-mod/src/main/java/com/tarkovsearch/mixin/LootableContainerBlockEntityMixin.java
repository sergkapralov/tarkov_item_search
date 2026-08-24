package com.tarkovsearch.mixin;

import com.tarkovsearch.network.ChestRevealPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Hooks into the vanilla "roll loot for this container" logic.
 * <p>
 * IMPORTANT: this mixin never changes what loot is generated, when it is
 * generated, or how many items appear. It only *observes* the moment the
 * loot table gets rolled (which vanilla already limits to a single,
 * first-open event per container) and tells the client who triggered it
 * which slots ended up non-empty, so the client can play a purely visual
 * "searching" animation over them.
 */
@Mixin(LootableContainerBlockEntity.class)
public abstract class LootableContainerBlockEntityMixin extends BlockEntity implements Inventory {

    // The pending loot table key. Vanilla sets this to null the instant it
    // rolls the loot, which is exactly the "first open" moment we care about.
    @Shadow
    @Nullable
    public RegistryKey<LootTable> lootTable;

    @Unique
    private boolean tarkovsearch$hadPendingLoot;

    // Fake constructor required by Mixin's "pretend to extend the target's
    // superclass" trick - it is stripped out and never actually runs.
    private LootableContainerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "checkLootInteraction", at = @At("HEAD"))
    private void tarkovsearch$captureBefore(@Nullable PlayerEntity player, CallbackInfo ci) {
        // If lootTable is still set here, this call is about to roll it for
        // the very first time - remember that so the TAIL injector below
        // knows loot generation actually just happened.
        this.tarkovsearch$hadPendingLoot = this.lootTable != null;
    }

    @Inject(method = "checkLootInteraction", at = @At("TAIL"))
    private void tarkovsearch$notifyReveal(@Nullable PlayerEntity player, CallbackInfo ci) {
        if (!this.tarkovsearch$hadPendingLoot) {
            return;
        }
        this.tarkovsearch$hadPendingLoot = false;

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        List<Integer> occupiedSlots = new ArrayList<>();
        for (int i = 0; i < this.size(); i++) {
            if (!this.getStack(i).isEmpty()) {
                occupiedSlots.add(i);
            }
        }
        if (occupiedSlots.isEmpty()) {
            return;
        }

        ServerPlayNetworking.send(serverPlayer, new ChestRevealPayload(this.getPos(), occupiedSlots));
    }
}
