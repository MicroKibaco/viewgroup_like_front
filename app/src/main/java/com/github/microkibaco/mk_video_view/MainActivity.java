package com.github.microkibaco.mk_video_view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        MkFloatLayout mkfloatLayout = findViewById(R.id.hot_list);

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        TextView hotWordTag = (TextView) layoutInflater.inflate(R.layout.view_search_word_tag, mkfloatLayout, false);
        hotWordTag.setText("你好");
        ImageView imgTag = (ImageView) layoutInflater.inflate(R.layout.view_image_word_tag, mkfloatLayout, false);
        mkfloatLayout.addView(hotWordTag);
        mkfloatLayout.addView(imgTag);
    }
}