package org.lunapark.dev.bullramp;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by znak on 08.02.2017.
 */
class Assets {

    private static Assets ourInstance = new Assets();
    private final String DATA_LEVEL = "level";
    int sfxExplosion;
    int sfxHit;
    int sfxFinished;
    int sfxFail;
    private SoundPool soundPool;

    private Context context;
    private long sfxHitTime = 0;
    private SharedPreferences preferences;

    private Assets() {
    }

    static Assets instance() {
        return ourInstance;
    }

    void load(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(DATA_LEVEL, MODE_PRIVATE);
        // Prepare sound
        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

            }
        });

        sfxExplosion = soundPool.load(context, R.raw.explosion, 1);
        sfxHit = soundPool.load(context, R.raw.hit, 1);
        sfxFinished = soundPool.load(context, R.raw.finishrace, 1);
        sfxFail = soundPool.load(context, R.raw.smex, 1);
    }

    void playSoundStereo(int id, float x, float playerPosX, float farLength) {

        float volumeRight, volumeLeft;
        float delta = x - playerPosX;
        if (Math.abs(delta) < 1) {
            volumeRight = 0.5f;
        } else {
            volumeRight = 0.5f + delta / farLength;
        }
        volumeLeft = 1 - volumeRight;
        soundPool.stop(id);
        soundPool.play(id, volumeLeft, volumeRight, 1, 0, 1);

    }

    void playSoundMono(int id) {
        if (id == sfxHit) {
            long time = System.currentTimeMillis() - sfxHitTime;
            if (time > 500) {
                sfxHitTime = System.currentTimeMillis();
                soundPool.play(id, 0.5f, 0.5f, 1, 0, 1);
            }
        } else {
            soundPool.stop(id);
            soundPool.play(id, 0.5f, 0.5f, 1, 0, 1);
        }
    }

    int getLevel() {
        return preferences.getInt(DATA_LEVEL, 1);
    }

    void saveLevel(int level) {


        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(DATA_LEVEL, level);
        editor.apply();
    }

    void dispose() {
        soundPool.release();
    }

}
