package org.lunapark.dev.bullramp.map;

import java.util.Random;

/**
 * Created by znak on 24.01.2017.
 */
public class MapGen {
    private static MapGen ourInstance = new MapGen();
    public int w = 6, h = 6; // baseMap size
    private int[][] baseMap;
    private Random random;

    private MapGen() {
        // Init baseMap
        baseMap = new int[w][h];
        for (int i = 0; i < w / 2; i++) {
            for (int j = 0; j < h / 2; j++) {
                baseMap[i * 2][j * 2] = 1;
            }
        }

        // Init random
        random = new Random();
    }

    public static MapGen getInstance() {
        return ourInstance;
    }

    public int[][] getMap() {
        int[][] mapnew = new int[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                mapnew[i][j] = baseMap[i][j];
            }
        }
        generate(mapnew);
        return mapnew;
    }

    private void generate(int[][] inmap) {
        for (int k = 0; k < 10; k++) {

            int i = random.nextInt(w - 2) + 1;
            int j = random.nextInt(h - 2) + 1;

            int a = inmap[i][j];
            if (a == 0) {
                int left = inmap[i - 1][j];
                int right = inmap[i + 1][j];
                int up = inmap[i][j + 1];
                int down = inmap[i][j - 1];

                int b = random.nextInt(100);

                if (b < 60) {
                    if ((left == 1 && right == 1) || (up == 1 && down == 1)) {
                        inmap[i][j] = 1;
                    }
                }
            }
        }
    }

}
