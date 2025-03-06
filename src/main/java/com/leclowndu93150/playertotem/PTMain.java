package com.leclowndu93150.playertotem;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(PTMain.MODID)
public class PTMain {
    public static final String MODID = "myminime";
    private static final Logger LOGGER = LogUtils.getLogger();
    static PTConfig config = new PTConfig();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> MINIME_TAB = CREATIVE_MODE_TABS.register("myminime", () -> CreativeModeTab.builder()
            .title(Component.literal("My Mini-Me")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> Registry.TOTEM_OF_UNDYING.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(Registry.TOTEM_OF_UNDYING.get());
            }).build());

    public PTMain() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Registry.ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        PTConfig.register();
    }
}
