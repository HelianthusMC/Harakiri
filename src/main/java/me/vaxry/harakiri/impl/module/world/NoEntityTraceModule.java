package me.vaxry.harakiri.impl.module.world;

import me.vaxry.harakiri.api.event.player.EventGetMouseOver;
import me.vaxry.harakiri.api.module.Module;
import me.vaxry.harakiri.api.value.Value;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemTool;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

/**
 * @author noil
 */
public final class NoEntityTraceModule extends Module {

    public Value<Boolean> toolsOnly = new Value<Boolean>("Tools", new String[]{"OnlyTools", "Tool", "Pickaxe", "Axe", "Shovel"}, "Only enable when holding a tool.", true);

    public NoEntityTraceModule() {
        super("MineThrough", new String[]{"MineThrough", "MineThrough", "MineThrough", "MineT"}, "Mine through entities.", "NONE", -1, ModuleType.WORLD);
    }

    @Listener
    public void onGetMouseOver(EventGetMouseOver event) {
        if (this.toolsOnly.getValue()) {
            final Minecraft mc = Minecraft.getMinecraft();
            if (mc.player != null) {
                if (mc.player.getHeldItemMainhand().getItem() instanceof ItemTool ||
                        mc.player.getHeldItemOffhand().getItem() instanceof ItemTool) {
                    event.setCanceled(true);
                }
            }
            return; // return so we don't cancel swords, swinging hand, etc
        }

        event.setCanceled(true);
    }
}