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

package com.linkedin.android.spyglass.suggestions;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import androidx.annotation.NonNull;

import com.linkedin.android.spyglass.suggestions.interfaces.Suggestible;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsListBuilder;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsVisibilityManager;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import com.linkedin.android.spyglass.tokenization.interfaces.TokenSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter class for displaying suggestions.
 */
public class SuggestionsAdapter extends BaseAdapter {

    private final Object mLock = new Object();
    private final Context mContext;
    private final Resources mResources;
    private final LayoutInflater mInflater;

    private SuggestionsVisibilityManager mSuggestionsVisibilityManager;
    private SuggestionsListBuilder mSuggestionsListBuilder;
    private final List<Suggestible> mSuggestions;

    // Map from a given bucket (defined by a unique string) to the latest query result for that bucket
    // Example buckets: "Person-Database", "Person-Network", "Companies-Database", "Companies-Network"
    private final Map<String, SuggestionsResult> mResultMap = new HashMap<>();
    private final Map<QueryToken, Set<String>> mWaitingForResults = new HashMap<>();

    public SuggestionsAdapter(final @NonNull Context context,
                              final @NonNull SuggestionsVisibilityManager suggestionsVisibilityManager,
                              final @NonNull SuggestionsListBuilder suggestionsListBuilder) {
        super();
        mContext = context;
        mResources = context.getResources();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSuggestionsVisibilityManager = suggestionsVisibilityManager;
        mSuggestionsListBuilder = suggestionsListBuilder;
        mSuggestions = new ArrayList<>();
    }

    // --------------------------------------------------
    // Public Methods
    // --------------------------------------------------

    /**
     * Method to notify the adapter that a new {@link QueryToken} has been received and that
     * suggestions will be added to the adapter once generated.
     *
     * @param queryToken the {@link QueryToken} that has been received
     * @param buckets    a list of string dictating which buckets the future query results will go into
     */

    public void notifyQueryTokenReceived(QueryToken queryToken, List<String> buckets) {
        synchronized (mLock) {
            Set<String> currentBuckets = mWaitingForResults.get(queryToken);
            if (currentBuckets == null) {
                currentBuckets = new HashSet<>();
            }
            currentBuckets.addAll(buckets);
            mWaitingForResults.put(queryToken, currentBuckets);
        }
    }

    /**
     * Add mention suggestions to a given bucket in the adapter. The adapter tracks the latest result for every given
     * bucket, and passes this information to the SuggestionsManager to construct the list of suggestions in the
     * appropriate order.
     * <p>
     * Note: This should be called exactly once for every bucket returned from the query client.
     *
     * @param result a {@link SuggestionsResult} containing the suggestions to add
     * @param bucket a string representing the group to place the {@link SuggestionsResult} into
     * @param source the associated {@link TokenSource} to use for reference
     */
    public void addSuggestions(final @NonNull SuggestionsResult result,
                               final @NonNull String bucket,
                               final @NonNull TokenSource source) {
        // Add result to proper bucket and remove from waiting
        QueryToken query = result.getQueryToken();
        synchronized (mLock) {
            mResultMap.put(bucket, result);
            Set<String> waitingForBuckets = mWaitingForResults.get(query);
            if (waitingForBuckets != null) {
                waitingForBuckets.remove(bucket);
                if (waitingForBuckets.size() == 0) {
                    mWaitingForResults.remove(query);
                }
            }
        }

        // Rebuild the list of suggestions in the appropriate order
        String currentTokenString = source.getCurrentTokenString();
        synchronized (mLock) {
            mSuggestions.clear();
            List<Suggestible> suggestions = mSuggestionsListBuilder.buildSuggestions(mResultMap, currentTokenString);

            // If we have suggestions, add them to the adapter and display them
            if (suggestions.size() > 0) {
                mSuggestions.addAll(suggestions);
                mSuggestionsVisibilityManager.displaySuggestions(true);
            } else {
                hideSuggestionsIfNecessary(result.getQueryToken(), source);
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Clear all data from adapter.
     */
    public void clear() {
        mResultMap.clear();
        notifyDataSetChanged();
    }

    // --------------------------------------------------
    // Private Helper Methods
    // --------------------------------------------------

    /**
     * Hides the suggestions if there are no more incoming queries.
     *
     * @param currentQuery the most recent {@link QueryToken}
     * @param source       the associated {@link TokenSource} to use for reference
     */
    private void hideSuggestionsIfNecessary(final @NonNull QueryToken currentQuery,
                                            final @NonNull TokenSource source) {
        String queryTS = currentQuery.getTokenString();
        String currentTS = source.getCurrentTokenString();
        if (!isWaitingForResults(currentQuery) && queryTS != null && queryTS.equals(currentTS)) {
            mSuggestionsVisibilityManager.displaySuggestions(false);
        }
    }

    /**
     * Determines if the adapter is still waiting for results for a given {@link QueryToken}
     *
     * @param currentQuery the {@link QueryToken} to check if waiting for results on
     *
     * @return true if still waiting for the results of the current query
     */
    private boolean isWaitingForResults(QueryToken currentQuery) {
        synchronized (mLock) {
            Set<String> buckets = mWaitingForResults.get(currentQuery);
            return buckets != null && buckets.size() > 0;
        }
    }

    // --------------------------------------------------
    // BaseAdapter Overrides
    // --------------------------------------------------

    @Override
    public int getCount() {
        return mSuggestions.size();
    }

    @Override
    public Suggestible getItem(int position) {
        Suggestible mention = null;
        if (position >= 0 && position < mSuggestions.size()) {
            mention = mSuggestions.get(position);
        }
        return mention;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Suggestible suggestion = getItem(position);
        View v = null;
        if (mSuggestionsVisibilityManager != null) {
            v = mSuggestionsListBuilder.getView(suggestion, convertView, parent, mContext, mInflater, mResources);
        }
        return v;
    }

    // --------------------------------------------------
    // Setters
    // --------------------------------------------------

    /**
     * Sets the {@link com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsVisibilityManager} to use.
     *
     * @param suggestionsVisibilityManager the {@link com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsVisibilityManager} to use
     */
    public void setSuggestionsManager(final @NonNull SuggestionsVisibilityManager suggestionsVisibilityManager) {
        mSuggestionsVisibilityManager = suggestionsVisibilityManager;
    }

    /**
     * Sets the {@link SuggestionsListBuilder} to use.
     *
     * @param suggestionsListBuilder the {@link SuggestionsListBuilder} to use
     */
    public void setSuggestionsListBuilder(final @NonNull SuggestionsListBuilder suggestionsListBuilder) {
        mSuggestionsListBuilder = suggestionsListBuilder;
    }

}
