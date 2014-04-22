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

package com.linkedin.android.spyglass.tokenization.interfaces;

import android.support.annotation.NonNull;

import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsResultListener;
import com.linkedin.android.spyglass.suggestions.SuggestionsResult;
import com.linkedin.android.spyglass.tokenization.QueryToken;

import java.util.List;

/**
 * Interface used to query an object with a {@link QueryToken}. The client is responsible for calling an instance of
 * {@link SuggestionsResultListener} with the results of the query once the query is complete.
 */
public interface QueryTokenReceiver {

    /**
     * Called to the client, expecting the client to return a {@link SuggestionsResult} at a later time via the
     * {@link SuggestionsResultListener} interface. It returns a List of String that the adapter will use to determine
     * if there are any ongoing queries at a given time.
     *
     * @param queryToken the {@link QueryToken} to process
     *
     * @return a List of String representing the buckets that will be used when calling {@link SuggestionsResultListener}
     */
    public List<String> onQueryReceived(final @NonNull QueryToken queryToken);
}
