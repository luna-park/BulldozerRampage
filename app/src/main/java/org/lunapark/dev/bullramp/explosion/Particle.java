package org.lunapark.dev.bullramp.explosion;

import fr.arnaudguyon.smartgl.opengl.Object3D;

/**
 * Created by znak on 14.01.2017.
 */

public class Particle {

    private final Object3D object3D;
    private float x, y, z, x0, y0, z0;
    private final float theta;
    private final float phi;
    private float r = 0.1f;
    private boolean visible;

    public Particle(float theta, float phi, Object3D object3D) {
        this.theta = theta;
        this.phi = phi;
        this.object3D = object3D;
        x0 = object3D.getPosX();
        y0 = object3D.getPosY();
        z0 = object3D.getPosZ();
    }

    private void compute() {
        x = (float) (r * Math.sin(theta) * Math.cos(phi));
        y = (float) (r * Math.sin(theta) * Math.sin(phi));
        z = (float) (r * Math.cos(theta));
    }

    public void setPosition(float x, float y, float z) {
        this.x0 = x;
        this.y0 = y;
        this.z0 = z;
    }

    public boolean isVisible() {
        visible = object3D.isVisible();
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        object3D.setVisible(visible);
    }

    public void setDistance(float distance) {
        r = distance + 0.1f;
        float scale = 2 / r;
        compute();
        object3D.setPos(x + x0, y + y0, z + z0);
        object3D.addRotX(10);
        object3D.addRotY(10);
        object3D.addRotZ(10);
        object3D.setScale(scale, scale, scale);
    }
}
