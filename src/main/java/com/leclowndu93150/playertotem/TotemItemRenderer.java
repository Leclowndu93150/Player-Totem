package com.leclowndu93150.playertotem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            if (displayContext != ItemDisplayContext.GUI) {
                poseStack.translate(0.5F, 0.5F, 0.5F);
            }

            float scale = (displayContext == ItemDisplayContext.GUI) ? 0.6F : 0.4F;
            poseStack.scale(scale, -scale, scale);

            ResourceLocation skinLocation = player.getSkin().texture();
            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(skinLocation));

            this.playerModel.setAllVisible(true);
            this.playerModel.head.xRot = -0.5F;
            this.playerModel.hat.xRot = -0.5F;

            float tick = Minecraft.getInstance().player.tickCount;
            playerModel.setupAnim(player, 0, 0, tick, 0, 0);

            playerModel.renderToBuffer(poseStack, vertexConsumer, combinedLight, OverlayTexture.NO_OVERLAY, 1);
        }

        System.out.println("Rendering totem item");
        poseStack.popPose();
    }
}
