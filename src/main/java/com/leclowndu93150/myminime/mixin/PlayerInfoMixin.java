package com.leclowndu93150.myminime.mixin;

import com.leclowndu93150.myminime.IPlayerInfoMixin;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public class PlayerInfoMixin implements IPlayerInfoMixin {
    private ResourceLocation temporarySkin = null;

    @Inject(method = "getSkinLocation", at = @At("HEAD"), cancellable = true)
    public void getSkinLocation(CallbackInfoReturnable<ResourceLocation> cir) {
        if (temporarySkin != null) {
            cir.setReturnValue(temporarySkin);
        }
    }

    @Override
    public void setTemporarySkin(ResourceLocation skin) {
        this.temporarySkin = skin;
    }

    @Override
    public void resetTemporarySkin() {
        this.temporarySkin = null;
    }
}