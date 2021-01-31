package me.vaxry.harakiri.impl.module.render;

import akka.japi.Pair;
import me.vaxry.harakiri.Harakiri;
import me.vaxry.harakiri.api.event.render.EventRender2D;
import me.vaxry.harakiri.api.module.Module;
import me.vaxry.harakiri.api.util.ColorUtil;
import me.vaxry.harakiri.api.util.GLUProjection;
import me.vaxry.harakiri.api.util.RenderUtil;
import me.vaxry.harakiri.api.util.Timer;
import me.vaxry.harakiri.api.value.Value;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.*;
import net.minecraft.util.math.AxisAlignedBB;
import org.locationtech.jts.geom.*;
import org.lwjgl.util.vector.Vector3f;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;


/**
 * Author Seth
 * 5/17/2019 @ 8:45 PM.
 */
public final class StorageESPModule extends Module {

    public final Value<Float> thickness = new Value<Float>("Thickness", new String[]{"Thickness", "Thick", "t"}, "Thickness of the line", 1.f, 0.1f, 2.f, 0.1f);
    public final Value<Boolean> rainbow = new Value<Boolean>("Rainbow", new String[]{"Rainbow", "Rain", "r"}, "Rainbow mode for the ESP", false);
    public final Value<Integer> rainspeed = new Value<Integer>("RainbowSpeed", new String[]{"RainbowSpeed", "RainSpeed", "rs"}, "Rainbow mode speed", 5, 1, 20, 1);

    private final ICamera camera = new Frustum();
    private final Timer timer = new Timer();

    private float hue = 0;

    public StorageESPModule() {
        super("StorageESP", new String[]{"StorageESP", "ChestFinder", "ChestESP"}, "Highlights different types of storage entities.", "NONE", -1, ModuleType.RENDER);
    }

    private float getJitter() {
        final float seconds = ((System.currentTimeMillis() - this.timer.getTime()) / 1000.0f) % 60.0f;

        final float desiredTimePerSecond = rainspeed.getValue() / 100.f;

        this.timer.reset();
        return Math.min(desiredTimePerSecond * seconds, 1.0f);
    }

    @Listener
    public void render2D(EventRender2D event) {

        // Rainbow stuf
        this.hue += getJitter();
        if(hue >= 1.f)
            hue = 0.f;

        try {
            // union array for storing the shit
            ArrayList<Pair<Geometry, Integer>> tileEntitiesPoly = new ArrayList<>();

            final Minecraft mc = Minecraft.getMinecraft();
            for (TileEntity te : mc.world.loadedTileEntityList) {
                if (te != null) {
                    if (this.isTileStorage(te)) {
                        final AxisAlignedBB bb = this.boundingBoxForEnt(te);
                        if (bb != null) {

                            Vector3f ppos = new Vector3f((float) mc.player.posX, (float) mc.player.posY, (float) mc.player.posZ);

                            camera.setPosition(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().posY, mc.getRenderViewEntity().posZ);

                            if (!camera.isBoundingBoxInFrustum(new AxisAlignedBB(bb.minX + mc.getRenderManager().viewerPosX,
                                    bb.minY + mc.getRenderManager().viewerPosY,
                                    bb.minZ + mc.getRenderManager().viewerPosZ,
                                    bb.maxX + mc.getRenderManager().viewerPosX,
                                    bb.maxY + mc.getRenderManager().viewerPosY,
                                    bb.maxZ + mc.getRenderManager().viewerPosZ))) {
                                continue;
                            }

                            // TileEnt in view, draw the outline.

                            //  -------------
                            //  |           |
                            //  |           |
                            //  |           |
                            //  |___________|

                            // Order of corners:
                            //
                            // 0   1
                            // 3   2
                            //
                            // front:
                            // 7   6
                            // 4   5

                            float distance = get3DDistance(bb.minX + mc.getRenderManager().viewerPosX + 0.5f, bb.minY + mc.getRenderManager().viewerPosY + 0.5f, bb.minZ + mc.getRenderManager().viewerPosZ + 0.5f);
                            float alpha = 255;
                            if(distance < 5){
                                alpha = Math.min(Math.max(distance - 2.5f, 0) * (0xFF / 2.5f),0xFF);
                            }

                            List<Coordinate> ProjectedPs = new ArrayList<>();

                            ProjectedPs.add(conv3Dto2DSpace(bb.minX, bb.maxY, bb.maxZ));
                            ProjectedPs.add(conv3Dto2DSpace(bb.maxX, bb.maxY, bb.maxZ));
                            ProjectedPs.add(conv3Dto2DSpace(bb.maxX, bb.minY, bb.maxZ));
                            ProjectedPs.add(conv3Dto2DSpace(bb.minX, bb.minY, bb.maxZ));

                            ProjectedPs.add(conv3Dto2DSpace(bb.minX, bb.minY, bb.minZ));
                            ProjectedPs.add(conv3Dto2DSpace(bb.maxX, bb.minY, bb.minZ));
                            ProjectedPs.add(conv3Dto2DSpace(bb.maxX, bb.maxY, bb.minZ));
                            ProjectedPs.add(conv3Dto2DSpace(bb.minX, bb.maxY, bb.minZ));

                            Polygon[] polys = new Polygon[6];
                            boolean[] is = new boolean[6];

                            if (bb.maxX < 0 || Math.abs(bb.maxX) < 5) {
                                polys[0] = new GeometryFactory().createPolygon(new Coordinate[]{
                                        new Coordinate(ProjectedPs.get(5).getX(), ProjectedPs.get(5).getY()),
                                        new Coordinate(ProjectedPs.get(2).getX(), ProjectedPs.get(2).getY()),
                                        new Coordinate(ProjectedPs.get(1).getX(), ProjectedPs.get(1).getY()),
                                        new Coordinate(ProjectedPs.get(6).getX(), ProjectedPs.get(6).getY()),
                                        new Coordinate(ProjectedPs.get(5).getX(), ProjectedPs.get(5).getY())
                                });
                                is[0] = true;
                            }
                            if (bb.maxY < mc.player.eyeHeight || Math.abs(bb.maxY) < 5) {
                                polys[1] = new GeometryFactory().createPolygon(new Coordinate[]{
                                        new Coordinate(ProjectedPs.get(6).getX(), ProjectedPs.get(6).getY()),
                                        new Coordinate(ProjectedPs.get(1).getX(), ProjectedPs.get(1).getY()),
                                        new Coordinate(ProjectedPs.get(0).getX(), ProjectedPs.get(0).getY()),
                                        new Coordinate(ProjectedPs.get(7).getX(), ProjectedPs.get(7).getY()),
                                        new Coordinate(ProjectedPs.get(6).getX(), ProjectedPs.get(6).getY())
                                });
                                is[1] = true;
                            }
                            if (bb.minX > 0 || Math.abs(bb.minX) < 5) {
                                polys[2] = new GeometryFactory().createPolygon(new Coordinate[]{
                                        new Coordinate(ProjectedPs.get(7).getX(), ProjectedPs.get(7).getY()),
                                        new Coordinate(ProjectedPs.get(0).getX(), ProjectedPs.get(0).getY()),
                                        new Coordinate(ProjectedPs.get(3).getX(), ProjectedPs.get(3).getY()),
                                        new Coordinate(ProjectedPs.get(4).getX(), ProjectedPs.get(4).getY()),
                                        new Coordinate(ProjectedPs.get(7).getX(), ProjectedPs.get(7).getY())
                                });
                                is[2] = true;
                            }
                            if (bb.minY > mc.player.eyeHeight || Math.abs(bb.minY) < 5) {
                                polys[3] = new GeometryFactory().createPolygon(new Coordinate[]{
                                        new Coordinate(ProjectedPs.get(5).getX(), ProjectedPs.get(5).getY()),
                                        new Coordinate(ProjectedPs.get(2).getX(), ProjectedPs.get(2).getY()),
                                        new Coordinate(ProjectedPs.get(3).getX(), ProjectedPs.get(3).getY()),
                                        new Coordinate(ProjectedPs.get(4).getX(), ProjectedPs.get(4).getY()),
                                        new Coordinate(ProjectedPs.get(5).getX(), ProjectedPs.get(5).getY())
                                });
                                is[3] = true;
                            }
                            if (bb.minZ > 0 || Math.abs(bb.minZ) < 5) {
                                polys[4] = new GeometryFactory().createPolygon(new Coordinate[]{
                                        new Coordinate(ProjectedPs.get(5).getX(), ProjectedPs.get(5).getY()),
                                        new Coordinate(ProjectedPs.get(6).getX(), ProjectedPs.get(6).getY()),
                                        new Coordinate(ProjectedPs.get(7).getX(), ProjectedPs.get(7).getY()),
                                        new Coordinate(ProjectedPs.get(4).getX(), ProjectedPs.get(4).getY()),
                                        new Coordinate(ProjectedPs.get(5).getX(), ProjectedPs.get(5).getY())
                                });
                                is[4] = true;
                            }
                            if (bb.maxZ < 0 || Math.abs(bb.maxZ) < 5) {
                                polys[5] = new GeometryFactory().createPolygon(new Coordinate[]{
                                        new Coordinate(ProjectedPs.get(2).getX(), ProjectedPs.get(2).getY()),
                                        new Coordinate(ProjectedPs.get(1).getX(), ProjectedPs.get(1).getY()),
                                        new Coordinate(ProjectedPs.get(0).getX(), ProjectedPs.get(0).getY()),
                                        new Coordinate(ProjectedPs.get(3).getX(), ProjectedPs.get(3).getY()),
                                        new Coordinate(ProjectedPs.get(2).getX(), ProjectedPs.get(2).getY())
                                });
                                is[5] = true;
                            }

                            Geometry union = new GeometryFactory().createPolygon();

                            // Connect all visible sides into one polygon, add them to the union.
                            for (int i = 0; i < 6; ++i) {
                                try {
                                    if (is[i])
                                        union = union.union(polys[i]);
                                }catch(Throwable t){
                                    //happens on incorrect clipping. Recode this to use 3d in the future.
                                }
                            }

                            // push it
                            tileEntitiesPoly.add(new Pair<>(union, rainbow.getValue() ? (int)(alpha * 0x1000000) + 0xFFFFFF : ColorUtil.changeAlpha(getColor(te), (int)alpha)));
                        }
                    }
                }
            }

            // Draw the final union.

            for(;;){
                if(tileEntitiesPoly.size() == 0)
                    break;

                boolean anyIntersects = false;
                for(int i = 0; i < tileEntitiesPoly.size(); ++i) {
                    for(int j = 0; j < tileEntitiesPoly.size(); ++j) {
                        Geometry g = tileEntitiesPoly.get(i).first();
                        Geometry g2 = tileEntitiesPoly.get(j).first();

                        if(g == g2 || !tileEntitiesPoly.get(i).second().equals(tileEntitiesPoly.get(j).second())) continue;

                        if(g.intersects(g2)){
                            try {
                                tileEntitiesPoly.set(i, new Pair<>(g.union(g2), tileEntitiesPoly.get(i).second()));
                                tileEntitiesPoly.remove(j);
                                anyIntersects = true;
                                break;
                            }catch (Throwable e){
                                // ignore
                            }
                        }
                    }
                }

                if(!anyIntersects)
                    break;
            }

            for(int i = 0; i < tileEntitiesPoly.size(); ++i) {
                RenderUtil.drawOutlinePolygon(tileEntitiesPoly.get(i).first(), thickness.getValue(), tileEntitiesPoly.get(i).second(), this.rainbow.getValue(), this.hue);
            }


        }catch(Throwable e){
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            Harakiri.INSTANCE.logChat("StorageESP Threw an Error: " + e.getMessage());
        }
    }

    private boolean isTileStorage(TileEntity te) {
        if (te instanceof TileEntityChest) {
            return true;
        }
        if (te instanceof TileEntityDropper) {
            return true;
        }
        if (te instanceof TileEntityDispenser) {
            return true;
        }
        if (te instanceof TileEntityFurnace) {
            return true;
        }
        //if (te instanceof TileEntityBrewingStand) {
        //    return true;
        //}
        if (te instanceof TileEntityEnderChest) {
            return true;
        }
        if (te instanceof TileEntityHopper) {
            return true;
        }
        if (te instanceof TileEntityShulkerBox) {
            return true;
        }
        return false;
    }

    private AxisAlignedBB boundingBoxForEnt(TileEntity te) {
        final Minecraft mc = Minecraft.getMinecraft();

        if (te != null) {
            if (te instanceof TileEntityChest) {
                TileEntityChest chest = (TileEntityChest) te;
                if (chest.adjacentChestXNeg != null) {
                    return new AxisAlignedBB(
                            te.getPos().getX() + 0.0625d - 1 - mc.getRenderManager().viewerPosX,
                            te.getPos().getY() - mc.getRenderManager().viewerPosY,
                            te.getPos().getZ() + 0.0625d - mc.getRenderManager().viewerPosZ,

                            te.getPos().getX() + 0.9375d - mc.getRenderManager().viewerPosX,
                            te.getPos().getY() + 0.875d - mc.getRenderManager().viewerPosY,
                            te.getPos().getZ() + 0.9375d - mc.getRenderManager().viewerPosZ);
                } else if (chest.adjacentChestZPos != null) {
                    return new AxisAlignedBB(
                            te.getPos().getX() + 0.0625d - mc.getRenderManager().viewerPosX,
                            te.getPos().getY() - mc.getRenderManager().viewerPosY,
                            te.getPos().getZ() + 0.0625d - mc.getRenderManager().viewerPosZ,

                            te.getPos().getX() + 0.9375d - mc.getRenderManager().viewerPosX,
                            te.getPos().getY() + 0.875d - mc.getRenderManager().viewerPosY,
                            te.getPos().getZ() + 0.9375d + 1 - mc.getRenderManager().viewerPosZ);
                } else if (chest.adjacentChestXPos == null && chest.adjacentChestZNeg == null) {
                    return new AxisAlignedBB(
                            te.getPos().getX() + 0.0625d - mc.getRenderManager().viewerPosX,
                            te.getPos().getY() - mc.getRenderManager().viewerPosY,
                            te.getPos().getZ() + 0.0625d - mc.getRenderManager().viewerPosZ,

                            te.getPos().getX() + 0.9375d - mc.getRenderManager().viewerPosX,
                            te.getPos().getY() + 0.875d - mc.getRenderManager().viewerPosY,
                            te.getPos().getZ() + 0.9375d - mc.getRenderManager().viewerPosZ);
                }
            } else if (te instanceof TileEntityEnderChest) {
                return new AxisAlignedBB(
                        te.getPos().getX() + 0.0625d - mc.getRenderManager().viewerPosX,
                        te.getPos().getY() - mc.getRenderManager().viewerPosY,
                        te.getPos().getZ() + 0.0625d - mc.getRenderManager().viewerPosZ,

                        te.getPos().getX() + 0.9375d - mc.getRenderManager().viewerPosX,
                        te.getPos().getY() + 0.875d - mc.getRenderManager().viewerPosY,
                        te.getPos().getZ() + 0.9375d - mc.getRenderManager().viewerPosZ);
            } else {
                return new AxisAlignedBB(
                        te.getPos().getX() - mc.getRenderManager().viewerPosX,
                        te.getPos().getY() - mc.getRenderManager().viewerPosY,
                        te.getPos().getZ() - mc.getRenderManager().viewerPosZ,

                        te.getPos().getX() + 1 - mc.getRenderManager().viewerPosX,
                        te.getPos().getY() + 1 - mc.getRenderManager().viewerPosY,
                        te.getPos().getZ() + 1 - mc.getRenderManager().viewerPosZ);
            }
        }

        return null;
    }


    private int getColor(TileEntity te) {
        if (te instanceof TileEntityChest) {
            return 0xFFF0F000;
        }
        if (te instanceof TileEntityDropper) {
            return 0xFFA6A6A6;
        }
        if (te instanceof TileEntityDispenser) {
            return 0xFF4E4E4E;
        }
        if (te instanceof TileEntityHopper) {
            return 0xFF4E4E4E;
        }
        if (te instanceof TileEntityFurnace) {
            return 0xFF2D2D2D;
        }
        if (te instanceof TileEntityBrewingStand) {
            return 0xFF17B9D2;
        }
        if (te instanceof TileEntityEnderChest) {
            return 0xFFCC00FF;
        }
        if (te instanceof TileEntityShulkerBox) {
            //final TileEntityShulkerBox shulkerBox = (TileEntityShulkerBox) te;
            //return (255 << 24) | shulkerBox.getColor().getColorValue();
            return 0xFFFF0066;
        }
        return 0xFFFFFFFF;
    }

    private Coordinate conv3Dto2DSpace(double x, double y, double z) {
        final GLUProjection.Projection projection = GLUProjection.getInstance().project(x, y, z, GLUProjection.ClampMode.NONE, false);

        final Coordinate returns = new Coordinate(projection.getX(), projection.getY());

        return returns;
    }

    private int get3DDistance(double x, double y, double z) {
        return (int)(Math.sqrt(Math.pow((Minecraft.getMinecraft().player.posX - x),2) + Math.pow((Minecraft.getMinecraft().player.posY - y),2) + Math.pow((Minecraft.getMinecraft().player.posZ - z),2)));
    }
}
