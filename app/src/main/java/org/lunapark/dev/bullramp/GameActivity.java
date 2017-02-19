package org.lunapark.dev.bullramp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
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

import static org.lunapark.dev.bullramp.Const.BONUS_CHANCE;
import static org.lunapark.dev.bullramp.Const.BRAKE;
import static org.lunapark.dev.bullramp.Const.CAMERA_SHAKE_DISTANCE;
import static org.lunapark.dev.bullramp.Const.CAM_ROT_X;
import static org.lunapark.dev.bullramp.Const.CAM_ROT_Z;
import static org.lunapark.dev.bullramp.Const.CAM_X_1;
import static org.lunapark.dev.bullramp.Const.CAM_X_2;
import static org.lunapark.dev.bullramp.Const.CAM_Y;
import static org.lunapark.dev.bullramp.Const.CAM_Z;
import static org.lunapark.dev.bullramp.Const.DELTA_Z;
import static org.lunapark.dev.bullramp.Const.FAR_LENGTH;
import static org.lunapark.dev.bullramp.Const.HALF_FAR_LENGTH;
import static org.lunapark.dev.bullramp.Const.LIGHTER_HEIGHT;
import static org.lunapark.dev.bullramp.Const.LIGHTER_Y;
import static org.lunapark.dev.bullramp.Const.LIGHT_SEGMENTS;
import static org.lunapark.dev.bullramp.Const.LIGHT_SEGMENT_LENGTH;
import static org.lunapark.dev.bullramp.Const.MATH_2PI;
import static org.lunapark.dev.bullramp.Const.MATH_TO_DEG;
import static org.lunapark.dev.bullramp.Const.MATH_TO_RAD;
import static org.lunapark.dev.bullramp.Const.MAX_ENEMIES;
import static org.lunapark.dev.bullramp.Const.NUM_BOTS;
import static org.lunapark.dev.bullramp.Const.ROAD_LIMIT;
import static org.lunapark.dev.bullramp.Const.ROAD_SEGMENTS;
import static org.lunapark.dev.bullramp.Const.ROAD_SEGMENT_LENGTH;
import static org.lunapark.dev.bullramp.Const.ROAD_Y;
import static org.lunapark.dev.bullramp.Const.SCALE_SIDE_ROAD;
import static org.lunapark.dev.bullramp.Const.SIDE_ROAD_X;

public class GameActivity extends Activity implements SmartGLViewController {

    private Random random;

    private float velocity = 0.02f; // 0.0002f;
    private float speedBase = 0.3f;
    private float speedLimit, speedOpponents, speedBots;
    private boolean bonusActive = false;

    private float camX = CAM_X_2;
    private SoundPool soundPool;
    private long sfxHitTime = 0, bonusTimer;

    private enum DIRECTION {STRAIGHT, ANALOG}

    private DIRECTION currentDirection = DIRECTION.STRAIGHT;
    private boolean joyVisible;
    private float joyDelta;
    private int joySize, halfJoySize;
    private Sprite joyBaseSprite, joyKnobSprite;
    private float joyX, joyY;
    // Player and bots params
    private float playerPosX, playerPosY, playerPosZ;
    private float joyCoefficient = 45; // Degrees of player's rotation
    private float speed;
    private int level;// = 7;
    private int place;// = level + 1;
    private int totalPlayers;// = place;
    private int finishDistance;// = (level + 1) * 400; // 500
    private int enemies;
    private boolean sfxFinishPlay = true;
    // Camera settings
    private float trackLength, trackX, trackY, trackMax; // for UI
    private ArrayList<Sprite> sprites;
    private boolean gameover = false;
    private SmartGLView mSmartGLView;
    private Handler handler;
    private Runnable runnable;
    private TextView tvResult, tvTip;
    private Sprite sprPlayer, sprTrack;
    private Object3D player, sideRoadUp, sideRoadDown, bonusShield, bonus;
    private RenderPassObject3D renderPassObject3D;
    private RenderPassSprite renderPassSprite, bgSprite;
    private ArrayList<Texture> textures;
    private ArrayList<Object3D> bots, opponents, road, lighters, uptown, downtown;
    private ArrayList<Explosion> explosions;
    private float shake;
    private boolean clox = true;
    private GameObject goBridge;
    private int screenW, screenH;
    // Colors
    private final int colHoloLight = Color.argb(128, 51, 181, 229);
    private final int colHoloDark = Color.argb(128, 0, 153, 204);
    private final int colYellow = Color.argb(128, 255, 255, 0);
    private final int colWhite = Color.argb(128, 255, 255, 255);
    private final int colHoloBright = Color.argb(128, 0, 221, 255);
    private final int colDkGray = Color.argb(128, 64, 64, 64);
    private final int colLtGray = Color.argb(128, 192, 192, 192);
    private final int colRed = Color.argb(128, 240, 0, 0);
    private final int colOrange = Color.argb(160, 255, 125, 11);
    private final int colGreen = Color.argb(100, 128, 255, 128);
    // Textures
    private Texture txHoloBright;
    private Texture txRoad;
    private Texture txDkGray;
    private Texture txExplosion;
    private Texture txRed;
    private Texture txLtGray;

    // Sounds
    private int sfxExplosion, sfxHit, sfxFinished, sfxFail, sfxPowerUp, sfxPowerDown;


    private void loadData() {
        level = Assets.instance().getLevel();
//        level  = 20;
        enemies = level % MAX_ENEMIES + 1;
        float deltaSpeed = level / MAX_ENEMIES;
        place = enemies + 1;
        totalPlayers = place;
        finishDistance = (enemies + 1) * 400; // 400

        speedBase += deltaSpeed / 20;
        velocity += deltaSpeed / 100;
        speedLimit = speedBase * 2;
        speedOpponents = speedBase * 1.2f;
        speedBots = speedBase * 20;

        Log.e("BULLRAMP", String.format("Speed base: %f", speedBase));
    }

    private void saveData() {
        Assets.instance().saveLevel(level);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set brightness
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = 0.9f;
        this.getWindow().setAttributes(layout);
        loadData();
        //
        mSmartGLView = (SmartGLView) findViewById(R.id.smartGLView);
        mSmartGLView.setDefaultRenderer(this);
        mSmartGLView.setController(this);

        tvResult = (TextView) findViewById(R.id.tvResult);
        tvResult.setVisibility(View.INVISIBLE);
        tvResult.setLayerType(View.LAYER_TYPE_SOFTWARE, tvResult.getPaint());

        tvTip = (TextView) findViewById(R.id.tvTip);
        tvTip.setVisibility(View.INVISIBLE);
        tvTip.setLayerType(View.LAYER_TYPE_SOFTWARE, tvTip.getPaint());

        // Prepare update UI
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (gameover) {
                    tvResult.setVisibility(View.VISIBLE);
                    tvTip.setVisibility(View.VISIBLE);
                    String result = String.format(getResources().getString(R.string.txt_result), place, totalPlayers);
                    tvResult.setText(result);
                    String tip;
                    if (place == 1) {
                        tip = getString(R.string.txt_levelup);
                    } else {
                        tip = getString(R.string.txt_tip);
                    }

                    tip += getString(R.string.txt_tap);
                    tvTip.setText(tip);

                }

            }
        };

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenW = size.x;
        screenH = size.y;

        // Prepare sound
        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

            }
        });
        loadSounds();
    }

    private void loadSounds() {
        sfxExplosion = soundPool.load(this, R.raw.explosion, 1);
        sfxFinished = soundPool.load(this, R.raw.finishrace, 1);
        sfxPowerDown = soundPool.load(this, R.raw.powerdown, 1);
        sfxPowerUp = soundPool.load(this, R.raw.powerup, 1);
        sfxFail = soundPool.load(this, R.raw.smex, 1);
        sfxHit = soundPool.load(this, R.raw.hit, 1);
    }

    private void releaseSounds() {
        soundPool.release();
    }

    private void playSoundStereo(int id, float x) {

        float volumeRight, volumeLeft;
        float delta = x - playerPosX;
        if (Math.abs(delta) < 1) {
            volumeRight = 0.5f;
        } else {
            volumeRight = 0.5f + delta / FAR_LENGTH;
        }
        volumeLeft = 1 - volumeRight;
        soundPool.stop(id);
        soundPool.play(id, volumeLeft, volumeRight, 1, 0, 1);

    }

    private void playSoundMono(int id) {
        if (id == sfxHit) {
            long time = System.currentTimeMillis() - sfxHitTime;
            if (time > 500) {
                sfxHitTime = System.currentTimeMillis();
                float pitch = random.nextFloat() / 2;
                soundPool.play(id, 0.5f, 0.5f, 1, 0, 0.75f + pitch);
                if (!bonusActive) speed *= BRAKE;
            }
        } else {
            soundPool.stop(id);
            soundPool.play(id, 0.5f, 0.5f, 1, 0, 1);
        }
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

        bgSprite = new RenderPassSprite();
        renderPassObject3D = new RenderPassObject3D(RenderPassObject3D.ShaderType.SHADER_TEXTURE, true, true);
        renderPassSprite = new RenderPassSprite();

        renderer.addRenderPass(bgSprite);
        renderer.addRenderPass(renderPassObject3D);  // add it only once for all 3D Objects
        renderer.addRenderPass(renderPassSprite);  // add it only once for all Sprites

        createEnvironment();
        resetGame();
    }

    private void resetGame() {
        player.setPos(0, -0.3f, 0);
        player.setRotation(0, 180, 0);
        speed = speedBase;

        for (int i = 0; i < opponents.size(); i++) {
            Object3D opponent = opponents.get(i);
            opponent.setPos(i * FAR_LENGTH + HALF_FAR_LENGTH / 2, -0.2f, 0);
        }

        for (int i = 0; i < bots.size(); i++) {
            Object3D bot = bots.get(i);
            bot.setVisible(false);
        }

        for (int i = 0; i < road.size(); i++) {
            Object3D ground = road.get(i);
            ground.setPos(ROAD_SEGMENT_LENGTH * i - HALF_FAR_LENGTH, ROAD_Y, ROAD_SEGMENT_LENGTH / 2 - DELTA_Z);
        }
        sideRoadDown.setPos(SIDE_ROAD_X, -0.5f, ROAD_SEGMENT_LENGTH * SCALE_SIDE_ROAD + ROAD_SEGMENT_LENGTH / 2 - DELTA_Z);
        sideRoadUp.setPos(SIDE_ROAD_X, -0.5f, -ROAD_SEGMENT_LENGTH / 2 - DELTA_Z);

        goBridge.setPosition(0, 0, -DELTA_Z);
    }

    private void createEnvironment() {
        // Textures
        textures = new ArrayList<>();
        txHoloBright = createTexture(colHoloBright);
        txExplosion = createTexture(Color.YELLOW, Color.rgb(255, 112, 16));
        txRoad = createTextureRoad();
        txDkGray = createTexture(Color.DKGRAY);
        txRed = createTexture(colRed);
        txLtGray = createTexture(colLtGray);
        Texture txPlayer = createTexture(colOrange);
        Texture txBonus = createTexture(colYellow);

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

        Texture txBackground = createBgTexture(Color.BLACK, Color.rgb(16, 16, 16), 256, 256);
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
        for (int i = 0; i < NUM_BOTS; i++) {
            Object3D bot = createObject(R.raw.truck, txHoloBright, true);
            bot.setScale(0.001f, 0.001f, 0.001f);
            bot.setVisible(false);
            bots.add(bot);
        }

        opponents = new ArrayList<>();
        for (int i = 0; i < enemies; i++) {
            Object3D opponent = createObject(R.raw.bulldozer, txRed, false);
//            opponent.setScale(2, 1, 1);
            opponent.setScale(0.001f, 0.001f, 0.001f);
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

        bonus = createObject(R.raw.cube, txExplosion, false);
        bonus.setPos(-FAR_LENGTH, 0, 0);
        bonusShield = createObject(R.raw.bulldozer, txRed, false);
        bonusShield.setScale(0.0011f, 0.0011f, 0.0011f);
//        bonusShield = createObject(R.raw.cube, txRed, false);
////        bonusShield.setScale(3, 1.5f, 2.5f);
//        bonusShield.setScale(3, 1.5f, 0.1f);
        bonusShield.setVisible(false);
        bonusShield.addRotY(180);

        // Create road
        road = new ArrayList<>();
        for (int i = 0; i < ROAD_SEGMENTS; i++) {
            Object3D ground = createObject(R.raw.plane, txRoad, false);
//            ground.setPos(ROAD_SEGMENT_LENGTH * i - HALF_FAR_LENGTH, ROAD_Y, ROAD_SEGMENT_LENGTH / 2);
            road.add(ground);
        }

        sideRoadDown = createObject(R.raw.plane, txDkGray, false);
        sideRoadUp = createObject(R.raw.plane, txDkGray, false);
        sideRoadDown.setScale(0.75f, 1, SCALE_SIDE_ROAD);
        sideRoadUp.setScale(0.75f, 1, SCALE_SIDE_ROAD);

        lighters = new ArrayList<>();
        for (int i = 0; i < LIGHT_SEGMENTS; i++) {
            float x = LIGHT_SEGMENT_LENGTH * i - HALF_FAR_LENGTH;
            Object3D lighter1 = createObject(R.raw.cube, txLtGray, false);
            lighter1.setPos(x, LIGHTER_Y, -ROAD_SEGMENT_LENGTH - DELTA_Z);
            lighter1.setScale(0.25f, LIGHTER_HEIGHT, 0.25f);

            Object3D lighter2 = createObject(R.raw.cube, txLtGray, false);
            lighter2.setPos(x, LIGHTER_Y, ROAD_SEGMENT_LENGTH - DELTA_Z);
            lighter2.setScale(0.25f, LIGHTER_HEIGHT, 0.25f);

            lighters.add(lighter1);
            lighters.add(lighter2);
        }

        ArrayList<Object3D> bridge = new ArrayList<>();
        Object3D bridge1 = createObject(R.raw.cube, txLtGray, false);
        bridge1.setPos(0, 1.5f, -50 - ROAD_SEGMENT_LENGTH);
        bridge1.setScale(10, 4, 100);
        bridge.add(bridge1);
        Object3D bridge2 = createObject(R.raw.cube, txLtGray, false);
        bridge2.setPos(0, 1.5f, 50 + ROAD_SEGMENT_LENGTH);
        bridge2.setScale(10, 4, 100);
        bridge.add(bridge2);

        Object3D bridge3 = createObject(R.raw.cube, txLtGray, false);
        bridge3.setPos(0, 4.0f, 0);
        bridge3.setScale(10, 1, 100);
        bridge.add(bridge3);

        int houseCount = 3;

        float houseStep = HALF_FAR_LENGTH / houseCount;

        uptown = new ArrayList<>();
        downtown = new ArrayList<>();

        for (int i = 0; i < houseCount; i++) {
            Object3D uptown1 = createObject(R.raw.cube, txLtGray, false);
            uptown1.setPos(20 + i * houseStep, 1.5f, -(10 + ROAD_SEGMENT_LENGTH));
            uptown1.setScale(10, 4, 10);
            Object3D uptown2 = createObject(R.raw.cube, txLtGray, false);
            uptown2.setPos(-20 - i * houseStep, 1.5f, -(10 + ROAD_SEGMENT_LENGTH));
            uptown2.setScale(10, 4, 10);

            bridge.add(uptown1);
            bridge.add(uptown2);
            uptown.add(uptown1);
            uptown.add(uptown2);

            Object3D downtown1 = createObject(R.raw.cube, txLtGray, false);
            downtown1.setPos(20 + i * houseStep, 1.5f, 10 + ROAD_SEGMENT_LENGTH);
            downtown1.setScale(10, 4, 10);
            Object3D downtown2 = createObject(R.raw.cube, txLtGray, false);
            downtown2.setPos(-20 - i * houseStep, 1.5f, 10 + ROAD_SEGMENT_LENGTH);
            downtown2.setScale(10, 4, 10);

            bridge.add(downtown1);
            bridge.add(downtown2);
            downtown.add(downtown1);
            downtown.add(downtown2);
        }

        goBridge = new GameObject();
        goBridge.setObjects(bridge);

        // Prepare explosions
        explosions = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Explosion explosion = new Explosion(this, renderPassObject3D, R.raw.cube, txExplosion);
            explosions.add(explosion);
        }
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

    private Texture createBgTexture(int colorBg, int colorDetails, int w, int h) {
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
        int size = ROAD_SEGMENT_LENGTH * 4;
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

        releaseSounds();

        bots.clear();
        opponents.clear();
        road.clear();
        lighters.clear();
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
                playSoundStereo(sfxExplosion, x);
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

                        if (!bonusActive) {
                            speed *= BRAKE;
                            shake = CAMERA_SHAKE_DISTANCE;
                        }
                    }

                    if (ox < playerPosX - HALF_FAR_LENGTH) {
                        object3D.setVisible(false);
//                        object3D.setPos(ox + FAR_LENGTH * 2, oy, oz);
                    }

                    if (oz > 0) {
                        ox += speedBots * delta;
                        object3D.setRotation(0, 0, 0);
                    } else {
                        ox -= speedBots * delta;
                        object3D.setRotation(0, 180, 0);
                    }
                    object3D.setPos(ox, oy, oz);
                }
            }

            updateOpponents();

            // Move player
            float playerAngle = MATH_2PI - playerRotY * MATH_TO_RAD;
            if (speed < speedLimit) speed += velocity * delta;
            float z = (float) (-speed * Math.sin(playerAngle));
            if (Math.abs(playerPosZ + z) > ROAD_LIMIT) {
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

            // Bonus
            float bonusDistance = getDistance(bonus.getPosX(), bonus.getPosZ(), playerPosX, playerPosZ);
            if (bonus.isVisible()) {
                bonus.addRotX(200 * delta);
                bonus.addRotY(300 * delta);
                bonus.addRotZ(200 * delta);
            }

            if (bonusDistance < 1.7f && !bonusActive && bonus.isVisible()) {
                bonusActive = true;
                bonusTimer = System.currentTimeMillis();
                bonusShield.setVisible(true);
                bonus.setVisible(false);
                playSoundMono(sfxPowerUp);
                speed = speedLimit;
            }

            if (bonusActive) {
                if (System.currentTimeMillis() > bonusTimer + 10000) {
                    bonusActive = false;
                    bonusShield.setVisible(false);
                    playSoundMono(sfxPowerDown);
                }
//                bonusShield.setPos(playerPosX + x, playerPosY + 0.7f, playerPosZ + z);
                bonusShield.setPos(playerPosX + x + 0.1f, playerPosY - 0.1f, playerPosZ + z);
                bonusShield.setRotation(playerRotX, playerRotY, playerRotZ);
//                bonusShield.addRotY(1000 * delta);
            }

            // Camera
            if (clox) {
                if (camX > CAM_X_1) {
                    camX -= delta * 1;
                } else {
                    clox = false;
                }
            } else {
                if (camX < CAM_X_2) {
                    camX += delta * 1;

                } else {
                    clox = true;
                }
            }

            // Update camera
            if (shake > 0) shake -= 5 * delta;
            float cx = playerPosX + camX + shake + 8;// + alpha;
            float cy = playerPosY + CAM_Y + shake;
            float cz = playerPosZ + CAM_Z + shake;// + alpha;

            float beta = (float) (MATH_TO_DEG * (Math.atan(camX / CAM_Z)));

            camera.setPosition(cx, cy, cz);
            camera.setRotation(-CAM_ROT_X, beta, CAM_ROT_Z);
        } else {
//            resetGame();
            if (sfxFinishPlay) {
                int sfxId;

                sfxFinishPlay = false;
                if (place == 1) {
                    level++;
                    saveData();
                    sfxId = sfxFinished;
                } else {
                    sfxId = sfxFail;
                }

                playSoundMono(sfxId);
                bonus.setVisible(false);
                bonusShield.setVisible(false);
            }
            speed = 0;
            gameover = true;
            float dy = 0.05f;
            flyaway(dy);
            float cX = camera.getPosX();

            player.addRotY(dy * 100);

            if (cX > playerPosX) {
                cX -= dy;
                float cY = camera.getPosY();
                float cZ = camera.getPosZ();
                camera.setPosition(cX, cY, cZ);
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
        flyaway(bots, dy, 1);
        flyaway(opponents, dy, 1);
        flyaway(road, -dy, 5);
        flyaway(lighters, dy, 1);
        flyaway(downtown, dy, 1);
        flyaway(uptown, -dy, 1);
    }

    private void flyaway(ArrayList<Object3D> object3Ds, float dy, float mult) {
        for (int i = 0; i < object3Ds.size(); i++) {
            Object3D object3D = object3Ds.get(i);
            float ox = object3D.getPosX();
            float oy = object3D.getPosY();
            float oz = object3D.getPosZ();

            object3D.setPos(ox, oy + dy * mult, oz);
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

    // TODO Optimize
    private void updateBots(float x) {
        float dz1 = ROAD_SEGMENT_LENGTH / 2 - DELTA_Z * 2;
        for (int i = 0; i < bots.size(); i++) {
            Object3D object3D = bots.get(i);
            if (!object3D.isVisible()) {
//                float dx = random.nextInt(ROAD_SEGMENT_LENGTH / 8);
                float dz = random.nextFloat() * dz1 + DELTA_Z;
                if (random.nextBoolean()) dz *= -1;
//                object3D.setPos(playerPosX + HALF_FAR_LENGTH + ROAD_SEGMENT_LENGTH * i, -0.4f, dz);
                object3D.setPos(x, -0.4f, dz);
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
            float dx1 = playerPosX - HALF_FAR_LENGTH;
            float dx2 = playerPosX + HALF_FAR_LENGTH;
            if ((ox > dx1) && (ox < dx2)) {
                float distance = getDistance(ox, oz, playerPosX, playerPosZ);
                if (distance < 1.7f) {
                    playSoundMono(sfxHit);
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
                oz = sinAlpha * ROAD_SEGMENT_LENGTH / 2 + DELTA_Z;

                float alpha = (float) Math.toDegrees(Math.asin(sinAlpha));

                float rox = object3D.getRotX();
                float roz = object3D.getRotZ();
                object3D.setRotation(rox, alpha / 2 - 180, roz);
            }

            ox += speedOpponents;
            object3D.setPos(ox, oy, oz);
        }
    }

    private void updateEnvironment() {
        float farFromPlayer = playerPosX - FAR_LENGTH;
        float dx1 = playerPosX - HALF_FAR_LENGTH;
        float dx2 = playerPosX + HALF_FAR_LENGTH;

        for (int i = 0; i < road.size(); i++) {
            Object3D object3D = road.get(i);

            float x = object3D.getPosX();
//            float y = ROAD_Y;
            float z = object3D.getPosZ();

            if (x < dx1) {
                x += FAR_LENGTH;
                object3D.setPos(x, ROAD_Y, z);
                updateBots(x);
                break;
            }

            if (x > dx2) {
                x -= FAR_LENGTH;
                object3D.setPos(x, ROAD_Y, z);
                updateBots(x);
                break;
            }
        }
        for (int i = 0; i < lighters.size(); i++) {
            Object3D object3D = lighters.get(i);
            float x = object3D.getPosX();
            float z = object3D.getPosZ();

            if (x < dx1) {
                object3D.setPos(x + FAR_LENGTH, LIGHTER_Y, z);
            }

            if (x > dx2) {
                object3D.setPos(x - FAR_LENGTH, LIGHTER_Y, z);
            }
        }

        float bridgeX = goBridge.getX();
        if (bridgeX < farFromPlayer) {
            goBridge.setX(bridgeX + FAR_LENGTH * 2);
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
            sideRoadUp.setPos(sideRoadPosX + FAR_LENGTH * 2, sideRoadUpPosY, sideRoadUpPosZ);
            sideRoadDown.setPos(sideRoadPosX + FAR_LENGTH * 2, sideRoadDownPosY, sideRoadDownPosZ);
            sideRoadDown.setVisible(random.nextBoolean());
            sideRoadUp.setVisible(random.nextBoolean());
        }

        // Bonus
        if (bonus.getPosX() < farFromPlayer && !bonusActive) {
            bonus.setPos(dx2 + random.nextInt(ROAD_SEGMENT_LENGTH * 5), 0.5f, (random.nextInt(ROAD_SEGMENT_LENGTH * 2) - ROAD_SEGMENT_LENGTH) * 0.5f);
            bonus.setVisible(random.nextInt(10) > BONUS_CHANCE);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                joyDelta = -halfJoySize;
                currentDirection = DIRECTION.ANALOG;
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                joyDelta = halfJoySize;
                currentDirection = DIRECTION.ANALOG;
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        joyDelta = 0;
        currentDirection = DIRECTION.STRAIGHT;
        if (gameover || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE)
            this.finish();
        return super.onKeyUp(keyCode, event);
    }

}
