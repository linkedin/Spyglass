/*
* Copyright 2015 LinkedIn Corp. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/

package com.linkedin.android.spyglass.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.linkedin.android.spyglass.sample.samples.ColorfulMentions;
import com.linkedin.android.spyglass.sample.samples.GridMentions;
import com.linkedin.android.spyglass.sample.samples.MultiSourceMentions;
import com.linkedin.android.spyglass.sample.samples.NetworkedMentions;
import com.linkedin.android.spyglass.sample.samples.SimpleMentions;

/**
 * Main samples activity containing buttons to launch all the different samples.
 */
public class SamplesActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.samples);

        findViewById(R.id.simple_sample)
            .setOnClickListener(v -> startActivity(new Intent(SamplesActivity.this, SimpleMentions.class)));

        findViewById(R.id.colorful_sample)
            .setOnClickListener(v -> startActivity(new Intent(SamplesActivity.this, ColorfulMentions.class)));

        findViewById(R.id.networked_sample)
            .setOnClickListener(v -> startActivity(new Intent(SamplesActivity.this, NetworkedMentions.class)));

        findViewById(R.id.multi_source_sample)
            .setOnClickListener(v -> startActivity(new Intent(SamplesActivity.this, MultiSourceMentions.class)));

        findViewById(R.id.grid_sample)
            .setOnClickListener(v -> startActivity(new Intent(SamplesActivity.this, GridMentions.class)));
    }
}
