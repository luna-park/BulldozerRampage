package org.lunapark.dev.bullramp;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

/**
 * Created by znak on 08.02.2017.
 */
class Assets {

    private static Assets ourInstance = new Assets();
    SoundPool soundPool;
    int sfxExplosion;
    int sfxHit;
    int sfxFinished;

    private Assets() {
    }

    static Assets instance() {
        return ourInstance;
    }

    void load(Context context) {
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
    }

    void dispose() {
        soundPool.release();
    }

}
