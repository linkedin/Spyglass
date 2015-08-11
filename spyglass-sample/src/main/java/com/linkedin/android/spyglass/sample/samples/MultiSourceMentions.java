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

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.linkedin.android.spyglass.sample.R;
import com.linkedin.android.spyglass.sample.data.models.City;
import com.linkedin.android.spyglass.sample.data.models.Person;
import com.linkedin.android.spyglass.suggestions.SuggestionsResult;
import com.linkedin.android.spyglass.suggestions.impl.BasicSuggestionsListBuilder;
import com.linkedin.android.spyglass.suggestions.interfaces.Suggestible;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsResultListener;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import com.linkedin.android.spyglass.tokenization.interfaces.QueryTokenReceiver;
import com.linkedin.android.spyglass.ui.RichEditorView;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates how mentions are handled when there is a delay retrieving the suggestions (i.e. over a network).
 */
public class MultiSourceMentions extends ActionBarActivity implements QueryTokenReceiver {

    private static final String PERSON_BUCKET = "people-database";
    private static final String CITY_BUCKET = "city-network";
    private static final int PERSON_DELAY = 10;
    private static final int CITY_DELAY = 2000;

    private RichEditorView editor;
    private CheckBox peopleCheckBox;
    private CheckBox citiesCheckBox;

    private Person.PersonLoader people;
    private City.CityLoader cities;

    private SuggestionsResult lastPersonSuggestions;
    private SuggestionsResult lastCitySuggestions;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multi_source_mentions);

        editor = (RichEditorView) findViewById(R.id.editor);
        editor.setQueryTokenReceiver(this);
        editor.setSuggestionsListBuilder(new CustomSuggestionsListBuilder());

        people = new Person.PersonLoader(getResources());
        cities = new City.CityLoader(getResources());

        peopleCheckBox = (CheckBox) findViewById(R.id.person_checkbox);
        peopleCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateSuggestions();
            }
        });

        citiesCheckBox = (CheckBox) findViewById(R.id.city_checkbox);
        citiesCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateSuggestions();
            }
        });

        updateSuggestions();
    }

    private void updateSuggestions() {
        final boolean hasPeople = peopleCheckBox.isChecked();
        final boolean hasCities = citiesCheckBox.isChecked();

        // Handle person mentions
        if (hasPeople && lastPersonSuggestions != null) {
            editor.onReceiveSuggestionsResult(lastPersonSuggestions, PERSON_BUCKET);
        } else if(lastPersonSuggestions != null) {
            SuggestionsResult emptySuggestions = new SuggestionsResult(lastPersonSuggestions.getQueryToken(), new ArrayList<Person>());
            editor.onReceiveSuggestionsResult(emptySuggestions, PERSON_BUCKET);
        }

        // Handle city mentions
        if (hasCities && lastCitySuggestions != null) {
            editor.onReceiveSuggestionsResult(lastCitySuggestions, CITY_BUCKET);
        } else if(lastCitySuggestions != null) {
            SuggestionsResult emptySuggestions = new SuggestionsResult(lastCitySuggestions.getQueryToken(), new ArrayList<City>());
            editor.onReceiveSuggestionsResult(emptySuggestions, CITY_BUCKET);
        }
        
        // Update the hint
        if (hasPeople && hasCities) {
            editor.setHint(getResources().getString(R.string.type_both));
        } else if (hasPeople && !hasCities) {
            editor.setHint(getResources().getString(R.string.type_person));
        } else if (!hasPeople && hasCities) {
            editor.setHint(getResources().getString(R.string.type_city));
        } else {
            editor.setHint(getResources().getString(R.string.type_neither));
        }
    }

    // --------------------------------------------------
    // QueryTokenReceiver Implementation
    // --------------------------------------------------

    @Override
    public List<String> onQueryReceived(final @NonNull QueryToken queryToken) {
        final boolean hasPeople = peopleCheckBox.isChecked();
        final boolean hasCities = citiesCheckBox.isChecked();

        final List<String> buckets = new ArrayList<>();
        final SuggestionsResultListener listener = editor;
        final Handler handler = new Handler(Looper.getMainLooper());

        // Fetch people if necessary
        if (hasPeople) {
            buckets.add(PERSON_BUCKET);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<Person> suggestions = people.getSuggestions(queryToken);
                    lastPersonSuggestions = new SuggestionsResult(queryToken, suggestions);
                    listener.onReceiveSuggestionsResult(lastPersonSuggestions, PERSON_BUCKET);
                }
            }, PERSON_DELAY);
        }

        // Fetch cities if necessary
        if (hasCities) {
            buckets.add(CITY_BUCKET);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<City> suggestions = cities.getSuggestions(queryToken);
                    lastCitySuggestions = new SuggestionsResult(queryToken, suggestions);
                    listener.onReceiveSuggestionsResult(lastCitySuggestions, CITY_BUCKET);
                }
            }, CITY_DELAY);
        }

        // Return buckets, one for each source (serves as promise to editor that we will call
        // onReceiveSuggestionsResult at a later time)
        return buckets;
    }

    // --------------------------------------------------
    // Inner class to customize appearance of suggestions
    // --------------------------------------------------

    private class CustomSuggestionsListBuilder extends BasicSuggestionsListBuilder {

        @NonNull
        @Override
        public View getView(@NonNull Suggestible suggestion,
                            @Nullable View convertView,
                            ViewGroup parent,
                            @NonNull Context context,
                            @NonNull LayoutInflater inflater,
                            @NonNull Resources resources) {

            View v =  super.getView(suggestion, convertView, parent, context, inflater, resources);
            if (!(v instanceof TextView)) {
                return v;
            }

            // Color text depending on the type of mention
            TextView tv = (TextView) v;
            if (suggestion instanceof Person) {
                tv.setTextColor(getResources().getColor(R.color.person_mention_text));
            } else if (suggestion instanceof City) {
                tv.setTextColor(getResources().getColor(R.color.city_mention_text));
            }

            return tv;
        }
    }
}