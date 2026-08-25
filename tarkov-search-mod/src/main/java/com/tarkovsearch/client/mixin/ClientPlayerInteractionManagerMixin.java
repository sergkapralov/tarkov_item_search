package com.tarkovsearch.client.mixin;

import com.tarkovsearch.client.RevealManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A screen (chest, barrel, shulker box...) doesn't know its own world
 * position once opened. We grab it here, the moment the player right-clicks
 * the block, right before the server would respond with an "open screen"
 * packet. Purely a lookup cache for {@code HandledScreenMixin} - changes no
 * gameplay behaviour whatsoever.
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void tarkovsearch$captureInteractedPos(ClientPlayerEntity player, net.minecraft.util.Hand hand,
                                                     BlockHitResult hitResult, CallbackInfoReturnable<?> cir) {
        RevealManager.lastInteractedPos = hitResult.getBlockPos();
    }
}
