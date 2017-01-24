package org.lunapark.dev.bullramp.map;

/**
 * Created by znak on 24.01.2017.
 */
public class MapGen {
    private static MapGen ourInstance = new MapGen();

    private int[][] map;
    public int w = 8, h = 6; // map size

    public static MapGen getInstance() {
        return ourInstance;
    }

    private MapGen() {
        // Init map
        map = new int[w][h];
        for (int i = 0; i < w / 2; i++) {
            for (int j = 0; j < h / 2; j++) {
                map[i * 2][j * 2] = 1;
            }
        }
    }

    public int[][] getMap() {
        int[][] mapnew = new int[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
               mapnew[i][j] = map[i][j];
            }
        }
        return mapnew;
    }
}
