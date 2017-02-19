package org.lunapark.dev.bullramp;

/**
 * Created by znak on 17.02.2017.
 */

public class Const {
    public static final String DATA_LEVEL = "level";
    public static final int MAX_ENEMIES = 6; //
    public static final float BRAKE = 0.7f; // коэффициент снижения скорости при столкновении
    public static final int NUM_BOTS = 10; // синие боты
    public static final int BONUS_CHANCE = 7; // часто [1...9] редко
    // Road params
    public static final int ROAD_SEGMENTS = 12;
    public static final int ROAD_SEGMENT_LENGTH = 10; // должно совпадать с размером plane.obj
    public static final float ROAD_LIMIT = ROAD_SEGMENT_LENGTH / 2;
    public static final float FAR_LENGTH = ROAD_SEGMENT_LENGTH * ROAD_SEGMENTS;
    public static final float HALF_FAR_LENGTH = FAR_LENGTH / 2;
    public static final float ROAD_POS_Y = -0.5f;
    public static final float DELTA_Z = 0.5f;
    // Lighters params
    public static final int LIGHT_SEGMENTS = ROAD_SEGMENTS / 3;
    public static final int LIGHT_SEGMENT_LENGTH = ROAD_SEGMENT_LENGTH * 3;
    public static final int LIGHTER_HEIGHT = 10;
    // Side roads params
    public static final int SCALE_SIDE_ROAD = 5;
    public static final float SIDE_ROAD_X = (FAR_LENGTH + LIGHT_SEGMENT_LENGTH - SCALE_SIDE_ROAD) / 2;

    // Camera settings
    public static final float CAMERA_SHAKE_DISTANCE = 2.0f;
    public static final float CAM_X_1 = -8f; // 8; -8
    public static final float CAM_Y = 10f; // 10; 10
    public static final float CAM_ROT_X = 40f; // 40; 40
    public static final float CAM_ROT_Z = 0; // 0; 0
    public static final float CAM_X_2 = -2f; // 8; -8
    public static final float CAM_Z = 15f; // 15; 0

}