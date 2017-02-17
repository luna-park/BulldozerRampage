package org.lunapark.dev.bullramp;

/**
 * Created by znak on 17.02.2017.
 */

public class Const {
    // Road params
    public static final int roadSegments = 12;
    public static final int roadSegmentLength = 10;
    public static final float roadLimit = roadSegmentLength / 2;
    public static final float farLength = roadSegmentLength * roadSegments;
    public static final float halfFarLength = farLength / 2;
    public static final float roadPosY = -0.5f;
    public static final float deltaZ = 0.5f;
    // Lighters params
    public static final int lightSegments = roadSegments / 3;
    public static final int lightSegmentLength = roadSegmentLength * 3;
    public static final int lighterHeight = 10;
    // Side roads params
    public static final int scaleSideRoad = 5;
    public static final float sideRoadX = (farLength + lightSegmentLength - scaleSideRoad) / 2;
    public static final float speedBase = 0.3f;
    public static final float speedLimit = speedBase * 2;

    public static final int numBots = 7;
    public static final float speedBots = speedBase / 4;
    public static final float speedOpponents = speedBase * 1.2f;
    public static final boolean cheatNoDeccelerate = false;
    public static final float cameraShakeDistance = 2.0f;
    public static final float camX1 = -8f; // 8; -8
    public static final float camY = 10f; // 10; 10
    public static final float camRotX = 40f; // 40; 40
    public static final float camRotZ = 0; // 0; 0
    public static final float camX2 = -2f; // 8; -8
    public static final float camZ = 15f; // 15; 0
}
