package com.leclowndu93150.playertotem;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class TotemItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Gson GSON = new Gson();
    private final Map<String, CompletableFuture<ResourceLocation>> skinFutures = new HashMap<>();
    private final Map<String, Boolean> slimModelCache = new HashMap<>();

    private PlayerModel<AbstractClientPlayer> playerModel;
    private PlayerModel<AbstractClientPlayer> slimPlayerModel;

    public TotemItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private void initializeModels() {
        if (playerModel == null || slimPlayerModel == null) {
            EntityModelSet entityModelSet = Minecraft.getInstance().getEntityModels();
            ModelPart modelPart = entityModelSet.bakeLayer(ModelLayers.PLAYER);
            ModelPart slimModelPart = entityModelSet.bakeLayer(ModelLayers.PLAYER_SLIM);

            this.playerModel = new PlayerModel<>(modelPart, false);
            this.slimPlayerModel = new PlayerModel<>(slimModelPart, true);
        }
    }

    private CompletableFuture<String> fetchUUIDAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String uuidUrl = String.format("https://api.mojang.com/users/profiles/minecraft/%s", username);
                String response = new Scanner(new URL(uuidUrl).openStream()).useDelimiter("\\A").next();
                MojangUUIDResponse uuidData = GSON.fromJson(response, MojangUUIDResponse.class);
                return uuidData.id;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private CompletableFuture<ResourceLocation> fetchSkinAsync(String username) {
        return fetchUUIDAsync(username).thenCompose(uuid -> {
            if (uuid == null || uuid.isEmpty()) {
                return CompletableFuture.completedFuture(Minecraft.getInstance().player.getSkin().texture());
            }

            return CompletableFuture.supplyAsync(() -> {
                try {
                    String profileUrl = String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s", uuid);
                    String profileResponse = new Scanner(new URL(profileUrl).openStream()).useDelimiter("\\A").next();
                    MojangProfileResponse profileData = GSON.fromJson(profileResponse, MojangProfileResponse.class);

                    String skinUrl = profileData.getSkinURL();
                    boolean isSlimModel = profileData.isSlimModel();

                    if (skinUrl == null || skinUrl.isEmpty()) {
                        throw new IOException("Skin URL is null or empty");
                    }

                    NativeImage nativeImage;
                    try (var stream = new URL(skinUrl).openStream()) {
                        nativeImage = NativeImage.read(stream);
                    }

                    if (nativeImage == null) {
                        throw new IOException("Failed to load NativeImage (null result)");
                    }

                    DynamicTexture texture = new DynamicTexture(nativeImage);
                    ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath("playertotem", "skin_" + username.toLowerCase());
                    Minecraft.getInstance().execute(() -> Minecraft.getInstance().getTextureManager().register(textureLocation, texture));
                    slimModelCache.put(username, isSlimModel);
                    return textureLocation;
                } catch (Exception e) {
                    e.printStackTrace();
                    return Minecraft.getInstance().player.getSkin().texture();
                }
            });
        });
    }

    private CompletableFuture<ResourceLocation> getSkin(String username) {
        return skinFutures.computeIfAbsent(username, this::fetchSkinAsync);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        initializeModels();

        if (!stack.has(DataComponents.CUSTOM_NAME)) {
            return;
        }

        String username = stack.get(DataComponents.CUSTOM_NAME).getString();
        if (username.isEmpty()) {
            return;
        }

        getSkin(username).thenAccept(skinLocation -> {
            PlayerModel<AbstractClientPlayer> modelToUse = slimModelCache.getOrDefault(username, false) ? slimPlayerModel : playerModel;

            poseStack.pushPose();
            switch (displayContext) {
                case THIRD_PERSON_RIGHT_HAND -> {
                    poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                    poseStack.translate(0.5, -0.6, -0.5);
                    poseStack.mulPose(Axis.YP.rotationDegrees(90f));
                    poseStack.scale(0.3F, 0.3F, 0.3F);
                }
                case THIRD_PERSON_LEFT_HAND -> {
                    poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                    poseStack.translate(0.5, -0.6, -0.5);
                    poseStack.mulPose(Axis.YP.rotationDegrees(270f));
                    poseStack.scale(0.3F, 0.3F, 0.3F);
                }
                case FIRST_PERSON_RIGHT_HAND -> {
                    poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                    poseStack.translate(0.60, -0.80, -0.45);
                    poseStack.mulPose(Axis.YP.rotationDegrees(70f));
                    poseStack.scale(0.3F, 0.3F, 0.3F);
                }
                case FIRST_PERSON_LEFT_HAND -> {
                    poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                    poseStack.translate(0.3, -0.80, -0.45);
                    poseStack.mulPose(Axis.YP.rotationDegrees(280f));
                    poseStack.scale(0.3F, 0.3F, 0.3F);
                }
                case GROUND -> {
                    poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                    poseStack.translate(0.5, -0.55, -0.5);
                    poseStack.scale(0.19F, 0.2F, 0.2F);
                }
                case GUI -> {
                    poseStack.translate(0.5D, 0.75D, 0D);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-180f));
                    poseStack.scale(0.5F, 0.5F, 0.49F);
                }
                case FIXED -> {
                    poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                    poseStack.translate(0.5, -0.7, -0.5);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180f));
                    poseStack.scale(0.5F, 0.5F, 0.5F);
                }
            }

            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(skinLocation));
            modelToUse.setAllVisible(true);
            modelToUse.young = false;

            float tick = Minecraft.getInstance().player.tickCount;
            if (PTMain.config.canMoveArms()) {
                modelToUse.setupAnim(Minecraft.getInstance().player, 0, 0, tick, 0, 0);
            }
            modelToUse.renderToBuffer(poseStack, vertexConsumer, combinedLight, OverlayTexture.NO_OVERLAY, -1);

            poseStack.popPose();
        });
    }
    private static class MojangProfileResponse {
        String id;
        String name;
        Property[] properties;

        String getSkinURL() {
            for (Property property : properties) {
                if ("textures".equals(property.name)) {
                    String decoded = new String(Base64.getDecoder().decode(property.value));
                    TexturesResponse textures = GSON.fromJson(decoded, TexturesResponse.class);
                    return textures.textures.SKIN.url;
                }
            }
            return null;
        }

        boolean isSlimModel() {
            for (Property property : properties) {
                if ("textures".equals(property.name)) {
                    String decoded = new String(Base64.getDecoder().decode(property.value));
                    TexturesResponse textures = GSON.fromJson(decoded, TexturesResponse.class);
                    return textures.textures.SKIN.metadata != null &&
                            "slim".equals(textures.textures.SKIN.metadata.model);
                }
            }
            return false;
        }
    }

    private static class Property {
        String name;
        String value;
    }

    private static class MojangUUIDResponse {
        String id;
    }

    private static class TexturesResponse {
        Textures textures;

        private static class Textures {
            Skin SKIN;
        }

        private static class Skin {
            String url;
            Metadata metadata;
        }

        private static class Metadata {
            String model;
        }
    }
}