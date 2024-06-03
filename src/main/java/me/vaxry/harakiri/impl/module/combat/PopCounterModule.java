package me.alpha432.oyvey.features.modules.misc;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.vaxry.harakiri.framework.Module.Command;
import me.vaxry.harakiri.framework.Module;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;

public class PopCounter extends Module {
    final Minecraft mc = Minecraft.getMinecraft();

    public PopCounter() {
        super("PopCounter", new String[]{"tm"}, "Chat based pop counter from OyVey", "NONE", -1, ModuleType.COMBAT);
    }

    public static PopCounter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PopCounter();
        }
        return INSTANCE;
    }

    private void setInstance() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        TotemPopContainer.clear();
    }

    public void onDeath(EntityPlayer player) {
        if (TotemPopContainer.containsKey(player.getName())) {
            int l_Count = TotemPopContainer.get(player.getName());
            TotemPopContainer.remove(player.getName());
            if (l_Count == 1) {
                Command.sendMessage(ChatFormatting.AQUA + "[Harakiri]" + player.getName() + " died after popping " + ChatFormatting.AQUA + l_Count + ChatFormatting.AQUA + " Totem!");
            } else {
                Command.sendMessage(ChatFormatting.AQUA + "[Harakiri]" + player.getName() + " died after popping " + ChatFormatting.AQUA + l_Count + ChatFormatting.AQUA + " Totems!");
            }
        }
    }

    public void onTotemPop(EntityPlayer player) {
        if (PopCounter.fullNullCheck()) {
            return;
        }
        if (PopCounter.mc.player.equals(player)) {
            return;
        }
        int l_Count = 1;
        if (TotemPopContainer.containsKey(player.getName())) {
            l_Count = TotemPopContainer.get(player.getName());
            TotemPopContainer.put(player.getName(), ++l_Count);
        } else {
            TotemPopContainer.put(player.getName(), l_Count);
        }
        if (l_Count == 1) {
            Command.sendMessage(ChatFormatting.AQUA + "[Harakiri]" + player.getName() + " popped " + ChatFormatting.AQUA + l_Count + ChatFormatting.AQUA + " Totem.");
        } else {
            Command.sendMessage(ChatFormatting.AQUA + "[Harakiri]" + player.getName() + " popped " + ChatFormatting.AQUA + l_Count + ChatFormatting.AQUA + " Totems.");
        }
    }
}

