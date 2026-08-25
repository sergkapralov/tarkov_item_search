package com.tarkovsearch.client.mixin;

import com.tarkovsearch.client.RevealManager;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Purely visual. For any slot that belongs to a real lootable container
 * (chest, barrel, shulker box, hopper, etc. - anything implementing
 * LootableInventory; never the player's own inventory, creative menu,
 * crafting grids, or any other screen) and that {@link RevealManager} says
 * is still "searching", we skip drawing the real item icon and draw a
 * spinning magnifying-glass icon instead, and suppress its tooltip. The
 * real ItemStack is completely untouched - clicking/shift-clicking the slot
 * still works normally even while it's visually hidden.
 * <p>
 * Gating on "slot.inventory instanceof LootableInventory" (rather than just
 * excluding PlayerInventory) is what keeps this effect out of the creative
 * inventory, crafting screens, and anything else that isn't an actual
 * world container - only real lootable containers implement that interface.
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

    /** Full 360-degree spin period, in milliseconds. */
    @Unique
    private static final long TARKOVSEARCH$SPIN_PERIOD_MS = 1400L;

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void tarkovsearch$maybeHideSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        BlockPos pos = RevealManager.lastInteractedPos;
        if (pos == null) {
            return;
        }
        // Only ever hide slots that belong to a real lootable container -
        // this is what keeps the effect out of creative inventory, crafting
        // grids, the player's own inventory, and any other unrelated screen.
        if (!(slot.inventory instanceof LootableInventory)) {
            return;
        }
        if (!slot.hasStack()) {
            return;
        }
        if (!RevealManager.isHidden(pos, slot.getIndex())) {
            return;
        }

        ci.cancel();

        float angle = (System.currentTimeMillis() % TARKOVSEARCH$SPIN_PERIOD_MS)
                / (float) TARKOVSEARCH$SPIN_PERIOD_MS * 360.0F;

        // As of this Minecraft version, GUI rendering uses a flat 2D
        // Matrix3x2fStack (JOML) instead of the old 3D MatrixStack - no z
        // parameter, and push/pop are named pushMatrix()/popMatrix().
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(slot.x + 8.0F, slot.y + 8.0F);
        matrices.rotate((float) Math.toRadians(angle));
        matrices.translate(-8.0F, -8.0F);
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TARKOVSEARCH$SEARCH_ICON,
                0, 0,
                0.0F, 0.0F,
                16, 16,
                16, 16,
                16, 16
        );
        matrices.popMatrix();
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void tarkovsearch$maybeHideTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        BlockPos pos = RevealManager.lastInteractedPos;
        Slot slot = this.focusedSlot;
        if (pos == null || slot == null) {
            return;
        }
        if (!(slot.inventory instanceof LootableInventory)) {
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
