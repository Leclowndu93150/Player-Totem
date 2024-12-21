package com.leclowndu93150.playertotem.mixin;

import com.leclowndu93150.playertotem.TotemItemRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Shadow public abstract Item asItem();

    @Unique
    private TotemItemRenderer totemRenderer;

    @Inject(method = "initializeClient", at = @At("HEAD"), cancellable = true)
    public void initializeClient(Consumer<IClientItemExtensions> consumer, CallbackInfo ci) {
        if ((Object) this == Items.TOTEM_OF_UNDYING) {
            consumer.accept(new IClientItemExtensions() {
                @Override
                public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (totemRenderer == null) {
                        totemRenderer = new TotemItemRenderer();
                    }
                    System.out.println("TotemItemRenderer created for " + Items.TOTEM_OF_UNDYING);
                    return totemRenderer;
                }
            });
            ci.cancel();
        }
    }
}
