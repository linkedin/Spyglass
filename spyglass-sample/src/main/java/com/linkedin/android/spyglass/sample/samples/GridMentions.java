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

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.linkedin.android.spyglass.mentions.MentionSpanConfig;
import com.linkedin.android.spyglass.sample.R;
import com.linkedin.android.spyglass.sample.data.models.Person;
import com.linkedin.android.spyglass.suggestions.SuggestionsResult;
import com.linkedin.android.spyglass.suggestions.interfaces.Suggestible;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsResultListener;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsVisibilityManager;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizer;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizerConfig;
import com.linkedin.android.spyglass.tokenization.interfaces.QueryTokenReceiver;
import com.linkedin.android.spyglass.ui.MentionsEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demonstrates how mentions are handled when there is a delay retrieving the suggestions (i.e. over a network).
 */
public class GridMentions extends AppCompatActivity implements QueryTokenReceiver, SuggestionsResultListener, SuggestionsVisibilityManager {

    private static final String BUCKET = "people-network";
    private static final WordTokenizerConfig tokenizerConfig = new WordTokenizerConfig
            .Builder()
            .setWordBreakChars(", ")
            .setExplicitChars("")
            .setMaxNumKeywords(2)
            .setThreshold(1)
            .build();

    private RecyclerView recyclerView;
    private MentionsEditText editor;
    private PersonMentionAdapter adapter;
    private Person.PersonLoader people;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grid_mentions);

        recyclerView = findViewById(R.id.mentions_grid);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new PersonMentionAdapter(new ArrayList<Person>());
        recyclerView.setAdapter(adapter);

        editor = findViewById(R.id.editor);
        editor.setTokenizer(new WordTokenizer(tokenizerConfig));
        editor.setQueryTokenReceiver(this);
        editor.setSuggestionsVisibilityManager(this);
        editor.setHint(getResources().getString(R.string.type_person));
        MentionSpanConfig mentionSpanConfig = new MentionSpanConfig.Builder()
            .setMentionTextStyle(Typeface.BOLD)
            .setSelectedMentionTextStyle(Typeface.ITALIC)
            .build();
        editor.setMentionSpanConfig(mentionSpanConfig);

        people = new Person.PersonLoader(getResources());
    }

    // --------------------------------------------------
    // QueryTokenReceiver Implementation
    // --------------------------------------------------

    @Override
    public List<String> onQueryReceived(final @NonNull QueryToken queryToken) {
        List<String> buckets = Collections.singletonList(BUCKET);
        List<Person> suggestions = people.getSuggestions(queryToken);
        SuggestionsResult result = new SuggestionsResult(queryToken, suggestions);
        // Have suggestions, now call the listener (which is this activity)
        onReceiveSuggestionsResult(result, BUCKET);
        return buckets;
    }

    // --------------------------------------------------
    // SuggestionsResultListener Implementation
    // --------------------------------------------------

    @Override
    public void onReceiveSuggestionsResult(@NonNull SuggestionsResult result, @NonNull String bucket) {
        List<? extends Suggestible> suggestions = result.getSuggestions();
        adapter = new PersonMentionAdapter(result.getSuggestions());
        recyclerView.swapAdapter(adapter, true);
        boolean display = suggestions != null && suggestions.size() > 0;
        displaySuggestions(display);
    }

    // --------------------------------------------------
    // SuggestionsManager Implementation
    // --------------------------------------------------

    @Override
    public void displaySuggestions(boolean display) {
        if (display) {
            recyclerView.setVisibility(RecyclerView.VISIBLE);
        } else {
            recyclerView.setVisibility(RecyclerView.GONE);
        }
    }

    @Override
    public boolean isDisplayingSuggestions() {
        return recyclerView.getVisibility() == RecyclerView.VISIBLE;
    }

    // --------------------------------------------------
    // PersonMentionAdapter Class
    // --------------------------------------------------

    private static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public ImageView picture;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.person_name);
            picture = itemView.findViewById(R.id.person_image);
        }
    }

    private class PersonMentionAdapter extends RecyclerView.Adapter<ViewHolder> {

        private List<? extends Suggestible> suggestions;

        public PersonMentionAdapter(List<? extends Suggestible> people) {
            suggestions = people;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.grid_mention_item, viewGroup, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            Suggestible suggestion = suggestions.get(i);
            if (!(suggestion instanceof Person)) {
                return;
            }

            final Person person = (Person) suggestion;
            viewHolder.name.setText(person.getFullName());
            Glide.with(viewHolder.picture.getContext())
                    .load(person.getPictureURL())
                    .crossFade()
                    .into(viewHolder.picture);

            viewHolder.itemView.setOnClickListener(v -> {
                editor.insertMention(person);
                recyclerView.swapAdapter(new PersonMentionAdapter(new ArrayList<Person>()), true);
                displaySuggestions(false);
                editor.requestFocus();
            });
        }

        @Override
        public int getItemCount() {
            return suggestions.size();
        }
    }

}