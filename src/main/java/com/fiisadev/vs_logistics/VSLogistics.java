package com.fiisadev.vs_logistics;

import com.fiisadev.vs_logistics.registry.*;
import com.fiisadev.vs_logistics.managers.JointManager;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.valkyrienskies.mod.api.ValkyrienSkies;

@Mod(VSLogistics.MOD_ID)
public class VSLogistics {
    public static final String MOD_ID = "vs_logistics";

    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
            .setTooltipModifierFactory((item) -> (
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)).andThen(TooltipModifier.mapNull(KineticStats.create(item))
            ));

    private static final Logger LOGGER = LogUtils.getLogger();

    public VSLogistics() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.register(this);

        LogisticsCreativeModeTabs.register(modEventBus);
        REGISTRATE.registerEventListeners(modEventBus);
        LogisticsBlocks.register();
        LogisticsItems.register();
        LogisticsBlockEntities.register();
        LogisticsEntityTypes.register();

        modEventBus.addListener(EventPriority.LOWEST, LogisticsDatagen::gatherData);
        modEventBus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ValkyrienSkies.api().getPhysTickEvent().on(JointManager::onPhysicsTick);
    }

    public void commonSetup(final FMLCommonSetupEvent event) { event.enqueueWork(LogisticsNetwork::register); }

    public static ResourceLocation asResource(String path) {
        //noinspection removal
        return new ResourceLocation(MOD_ID, path);
    }

    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }
}
