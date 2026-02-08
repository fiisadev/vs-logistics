package com.fiisadev.vs_logistics;

import com.fiisadev.vs_logistics.config.LogisticsClientConfig;
import com.fiisadev.vs_logistics.config.LogisticsCommonConfig;
import com.fiisadev.vs_logistics.registry.*;
import com.fiisadev.vs_logistics.managers.JointManager;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.valkyrienskies.mod.api.ValkyrienSkies;

@Mod(VSLogistics.MOD_ID)
public class VSLogistics {
    public static final String MOD_ID = "vs_logistics";

    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
            .setTooltipModifierFactory((item) -> (
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)).andThen(TooltipModifier.mapNull(KineticStats.create(item))
            ));

//    private static final Logger LOGGER = LogUtils.getLogger();

    public VSLogistics(FMLJavaModLoadingContext ctx) {
        IEventBus modEventBus = ctx.getModEventBus();

        MinecraftForge.EVENT_BUS.register(this);

        LogisticsCreativeModeTabs.register(modEventBus);
        REGISTRATE.registerEventListeners(modEventBus);
        LogisticsBlocks.register();
        LogisticsItems.register();
        LogisticsBlockEntities.register();
        LogisticsEntityTypes.register();

        modEventBus.addListener(EventPriority.LOWEST, LogisticsDatagen::gatherData);
        modEventBus.addListener(this::commonSetup);

        ValkyrienSkies.api().getPhysTickEvent().on(JointManager::onPhysicsTick);

        ctx.registerConfig(ModConfig.Type.CLIENT, LogisticsClientConfig.SPEC);
        ctx.registerConfig(ModConfig.Type.COMMON, LogisticsCommonConfig.SPEC);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> LogisticsPonderPlugin::registerPlugin);
    }

    public void commonSetup(final FMLCommonSetupEvent event) { event.enqueueWork(LogisticsNetwork::register); }

    public static ResourceLocation asResource(String path) {
        //noinspection removal
        return new ResourceLocation(MOD_ID, path);
    }

    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModLoadingContext {
        @SubscribeEvent
        public static void onLoadComplete(FMLLoadCompleteEvent event) {
            ModContainer createContainer = ModList.get()
                    .getModContainerById(MOD_ID)
                    .orElseThrow(() -> new IllegalStateException("VS Logistics mod container missing on LoadComplete"));
            createContainer.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (mc, previousScreen) -> new BaseConfigScreen(previousScreen, MOD_ID)));
        }
    }
}
