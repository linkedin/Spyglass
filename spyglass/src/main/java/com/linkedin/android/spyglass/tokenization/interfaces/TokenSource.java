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
import android.support.annotation.Nullable;
import com.linkedin.android.spyglass.tokenization.QueryToken;

/**
 * Interface representing a source to generate and retrieve tokens.
 */
public interface TokenSource {

    /**
     * Gets the text that the {@link Tokenizer} is currently considering for suggestions. Note that this text does not
     * have to resemble a valid query token.
     *
     * @return a string representing currently being considered for a possible query, as the user typed it
     */
    @NonNull
    public String getCurrentTokenString();

    /**
     * Determine if the token between the given start and end indexes represents a valid token. If it is valid, return
     * the corresponding {@link QueryToken}. Otherwise, return null.
     *
     * @return the valid {@link QueryToken} if it is valid, otherwise null
     */
    @Nullable
    public QueryToken getQueryTokenIfValid();

}
