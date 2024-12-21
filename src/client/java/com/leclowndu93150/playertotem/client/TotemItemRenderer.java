package com.leclowndu93150.playertotem.client;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TotemItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().executor(Executors.newFixedThreadPool(2)).build();
    private static final Executor ASYNC_EXECUTOR = Executors.newFixedThreadPool(2);

    private final Map<String, String> uuidCache = new ConcurrentHashMap<>();
    private final Map<String, ResourceLocation> skinCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> slimModelCache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> loadingFutures = new ConcurrentHashMap<>();

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

    private CompletableFuture<String> fetchUUIDFromAPI(String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://api.mojang.com/users/profiles/minecraft/%s", username)))
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(response -> {
                    MojangUUIDResponse uuidData = GSON.fromJson(response, MojangUUIDResponse.class);
                    return uuidData != null ? uuidData.id : null;
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    private void loadSkinForName(String username) {
        if (skinCache.containsKey(username) || loadingFutures.containsKey(username)) {
            return;
        }

        CompletableFuture<Void> loadingFuture = CompletableFuture.supplyAsync(() ->
                        uuidCache.computeIfAbsent(username, name -> {
                            try {
                                return fetchUUIDFromAPI(name).get();
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }), ASYNC_EXECUTOR)
                .thenCompose(uuid -> {
                    if (uuid == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s", uuid)))
                            .GET()
                            .build();

                    return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenApply(HttpResponse::body)
                            .thenApply(response -> GSON.fromJson(response, MojangProfileResponse.class));
                })
                .thenCompose(profileData -> {
                    if (profileData == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    String skinUrl = profileData.getSkinURL();
                    boolean isSlimModel = profileData.isSlimModel();

                    if (skinUrl == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(skinUrl))
                            .GET()
                            .build();

                    return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                            .thenApply(response -> {
                                try {
                                    NativeImage nativeImage = NativeImage.read(response.body());
                                    return new SkinData(nativeImage, isSlimModel);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            });
                })
                .thenAccept(skinData -> {
                    if (skinData == null) {
                        setDefaultSkin(username);
                        return;
                    }

                    Minecraft.getInstance().execute(() -> {
                        try {
                            DynamicTexture texture = new DynamicTexture(skinData.nativeImage);
                            ResourceLocation textureLocation = new ResourceLocation("playertotem", "skin_" + username.toLowerCase());
                            Minecraft.getInstance().getTextureManager().register(textureLocation, texture);
                            skinCache.put(username, textureLocation);
                            slimModelCache.put(username, skinData.isSlimModel);
                        } catch (Exception e) {
                            e.printStackTrace();
                            setDefaultSkin(username);
                        } finally {
                            skinData.nativeImage.close();
                            loadingFutures.remove(username);
                        }
                    });
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    setDefaultSkin(username);
                    loadingFutures.remove(username);
                    return null;
                });

        loadingFutures.put(username, loadingFuture);
    }

    private void setDefaultSkin(String username) {
        Minecraft.getInstance().execute(() -> {
            skinCache.put(username, Minecraft.getInstance().player.getSkinTextureLocation());
            slimModelCache.put(username, false);
        });
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        initializeModels();

        ResourceLocation skinLocation;
        boolean isSlimModel = false;

        if (stack.hasCustomHoverName()) {
            String username = stack.getHoverName().getString();
            if (!username.isEmpty() && !(Minecraft.getInstance().screen instanceof AnvilScreen)) {
                loadSkinForName(username);
            }
            skinLocation = skinCache.getOrDefault(username, Minecraft.getInstance().player.getSkinTextureLocation());
            isSlimModel = slimModelCache.getOrDefault(username, false);
        } else {
            skinLocation = Minecraft.getInstance().player.getSkinTextureLocation();
        }

        PlayerModel<AbstractClientPlayer> modelToUse = isSlimModel ? slimPlayerModel : playerModel;

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
        if (PTMainClient.config.canMoveArms()) {
            modelToUse.setupAnim(Minecraft.getInstance().player, 0, 0, tick, 0, 0);
        }
        modelToUse.renderToBuffer(poseStack, vertexConsumer, combinedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        poseStack.popPose();
    }

    private record SkinData(NativeImage nativeImage, boolean isSlimModel) {}

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