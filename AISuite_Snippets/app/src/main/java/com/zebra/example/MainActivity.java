// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.example;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private final String TAG="MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        TextView javaEntryPoint = findViewById(R.id.java_entry_point);
        TextView kotlinEntryPoint = findViewById(R.id.kotlin_entry_point);

        javaEntryPoint.setOnClickListener(v->{
            Log.d(TAG, "java is clicked");
        });
        kotlinEntryPoint.setOnClickListener(v->{
            Log.d(TAG, "kotlin is clicked");
        });
    }
}