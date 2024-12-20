package com.leclowndu93150.playertotem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class TotemItemRenderer extends BlockEntityWithoutLevelRenderer {

    private PlayerModel<AbstractClientPlayer> playerModel;
    private ResourceLocation customSkin;

    public TotemItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.customSkin = null;
    }

    private void initializeModel() {
        if (playerModel == null) {
            EntityModelSet entityModelSet = Minecraft.getInstance().getEntityModels();
            ModelPart modelPart = entityModelSet.bakeLayer(ModelLayers.PLAYER);
            this.playerModel = new PlayerModel<>(modelPart, false);
        }
    }

    public void setCustomSkin(ResourceLocation skin) {
        this.customSkin = skin;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        initializeModel();
        poseStack.pushPose();

        switch (displayContext) {
            case GUI -> {
                // rotation=[0,-180,0], translation=[-4.25,-4.25,0], scale=[0.5,0.5,0.49]
                poseStack.translate(-4.25/16.0F, -4.25/16.0F, 0);
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.scale(0.5F, 0.5F, 0.49F);
            }
            case GROUND -> {
                // translation=[1.5,0,1.5], scale=[0.19,0.2,0.2]
                poseStack.translate(1.5/16.0F, 0, 1.5/16.0F);
                poseStack.scale(0.19F, 0.2F, 0.2F);
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                // rotation=[0,90,0], translation=[1.75,-0.25,0.25], scale=[0.2,0.2,0.2]
                poseStack.translate(1.75/16.0F, -0.25/16.0F, 0.25/16.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.scale(0.2F, 0.2F, 0.2F);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                // rotation=[0,90,0], translation=[1.75,-0.25,3.5], scale=[0.2,0.2,0.2]
                poseStack.translate(1.75/16.0F, -0.25/16.0F, 3.5/16.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.scale(0.2F, 0.2F, 0.2F);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                // rotation=[0,103,0], translation=[-0.5,3,2], scale=[0.2,0.2,0.2]
                poseStack.translate(-0.5/16.0F, 3/16.0F, 2/16.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(103));
                poseStack.scale(0.2F, 0.2F, 0.2F);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                // rotation=[0,103,0], translation=[0,3,5], scale=[0.2,0.2,0.2]
                poseStack.translate(0, 3/16.0F, 5/16.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(103));
                poseStack.scale(0.2F, 0.2F, 0.2F);
            }
            case FIXED -> {
                // rotation=[-5,0,0], translation=[3,-3,2], scale=[0.4,0.4,0.4]
                poseStack.translate(3/16.0F, -3/16.0F, 2/16.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-5));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            default -> {
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
        }

        ResourceLocation skinLocation = customSkin != null ? customSkin : Minecraft.getInstance().player.getSkin().texture();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(skinLocation));

        this.playerModel.setAllVisible(true);
        this.playerModel.young = false;
        this.playerModel.head.xRot = -0.5F;
        this.playerModel.hat.xRot = -0.5F;

        float tick = Minecraft.getInstance().player.tickCount;
        playerModel.setupAnim(Minecraft.getInstance().player, 0, 0, tick, 0, 0);

        int maxLight = 0xF000F0;
        playerModel.renderToBuffer(poseStack, vertexConsumer, maxLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();

        //DEBUG TEXT
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        poseStack.scale(0.01F, 0.01F, 0.01F);

        Minecraft.getInstance().font.drawInBatch(
                customSkin != null ? customSkin.toString() : "Default Skin",
                0, 0,
                0xFFFFFF,
                false,
                poseStack.last().pose(),
                buffer,
                Font.DisplayMode.NORMAL,
                0,
                combinedLight
        );

        poseStack.popPose();
    }
}