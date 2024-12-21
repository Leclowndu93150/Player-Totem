package com.leclowndu93150.playertotem;

import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class TotemItem extends Item {
    public TotemItem(Properties properties) {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    static TotemItemRenderer TOTEM_ITEM_RENDERER = new TotemItemRenderer();

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return TOTEM_ITEM_RENDERER;
            }
        });
    }
}
