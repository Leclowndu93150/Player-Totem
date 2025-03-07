package com.leclowndu93150.myminime;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PTMain.MODID, value = Dist.CLIENT)
public class KeyInputHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (KeyBindings.TOGGLE_ROCKET_RENDER.consumeClick()) {
            boolean newValue = !PTConfig.shouldRenderInsteadOfRocket();
            PTConfig.RENDER_INSTEAD_OF_ROCKET.set(newValue);
        }
    }
}
