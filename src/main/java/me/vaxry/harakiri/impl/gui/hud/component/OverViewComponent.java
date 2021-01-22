package me.vaxry.harakiri.impl.gui.hud.component;

import me.vaxry.harakiri.Harakiri;
import me.vaxry.harakiri.api.camera.Camera;
import me.vaxry.harakiri.api.gui.hud.component.ResizableHudComponent;
import me.vaxry.harakiri.api.util.RenderUtil;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Mouse;

/**
 * Author Seth
 * 12/9/2019 @ 3:16 AM.
 */
public final class OverViewComponent extends ResizableHudComponent {

    private final Camera overviewCamera = new Camera(600, 400);

    private float scroll;
    private float lastScroll;
    private float distance = 35.0f;

    public OverViewComponent() {
        super("OverView", 120, 120, 400, 400);
        Harakiri.INSTANCE.getCameraManager().addCamera(overviewCamera);
        this.setW(120);
        this.setH(120);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);

        RenderUtil.drawRect(this.getX() - 1, this.getY() - 1, this.getX() + this.getW() + 1, this.getY() + this.getH() + 1, 0x99101010);
        RenderUtil.drawRect(this.getX(), this.getY(), this.getX() + this.getW(), this.getY() + this.getH(), 0xFF202020);
        mc.fontRenderer.drawStringWithShadow(this.getName(), this.getX() + 2, this.getY() + 2, 0xFFFFFFFF);

        if (mc.player != null && mc.world != null) {
            this.overviewCamera.setRendering(true);

            if (this.overviewCamera.isValid()) {

                final Vec3d ground = this.getGround(partialTicks);

                if (ground != null) {

                /*
                "isometric" view
                final Vec3d forward = MathUtil.direction(mc.player.rotationYaw);
                final float factor = 30.0f;
                this.overviewCamera.setPos(ground.add(0, this.getDist(partialTicks), 0).subtract(forward.x * factor, forward.y * factor, forward.z * factor));
                this.overviewCamera.setYaw(mc.player.rotationYaw);
                this.overviewCamera.setPitch(65.0f);
                this.overviewCamera.render(this.getX() + 2, this.getY() + 12, this.getX() + this.getW() - 2, this.getY() + this.getH() - 2);
                */

                    this.overviewCamera.setPos(ground.add(0, this.getDist(partialTicks), 0));
                    this.overviewCamera.setYaw(mc.player.rotationYaw);
                    this.overviewCamera.setPitch(90.0f);
                    this.overviewCamera.render(this.getX() + 2, this.getY() + 12, this.getX() + this.getW() - 2, this.getY() + this.getH() - 2);
                }
            }
        }

        if (this.isMouseInside(mouseX, mouseY)) {
            this.handleScrolling(mouseX, mouseY);
            mc.fontRenderer.drawStringWithShadow("Zoom: " + this.distance, this.getX() + 4, this.getY() + this.getH() - mc.fontRenderer.FONT_HEIGHT - 2, 0xFFFFFFFF);
        }
    }

    private Vec3d getGround(float partialTicks) {
        final Vec3d eyes = mc.player.getPositionEyes(partialTicks);
        final RayTraceResult ray = mc.world.rayTraceBlocks(eyes, eyes.subtract(0, 3, 0), false);

        if (ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
            return ray.hitVec;
        }

        return eyes;
    }

    private double getDist(float partialTicks) {
        final Vec3d eyes = mc.player.getPositionEyes(partialTicks);
        final RayTraceResult ray = mc.world.rayTraceBlocks(eyes, eyes.add(0, this.distance, 0), false);

        if (ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
            return mc.player.getDistance(ray.hitVec.x, ray.hitVec.y, ray.hitVec.z) - 4;
        }

        return this.distance;
    }

    private void handleScrolling(int mouseX, int mouseY) {
        if (this.isMouseInside(mouseX, mouseY) && Mouse.hasWheel()) {
            this.scroll += -(Mouse.getDWheel() / 100.0f);

            if (this.scroll <= 0) {
                this.scroll = 0;
            }

            if (this.scroll >= 10) {
                this.scroll = 10;
            }

            if (this.lastScroll != this.scroll) {
                this.lastScroll = this.scroll;
                this.distance = this.scroll * 10;
                //TODO update fbo
            }
        }
    }

}
