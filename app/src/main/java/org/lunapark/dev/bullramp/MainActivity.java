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

import org.lunapark.dev.bullramp.explosion.Explosion;
import org.lunapark.dev.bullramp.map.MapGen;

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

import static android.opengl.GLES20.GL_POLYGON_OFFSET_FILL;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glPolygonOffset;

public class MainActivity extends Activity implements SmartGLViewController, View.OnTouchListener {

    private SmartGLView mSmartGLView;
    private ImageButton btnLeft, btnRight;
    private final int divider = 1, roadSegments = 10, segmentLength = 10;
    private float farLength = segmentLength * roadSegments;

    private Texture mSpriteTexture;
    private Sprite mSprite;

    private Object3D player;
    private RenderPassObject3D renderPassObject3D;
    private RenderPassSprite renderPassSprite;

    private ArrayList<Texture> textures;
    private ArrayList<Object3D> object3Ds, road, lighters;
    private int lightMult = 4;

    private ArrayList<Explosion> explosions;

    private float cameraShakeDistance = 1.0f, shake;
    private float camX = 5f, camY = 9f, camZ = 15f;
    //    private float camX = 2f, camY = 4f, camZ = 7f;
    //    private float camX = 0f, camY = 9f, camZ = 0f;
    private float camRotX = -90;

    private SoundPool soundPool;
    private int soundIdExplosion;
    private GameObject goBridge;
    private int screenW, screenH;

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

    private enum DIRECTION {RIGHT, LEFT, STRAIGHT}

    private DIRECTION currentDirection = DIRECTION.STRAIGHT;

    private Random random;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSmartGLView = (SmartGLView) findViewById(R.id.smartGLView);
        mSmartGLView.setDefaultRenderer(this);
        mSmartGLView.setController(this);

        btnLeft = (ImageButton) findViewById(R.id.btnLeft);
        btnRight = (ImageButton) findViewById(R.id.btnRight);
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

        smartGLView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        SmartGLRenderer renderer = smartGLView.getSmartGLRenderer();
//        renderer.setClearColor(81f / 255f, 149f / 255f, 58f / 255f, 1f);
        renderer.setClearColor(0, 0, 0, 1f);

        RenderPassSprite bgSprite = new RenderPassSprite();

        renderPassObject3D = new RenderPassObject3D(RenderPassObject3D.ShaderType.SHADER_TEXTURE, true, true);
        renderPassSprite = new RenderPassSprite();

        renderer.addRenderPass(bgSprite);
        renderer.addRenderPass(renderPassObject3D);  // add it only once for all 3D Objects
        renderer.addRenderPass(renderPassSprite);  // add it only once for all Sprites

        // Colors

//        holo blue light = 33b5e5 ( rgb: 51, 181, 229 )
//        holo blue dark = 0099cc ( rgb: 0, 153 204 )
//        holo blue bright = 00ddff ( rgb: 0, 221, 255 )﻿

        int colHoloLight = Color.argb(128, 51, 181, 229);
        int colHoloDark = Color.argb(128, 0, 153, 204);
        int colHoloBright = Color.argb(128, 0, 221, 255);
        int colDkGray = Color.argb(180, 64, 64, 64);
        int colRed = Color.argb(140, 240, 0, 0);
        // Textures
        textures = new ArrayList<>();
//        Texture txGreen = createTexture(Color.GREEN, Color.BLACK);
        Texture txRed = createTexture(colHoloBright, Color.TRANSPARENT);
//        Texture txRuby = createTexture(Color.BLACK, Color.RED);
//        Texture txYellow = createTexture(Color.YELLOW, Color.BLACK);
//        Texture txDkGray = createTexture(Color.DKGRAY, Color.BLACK);
//        Texture txLtGray = createTexture(Color.LTGRAY, Color.BLACK);
        Texture txOrange = createTexture(Color.YELLOW, Color.rgb(255, 112, 16));
//        txTruck = createTexture(Color.BLACK, Color.DKGRAY);
        Texture txRoad = createTextureRoad();

        Texture txWhite = createTexture(colDkGray, colDkGray);
        Texture txPlayer = createTexture(colRed, colRed);


        // Create sprites
//        mSpriteTexture = new Texture(this, R.drawable.col_circle);
        mSpriteTexture = createMap();
        textures.add(mSpriteTexture);

        mSprite = new Sprite(60, 60); // 120 x 120 pixels
        mSprite.setPivot(0.5f, 0.5f);  // set position / rotation axis to the middle of the sprite
        mSprite.setPos(60, 60);
        mSprite.setTexture(mSpriteTexture);

//        bgSprite.addSprite(mSprite);
        renderPassSprite.addSprite(mSprite);

        Texture txBackground = createTexture(Color.BLACK, Color.DKGRAY, 400, 240);
        Sprite spriteBg = new Sprite(screenW, screenH);
//        spriteBg.setPos(screenW / 2, screenH / 2);
//        spriteBg.setPivot(0.5f, 0.5f);
        spriteBg.setTexture(txBackground);
        bgSprite.addSprite(spriteBg);

        // Create 3D objects
        object3Ds = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Object3D cube = createObject(R.raw.cube, txRed, true);
            cube.setVisible(false);
            object3Ds.add(cube);
        }


        // Create player
//        player = createObject3(R.raw.bulldozer, txTruck, txRuby, txOrange, false);
        player = createObject(R.raw.bulldozer, txPlayer, false);
        player.setScale(0.001f, 0.001f, 0.001f);
        player.setPos(0, 0, 0);
        player.addRotY(180);

        // Create road
        road = new ArrayList<>();
        for (int i = 0; i < roadSegments; i++) {
            Object3D ground = createObject(R.raw.plane, txRoad, false);
            ground.setPos(segmentLength * i - farLength / 2, -0.5f, segmentLength / 2);
//            ground.setScale(segmentLength , 0, segmentLength);
            road.add(ground);
        }

        int he = 20;
        lighters = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Object3D lighter1 = createObject(R.raw.cube, txWhite, false);
            lighter1.setPos(segmentLength * i * lightMult - farLength / 2, he / 2, -segmentLength);
            lighter1.setScale(0.25f, he, 0.25f);

            Object3D lighter2 = createObject(R.raw.cube, txWhite, false);
            lighter2.setPos(segmentLength * i * lightMult - farLength / 2, he / 2, segmentLength);
            lighter2.setScale(0.25f, he, 0.25f);

            lighters.add(lighter1);
            lighters.add(lighter2);
        }

        ArrayList<Object3D> bridge = new ArrayList<>();
        Object3D bridge1 = createObject(R.raw.cube, txWhite, false);
        bridge1.setPos(0, 2.5f, -50 - segmentLength);
        bridge1.setScale(10, 4, 100);
        bridge.add(bridge1);
        Object3D bridge2 = createObject(R.raw.cube, txWhite, false);
        bridge2.setPos(0, 2.5f, 50 + segmentLength);
        bridge2.setScale(10, 4, 100);
        bridge.add(bridge2);

        Object3D bridge3 = createObject(R.raw.cube, txWhite, false);
        bridge3.setPos(0, 5, 0);
        bridge3.setScale(10, 0.5f, 40);
        bridge.add(bridge3);

        goBridge = new GameObject();
        goBridge.setObjects(bridge);
        goBridge.setPosition(20, 0, 0);


        // Prepare explosions
        explosions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Explosion explosion = new Explosion(this, renderPassObject3D, R.raw.cube, txOrange);
            explosions.add(explosion);

        }
    }

    private Texture createMap() {
        int[][] map = MapGen.getInstance().getMap();
        int w = MapGen.getInstance().w;
        int h = MapGen.getInstance().h;
        Bitmap bitmap = Bitmap.createBitmap(w + 1, h + 1, Bitmap.Config.ARGB_8888);

        bitmap.eraseColor(Color.BLACK); // Закрашиваем цветом

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (map[i][j] == 1) bitmap.setPixel(i, j, Color.WHITE);
            }
        }
        Texture texture = new Texture(w, h, bitmap);
        textures.add(texture);
        return texture;

    }

    private void addObject(float x, float z) {
        for (Object3D object3D : object3Ds) {
            if (!object3D.isVisible()) {
                float dx = random.nextInt(segmentLength) + x;
                float dz = random.nextInt(segmentLength) - segmentLength / 2;
//                Object3D cube = createObject(R.raw.cube, txDkGray, true);
                object3D.setPos(dx, 0.5f, dz);
                object3D.setVisible(true);
                break;
            }
        }
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
        return texture;
    }

    private Texture createTexture(int colorBg, int colorDetails, int w, int h) {
        Bitmap bitmap = Bitmap.createBitmap(w + 1, h + 1, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(colorBg); // Закрашиваем цветом

        for (int i = 0; i < w / 4; i++) {
            for (int j = 0; j < h / 4; j++) {
//                int offset = j % 2;
                bitmap.setPixel(i * 4, j * 4, colorDetails);
            }
        }


        Texture texture = new Texture(w, h, bitmap);

        textures.add(texture);
        return texture;
    }

    private Texture createTextureRoad() {
        int size = segmentLength * 4;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        bitmap.eraseColor(Color.DKGRAY); // Закрашиваем цветом
        for (int i = 0; i < size / 2; i++) {
            bitmap.setPixel(i, size / 2, Color.TRANSPARENT);
        }

        Texture texture = new Texture(size, size, bitmap);
        textures.add(texture);
        return texture;
    }


    private Object3D createObject(int objFile, Texture texture, boolean addToList) {
        WavefrontModel model = new WavefrontModel.Builder(this, objFile)
                .addTexture("", texture)
                .create();
        Object3D object3D = model.toObject3D();
        if (addToList) object3Ds.add(object3D);
        renderPassObject3D.addObject(object3D);
        return object3D;
    }

    private Object3D createObject3(int objFile, Texture texture1, Texture texture2, Texture texture3, boolean addToList) {
        WavefrontModel model = new WavefrontModel.Builder(this, objFile)
                .addTexture("Color1", texture1)
                .addTexture("Color2", texture2)
                .addTexture("Color3", texture3)
                .create();
        Object3D object3D = model.toObject3D();
        if (addToList) object3Ds.add(object3D);
        renderPassObject3D.addObject(object3D);
        return object3D;
    }

    @Override
    public void onReleaseView(SmartGLView smartGLView) {
        for (Texture t : textures) {
            if (t != null) t.release();
        }
    }

    @Override
    public void onResizeView(SmartGLView smartGLView) {

    }

    @Override
    public void onTick(SmartGLView smartGLView) {
        SmartGLRenderer renderer = smartGLView.getSmartGLRenderer();
//        GLES20.glEnable(GL_POLYGON_OFFSET_FILL);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(2.0f, 2.0f);

        glDisable(GL_POLYGON_OFFSET_FILL);
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

//        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 100);
    }

    private void update(float delta, OpenGLCamera camera) {

        // Update sprite
        if (mSprite != null) {
//            float newX = mSprite.getPosX() + (frameDuration * 100);
//            float newY = mSprite.getPosY();
//            if (newX > 600) {
//                newX = 0;
//            }
//            mSprite.setPos(newX, newY);

            float newRot = mSprite.getRotation() + (delta * 100);
            mSprite.setRotation(newRot);
        }

        // Update environment
        updateEnvironment();

        // Player direction
        float k = 0;
        float playerRotX = player.getRotX();
        float playerRotY = player.getRotY();
        float playerRotZ = player.getRotZ();

        float pZ = player.getPosZ();
        if (pZ > segmentLength / 2 && currentDirection == DIRECTION.RIGHT) {
            currentDirection = DIRECTION.STRAIGHT;
        }
        if (pZ < -segmentLength / 2 && currentDirection == DIRECTION.LEFT) {
            currentDirection = DIRECTION.STRAIGHT;
        }
        switch (currentDirection) {
            case LEFT:
                if (playerRotY < 215) k = 200;
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
                    player.setRotation(playerRotX, 180, playerRotZ);
                }
        }


//        if (playerRotY > 135 && playerRotY < 215)
        player.addRotY(k * delta);
//        if (currentDirection == DIRECTION.STRAIGHT) player.setRotation(playerRotX, , playerRotZ);

        // Move player
        double playerAngle = Math.toRadians(360 - player.getRotY());
        float speed = 0.3f;
        float z = (float) (-speed * Math.sin(playerAngle));
        float x = (float) (-speed * Math.cos(playerAngle));

        float playerPosX = player.getPosX() + x;
        float playerPosY = player.getPosY();
        float playerPosZ = player.getPosZ() + z;

        player.setPos(playerPosX, playerPosY, playerPosZ);

        // Check collisions
        //        for (Iterator<Object3D> iterator = object3Ds.iterator(); iterator.hasNext(); ) {
//            Object3D object3D = iterator.next();
//            float ox = object3D.getPosX();
//            float oy = object3D.getPosY();
//            float oz = object3D.getPosZ();
//            float distance = getDistance(ox, oz, player.getPosX(), player.getPosZ());
//            if (distance < 1.7f) {
//                object3D.setVisible(false);
//                explosion(ox, oy, oz);
//                boom = true;
////                iterator.remove();
//            } else {
//                object3D.addRotY(100 * delta);
//            }
//        }

        for (Object3D object3D : object3Ds) {
            if (object3D.isVisible()) {
                float ox = object3D.getPosX();
                float oy = object3D.getPosY();
                float oz = object3D.getPosZ();
                float distance = getDistance(ox, oz, player.getPosX(), player.getPosZ());
                if (distance < 1.7f) {
                    object3D.setVisible(false);
                    explosion(ox, oy, oz);

                } else {
                    object3D.addRotY(100 * delta);
                }

                if (distance > 100) {
                    object3D.setVisible(false);
                }
            }
        }

        // Check explosions
//        if (!explosions.isEmpty()) {
//
//            for (Iterator<Explosion> iterator = explosions.iterator(); iterator.hasNext(); ) {
//                Explosion explosion = iterator.next();
//                if (explosion.getSize() > 10) {
//                    explosion.dispose();
//                    iterator.remove();
//                } else {
//                    explosion.update();
//                }
//            }
//        }

        if (!explosions.isEmpty()) {
            for (Explosion explosion : explosions) {
                explosion.update();
            }
        }

        // Update camera

        // Camera follow player
//        playerPosX = player.getPosX();
//        playerPosY = player.getPosY();
//        playerPosZ = player.getPosZ();

        float cx;
        float cy;
        float cz;


        float alpha = (float) Math.sin(playerPosX * 0.1f);


        if (shake > 0) shake -= 2 * delta;

        cx = playerPosX + camX + shake + alpha;
        cy = playerPosY + camY + shake;
        cz = playerPosZ + camZ + shake + alpha;

        camera.setPosition(cx, cy, cz);
        camera.setRotation(alpha - 40, alpha * 2, 0);
    }

    private void updateEnvironment() {
        for (Object3D object3D : road) {
            float x = object3D.getPosX();
            float y = object3D.getPosY();
            float z = object3D.getPosZ();

            if (x < player.getPosX() - farLength / 2) {
                object3D.setPos(x + farLength, y, z);
                addObject(object3D.getPosX(), z);
                break;
            }

            if (x > player.getPosX() + farLength / 2) {
                object3D.setPos(x - farLength, y, z);
                addObject(object3D.getPosX(), z);
                break;
            }
        }

        for (Object3D object3D : lighters) {
            float x = object3D.getPosX();
            float y = object3D.getPosY();
            float z = object3D.getPosZ();

            if (x < player.getPosX() - farLength / 2) {
                object3D.setPos(x + farLength * lightMult / 2, y, z);
            }

            if (x > player.getPosX() + farLength / 2) {
                object3D.setPos(x - farLength * lightMult / 2, y, z);
            }
        }

        if (goBridge.getX() < player.getPosX() - farLength) {
            goBridge.setX(player.getPosX() + farLength);
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

//        SmartGLRenderer renderer = smartGLView.getSmartGLRenderer();
//
//        if (touchHelperEvent.getType() == TouchHelperEvent.TouchEventType.SINGLETOUCH) {
//            float x = touchHelperEvent.getX(0);
//            float y = touchHelperEvent.getY(0);
//
//            float h = renderer.getHeight();
//            float w = renderer.getWidth();
//
//
//            if (x < h / 2) {
//                currentDirection = DIRECTION.LEFT;
//            } else {
//
//                currentDirection = DIRECTION.RIGHT;
//            }
//        }
//
//        if (touchHelperEvent.getType() == TouchHelperEvent.TouchEventType.SINGLEUNTOUCH) {
//            currentDirection = DIRECTION.STRAIGHT;
//        }
    }
}
