package com.leclowndu93150.myminime;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PTMain.MODID, value = Dist.CLIENT)
public class HandRenderHandler {

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!PTConfig.shouldRenderInsteadOfRocket()) {
            return;
        }

        if (event.getHand() == InteractionHand.MAIN_HAND && Minecraft.getInstance().player.isFallFlying()) {
            event.setCanceled(true);
        }
    }
}