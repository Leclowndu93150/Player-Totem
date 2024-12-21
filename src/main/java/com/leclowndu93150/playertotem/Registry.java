package com.leclowndu93150.playertotem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Registry{

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "minecraft");

    public static final RegistryObject<TotemItem> TOTEM_OF_UNDYING = ITEMS.register("totem_of_undying", () -> new TotemItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

}
