package com.leclowndu93150.myminime;

import com.google.gson.Gson;
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
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;

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
    private ItemStack originalMainHandItem;
    private ItemStack originalOffHandItem;

    // Fields for static model
    private PlayerModel<AbstractClientPlayer> playerModel;
    private PlayerModel<AbstractClientPlayer> slimPlayerModel;

    // Fields to store rotation values
    private float yBodyRot, yRot, yRotO, yBodyRotO, xRot, xRotO, yHeadRotO, yHeadRot;

    // Fields to store swinging state
    private boolean wasSwinging;
    private InteractionHand originalSwingingArm;

    private static boolean isRenderingTotem = false;

    // Skin loading and caching fields
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().executor(Executors.newFixedThreadPool(2)).build();
    private static final Executor ASYNC_EXECUTOR = Executors.newFixedThreadPool(2);

    private final Map<String, String> uuidCache = new ConcurrentHashMap<>();
    private final Map<String, ResourceLocation> skinCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> slimModelCache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> loadingFutures = new ConcurrentHashMap<>();

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

        ResourceLocation skinLocation;
        boolean isSlimModel = false;

        // Check for custom named totem and load skin
        if (stack.hasCustomHoverName()) {
            String username = stack.getHoverName().getString();
            if (!username.isEmpty() && !(Minecraft.getInstance().screen instanceof AnvilScreen)) {
                loadSkinForName(username);
            }
            skinLocation = skinCache.getOrDefault(username, player.getSkinTextureLocation());
            isSlimModel = slimModelCache.getOrDefault(username, false);
        } else {
            skinLocation = player.getSkinTextureLocation();
            isSlimModel = player.getModelName().equals("slim");
        }

        PlayerModel<AbstractClientPlayer> modelToUse = isSlimModel ? slimPlayerModel : playerModel;

        poseStack.pushPose();

        poseStack.translate(0.5D, 0.75D, 0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-180f));
        poseStack.scale(0.5F, 0.5F, 0.49F);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(skinLocation));
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

        // Check for custom named totem for custom skin
        ResourceLocation customSkin = null;
        if (stack.hasCustomHoverName()) {
            String username = stack.getHoverName().getString();
            if (!username.isEmpty() && !(Minecraft.getInstance().screen instanceof AnvilScreen)) {
                loadSkinForName(username);
                if (skinCache.containsKey(username)) {
                    customSkin = skinCache.get(username);
                }
            }
        }

        if (customSkin != null && playerToRender.getPlayerInfo() != null) {
            Object playerInfo = playerToRender.getPlayerInfo();
            if (playerInfo instanceof IPlayerInfoMixin mixin) {
                mixin.setTemporarySkin(customSkin);
            }
        }

        PlayerRenderer renderer = (PlayerRenderer) dispatcher.getRenderer(playerToRender);

        poseStack.pushPose();
        setupDisplayContextTransformation(displayContext, poseStack, playerToRender);

        float partialTick = Minecraft.getInstance().getFrameTime();

        renderer.render(playerToRender, 0, partialTick, poseStack, buffer, combinedLight);

        poseStack.popPose();

        if (customSkin != null && playerToRender.getPlayerInfo() != null) {
            Object playerInfo = playerToRender.getPlayerInfo();
            if (playerInfo instanceof IPlayerInfoMixin mixin) {
                mixin.resetTemporarySkin();
            }
        }

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

        // Save swinging state
        wasSwinging = player.swinging;
        originalSwingingArm = player.swingingArm;

        // If flipping items and player is swinging, flip the arm only for rendering
        if (PTConfig.shouldFlipHeldItemPosition() && player.swinging) {
            player.swingingArm = player.swingingArm == InteractionHand.MAIN_HAND ?
                    InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        }

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

        // Restore swinging state
        player.swinging = wasSwinging;
        player.swingingArm = originalSwingingArm;
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

    // Skin loading methods
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
                                    com.mojang.blaze3d.platform.NativeImage nativeImage =
                                            com.mojang.blaze3d.platform.NativeImage.read(response.body());
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
                            ResourceLocation textureLocation = new ResourceLocation("myminime", "skin_" + username.toLowerCase());
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

    // Support classes for JSON parsing
    private record SkinData(com.mojang.blaze3d.platform.NativeImage nativeImage, boolean isSlimModel) {}

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