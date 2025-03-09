package com.leclowndu93150.myminime;

import net.minecraft.resources.ResourceLocation;

public interface IPlayerInfoMixin {
    void setTemporarySkin(ResourceLocation skin);
    void resetTemporarySkin();
}