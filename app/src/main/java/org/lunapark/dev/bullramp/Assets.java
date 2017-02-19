package org.lunapark.dev.bullramp;

import android.content.SharedPreferences;

import static org.lunapark.dev.bullramp.Const.DATA_LEVEL;


/**
 * Created by znak on 08.02.2017.
 */
class Assets {

    private static Assets ourInstance = new Assets();


    private SharedPreferences preferences;


    private Assets() {
    }

    static Assets instance() {
        return ourInstance;
    }

    void load(SharedPreferences preferences) {
        this.preferences = preferences;
    }


    int getLevel() {
        return preferences.getInt(DATA_LEVEL, 0);
    }

    void saveLevel(int level) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(DATA_LEVEL, level);
        editor.apply();
    }

    void dispose() {
    }

}
