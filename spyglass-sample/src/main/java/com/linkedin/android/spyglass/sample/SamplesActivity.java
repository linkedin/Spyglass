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
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;

import com.linkedin.android.spyglass.sample.samples.GridMentions;
import com.linkedin.android.spyglass.sample.samples.MultiSourceMentions;
import com.linkedin.android.spyglass.sample.samples.NetworkedMentions;
import com.linkedin.android.spyglass.sample.samples.SimpleMentions;

/**
 * Main samples activity containing buttons to launch all the different samples.
 */
public class SamplesActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.samples);

        Button simpleSample = (Button) findViewById(R.id.simple_sample);
        simpleSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SamplesActivity.this, SimpleMentions.class));
            }
        });

        Button networkedSample = (Button) findViewById(R.id.networked_sample);
        networkedSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SamplesActivity.this, NetworkedMentions.class));
            }
        });

        Button multiSourceSample = (Button) findViewById(R.id.multi_source_sample);
        multiSourceSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SamplesActivity.this, MultiSourceMentions.class));
            }
        });

        Button gridSample = (Button) findViewById(R.id.grid_sample);
        gridSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SamplesActivity.this, GridMentions.class));
            }
        });
    }
}
