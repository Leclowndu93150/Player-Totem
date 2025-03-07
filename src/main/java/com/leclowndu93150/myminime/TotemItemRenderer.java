package com.leclowndu93150.myminime;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class TotemItemRenderer extends BlockEntityWithoutLevelRenderer {
    private ItemStack originalMainHandItem;
    private ItemStack originalOffHandItem;

    // Fields for static model
    private PlayerModel<AbstractClientPlayer> playerModel;
    private PlayerModel<AbstractClientPlayer> slimPlayerModel;

    // Fields to store rotation values
    private float yBodyRot, yRot, yRotO, yBodyRotO, xRot, xRotO, yHeadRotO, yHeadRot;

    private static boolean isRenderingTotem = false;

    public TotemItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private void initializeModels() {
        if (playerModel == null || slimPlayerModel == null) {
            ModelPart modelPart = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER);
            ModelPart slimModelPart = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM);

            this.playerModel = new PlayerModel<>(modelPart, false);
            this.slimPlayerModel = new PlayerModel<>(slimModelPart, true);
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (isRenderingTotem) return;
        isRenderingTotem = true;

        try {
            Minecraft mc = Minecraft.getInstance();
            AbstractClientPlayer playerToRender = mc.player;
            if (playerToRender == null) return;

            // Use static model only for GUI
            if (displayContext == ItemDisplayContext.GUI) {
                renderStaticModelInGui(playerToRender, stack, poseStack, buffer, combinedLight, combinedOverlay);
            } else {
                renderEntityModel(playerToRender, stack, displayContext, poseStack, buffer, combinedLight, combinedOverlay);
            }
        } finally {
            isRenderingTotem = false;
        }
    }

    private void renderStaticModelInGui(AbstractClientPlayer player, ItemStack stack, PoseStack poseStack,
                                        MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        initializeModels();

        boolean isSlimModel = player.getModelName().equals("slim");
        PlayerModel<AbstractClientPlayer> modelToUse = isSlimModel ? slimPlayerModel : playerModel;

        poseStack.pushPose();

        poseStack.translate(0.5D, 0.75D, 0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-180f));
        poseStack.scale(0.5F, 0.5F, 0.49F);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
        modelToUse.setAllVisible(true);
        modelToUse.young = false;

        modelToUse.renderToBuffer(poseStack, vertexConsumer, combinedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        poseStack.popPose();
    }

    private void renderEntityModel(AbstractClientPlayer playerToRender, ItemStack stack, ItemDisplayContext displayContext,
                                   PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        savePlayerEquipment(playerToRender);
        checkAndReplaceTotemItems(playerToRender, stack);
        saveAndSetRotations(playerToRender);

        PlayerRenderer renderer = (PlayerRenderer) dispatcher.getRenderer(playerToRender);

        poseStack.pushPose();
        setupDisplayContextTransformation(displayContext, poseStack, playerToRender);

        float partialTick = Minecraft.getInstance().getFrameTime();

        renderer.render(playerToRender, 0, partialTick, poseStack, buffer, combinedLight);

        poseStack.popPose();
        restoreRotations(playerToRender);
        restorePlayerEquipment(playerToRender);
    }

    private void saveAndSetRotations(AbstractClientPlayer player) {
        // Save original rotations
        yBodyRot = player.yBodyRot;
        yRot = player.getYRot();
        yRotO = player.yRotO;
        yBodyRotO = player.yBodyRotO;
        xRot = player.getXRot();
        xRotO = player.xRotO;
        yHeadRotO = player.yHeadRotO;
        yHeadRot = player.yHeadRot;

        // Set fixed rotations for rendering
        player.yBodyRot = 180.0F;
        player.setYRot(180.0F);
        player.yBodyRotO = player.yBodyRot;
        player.yRotO = player.getYRot();
        player.setXRot(0);
        player.xRotO = player.getXRot();
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();
    }

    private void restoreRotations(AbstractClientPlayer player) {
        // Restore original rotations
        player.yBodyRot = yBodyRot;
        player.yBodyRotO = yBodyRotO;
        player.setYRot(yRot);
        player.yRotO = yRotO;
        player.setXRot(xRot);
        player.xRotO = xRotO;
        player.yHeadRotO = yHeadRotO;
        player.yHeadRot = yHeadRot;
    }

    private void savePlayerEquipment(AbstractClientPlayer player) {
        originalMainHandItem = player.getItemBySlot(EquipmentSlot.MAINHAND).copy();
        originalOffHandItem = player.getItemBySlot(EquipmentSlot.OFFHAND).copy();
    }

    private void checkAndReplaceTotemItems(AbstractClientPlayer player, ItemStack currentTotem) {
        // Check if main hand or offhand item is our totem
        boolean mainHandHasTotem = isSameTotemItem(player.getItemBySlot(EquipmentSlot.MAINHAND), currentTotem);
        boolean offHandHasTotem = isSameTotemItem(player.getItemBySlot(EquipmentSlot.OFFHAND), currentTotem);

        if (mainHandHasTotem) {
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }

        if (offHandHasTotem) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }

        // If the flip setting is enabled, swap the mainhand and offhand items
        if (PTConfig.shouldFlipHeldItemPosition()) {
            ItemStack mainHandCopy = player.getItemBySlot(EquipmentSlot.MAINHAND).copy();
            ItemStack offHandCopy = player.getItemBySlot(EquipmentSlot.OFFHAND).copy();

            player.setItemSlot(EquipmentSlot.MAINHAND, offHandCopy);
            player.setItemSlot(EquipmentSlot.OFFHAND, mainHandCopy);
        }
    }

    private boolean isSameTotemItem(ItemStack stack, ItemStack totemStack) {
        return !stack.isEmpty() && stack.getItem() == totemStack.getItem();
    }

    private void restorePlayerEquipment(AbstractClientPlayer player) {
        player.setItemSlot(EquipmentSlot.MAINHAND, originalMainHandItem);
        player.setItemSlot(EquipmentSlot.OFFHAND, originalOffHandItem);
    }

    private void setupDisplayContextTransformation(ItemDisplayContext displayContext, PoseStack poseStack, AbstractClientPlayer playerToRender) {
        switch (displayContext) {
            case THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.5, 0.2, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(90f));
                poseStack.scale(0.3F, 0.3F, 0.3F);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.5, 0.2, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(270f));
                poseStack.scale(0.3F, 0.3F, 0.3F);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                if (playerToRender.isFallFlying()) {
                    poseStack.translate(0.9, 0.2, 0);
                    poseStack.mulPose(Axis.YP.rotationDegrees(368f));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-10f));
                    poseStack.scale(0.45F, 0.45F, 0.45F);
                } else {
                    poseStack.translate(0.9, 0.2, 0);
                    poseStack.mulPose(Axis.YP.rotationDegrees(98f));
                    poseStack.scale(0.45F, 0.45F, 0.45F);
                }
            }
            case FIRST_PERSON_LEFT_HAND -> {
                if (playerToRender.isFallFlying()) {
                    if(PTConfig.shouldRenderInsteadOfRocket()){
                        poseStack.translate(2, 0.2, 0);
                        poseStack.mulPose(Axis.YP.rotationDegrees(-368f));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-10f));
                        poseStack.scale(0.45F, 0.45F, 0.45F);
                    }else {
                        poseStack.translate(0.35, 0.4, 0.45);
                        poseStack.mulPose(Axis.YP.rotationDegrees(370f));
                        poseStack.scale(0.3F, 0.3F, 0.3F);
                    }
                } else {
                    poseStack.translate(0.35, 0.4, 0.45);
                    poseStack.mulPose(Axis.YP.rotationDegrees(280f));
                    poseStack.scale(0.3F, 0.3F, 0.3F);
                }
            }
            case GROUND -> {
                poseStack.translate(0.5D, 0.3D, 0.5D);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> {
                poseStack.translate(0.5D, 0D, 0.45D);
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
        }
    }
}