package org.lunapark.dev.bullramp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static org.lunapark.dev.bullramp.Const.DATA_LEVEL;
import static org.lunapark.dev.bullramp.Const.MAX_ENEMIES;

public class MainActivity extends Activity implements View.OnClickListener {

    private String[] title;
    private TextView tvTitle, tvLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Button btnStart = (Button) findViewById(R.id.btnStart);
        Button btnQuit = (Button) findViewById(R.id.btnQuit);
        btnStart.setOnClickListener(this);
        btnQuit.setOnClickListener(this);

        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvLevel = (TextView) findViewById(R.id.tvLevel);

        SharedPreferences preferences = getSharedPreferences(DATA_LEVEL, MODE_PRIVATE);
        Assets.instance().load(preferences);
        title = getResources().getStringArray(R.array.title_anim);

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            int frame = 0;

            @Override
            public void run() {
                tvTitle.setText(title[frame]);
                if (frame < title.length - 1) {
                    frame++;
                } else {
                    frame = 0;
                }
                // update textView here
                handler.postDelayed(this, 150); // set time here to refresh textView
            }
        };

        handler.post(runnable);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                Intent intent = new Intent(this, GameActivity.class);
                startActivity(intent);
                break;
            case R.id.btnQuit:
                System.exit(0);
                break;
        }
    }


    @Override
    protected void onDestroy() {
        Assets.instance().dispose();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int level = Assets.instance().getLevel();
        int levelMajor = level / MAX_ENEMIES + 1;
        int levelMinor = level % MAX_ENEMIES + 1;
        tvLevel.setText(String.format(getString(R.string.txt_level), levelMajor, levelMinor));
    }
}