package org.lunapark.dev.bullramp.explosion;

import android.content.Context;

import java.util.ArrayList;

import fr.arnaudguyon.smartgl.opengl.Object3D;
import fr.arnaudguyon.smartgl.opengl.RenderPassObject3D;
import fr.arnaudguyon.smartgl.opengl.Texture;
import fr.arnaudguyon.smartgl.tools.WavefrontModel;

import static org.lunapark.dev.bullramp.Const.EXPLOSION_SPEED;
import static org.lunapark.dev.bullramp.Const.NUM_PARTICLES;

/**
 * Created by znak on 14.01.2017.
 */

public class Explosion {

    private float x, y, z, size;
    private final int objFile;
    private final Texture texture;
    private ArrayList<Particle> particles;
    private final Context context;
    private final RenderPassObject3D renderPassObject3D;
    private boolean visible;

    public Explosion(Context context, RenderPassObject3D renderPassObject3D, int objFile, Texture texture) {
        this.context = context;
        this.renderPassObject3D = renderPassObject3D;
        this.objFile = objFile;
        this.texture = texture;
        visible = false;
        create();
    }

    private void create() {
        particles = new ArrayList<>();
        for (int i = 1; i < NUM_PARTICLES; i++) {
            for (int j = 1; j < NUM_PARTICLES; j++) {
                WavefrontModel model = new WavefrontModel.Builder(context, objFile)
                        .addTexture("", texture)
                        .create();
                Object3D object3D = model.toObject3D();
                object3D.setPos(x, y, z);
//                object3D.setScale(0.5f, 0.5f, 0.5f);
                object3D.setVisible(false);
                renderPassObject3D.addObject(object3D);
                float theta = 90 * i / NUM_PARTICLES;
                float phi = 90 * j / NUM_PARTICLES;
                Particle particle = new Particle(theta, phi, object3D);
                particles.add(particle);
            }
        }
    }

    public void update() {
        if (visible) {
            if (size < 10) {
                size += EXPLOSION_SPEED;
                for (int i = 0; i < particles.size(); i++) {
                    particles.get(i).setDistance(size);
                }
            } else {
                setVisible(false);
                size = 0;
            }
        }
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        for (int i = 0; i < particles.size(); i++) {
            particles.get(i).setPosition(x, y, z);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;

        for (int i = 0; i < particles.size(); i++) {
            particles.get(i).setVisible(visible);
        }
    }
}
