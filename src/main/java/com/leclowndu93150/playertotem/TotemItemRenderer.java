package com.leclowndu93150.playertotem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class TotemItemRenderer extends BlockEntityWithoutLevelRenderer {

    private PlayerModel<AbstractClientPlayer> playerModel;

    public TotemItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private void initializeModel() {
        if (playerModel == null) {
            EntityModelSet entityModelSet = Minecraft.getInstance().getEntityModels();
            ModelPart modelPart = entityModelSet.bakeLayer(ModelLayers.PLAYER);
            this.playerModel = new PlayerModel<>(modelPart, false);
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        initializeModel();
        poseStack.pushPose();

        switch (displayContext) {
            case GUI -> {
                poseStack.translate(0, -3.25 / 16.0F, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(30));
                poseStack.mulPose(Axis.YP.rotationDegrees(225));
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
            case GROUND -> {
                poseStack.translate(0, 2 / 16.0F, 0);
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.25 / 16.0F, 0.5 / 16.0F, 1.5 / 16.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(0));
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-10));
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(3 / 16.0F, 0.5 / 16.0F, 1.5 / 16.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(0));
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-10));
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            default -> {
                poseStack.scale(0.4F, -0.4F, 0.4F);
            }
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ResourceLocation skinLocation = player.getSkin().texture();
            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(skinLocation));

            this.playerModel.setAllVisible(true);
            this.playerModel.head.xRot = -0.5F;
            this.playerModel.hat.xRot = -0.5F;

            float tick = Minecraft.getInstance().player.tickCount;
            playerModel.setupAnim(player, 0, 0, tick, 0, 0);

            playerModel.renderToBuffer(poseStack, vertexConsumer, combinedLight, OverlayTexture.NO_OVERLAY, 1);
        }

        poseStack.popPose();
    }

}
