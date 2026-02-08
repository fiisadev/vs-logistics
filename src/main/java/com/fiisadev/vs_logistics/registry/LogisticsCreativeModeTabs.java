package com.fiisadev.vs_logistics.registry;

import com.fiisadev.vs_logistics.VSLogistics;
import com.fiisadev.vs_logistics.content.pipe_wrench.PipeWrenchItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class LogisticsCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VSLogistics.MOD_ID);

    public static final RegistryObject<CreativeModeTab> BASE_CREATIVE_TAB = CREATIVE_MODE_TABS.register("base",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.base"))
                    .icon(() -> new ItemStack(LogisticsBlocks.FLUID_PUMP.get()))
                    .displayItems(((parameters, output) -> {
                        output.accept(LogisticsBlocks.FLUID_PORT.get());
                        output.accept(LogisticsBlocks.FLUID_PUMP.get());
                        output.accept(PipeWrenchItem.getPipeWrench());
                    }))
                    .build()
    );

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}
