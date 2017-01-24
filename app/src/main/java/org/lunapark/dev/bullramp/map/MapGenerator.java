package org.lunapark.dev.bullramp.map;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by znak on 14.01.2017.
 */

public class MapGenerator {

    private int roomsNumber;
    private Point levelSize;

    private ArrayList<Room> rooms;

    private ArrayList<Integer> level;

    private Random random;

    public MapGenerator() {
        random = new Random();
    }

    public ArrayList<Integer> generate(Point levelSize) {

        level = new ArrayList<>();
        rooms = new ArrayList<>();

        // Generate room
        int x, y, maxW, maxH;
        do {
            x = random.nextInt(levelSize.x);
            y = random.nextInt(levelSize.y);

            maxW = levelSize.x - x;
            maxH = levelSize.y - y;

        } while (maxW > 0 && maxH > 0);

        int w = random.nextInt(maxW);
        int h = random.nextInt(maxH);

        Room room = new Room(x, y, w, h);
        rooms.add(room);

        return level;
    }

}
