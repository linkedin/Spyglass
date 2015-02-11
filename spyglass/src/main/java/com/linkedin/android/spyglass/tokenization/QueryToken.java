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

package com.linkedin.android.spyglass.tokenization;

import com.linkedin.android.spyglass.tokenization.interfaces.QueryTokenReceiver;
import com.linkedin.android.spyglass.tokenization.interfaces.Tokenizer;

import java.io.Serializable;

/**
 * Class that represents a token from a {@link Tokenizer} that can be used to query for suggestions.
 * <p>
 * Note that if the query is explicit, the explicit character has not been removed from the start of the token string.
 * To get the string without any explicit character, use {@link #getKeywords()}.
 */
public class QueryToken implements Serializable {

    // what the user typed, exactly, as detected by the tokenizer
    private String mTokenString;

    // if the query was explicit, then this was the character the user typed (otherwise, null char)
    private char mExplicitChar = 0;

    public QueryToken(String tokenString) {
        mTokenString = tokenString;
    }

    public QueryToken(String tokenString, char explicitChar) {
        this(tokenString);
        mExplicitChar = explicitChar;
    }

    /**
     * @return query as typed by the user and detected by the {@link Tokenizer}
     */
    public String getTokenString() {
        return mTokenString;
    }

    /**
     * Returns a String that should be used to perform the query. It is equivalent to the token string without an explicit
     * character if it exists.
     *
     * @return one or more words that the {@link QueryTokenReceiver} should use for the query
     */
    public String getKeywords() {
        return (mExplicitChar != 0) ? mTokenString.substring(1) : mTokenString;
    }

    /**
     * @return the explicit character used in the query, or the null character if the query is implicit
     */
    public char getExplicitChar() {
        return mExplicitChar;
    }

    /**
     * @return true if the query is explicit
     */
    public boolean isExplicit() {
        return mExplicitChar != 0;
    }

    @Override
    public boolean equals(Object o) {
        QueryToken that = (QueryToken) o;
        return mTokenString != null && that != null && mTokenString.equals(that.getTokenString());
    }

    @Override
    public int hashCode() {
        return mTokenString.hashCode();
    }
}
