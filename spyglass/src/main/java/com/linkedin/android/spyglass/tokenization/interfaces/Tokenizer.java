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
import android.text.Spanned;

/**
 * An interface representing a tokenizer. Similar to {@link android.widget.MultiAutoCompleteTextView.Tokenizer}, but
 * it operates on {@link Spanned} objects instead of {@link CharSequence} objects.
 */
public interface Tokenizer {

    /**
     * Returns the start of the token that ends at offset cursor within text.
     *
     * @param text   the {@link Spanned} to find the token in
     * @param cursor position of the cursor in text
     *
     * @return index of the first character in the token
     */
    public int findTokenStart(final @NonNull Spanned text, final int cursor);

    /**
     * Returns the end of the token that begins at offset cursor within text.
     *
     * @param text   the {@link Spanned} to find the token in
     * @param cursor position of the cursor in text
     *
     * @return index after the last character in the token
     */
    public int findTokenEnd(final @NonNull Spanned text, final int cursor);

    /**
     * Return true if the given text is a valid token (either explicit or implicit).
     *
     * @param text  the {@link Spanned} to check for a valid token
     * @param start index of the first character in the token (see {@link #findTokenStart(Spanned, int)})
     * @param end   index after the last character in the token (see (see {@link #findTokenEnd(Spanned, int)})
     *
     * @return true if input is a valid mention
     */
    public boolean isValidMention(final @NonNull Spanned text, final int start, final int end);

    /**
     * Returns text, modified, to ensure that it ends with a token terminator if necessary.
     *
     * @param text the given {@link Spanned} object to modify if necessary
     *
     * @return the modified version of the text
     */
    @NonNull
    public Spanned terminateToken(final @NonNull Spanned text);

    /**
     * Determines if given character is an explicit character according to the current settings of the tokenizer.
     *
     * @param c character to test
     *
     * @return true if c is an explicit character
     */
    public boolean isExplicitChar(final char c);

}
