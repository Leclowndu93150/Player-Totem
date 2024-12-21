package com.leclowndu93150.playertotem.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Items;

public class PTMainClient implements ClientModInitializer {

    static PTConfig config = new PTConfig();

    @Override
    public void onInitializeClient() {
        config.loadConfig();
        BuiltinItemRendererRegistry.INSTANCE.register(Items.TOTEM_OF_UNDYING, new TotemItemRenderer());
    }

}
