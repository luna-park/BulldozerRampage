package org.lunapark.dev.bullramp.explosion;

import android.content.Context;

import java.util.ArrayList;

import fr.arnaudguyon.smartgl.opengl.Object3D;
import fr.arnaudguyon.smartgl.opengl.RenderPassObject3D;
import fr.arnaudguyon.smartgl.opengl.Texture;
import fr.arnaudguyon.smartgl.tools.WavefrontModel;

/**
 * Created by znak on 14.01.2017.
 */

public class Explosion {

    private float x, y, z, size;
    private int num = 5;
    private int objFile;
    private Texture texture;
    private ArrayList<Particle> particles;
    private Context context;
    private RenderPassObject3D renderPassObject3D;
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
        for (int i = 1; i < num; i++) {
            for (int j = 1; j < num; j++) {
                WavefrontModel model = new WavefrontModel.Builder(context, objFile)
                        .addTexture("", texture)
                        .create();
                Object3D object3D = model.toObject3D();
                object3D.setPos(x, y, z);
//                object3D.setScale(0.5f, 0.5f, 0.5f);
                object3D.setVisible(false);
                renderPassObject3D.addObject(object3D);
                float theta = 180 * i / num;
                float phi = 180 * j / num;
                Particle particle = new Particle(theta, phi, object3D);
                particles.add(particle);
            }
        }
    }

    public void update() {
        if (visible) {
            if (size < 10) {
                size += 0.5f;
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
