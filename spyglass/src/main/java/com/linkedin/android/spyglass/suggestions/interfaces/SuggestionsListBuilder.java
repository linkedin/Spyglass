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

package com.linkedin.android.spyglass.suggestions.interfaces;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.linkedin.android.spyglass.suggestions.SuggestionsResult;

import java.util.List;
import java.util.Map;

/**
 * Interface that defines the list of suggestions to display and how to display them.
 */
public interface SuggestionsListBuilder {

    /**
     * Create the list of suggestions from the newest {@link SuggestionsResult} received for every bucket. This
     * allows you to control the exact order of the suggestions.
     *
     * @param latestResults      newest {@link SuggestionsResult} for every bucket
     * @param currentTokenString the most recent token, as typed by the user
     *
     * @return a list of {@link Suggestible} representing the suggestions in proper order
     */
    @NonNull
    public List<Suggestible> buildSuggestions(final @NonNull Map<String, SuggestionsResult> latestResults,
                                              final @NonNull String currentTokenString);

    /**
     * Build a basic view for the given object.
     *
     * @param suggestion  object implementing {@link Suggestible} to build a view for
     * @param convertView the old view to reuse, if possible
     * @param parent      parent view
     * @param context     current {@link android.content.Context} within the adapter
     * @param inflater    {@link android.view.LayoutInflater} to use
     * @param resources   {@link android.content.res.Resources} to use
     *
     * @return a view for the corresponding {@link Suggestible} object in the adapter
     */
    @NonNull
    public View getView(final @NonNull Suggestible suggestion,
                        @Nullable View convertView,
                        ViewGroup parent,
                        final @NonNull Context context,
                        final @NonNull LayoutInflater inflater,
                        final @NonNull Resources resources);

}
