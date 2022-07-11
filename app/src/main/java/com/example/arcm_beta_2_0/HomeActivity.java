package com.example.arcm_beta_2_0;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.facebook.stetho.Stetho;


public class HomeActivity extends AppCompatActivity {
    ImageView imageView2;
    LinearLayout linearLayout;
    Animation frombottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Stetho.initializeWithDefaults(this);

        linearLayout = findViewById(R.id.linearLayout);
        imageView2 = findViewById(R.id.imageView2);
        frombottom = AnimationUtils.loadAnimation(this,R.anim.frombottom);

        imageView2.animate().translationY(-900).setDuration(500).setStartDelay(500);
        linearLayout.animate().alpha(50).setDuration(1600).setStartDelay(300);
        linearLayout.startAnimation(frombottom);
        imageView2.startAnimation(frombottom);
    }

    public void goMainActivity(View view) {
        startActivity(new Intent(HomeActivity.this, MainActivity.class));
    }

    public void goHistoryActivity(View view) {
        startActivity(new Intent(HomeActivity.this, HistoryActivity.class));
    }

}
