package org.lunapark.dev.bullramp.entity;

import java.util.ArrayList;

import fr.arnaudguyon.smartgl.opengl.Object3D;

/**
 * Created by znak on 22.01.2017.
 */

public class GameObject {

    private ArrayList<Object3D> object3Ds;
    private ArrayList<Point3D> point3Ds;
    private float x, y, z;

    public GameObject() {
    }

    public void setObjects(ArrayList<Object3D> object3Ds) {
        this.object3Ds = object3Ds;
        point3Ds = new ArrayList<>();
        for (int i = 0; i < object3Ds.size(); i++) {
            point3Ds.add(i, getPoint3D(object3Ds.get(i)));
        }
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        for (int i = 0; i < object3Ds.size(); i++) {
            object3Ds.get(i).setPos(point3Ds.get(i).x + x, point3Ds.get(i).y + y, point3Ds.get(i).z + z);
        }
    }

    private Point3D getPoint3D(Object3D object3D) {
        Point3D point3D = new Point3D();
        point3D.x = object3D.getPosX();
        point3D.y = object3D.getPosY();
        point3D.z = object3D.getPosZ();
        return point3D;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
        setPosition(x, y, z);
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
        setPosition(x, y, z);

    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
        setPosition(x, y, z);
    }
}
