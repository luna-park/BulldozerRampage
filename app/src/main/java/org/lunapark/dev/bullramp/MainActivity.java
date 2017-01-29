package org.lunapark.dev.bullramp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import org.lunapark.dev.bullramp.entity.GameObject;
import org.lunapark.dev.bullramp.explosion.Explosion;

import java.util.ArrayList;
import java.util.Random;

import fr.arnaudguyon.smartgl.opengl.Object3D;
import fr.arnaudguyon.smartgl.opengl.OpenGLCamera;
import fr.arnaudguyon.smartgl.opengl.RenderPassObject3D;
import fr.arnaudguyon.smartgl.opengl.RenderPassSprite;
import fr.arnaudguyon.smartgl.opengl.SmartGLRenderer;
import fr.arnaudguyon.smartgl.opengl.SmartGLView;
import fr.arnaudguyon.smartgl.opengl.SmartGLViewController;
import fr.arnaudguyon.smartgl.opengl.Sprite;
import fr.arnaudguyon.smartgl.opengl.Texture;
import fr.arnaudguyon.smartgl.tools.WavefrontModel;
import fr.arnaudguyon.smartgl.touch.TouchHelperEvent;

public class MainActivity extends Activity implements SmartGLViewController, View.OnTouchListener {

    private final int divider = 1;

    // Road params
    private int roadSegments = 10, roadSegmentLength = 10;
    private float roadLimit = roadSegmentLength / 2;
    private float farLength = roadSegmentLength * roadSegments;
    private float halfFarLength = farLength / 2;

    // Lighters params
    private int lightSegments = roadSegments / 3, lightSegmentLength = roadSegmentLength * 3;


    private Texture txRed;
    private float speedBase = 0.3f, speed, speedLimit = speedBase * 3;
    private SmartGLView mSmartGLView;

    private Texture mSpriteTexture;
    private Sprite mSprite;
    private Object3D player, sideRoadUp, sideRoadDown;
    private RenderPassObject3D renderPassObject3D;
    private RenderPassSprite renderPassSprite;
    private ArrayList<Texture> textures;
    private ArrayList<Object3D> object3Ds, road, lighters;
    private ArrayList<Bitmap> bitmaps;
    private ArrayList<Explosion> explosions;
    private float cameraShakeDistance = 2.0f, shake;
    private float camX = 8f, camY = 10f, camZ = 15f;
    //    private float camX = 2f, camY = 4f, camZ = 7f;
    //    private float camX = 0f, camY = 9f, camZ = 0f;
    private float camRotX = -90;
    private SoundPool soundPool;
    private int soundIdExplosion;
    private GameObject goBridge;
    private int screenW, screenH;
    // Colors
    private int colHoloLight = Color.argb(128, 51, 181, 229);
    private int colHoloDark = Color.argb(128, 0, 153, 204);
    private int colHoloBright = Color.argb(128, 0, 221, 255);
    private int colDkGray = Color.argb(128, 64, 64, 64);
    private int colLtGray = Color.argb(128, 192, 192, 192);
    private int colRed = Color.argb(128, 240, 0, 0);
    private int colOrange = Color.argb(160, 255, 125, 11);
    private int colYellow = Color.argb(128, 255, 255, 0);
    private int colWhite = Color.argb(128, 255, 255, 255);
    // Textures
    private Texture txHoloBright;
    private Texture txRoad;
    private Texture txDkGray;
    private Texture txExplosion;
    private DIRECTION currentDirection = DIRECTION.STRAIGHT;
    private Random random;
    private Texture txLtGray;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                switch (v.getId()) {
                    case R.id.btnLeft:
                        currentDirection = DIRECTION.LEFT;
                        break;
                    case R.id.btnRight:
                        currentDirection = DIRECTION.RIGHT;
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                currentDirection = DIRECTION.STRAIGHT;
                break;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set brightness
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = 1F;
        getWindow().setAttributes(layout);

        mSmartGLView = (SmartGLView) findViewById(R.id.smartGLView);
        mSmartGLView.setDefaultRenderer(this);
        mSmartGLView.setController(this);

        ImageButton btnLeft = (ImageButton) findViewById(R.id.btnLeft);
        ImageButton btnRight = (ImageButton) findViewById(R.id.btnRight);
        btnLeft.setOnTouchListener(this);
        btnRight.setOnTouchListener(this);

        // Double pixels
        SurfaceHolder surfaceHolder = mSmartGLView.getHolder();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenW = size.x;
        screenH = size.y;
        surfaceHolder.setFixedSize(screenW / divider, screenH / divider);


        // Prepare sound
        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

            }
        });

        soundIdExplosion = soundPool.load(this, R.raw.explosion, 1);
    }

    @Override
    protected void onPause() {
        if (mSmartGLView != null) {
            mSmartGLView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSmartGLView != null) {
            mSmartGLView.onResume();
        }
    }

    @Override
    public void onPrepareView(SmartGLView smartGLView) {
        random = new Random();
        SmartGLRenderer renderer = smartGLView.getSmartGLRenderer();
        renderer.setClearColor(0, 0, 0, 1);

        RenderPassSprite bgSprite = new RenderPassSprite();
        renderPassObject3D = new RenderPassObject3D(RenderPassObject3D.ShaderType.SHADER_TEXTURE, true, true);
        renderPassSprite = new RenderPassSprite();

        renderer.addRenderPass(bgSprite);
        renderer.addRenderPass(renderPassObject3D);  // add it only once for all 3D Objects
        renderer.addRenderPass(renderPassSprite);  // add it only once for all Sprites

        // Textures
        textures = new ArrayList<>();
        bitmaps = new ArrayList<>();
        txHoloBright = createTexture(colHoloBright);
        txExplosion = createTexture(Color.YELLOW, Color.rgb(255, 112, 16));
        txRoad = createTextureRoad();
        txDkGray = createTexture(colDkGray);
        txRed = createTexture(colRed);
        txLtGray = createTexture(colLtGray);

        // Create sprites
        mSpriteTexture = new Texture(this, R.drawable.doubleaxes);
        textures.add(mSpriteTexture);

        mSprite = new Sprite(120 / divider, 120 / divider); // 120 x 120 pixels
        mSprite.setPivot(0.5f, 0.5f);  // set position / rotation axis to the middle of the sprite
        mSprite.setPos(60 / divider, 60 / divider);
        mSprite.setTexture(mSpriteTexture);
        renderPassSprite.addSprite(mSprite);

        Texture txBackground = createTexture(Color.BLACK, Color.rgb(16, 16, 16), 256, 256);
        Sprite spriteBg = new Sprite(screenW, screenH);
        spriteBg.setTexture(txBackground);
        bgSprite.addSprite(spriteBg);

        // Create 3D objects
        object3Ds = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Object3D cube = createObject(R.raw.cube, txHoloBright, true);
            cube.setVisible(false);
            cube.setScale(2, 1, 1);
            object3Ds.add(cube);
        }

        // Create player
        player = createObject(R.raw.bulldozer, createTexture(colOrange), false);
        player.setScale(0.001f, 0.001f, 0.001f);
        player.setPos(0, -0.3f, 0);
        player.addRotY(180);
        speed = speedBase;

        createEnvironment();

        // Prepare explosions
        explosions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Explosion explosion = new Explosion(this, renderPassObject3D, R.raw.cube, txExplosion);
            explosions.add(explosion);
        }
    }

    private void createEnvironment() {
        // Create road
        road = new ArrayList<>();
        for (int i = 0; i < roadSegments; i++) {
            Object3D ground = createObject(R.raw.plane, txRoad, false);
            ground.setPos(roadSegmentLength * i - halfFarLength, -0.5f, roadSegmentLength / 2);
//            ground.setScale(roadSegmentLength , 0, roadSegmentLength);
            road.add(ground);
        }

        int lighterHeight = 10;
        lighters = new ArrayList<>();
        for (int i = 0; i < lightSegments; i++) {
            float x = lightSegmentLength * i - halfFarLength;
            Object3D lighter1 = createObject(R.raw.cube, txLtGray, false);
            lighter1.setPos(x, lighterHeight / 2 - 0.5f, -roadSegmentLength);
            lighter1.setScale(0.25f, lighterHeight, 0.25f);

            Object3D lighter2 = createObject(R.raw.cube, txLtGray, false);
            lighter2.setPos(x, lighterHeight / 2 - 0.5f, roadSegmentLength);
            lighter2.setScale(0.25f, lighterHeight, 0.25f);

            lighters.add(lighter1);
            lighters.add(lighter2);
        }

        int scale = 5;
        float sideRoadX = (farLength + lightSegmentLength - scale) / 2;
        sideRoadDown = createObject(R.raw.plane, createTexture(Color.DKGRAY), false);
        sideRoadDown.setPos(sideRoadX, -0.5f, roadSegmentLength * scale + roadSegmentLength / 2);
        sideRoadDown.setScale(0.75f, 1, scale);

        sideRoadUp = createObject(R.raw.plane, createTexture(Color.DKGRAY), false);
        sideRoadUp.setPos(sideRoadX, -0.5f, -roadSegmentLength / 2);
        sideRoadUp.setScale(0.75f, 1, scale);

        ArrayList<Object3D> bridge = new ArrayList<>();
        Object3D bridge1 = createObject(R.raw.cube, txLtGray, false);
        bridge1.setPos(0, 1.5f, -50 - roadSegmentLength);
        bridge1.setScale(10, 4, 100);
        bridge.add(bridge1);
        Object3D bridge2 = createObject(R.raw.cube, txLtGray, false);
        bridge2.setPos(0, 1.5f, 50 + roadSegmentLength);
        bridge2.setScale(10, 4, 100);
        bridge.add(bridge2);

        Object3D bridge3 = createObject(R.raw.cube, txLtGray, false);
        bridge3.setPos(0, 4.0f, 0);
        bridge3.setScale(10, 1, 100);
        bridge.add(bridge3);

//        Object3D plane = createObject(R.raw.plane, createTexture(Color.RED), false);
//        plane.setPos(-10, -0.5f, -10);
//        plane.setScale(5, 5, 5);
//        bridge.add(plane);

        goBridge = new GameObject();
        goBridge.setObjects(bridge);
        goBridge.setPosition(0, 0, 0);
    }

    private void addObject(float x) {
        for (Object3D object3D : object3Ds) {
            if (!object3D.isVisible()) {
                float dx = random.nextInt(roadSegmentLength / 4) + x;
                float dz = random.nextInt(roadSegmentLength) - roadSegmentLength / 2;
                object3D.setPos(dx, 0.15f, dz);
                object3D.setVisible(true);
                break;
            }
        }
    }

    private Texture createTexture(int colorBg) {
        int tx = 1;
        Bitmap bitmap = Bitmap.createBitmap(tx + 1, tx + 1, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(colorBg); // Закрашиваем цветом
        Texture texture = new Texture(tx, tx, bitmap);
        textures.add(texture);
        bitmaps.add(bitmap);
        return texture;
    }

    private Texture createTexture(int colorBg, int colorDetails) {
        int tx = 3;
        Bitmap bitmap = Bitmap.createBitmap(tx + 1, tx + 1, Bitmap.Config.ARGB_8888);

        bitmap.eraseColor(colorBg); // Закрашиваем цветом
        bitmap.setPixel(0, 0, colorDetails);
        bitmap.setPixel(0, tx, colorDetails);
        bitmap.setPixel(tx, 0, colorDetails);
        bitmap.setPixel(tx, tx, colorDetails);
        Texture texture = new Texture(tx, tx, bitmap);
        textures.add(texture);
        bitmaps.add(bitmap);
        return texture;
    }

    private Texture createTexture(int colorBg, int colorDetails, int w, int h) {
        Bitmap bitmap = Bitmap.createBitmap(w + 1, h + 1, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(colorBg); // Закрашиваем цветом

        int size = 8;

        for (int i = 0; i < w / size; i++) {
            for (int j = 0; j < h / size; j++) {

                for (int k = 0; k < size - 2; k++) {
                    for (int l = 0; l < size - 2; l++) {
                        bitmap.setPixel(i * size + k, j * size + l, colorDetails);
                    }
                }


            }
        }
        Texture texture = new Texture(w, h, bitmap);
        textures.add(texture);
        bitmaps.add(bitmap);
        return texture;
    }

    private Texture createTextureRoad() {
        int size = roadSegmentLength * 4;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        bitmap.eraseColor(Color.DKGRAY); // Закрашиваем цветом
        for (int i = 0; i < size / 2; i++) {
            bitmap.setPixel(i, size / 2, Color.TRANSPARENT);
        }

        Texture texture = new Texture(size, size, bitmap);
        textures.add(texture);
        bitmaps.add(bitmap);
        return texture;
    }

    private Object3D createObject(int objFile, Texture texture, boolean breakable) {
        WavefrontModel model = new WavefrontModel.Builder(this, objFile)
                .addTexture("", texture)
                .create();
        Object3D object3D = model.toObject3D();
        if (breakable) object3Ds.add(object3D);
        renderPassObject3D.addObject(object3D);
        return object3D;
    }

    @Override
    public void onReleaseView(SmartGLView smartGLView) {
        for (Texture t : textures) {
            if (t != null) t.release();
        }

        for (Bitmap bitmap : bitmaps) {
            bitmap.recycle();
        }
    }

    @Override
    public void onResizeView(SmartGLView smartGLView) {

    }

    @Override
    public void onTick(SmartGLView smartGLView) {
        SmartGLRenderer renderer = smartGLView.getSmartGLRenderer();
        float frameDuration = renderer.getFrameDuration();
        update(frameDuration, renderer.getCamera());
    }

    // Explosion
    private void explosion(float x, float y, float z) {
        for (Explosion explosion : explosions) {

            if (!explosion.isVisible()) {
                explosion.setPosition(x, y, z);
                explosion.setVisible(true);
                shake = cameraShakeDistance;
                soundPool.play(soundIdExplosion, 1, 1, 0, 0, 1);
                break;
            }
        }
    }

    private void update(float delta, OpenGLCamera camera) {
        float playerRotX = player.getRotX();
        float playerRotY = player.getRotY();
        float playerRotZ = player.getRotZ();

        float playerPosX = player.getPosX();
        float playerPosY = player.getPosY();
        float playerPosZ = player.getPosZ();

        // Update sprite
        if (mSprite != null) {
            float newRot = mSprite.getRotation() + (speed * 10);
            mSprite.setRotation(newRot);
        }

        // Update environment
        updateEnvironment(playerPosX);

        // Check collisions
        for (Object3D object3D : object3Ds) {
            if (object3D.isVisible()) {
                float ox = object3D.getPosX();
                float oy = object3D.getPosY();
                float oz = object3D.getPosZ();
                float distance = getDistance(ox, oz, playerPosX, playerPosZ);
                if (distance < 1.7f) {
                    object3D.setVisible(false);
                    explosion(ox, oy, oz);
//                    speed = speedBase;
                } else {
//                    object3D.addRotY(100 * delta);

                }

                if (ox < playerPosX - halfFarLength) {
                    object3D.setVisible(false);
                    object3D.setPos(ox + farLength * 2, oy, oz);
                }

                if (oz > 0) {
                    ox += speedBase / 4;
                } else {
                    ox -= speedBase / 4;
                }
                object3D.setPos(ox, oy, oz);
            }
        }

        if (!explosions.isEmpty()) {
            for (Explosion explosion : explosions) {
                explosion.update();
            }
        }

        // Update camera
        float alpha = (float) Math.sin(playerPosX * 0.1f);
        if (shake > 0) shake -= 2 * delta;
        float cx = playerPosX + camX + shake + alpha;
        float cy = playerPosY + camY + shake;
        float cz = playerPosZ + camZ + shake + alpha;

        camera.setPosition(cx, cy, cz);
        camera.setRotation(alpha - 40, alpha * 2, 0);


        // Move player
        double playerAngle = Math.toRadians(360 - playerRotY);
        if (speed < speedLimit) speed += 0.0001f;
        float z = (float) (-speed * Math.sin(playerAngle));
        if (Math.abs(playerPosZ + z) > roadLimit) {
            z = 0;
            currentDirection = DIRECTION.STRAIGHT;
        }
        float x = (float) (-speed * Math.cos(playerAngle));

        player.setPos(playerPosX + x, playerPosY, playerPosZ + z);

        // Player rotation
        float k = 0;

//        if (playerPosZ > roadLimit && currentDirection == DIRECTION.RIGHT) {
//            currentDirection = DIRECTION.STRAIGHT;
//        }
//        if (playerPosZ < -roadLimit && currentDirection == DIRECTION.LEFT) {
//            currentDirection = DIRECTION.STRAIGHT;
//        }
        switch (currentDirection) {
            case LEFT:
                if (playerRotY < 225) k = 200;
                break;
            case RIGHT:
                if (playerRotY > 135) k = -200;
                break;
            case STRAIGHT:
                if (playerRotY < 177) {
                    k = 200;
                } else if (playerRotY > 183) {
                    k = -200;
                } else {
                    k = 0;
                    player.setRotation(playerRotX, 180, playerRotZ);
                }
        }

        player.addRotY(k * delta);
    }

    private void updateEnvironment(float playerPosX) {

        float farFromPlayer = playerPosX - farLength;

        for (Object3D object3D : road) {
            float x = object3D.getPosX();
            float y = object3D.getPosY();
            float z = object3D.getPosZ();

            if (x < playerPosX - halfFarLength) {
                object3D.setPos(x + farLength, y, z);
                addObject(object3D.getPosX());
                break;
            }

            if (x > playerPosX + halfFarLength) {
                object3D.setPos(x - farLength, y, z);
                addObject(object3D.getPosX());
                break;
            }
        }

        for (Object3D object3D : lighters) {
            float x = object3D.getPosX();
            float y = object3D.getPosY();
            float z = object3D.getPosZ();


            if (x < playerPosX - halfFarLength) {
                object3D.setPos(x + farLength, y, z);
            }

//            if (x > playerPosX + halfFarLength) {
//                object3D.setPos(x - farLength * lightMult / 2, y, z);
//            }


        }

        float bridgeX = goBridge.getX();
        if (bridgeX < farFromPlayer) {
            goBridge.setX(bridgeX + farLength * 2);
        }


        float sideRoadUpPosY = sideRoadUp.getPosY();
        float sideRoadUpPosZ = sideRoadUp.getPosZ();

        float sideRoadPosX = sideRoadDown.getPosX();
        float sideRoadDownPosY = sideRoadDown.getPosY();
        float sideRoadDownPosZ = sideRoadDown.getPosZ();

        if (sideRoadPosX < farFromPlayer) {
            sideRoadUp.setPos(sideRoadPosX + farLength * 2, sideRoadUpPosY, sideRoadUpPosZ);
            sideRoadDown.setPos(sideRoadPosX + farLength * 2, sideRoadDownPosY, sideRoadDownPosZ);
            sideRoadDown.setVisible(random.nextBoolean());
            sideRoadUp.setVisible(random.nextBoolean());
        }
    }

    private float getDistance(float x1, float y1, float x2, float y2) {
        float mDX = x2 - x1;
        float mDY = y2 - y1;
        if (mDX == 0.0F && mDY == 0.0F) {
            return 0;
        } else {
            return (float) Math.sqrt(mDX * mDX + mDY * mDY);
        }
    }

    @Override
    public void onTouchEvent(SmartGLView smartGLView, TouchHelperEvent touchHelperEvent) {
    }

    private enum DIRECTION {RIGHT, LEFT, STRAIGHT}

}