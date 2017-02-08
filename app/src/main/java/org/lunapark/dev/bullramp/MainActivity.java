package org.lunapark.dev.bullramp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Button btnStart = (Button) findViewById(R.id.btnStart);
        Button btnQuit = (Button) findViewById(R.id.btnQuit);
        btnStart.setOnClickListener(this);
        btnQuit.setOnClickListener(this);

        Assets.instance().load(this);
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
    protected void onStop() {
        Assets.instance().dispose();
        super.onDestroy();
    }
}