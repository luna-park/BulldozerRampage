package org.lunapark.dev.bullramp;

/**
 * Created by znak on 17.02.2017.
 */

public class Const {
    // Gameplay stuff
    public static final String DATA_LEVEL = "level";
    public static final int MAX_ENEMIES = 6; //
    public static final int MAX_LEVEL = MAX_ENEMIES * 2 + 1;
    public static final float BRAKE = 0.7f; // 0.7f коэффициент снижения скорости при столкновении
    public static final int NUM_BOTS = 10; // синие боты
    public static final int BONUS_CHANCE = 1; // часто [1...9] редко
    // Explosion
    public static final int NUM_PARTICLES = 5; // 5 Количество частиц во взрыве
    public static final float EXPLOSION_SPEED = 0.5f;
    // Road params
    public static final int ROAD_SEGMENTS = 12;
    public static final int ROAD_SEGMENT_LENGTH = 10; // должно совпадать с размером plane.obj
    public static final float ROAD_LIMIT = ROAD_SEGMENT_LENGTH / 2;
    public static final float FAR_LENGTH = ROAD_SEGMENT_LENGTH * ROAD_SEGMENTS;
    public static final float HALF_FAR_LENGTH = FAR_LENGTH / 2;
    public static final float ROAD_Y = -0.5f;
    public static final float DELTA_Z = 0.2f; // 0.5
    public static final float PLAYER_Y = ROAD_Y + 0.2f; // 0.2f
    public static final float BOTS_Y = ROAD_Y + 0.1f;
    // Environment
    public static final int HOUSE_COUNT = 3;
    public static final float HOUSE_STEP = HALF_FAR_LENGTH / HOUSE_COUNT;
    // Lighters params
    public static final int LIGHT_SEGMENTS = ROAD_SEGMENTS / 3;
    public static final int LIGHT_SEGMENT_LENGTH = ROAD_SEGMENT_LENGTH * 3;
    public static final int LIGHTER_HEIGHT = 10;
    public static final float LIGHTER_Y = LIGHTER_HEIGHT / 2 + ROAD_Y;
    // Side roads params
    public static final int SCALE_SIDE_ROAD = 5;
    public static final float SIDE_ROAD_X = (FAR_LENGTH + LIGHT_SEGMENT_LENGTH - SCALE_SIDE_ROAD) / 2;

    // Camera settings
    public static final float CAMERA_SHAKE_DISTANCE = 2.0f;
    public static final float CAM_X_1 = -8f; // -8
    public static final float CAM_Y = 10f; // 10
    public static final float CAM_ROT_X = 40f; // 40
    public static final float CAM_ROT_Z = 0; // 0
    public static final float CAM_X_2 = -4f; // -2
    public static final float CAM_Z = 15f; // 15

    // Math
    public static final float MATH_TO_RAD = (float) (Math.PI / 180);
    public static final float MATH_TO_DEG = (float) (180 / Math.PI);
    public static final float MATH_2PI = (float) (2 * Math.PI);

}