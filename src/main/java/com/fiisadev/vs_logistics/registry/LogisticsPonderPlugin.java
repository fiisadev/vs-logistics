package com.fiisadev.vs_logistics.registry;

import com.fiisadev.vs_logistics.VSLogistics;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;

public class LogisticsPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return VSLogistics.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        LogisticsPonders.register(helper);
    }

    public static void registerPlugin() {
        PonderIndex.addPlugin(new LogisticsPonderPlugin());
    }
}
