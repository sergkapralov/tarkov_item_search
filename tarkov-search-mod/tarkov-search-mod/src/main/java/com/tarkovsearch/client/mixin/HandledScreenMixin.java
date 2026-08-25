package com.tarkovsearch.client.mixin;

import com.tarkovsearch.client.RevealManager;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Purely visual. For any slot that belongs to the chest's own inventory
 * (never the player's own inventory/hotbar) and that {@link RevealManager}
 * says is still "searching", we skip drawing the real item icon and draw a
 * magnifying-glass icon instead, and suppress its tooltip. The real
 * ItemStack is completely untouched - clicking/shift-clicking the slot
 * still works normally even while it's visually hidden.
 * <p>
 * NOTE: as of this Minecraft version, HandledScreen#drawSlot takes two
 * extra int parameters (mouse position) compared to older versions; the
 * @Inject signature below must match exactly or Mixin refuses to apply.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Unique
    private static final Identifier TARKOVSEARCH$SEARCH_ICON =
            Identifier.of("tarkovsearch", "textures/gui/search_icon.png");

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void tarkovsearch$maybeHideSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        BlockPos pos = RevealManager.lastInteractedPos;
        if (pos == null) {
            return;
        }
        // Never touch the player's own inventory/hotbar slots - only the
        // chest/barrel/shulker's own inventory is subject to the effect.
        if (slot.inventory instanceof PlayerInventory) {
            return;
        }
        if (!slot.hasStack()) {
            return;
        }
        if (!RevealManager.isHidden(pos, slot.getIndex())) {
            return;
        }

        ci.cancel();
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TARKOVSEARCH$SEARCH_ICON,
                slot.x, slot.y,
                0.0F, 0.0F,
                16, 16,
                16, 16,
                16, 16
        );
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void tarkovsearch$maybeHideTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        BlockPos pos = RevealManager.lastInteractedPos;
        Slot slot = this.focusedSlot;
        if (pos == null || slot == null) {
            return;
        }
        if (slot.inventory instanceof PlayerInventory) {
            return;
        }
        if (!slot.hasStack()) {
            return;
        }
        if (RevealManager.isHidden(pos, slot.getIndex())) {
            ci.cancel();
        }
    }
}
