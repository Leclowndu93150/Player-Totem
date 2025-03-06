package com.leclowndu93150.playertotem.mixin;

import com.leclowndu93150.playertotem.PTConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class LeftHandF1Mixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ItemInHandRenderer itemInHandRenderer;

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void onRenderItemInHand(PoseStack poseStack, Camera camera, float partialTicks, CallbackInfo ci) {
        if (this.minecraft.options.hideGui) {
            ci.cancel();

            if (!minecraft.player.getOffhandItem().isEmpty() && PTConfig.shouldRenderInsteadOfRocket()) {
                poseStack.pushPose();

                boolean originalHideGui = this.minecraft.options.hideGui;
                this.minecraft.options.hideGui = false;

                itemInHandRenderer.renderArmWithItem(
                        minecraft.player,
                        partialTicks,
                        camera.getXRot(),
                        InteractionHand.OFF_HAND,
                        0.0F,
                        minecraft.player.getOffhandItem(),
                        1.0F,
                        poseStack,
                        minecraft.renderBuffers.bufferSource(),
                        minecraft.getEntityRenderDispatcher().getPackedLightCoords(minecraft.player, partialTicks)
                );

                this.minecraft.options.hideGui = originalHideGui;

                poseStack.popPose();
                minecraft.renderBuffers.bufferSource().endBatch();
            }
        }
    }
}