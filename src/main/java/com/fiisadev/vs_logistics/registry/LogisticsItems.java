package com.fiisadev.vs_logistics.registry;

import com.fiisadev.vs_logistics.VSLogistics;
import com.fiisadev.vs_logistics.content.pipe_wrench.PipeWrenchItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

public class LogisticsItems {
    private static final CreateRegistrate REGISTRATE = VSLogistics.registrate();

    public static ItemEntry<Item> NOZZLE = REGISTRATE.item("nozzle", Item::new)
            .model((c, p) -> p.getExistingFile(VSLogistics.asResource("item/nozzle")))
            .register();

    public static ItemEntry<PipeWrenchItem> PIPE_WRENCH = REGISTRATE.item("pipe_wrench", PipeWrenchItem::new)
            .model((c, p) -> p.getExistingFile(VSLogistics.asResource("item/pipe_wrench")))
            .properties(p -> p.stacksTo(1))
            .register();

    public static void register() {}
}
