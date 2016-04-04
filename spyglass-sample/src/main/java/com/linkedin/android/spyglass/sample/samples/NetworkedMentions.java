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

package com.linkedin.android.spyglass.sample.samples;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;
import android.widget.TextView;

import com.linkedin.android.spyglass.sample.R;
import com.linkedin.android.spyglass.sample.data.models.City;
import com.linkedin.android.spyglass.suggestions.SuggestionsResult;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsResultListener;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import com.linkedin.android.spyglass.tokenization.interfaces.QueryTokenReceiver;
import com.linkedin.android.spyglass.ui.RichEditorView;

import java.util.Arrays;
import java.util.List;

/**
 * Demonstrates how mentions are handled when there is a delay retrieving the suggestions (i.e. over a network).
 */
public class NetworkedMentions extends AppCompatActivity implements QueryTokenReceiver {

    private static final String BUCKET = "people-network";

    private TextView delayLabel;
    private SeekBar delaySeek;
    private RichEditorView editor;

    private City.CityLoader cities;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.networked_mentions);
        delayLabel = (TextView) findViewById(R.id.label_network_delay);
        delaySeek = (SeekBar) findViewById(R.id.seek_network_delay);
        editor = (RichEditorView) findViewById(R.id.editor);

        // Update network delay text
        delayLabel.setText("Mock Network Delay: 2.0 seconds");
        delaySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            double progressValue = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue = ((float) progress) / 10.0;
                delayLabel.setText("Mock Network Delay: " + String.valueOf(progressValue) + " seconds");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });

        editor.setQueryTokenReceiver(this);
        editor.setHint(getResources().getString(R.string.type_city));
        cities = new City.CityLoader(getResources());
    }

    @Override
    public List<String> onQueryReceived(final @NonNull QueryToken queryToken) {
        List<String> buckets = Arrays.asList(BUCKET);

        final SuggestionsResultListener listener = editor;
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                List<City> suggestions = cities.getSuggestions(queryToken);
                SuggestionsResult result = new SuggestionsResult(queryToken, suggestions);
                listener.onReceiveSuggestionsResult(result, BUCKET);
            }
        }, getDelayValue());

        return buckets;
    }

    private int getDelayValue() {
        return delaySeek.getProgress() * 100;
    }
}