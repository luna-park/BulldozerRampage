package org.lunapark.dev.bullramp.map;

/**
 * Created by znak on 13.01.2017.
 */

public class Room {

    public Point center;
    public int x1, y1, x2, y2;
    public int w, h;

    public Room(int x1, int y1, int w, int h) {
        this.x1 = x1;
        this.y1 = y1;
        this.w = w;
        this.h = h;

        x2 = x1 + w;
        y2 = y1 + h;

        center = new Point();
        center.x = Math.round((x1 + x2) / 2);
        center.y = Math.round((y1 + y2) / 2);
    }

    public boolean intersect(Room room) {
        return (x1 <= room.x2 && x2 >= room.x1 && y1 <= room.y2 && room.y2 >= room.y1);
    }
}
