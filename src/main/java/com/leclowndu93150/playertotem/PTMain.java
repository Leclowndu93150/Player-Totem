package com.leclowndu93150.playertotem;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(PTMain.MODID)
public class PTMain {
    public static final String MODID = "playertotem";
    private static final Logger LOGGER = LogUtils.getLogger();

    public PTMain(IEventBus modEventBus, ModContainer modContainer) {

    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSetup {

        public static final TotemItemRenderer TOTEM_ITEM_RENDERER = new TotemItemRenderer();

        @SubscribeEvent
        public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
            LOGGER.info("Registering client extensions");
            event.registerItem(new IClientItemExtensions() {
                @Override
                public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return TOTEM_ITEM_RENDERER;
                }
            }, Items.TOTEM_OF_UNDYING);
        }
    }

}
