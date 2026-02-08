package com.fiisadev.vs_logistics.content.pipe_wrench;

import com.fiisadev.vs_logistics.registry.LogisticsItems;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;

public class PipeWrenchItem extends Item {
    public PipeWrenchItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new PipeWrenchItemRenderer()));
    }

    private static Item PIPE_WRENCH;
    public static Item getPipeWrench() {
        if (PIPE_WRENCH != null)
            return PIPE_WRENCH;

        if (ModList.get().isLoaded("cmparallelpipes")) {
            //noinspection removal
            PIPE_WRENCH = ForgeRegistries.ITEMS.getValue(new ResourceLocation("cmparallelpipes", "pipe_wrench"));
            return PIPE_WRENCH;
        }

        PIPE_WRENCH = LogisticsItems.PIPE_WRENCH.get();
        return PIPE_WRENCH;
    }
}
