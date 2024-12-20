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
import net.minecraft.world.item.Item;
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
            case THIRD_PERSON_RIGHT_HAND:
                poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                poseStack.translate(0.5,-0.6,-0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(90f));
                poseStack.scale(0.3F, 0.3F, 0.3F);
                break;
            case THIRD_PERSON_LEFT_HAND:
                poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                poseStack.translate(0.5,-0.6,-0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(270f));
                poseStack.scale(0.3F, 0.3F, 0.3F);
                break;
            case FIRST_PERSON_RIGHT_HAND:
                poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                poseStack.translate(0.60,-0.80,-0.45);
                poseStack.mulPose(Axis.YP.rotationDegrees(70f));
                poseStack.scale(0.3F, 0.3F, 0.3F);
                break;
            case FIRST_PERSON_LEFT_HAND:
                poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                poseStack.translate(0.3,-0.80,-0.45);
                poseStack.mulPose(Axis.YP.rotationDegrees(280f));
                poseStack.scale(0.3F, 0.3F, 0.3F);
                break;
            case GROUND:
                poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                poseStack.translate(0.5,-0.55,-0.5);
                poseStack.scale(0.19F, 0.2F, 0.2F);
                break;
            case GUI:
                poseStack.translate(0.5D, 0.75D, 0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(-180f));
                poseStack.scale(0.5F, 0.5F, 0.49F);
                break;
            case FIXED:
                poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                poseStack.translate(0.5,-0.7,-0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(180f));
                poseStack.scale(0.5F, 0.5F, 0.5F);
                break;
        }

        ResourceLocation skinLocation = customSkin != null ? customSkin : Minecraft.getInstance().player.getSkin().texture();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(skinLocation));

        this.playerModel.setAllVisible(true);
        this.playerModel.young = false;

        float tick = Minecraft.getInstance().player.tickCount;
        playerModel.setupAnim(Minecraft.getInstance().player, 0, 0, tick, 0, 0);

        playerModel.renderToBuffer(poseStack, vertexConsumer, combinedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();

        }
    }