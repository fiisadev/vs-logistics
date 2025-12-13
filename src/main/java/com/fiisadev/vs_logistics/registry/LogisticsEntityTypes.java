package com.fiisadev.vs_logistics.registry;

import com.fiisadev.vs_logistics.VSLogistics;
import com.fiisadev.vs_logistics.content.nozzle.NozzleEntity;
import com.fiisadev.vs_logistics.content.nozzle.NozzleEntityRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.EntityEntry;
import net.minecraft.world.entity.MobCategory;

public class LogisticsEntityTypes {
    private static final CreateRegistrate REGISTRATE = VSLogistics.registrate();

    public static final EntityEntry<NozzleEntity> NOZZLE = REGISTRATE.entity("nozzle", NozzleEntity::new, MobCategory.MISC)
            .renderer(() -> NozzleEntityRenderer::new)
            .register();

    public static void register() {}
}
