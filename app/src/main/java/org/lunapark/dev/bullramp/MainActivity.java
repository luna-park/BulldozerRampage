package org.lunapark.dev.bullramp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

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

    private int divider = 1; // pixel size
    private boolean joyVisible;
    private float joyDelta;
    private int joySize, halfJoySize;
    private Sprite joyBaseSprite, joyKnobSprite;
    private float joyX, joyY;

    // Road params
    private int roadSegments = 10, roadSegmentLength = 10;
    private float roadLimit = roadSegmentLength / 2;
    private float farLength = roadSegmentLength * roadSegments;
    private float halfFarLength = farLength / 2;
    private float roadPosY = -0.5f;
    private float deltaZ = 0.5f;

    // Lighters params
    private int lightSegments = roadSegments / 3, lightSegmentLength = roadSegmentLength * 3;
    private int lighterHeight = 10;

    // Side roads params
    private int scaleSideRoad = 5;
    private float sideRoadX = (farLength + lightSegmentLength - scaleSideRoad) / 2;

    // Player and bots params
    private float speedBase = 0.3f, speed, speedLimit = speedBase * 2;
    private float velocity = 0.0005f; // 0.0002f;
    private float speedBots = speedBase / 4;
    private float speedOpponents = speedBase * 1.2f;
    private int numOpponents = 7;
    private int place = numOpponents + 1;
    private int totalPlayers = place;
    private int distance = 0, finishDistance = (numOpponents + 1) * 1000;
    private boolean cheatNoDeccelerate = true;

    private SmartGLView mSmartGLView;

    private Handler handler;
    private Runnable runnable;
    private TextView tvPlace, tvDistance;

    private Texture mSpriteTexture;
    private Sprite mSprite;
    private Object3D player, sideRoadUp, sideRoadDown;
    private RenderPassObject3D renderPassObject3D;
    private RenderPassSprite renderPassSprite;
    private ArrayList<Texture> textures;
    private ArrayList<Object3D> bots, opponents, road, lighters, uptown, downtown;
    private ArrayList<Explosion> explosions;
    private float cameraShakeDistance = 2.0f, shake;
    private float camXgame = 8f, camYgame = 10f, camZgame = 15f;
    private float camRotXgame = 40, camRotYgame = -1, camRotZgame = 0;


    private SoundPool soundPool;
    private int sfxExplosion, sfxHit;
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
    private Texture txRed;
    private Texture txLtGray;
    private DIRECTION currentDirection = DIRECTION.STRAIGHT;
    private Random random;


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
        layout.screenBrightness = 0.5f;
        getWindow().setAttributes(layout);

        //
        mSmartGLView = (SmartGLView) findViewById(R.id.smartGLView);
        mSmartGLView.setDefaultRenderer(this);
        mSmartGLView.setController(this);

        ImageButton btnLeft = (ImageButton) findViewById(R.id.btnLeft);
        ImageButton btnRight = (ImageButton) findViewById(R.id.btnRight);
        btnLeft.setOnTouchListener(this);
        btnRight.setOnTouchListener(this);

        tvPlace = (TextView) findViewById(R.id.tvPlace);
        tvPlace.setText(place + "/" + totalPlayers);

        tvDistance = (TextView) findViewById(R.id.tvDistance);

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

        sfxExplosion = soundPool.load(this, R.raw.explosion, 1);
        sfxHit = soundPool.load(this, R.raw.hit, 1);

        // Prepare update UI
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                tvPlace.setText(place + "/" + totalPlayers);
                tvDistance.setText(getString(R.string.distance) + distance);
            }
        };
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

        // Create joystick
        Texture joyTexture = new Texture(this, R.drawable.col_circle);
        textures.add(joyTexture);

        joySize = screenH / 3;
        halfJoySize = joySize / 2;

        joyBaseSprite = new Sprite(joySize, joySize);
        joyBaseSprite.setPivot(0.5f, 0.5f);
        joyBaseSprite.setTexture(joyTexture);
        joyBaseSprite.setVisible(joyVisible);

        joyKnobSprite = new Sprite(screenH / 6, screenH / 6);
        joyKnobSprite.setPivot(0.5f, 0.5f);
        joyKnobSprite.setTexture(joyTexture);
        joyKnobSprite.setVisible(joyVisible);

        renderPassSprite.addSprite(joyBaseSprite);
        renderPassSprite.addSprite(joyKnobSprite);

        // Create 3D objects
        bots = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Object3D bot = createObject(R.raw.truck, txHoloBright, true);
            bot.setScale(0.001f, 0.001f, 0.001f);
            bot.setVisible(false);
            bots.add(bot);
        }

        opponents = new ArrayList<>();
        for (int i = 0; i < numOpponents; i++) {
            Object3D opponent = createObject(R.raw.cube, txRed, false);
            opponent.setScale(2, 1, 1);
            opponent.setVisible(true);
            opponents.add(opponent);
        }

        // Create player
        player = createObject(R.raw.bulldozer, createTexture(colOrange), false);
        player.setScale(0.001f, 0.001f, 0.001f);


        createEnvironment();

        // Prepare explosions
        explosions = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Explosion explosion = new Explosion(this, renderPassObject3D, R.raw.cube, txExplosion);
            explosions.add(explosion);
        }

        resetGame();
    }

    private void resetGame() {
        player.setPos(0, -0.3f, 0);
        player.setRotation(0, 180, 0);
        speed = speedBase;

        for (int i = 0; i < opponents.size(); i++) {
            Object3D opponent = opponents.get(i);
            opponent.setPos(i * farLength + halfFarLength / 2, 0.2f, 0);
        }

        for (int i = 0; i < bots.size(); i++) {
            Object3D bot = bots.get(i);
            bot.setVisible(false);
        }

        for (int i = 0; i < road.size(); i++) {
            Object3D ground = road.get(i);
            ground.setPos(roadSegmentLength * i - halfFarLength, roadPosY, roadSegmentLength / 2 - deltaZ);
        }
        sideRoadDown.setPos(sideRoadX, -0.5f, roadSegmentLength * scaleSideRoad + roadSegmentLength / 2 - deltaZ);
        sideRoadUp.setPos(sideRoadX, -0.5f, -roadSegmentLength / 2 - deltaZ);

        goBridge.setPosition(0, 0, -deltaZ);
    }

    private void createEnvironment() {
        // Create road
        road = new ArrayList<>();
        for (int i = 0; i < roadSegments; i++) {
            Object3D ground = createObject(R.raw.plane, txRoad, false);
//            ground.setPos(roadSegmentLength * i - halfFarLength, roadPosY, roadSegmentLength / 2);
            road.add(ground);
        }

        sideRoadDown = createObject(R.raw.plane, createTexture(Color.DKGRAY), false);
        sideRoadUp = createObject(R.raw.plane, createTexture(Color.DKGRAY), false);
        sideRoadDown.setScale(0.75f, 1, scaleSideRoad);
        sideRoadUp.setScale(0.75f, 1, scaleSideRoad);

        lighters = new ArrayList<>();
        for (int i = 0; i < lightSegments; i++) {
            float x = lightSegmentLength * i - halfFarLength;
            Object3D lighter1 = createObject(R.raw.cube, txLtGray, false);
            lighter1.setPos(x, lighterHeight / 2 - 0.5f, -roadSegmentLength - deltaZ);
            lighter1.setScale(0.25f, lighterHeight, 0.25f);

            Object3D lighter2 = createObject(R.raw.cube, txLtGray, false);
            lighter2.setPos(x, lighterHeight / 2 - 0.5f, roadSegmentLength - deltaZ);
            lighter2.setScale(0.25f, lighterHeight, 0.25f);

            lighters.add(lighter1);
            lighters.add(lighter2);
        }

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

//        Object3D uptown1 = createObject(R.raw.cube, txLtGray, false);
//        uptown1.setPos(20, 1.5f, (10 + roadSegmentLength));
//        uptown1.setScale(10, 4, 10);
//        bridge.add(uptown1);
//
//        Object3D uptown2 = createObject(R.raw.cube, txLtGray, false);
//        uptown2.setPos(40, 1.5f, (10 + roadSegmentLength));
//        uptown2.setScale(10, 4, 10);
//        bridge.add(uptown2);

        int houseCount = 3;

        float houseStep = halfFarLength / houseCount;

        uptown = new ArrayList<>();
        downtown = new ArrayList<>();

        for (int i = 0; i < houseCount; i++) {
            Object3D uptown1 = createObject(R.raw.cube, txLtGray, false);
            uptown1.setPos(20 + i * houseStep, 1.5f, -(10 + roadSegmentLength));
            uptown1.setScale(10, 4, 10);
            Object3D uptown2 = createObject(R.raw.cube, txLtGray, false);
            uptown2.setPos(-20 - i * houseStep, 1.5f, -(10 + roadSegmentLength));
            uptown2.setScale(10, 4, 10);

            bridge.add(uptown1);
            bridge.add(uptown2);
            uptown.add(uptown1);
            uptown.add(uptown2);

            Object3D downtown1 = createObject(R.raw.cube, txLtGray, false);
            downtown1.setPos(20 + i * houseStep, 1.5f, 10 + roadSegmentLength);
            downtown1.setScale(10, 4, 10);
            Object3D downtown2 = createObject(R.raw.cube, txLtGray, false);
            downtown2.setPos(-20 - i * houseStep, 1.5f, 10 + roadSegmentLength);
            downtown2.setScale(10, 4, 10);

            bridge.add(downtown1);
            bridge.add(downtown2);
            downtown.add(downtown1);
            downtown.add(downtown2);
        }

        goBridge = new GameObject();
        goBridge.setObjects(bridge);
    }


    private Texture createTexture(int colorBg) {
        int tx = 1;
        Bitmap bitmap = Bitmap.createBitmap(tx + 1, tx + 1, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(colorBg); // Закрашиваем цветом
        Texture texture = new Texture(tx, tx, bitmap);
        textures.add(texture);
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
        return texture;
    }

    private Object3D createObject(int objFile, Texture texture, boolean breakable) {
        WavefrontModel model = new WavefrontModel.Builder(this, objFile)
                .addTexture("", texture)
                .create();
        Object3D object3D = model.toObject3D();
        if (breakable) bots.add(object3D);
        renderPassObject3D.addObject(object3D);
        return object3D;
    }

    @Override
    public void onReleaseView(SmartGLView smartGLView) {
        if (textures != null)
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
        float frameDuration = renderer.getFrameDuration();
        update(frameDuration, renderer.getCamera());
    }

    // Explosion
    private void explosion(float x, float y, float z) {
        for (int i = 0; i < explosions.size(); i++) {
            Explosion explosion = explosions.get(i);
            if (!explosion.isVisible()) {
                explosion.setPosition(x, y, z);
                explosion.setVisible(true);
                soundPool.play(sfxExplosion, 1, 1, 0, 0, 1);
                break;
            }
        }
    }

    private void update(float delta, OpenGLCamera camera) {

        if (player.getPosX() < finishDistance) {
            float playerRotX = player.getRotX();
            float playerRotY = player.getRotY();
            float playerRotZ = player.getRotZ();

            float playerPosX = player.getPosX();
            float playerPosY = player.getPosY();
            float playerPosZ = player.getPosZ();


            float camRotX, camRotY, camRotZ, camPosX, camPosY, camPosZ;

            distance = Math.round(playerPosX);
            // Update sprite
            if (mSprite != null) {
                float newRot = mSprite.getRotation() + (speed * 10);
                mSprite.setRotation(newRot);
            }

            // Update environment
            updateEnvironment(playerPosX);

            // Check collisions
            for (int i = 0; i < bots.size(); i++) {
                Object3D object3D = bots.get(i);
                if (object3D.isVisible()) {
                    float ox = object3D.getPosX();
                    float oy = object3D.getPosY();
                    float oz = object3D.getPosZ();
                    float distance = getDistance(ox, oz, playerPosX, playerPosZ);

                    if (distance < 1.7f) {
                        object3D.setVisible(false);
                        explosion(ox, oy, oz);
                        shake = cameraShakeDistance;
                        if (!cheatNoDeccelerate) speed = speedBase;
                    }

                    if (ox < playerPosX - halfFarLength) {
                        object3D.setVisible(false);
                        object3D.setPos(ox + farLength * 2, oy, oz);
                    }

                    if (oz > 0) {
                        ox += speedBots;
                        object3D.setRotation(0, 0, 0);
                    } else {
                        ox -= speedBots;
                        object3D.setRotation(0, 180, 0);
                    }
                    object3D.setPos(ox, oy, oz);
                }
            }

            updateOpponents(playerPosX, playerPosZ);

            // Update UI
            handler.post(runnable);

            // Update explosions
            if (!explosions.isEmpty()) {
                for (int i = 0; i < explosions.size(); i++) {
                    explosions.get(i).update();
                }
            }

            // Move player
            double playerAngle = Math.toRadians(360 - playerRotY);
            if (speed < speedLimit) speed += velocity;
            float z = (float) (-speed * Math.sin(playerAngle));
            if (Math.abs(playerPosZ + z) > roadLimit) {
                z = 0;
                currentDirection = DIRECTION.STRAIGHT;
            }
            float x = (float) (-speed * Math.cos(playerAngle));

            player.setPos(playerPosX + x, playerPosY, playerPosZ + z);

            // Player rotation

            float k = 0;

            switch (currentDirection) {
                case ANALOG:
                    if (playerRotY > 135 && playerRotY < 225)
                        k = -joyDelta * 4;
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


//            switch (currentDirection) {
//                case LEFT:
//                    if (playerRotY < 225) k = 200;
//                    break;
//                case RIGHT:
//                    if (playerRotY > 135) k = -200;
//                    break;
//                case STRAIGHT:
//                    if (playerRotY < 177) {
//                        k = 200;
//                    } else if (playerRotY > 183) {
//                        k = -200;
//                    } else {
//                        k = 0;
//                        player.setRotation(playerRotX, 180, playerRotZ);
//                    }
//            }

            player.addRotY(k * delta);

            // Camera
            camRotX = camRotXgame;
            camRotY = camRotYgame;
            camRotZ = camRotZgame;

            camPosX = camXgame;
            camPosY = camYgame;
            camPosZ = camZgame;
            // Update camera
            float alpha = (float) Math.sin(playerPosX * 0.1f);
            if (shake > 0) shake -= 2 * delta;
            float cx = playerPosX + camPosX + shake + alpha;
            float cy = playerPosY + camPosY + shake;
            float cz = playerPosZ + camPosZ + shake + alpha;

            camera.setPosition(cx, cy, cz);
            camera.setRotation(alpha - camRotX, alpha * 2 + camRotY, camRotZ);
        } else {
            resetGame();
        }

    }

    private void updateBots(float x) {
        float dz1 = roadSegmentLength / 2 - deltaZ * 2;
        for (int i = 0; i < bots.size(); i++) {
            Object3D object3D = bots.get(i);
            if (!object3D.isVisible()) {
                float dx = random.nextInt(roadSegmentLength / 4) + x;
                float dz = random.nextFloat() * dz1 + deltaZ;
                if (random.nextBoolean()) dz *= -1;
                object3D.setPos(dx, -0.4f, dz);
                object3D.setVisible(true);
                break;
            }
        }
    }

    private void updateOpponents(float playerPosX, float playerPosZ) {
        // Update opponents
        for (int i = 0; i < opponents.size(); i++) {
            Object3D object3D = opponents.get(i);
            float ox = object3D.getPosX();
            float oy = object3D.getPosY();
            float oz = object3D.getPosZ();
            float dx1 = playerPosX - halfFarLength;
            float dx2 = playerPosX + halfFarLength;
            if ((ox > dx1) && (ox < dx2)) {
                float distance = getDistance(ox, oz, playerPosX, playerPosZ);
                if (distance < 1.7f) {
                    soundPool.play(sfxHit, 0.5f, 0.5f, 1, 0, 1);
                    speed = speedBase;
                }

                if (ox < playerPosX) {
                    place = totalPlayers - i - 1;
                } else {
                    place = totalPlayers - i;
                }

                for (int j = 0; j < bots.size(); j++) {
                    Object3D bot = bots.get(j);
                    if (bot.isVisible()) {
                        float botPosX = bot.getPosX();
                        float botPosY = bot.getPosY();
                        float botPosZ = bot.getPosZ();
                        distance = getDistance(botPosX, botPosZ, ox, oz);
                        if (distance < 1.7f) {
                            bot.setVisible(false);
                            explosion(botPosX, botPosY, botPosZ);
                        }
                    }
                }

                float sinAlpha = (float) Math.sin(ox * 0.1f);
                oz = sinAlpha * roadSegmentLength / 2;

                float alpha = (float) Math.toDegrees(Math.asin(sinAlpha));

                float rox = object3D.getRotX();
                float roy = object3D.getRotY();
                float roz = object3D.getRotZ();

//                object3D.addRotY((float) (90 - Math.toDegrees(Math.asin(sinAlpha))));
                object3D.setRotation(rox, alpha / 2 - 180, roz);


            }
            ox += speedOpponents;
            object3D.setPos(ox, oy, oz);
        }
    }

    private void updateEnvironment(float playerPosX) {

        float farFromPlayer = playerPosX - farLength;
        float dx1 = playerPosX - halfFarLength;
        float dx2 = playerPosX + halfFarLength;

        for (int i = 0; i < road.size(); i++) {
            Object3D object3D = road.get(i);

            float x = object3D.getPosX();
            float y = roadPosY;
            float z = object3D.getPosZ();

            if (x < dx1) {
                object3D.setPos(x + farLength, y, z);
                updateBots(object3D.getPosX());
                break;
            }

            if (x > dx2) {
                object3D.setPos(x - farLength, y, z);
                updateBots(object3D.getPosX());
                break;
            }
        }
        for (int i = 0; i < lighters.size(); i++) {
            Object3D object3D = lighters.get(i);
            float x = object3D.getPosX();
            float y = object3D.getPosY();
            float z = object3D.getPosZ();

            if (x < dx1) {
                object3D.setPos(x + farLength, y, z);
            }

            if (x > dx2) {
                object3D.setPos(x - farLength, y, z);
            }
        }

        float bridgeX = goBridge.getX();
        if (bridgeX < farFromPlayer) {
            goBridge.setX(bridgeX + farLength * 2);
            for (int i = 0; i < uptown.size(); i++) {
                Object3D object3D = uptown.get(i);
                object3D.setVisible(random.nextBoolean());
            }

            for (int i = 0; i < downtown.size(); i++) {
                Object3D object3D = downtown.get(i);
                object3D.setVisible(random.nextBoolean());
            }
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
        TouchHelperEvent.TouchEventType type = touchHelperEvent.getType();
        switch (type) {
            case SINGLETOUCH:
                joyVisible = true;
                joyX = touchHelperEvent.getX(0);
                joyY = touchHelperEvent.getY(0);
                break;
            case SINGLEMOVE:
                float y = touchHelperEvent.getY(0) - joyY;
                System.out.println(y);
                if (Math.abs(y) < halfJoySize) joyDelta = y;
                currentDirection = DIRECTION.ANALOG;
                break;
            case SINGLEUNTOUCH:
                joyVisible = false;
                joyDelta = 0;
                currentDirection = DIRECTION.STRAIGHT;
                break;
        }
        updateJoystick();
    }

    private void updateJoystick() {
        joyBaseSprite.setVisible(joyVisible);
        joyKnobSprite.setVisible(joyVisible);

        joyBaseSprite.setPos(joyX, joyY);
        joyKnobSprite.setPos(joyX, joyY + joyDelta);
    }

    private enum DIRECTION {RIGHT, LEFT, STRAIGHT, ANALOG}

}