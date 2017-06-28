package com.example.gy.easiget;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

public class NavgateActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navgate);
        Handler h = new Handler();

        h.postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(NavgateActivity.this, GetpageActivity.class);
                startActivity(intent);
                //Toast.makeText(NavgateActivity.this,"launched",Toast.LENGTH_SHORT).show();
                finish();
            }
        }, 1666);
    }
}
