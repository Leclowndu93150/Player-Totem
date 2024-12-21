package com.leclowndu93150.playertotem;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

@Mod(PTMain.MODID)
public class PTMain {
    public static final String MODID = "playertotem";
    private static final Logger LOGGER = LogUtils.getLogger();
    static PTConfig config = new PTConfig();

    public PTMain() {
        LOGGER.info("Player Totem mod loaded");
        config.loadConfig();
    }

}
