package org.lunapark.dev.bullramp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
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

public class GameActivity extends Activity implements SmartGLViewController {

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
    private float playerPosX, playerPosY, playerPosZ;
    private float joyCoefficient = 45; // Degrees of player's rotation
    private float speedBase = 0.3f, speed, speedLimit = speedBase * 2;
    private float velocity = 0.0005f; // 0.0002f;
    private float speedBots = speedBase / 4;
    private float speedOpponents = speedBase * 1.2f;
    private int level;// = 7;
    private int place;// = level + 1;
    private int totalPlayers;// = place;
    private int finishDistance;// = (level + 1) * 400; // 500

    private boolean cheatNoDeccelerate = true, sfxFinishPlay = true;

    private float trackLength, trackX, trackY, trackMax; // for UI
    private ArrayList<Sprite> sprites;

    private boolean gameover = false;

    private SmartGLView mSmartGLView;

    private Handler handler;
    private Runnable runnable;
    private TextView tvResult;

    private Sprite sprPlayer, sprTrack;
    private Object3D player, sideRoadUp, sideRoadDown;
    private RenderPassObject3D renderPassObject3D;
    private RenderPassSprite renderPassSprite;
    private ArrayList<Texture> textures;
    private ArrayList<Object3D> bots, opponents, road, lighters, uptown, downtown;
    private ArrayList<Explosion> explosions;
    private float cameraShakeDistance = 2.0f, shake;
    private float camXgame = 8f, camYgame = 10f, camZgame = 15f;
    private float camRotXgame = 40, camRotYgame = -1, camRotZgame = 0;


    //    private SoundPool soundPool;
    private int sfxExplosion = Assets.instance().sfxExplosion,
            sfxHit = Assets.instance().sfxHit, sfxFinished = Assets.instance().sfxFinished;
    private long sfxHitTime;
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

    //
    private SharedPreferences preferences;
    private String DATA_LEVEL = "level";

    private void loadData() {

        level = preferences.getInt(DATA_LEVEL, 1);
        place = level + 1;
        totalPlayers = place;
        finishDistance = (level + 1) * 400; // 500
    }

    private void saveData() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(DATA_LEVEL, level);
        editor.apply();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set brightness
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = 0.5f;
        getWindow().setAttributes(layout);

        preferences = getPreferences(MODE_PRIVATE);
        loadData();
        //
        mSmartGLView = (SmartGLView) findViewById(R.id.smartGLView);
        mSmartGLView.setDefaultRenderer(this);
        mSmartGLView.setController(this);

        tvResult = (TextView) findViewById(R.id.tvResult);
        tvResult.setVisibility(View.INVISIBLE);
        tvResult.setLayerType(View.LAYER_TYPE_SOFTWARE, tvResult.getPaint());

        // Prepare update UI
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (gameover) {
                    tvResult.setVisibility(View.VISIBLE);
                    String result = place + "/" + totalPlayers;
                    tvResult.setText(String.format(getResources().getString(R.string.txt_result), result));

                }

            }
        };

        // Double pixels
        SurfaceHolder surfaceHolder = mSmartGLView.getHolder();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenW = size.x;
        screenH = size.y;
        surfaceHolder.setFixedSize(screenW / divider, screenH / divider);
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
        Texture txPlayer = createTexture(colOrange);

        // Create sprites
        trackLength = screenW * 0.8f;
        trackX = screenW * 0.1f;
        trackY = 8;
        trackMax = trackX + trackLength;
        int sprH = screenH / 48;
        sprTrack = new Sprite(Math.round(trackLength), sprH);
        sprTrack.setPivot(0, 0.5f);
        sprTrack.setTexture(txHoloBright);
        sprTrack.setPos(trackX, trackY);
        renderPassSprite.addSprite(sprTrack);

        sprites = new ArrayList<>();

        Texture txBackground = createTexture(Color.BLACK, Color.rgb(16, 16, 16), 256, 256);
        Sprite spriteBg = new Sprite(screenW, screenH);
        spriteBg.setTexture(txBackground);
        bgSprite.addSprite(spriteBg);

        // Create joystick
        Texture joyTexture = new Texture(this, R.drawable.gamepad);
        Texture knobTexture = new Texture(this, R.drawable.knob);
        textures.add(joyTexture);
        textures.add(knobTexture);

        joySize = screenH / 3;
        halfJoySize = joySize / 2;

        joyCoefficient /= halfJoySize;
        joyBaseSprite = new Sprite(joySize, joySize);
        joyBaseSprite.setPivot(0.5f, 0.5f);
        joyBaseSprite.setTexture(joyTexture);
        joyBaseSprite.setVisible(joyVisible);

        joyKnobSprite = new Sprite(joySize / 2, joySize / 2);
        joyKnobSprite.setPivot(0.5f, 0.5f);
        joyKnobSprite.setTexture(knobTexture);
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
        for (int i = 0; i < level; i++) {
            Object3D opponent = createObject(R.raw.cube, txRed, false);
            opponent.setScale(2, 1, 1);
            opponent.setVisible(true);
            opponents.add(opponent);

            Sprite sprite = new Sprite(sprH, sprH);
            sprite.setPivot(0.5f, 0.5f);
            sprite.setTexture(txRed);
            renderPassSprite.addSprite(sprite);
            sprites.add(sprite);
        }

        // Create player
        player = createObject(R.raw.bulldozer, txPlayer, false);
        player.setScale(0.001f, 0.001f, 0.001f);
        sprPlayer = new Sprite(sprH, sprH);
        sprPlayer.setPivot(0.5f, 0.5f);
        sprPlayer.setTexture(txPlayer);
        renderPassSprite.addSprite(sprPlayer);

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
    public void onReleaseView(final SmartGLView smartGLView) {
        if (textures != null)
            for (Texture t : textures) {
                if (t != null) t.release();
            }

        if (smartGLView != null) {
            smartGLView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    smartGLView.destroyDrawingCache();
                }
            });
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
//                soundPool.play(sfxExplosion, 1, 1, 0, 0, 1);
                playSound(sfxExplosion);
                break;
            }
        }
    }

    private void update(float delta, OpenGLCamera camera) {

        if (player.getPosX() < finishDistance) {
            float playerRotX = player.getRotX();
            float playerRotY = player.getRotY();
            float playerRotZ = player.getRotZ();

            playerPosX = player.getPosX();
            playerPosY = player.getPosY();
            playerPosZ = player.getPosZ();

            float camRotX, camRotY, camRotZ, camPosX, camPosY, camPosZ;

            // Update environment
            updateEnvironment();

            // Check collisions
            for (int i = 0; i < bots.size(); i++) {
                Object3D object3D = bots.get(i);
                if (object3D.isVisible()) {
                    float ox = object3D.getPosX();
                    float oy = object3D.getPosY();
                    float oz = object3D.getPosZ();
                    float distance = getDistance(ox, oz, playerPosX, playerPosZ);

                    if (distance < 1.5f) {
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

            updateOpponents();

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
            switch (currentDirection) {
                case ANALOG:
                    if (playerRotY > 134 && playerRotY < 226) {
//                        k = -joyDelta * 3;
                        float al = 180 - joyDelta * joyCoefficient;
                        player.setRotation(playerRotX, al, playerRotZ);
                    }
                    break;
                case STRAIGHT:
                    float k;
                    if (playerRotY < 177) {
                        k = 200;
                    } else if (playerRotY > 183) {
                        k = -200;
                    } else {
                        k = 0;
                        player.setRotation(playerRotX, 180, playerRotZ);
                    }
                    player.addRotY(k * delta);
            }


            // Camera
            camRotX = camRotXgame;
            camRotY = camRotYgame;
            camRotZ = camRotZgame;

            camPosX = camXgame;
            camPosY = camYgame;
            camPosZ = camZgame;
            // Update camera
            float alpha = (float) Math.sin(playerPosX * 0.1f);
            if (shake > 0) shake -= 5 * delta;
            float cx = playerPosX + camPosX + shake + alpha;
            float cy = playerPosY + camPosY + shake;
            float cz = playerPosZ + camPosZ + shake + alpha;

            camera.setPosition(cx, cy, cz);
            camera.setRotation(alpha - camRotX, alpha * 2 + camRotY, camRotZ);
        } else {
//            resetGame();
            if (sfxFinishPlay) {
                playSound(sfxFinished);
                sfxFinishPlay = false;
                if (place == 1) {
                    level++;
                    saveData();
                }
            }
            speed = 0;
            gameover = true;
            float dy = 0.05f;
            flyaway(dy);
            float camX = camera.getPosX();

            player.addRotY(dy * 100);

            if (camX > playerPosX) {
                camX -= dy;
                float camY = camera.getPosY();
                float camZ = camera.getPosZ();
                camera.setPosition(camX, camY - dy / 2, camZ - dy / 2);
            }
        }

        // Update explosions
        if (!explosions.isEmpty()) {
            for (int i = 0; i < explosions.size(); i++) {
                explosions.get(i).update();
            }
        }
        // Update UI
        handler.post(runnable);
        updateSprites();
    }

    private void updateSprites() {
        float x;
        float y;
        for (int i = 0; i < opponents.size(); i++) {
            x = getUIX(opponents.get(i).getPosX());
            y = trackY;
            if (x < trackMax) sprites.get(i).setPos(x, y);
        }

        x = getUIX(playerPosX);
        y = trackY;
        sprPlayer.setPos(x, y);
    }

    private float getUIX(float x) {
        return trackX + x / finishDistance * trackLength;
    }

    private void flyaway(float dy) {
        flyaway(bots, dy);
        flyaway(opponents, dy);
        flyaway(road, -dy);
        flyaway(lighters, dy);
        flyaway(downtown, dy);
        flyaway(uptown, -dy);
    }

    private void flyaway(ArrayList<Object3D> object3Ds, float dy) {
        for (int i = 0; i < object3Ds.size(); i++) {
            Object3D object3D = object3Ds.get(i);
            float ox = object3D.getPosX();
            float oy = object3D.getPosY();
            float oz = object3D.getPosZ();

            object3D.setPos(ox, oy + dy, oz);
        }
        float bridgeY = goBridge.getY();
        goBridge.setY(bridgeY + dy);

        float sideUX = sideRoadUp.getPosX();
        float sideUY = sideRoadUp.getPosY();
        float sideUZ = sideRoadUp.getPosZ();

        sideRoadUp.setPos(sideUX, sideUY + dy, sideUZ);

        sideUX = sideRoadDown.getPosX();
        sideUY = sideRoadDown.getPosY();
        sideUZ = sideRoadDown.getPosZ();

        sideRoadDown.setPos(sideUX, sideUY + dy, sideUZ);

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

    private void updateOpponents() {
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
                    playSound(sfxHit);
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
                float roz = object3D.getRotZ();

//                object3D.addRotY((float) (90 - Math.toDegrees(Math.asin(sinAlpha))));
                object3D.setRotation(rox, alpha / 2 - 180, roz);


            }

            ox += speedOpponents;
            object3D.setPos(ox, oy, oz);
        }
    }

    private void playSound(int id) {

        if (id == sfxHit) {
            long time = System.currentTimeMillis() - sfxHitTime;
            if (time > 200) {
                sfxHitTime = System.currentTimeMillis();
                Assets.instance().soundPool.play(id, 0.5f, 0.5f, 1, 0, 1);
            }
        } else {
            Assets.instance().soundPool.stop(id);
            Assets.instance().soundPool.play(id, 0.5f, 0.5f, 1, 0, 1);
        }
    }

    private void updateEnvironment() {

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
        if (!gameover) {
            switch (type) {
                case SINGLETOUCH:
                    joyVisible = true;
                    joyX = touchHelperEvent.getX(0);
                    joyY = touchHelperEvent.getY(0);
                    break;
                case SINGLEMOVE:
                    float y = touchHelperEvent.getY(0) - joyY;
                    if (Math.abs(y) < halfJoySize) joyDelta = y;
                    currentDirection = DIRECTION.ANALOG;
                    break;
                case SINGLEUNTOUCH:
                    joyVisible = false;
                    joyDelta = 0;
                    currentDirection = DIRECTION.STRAIGHT;
                    break;
            }

        } else {
            joyVisible = false;
            if (type == TouchHelperEvent.TouchEventType.TAPPING) this.finish();
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
