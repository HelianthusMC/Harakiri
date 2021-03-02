package me.vaxry.harakiri.framework.lua.api;

import me.vaxry.harakiri.Harakiri;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class GlobalAPI extends TwoArgFunction {

    public static GlobalAPI GLOBALAPI = null;

    public GlobalAPI() { GLOBALAPI = this; }

    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable global = new LuaTable(0,30);
        global.set( "curtime", new curtime() );
        global.set( "scaledX", new scaledX() );
        global.set( "scaledY", new scaledY() );
        env.set( "global", global );
        env.get("package").get("loaded").set("global", global);
        return global;
    }

    protected static class curtime extends ZeroArgFunction {
        public LuaValue call(){
            return LuaValue.valueOf(System.currentTimeMillis());
        }
    }

    protected static class scaledX extends ZeroArgFunction {
        public LuaValue call(){
            ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
            return LuaValue.valueOf(res.getScaledWidth());
        }
    }

    protected static class scaledY extends ZeroArgFunction {
        public LuaValue call(){
            ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
            return LuaValue.valueOf(res.getScaledHeight());
        }
    }
}