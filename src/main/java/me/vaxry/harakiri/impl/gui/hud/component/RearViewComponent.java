package me.vaxry.harakiri.impl.gui.hud.component;

import me.vaxry.harakiri.Harakiri;
import me.vaxry.harakiri.api.camera.Camera;
import me.vaxry.harakiri.api.gui.hud.component.ResizableHudComponent;
import me.vaxry.harakiri.api.util.RenderUtil;

/**
 * Author Seth
 * 12/10/2019 @ 1:49 AM.
 */
public final class RearViewComponent extends ResizableHudComponent {

    private final Camera rearviewCamera = new Camera(600, 400);

    public RearViewComponent() {
        super("RearView", 120, 120, 400, 400);
        Harakiri.INSTANCE.getCameraManager().addCamera(rearviewCamera);
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
            this.rearviewCamera.setRendering(true);

            if (this.rearviewCamera.isValid()) {
                this.rearviewCamera.setPos(mc.player.getPositionEyes(partialTicks).subtract(0, 1, 0));
                this.rearviewCamera.setYaw(mc.player.rotationYaw - 180.0f);
                this.rearviewCamera.setPitch(0.0f);
                this.rearviewCamera.render(this.getX() + 2, this.getY() + 12, this.getX() + this.getW() - 2, this.getY() + this.getH() - 2);
            }
        }
    }

}
