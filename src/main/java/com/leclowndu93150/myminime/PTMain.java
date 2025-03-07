package com.leclowndu93150.myminime;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
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
