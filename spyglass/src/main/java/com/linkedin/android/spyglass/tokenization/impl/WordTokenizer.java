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

package com.linkedin.android.spyglass.tokenization.impl;

import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.EditText;
import com.linkedin.android.spyglass.mentions.MentionSpan;
import com.linkedin.android.spyglass.tokenization.interfaces.Tokenizer;

/**
 * Tokenizer class used to determine the keywords to be used when querying for mention suggestions.
 */
public class WordTokenizer implements Tokenizer {

    private final WordTokenizerConfig mConfig;

    public WordTokenizer() {
        this(new WordTokenizerConfig.Builder().build());
    }

    public WordTokenizer(final @NonNull WordTokenizerConfig config) {
        mConfig = config;
    }

    // --------------------------------------------------
    // Tokenizer Interface Implementation
    // --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int findTokenStart(final @NonNull Spanned text, final int cursor) {
        int start = getSearchStartIndex(text, cursor);
        int i = cursor;

        // If it is explicit, return the index of the first explicit character
        if (isExplicit(text, cursor)) {
            i--;
            while (i >= start) {
                char currentChar = text.charAt(i);
                if (isExplicitChar(currentChar)) {
                    if (i == 0 || isWordBreakingChar(text.charAt(i - 1))) {
                        return i;
                    }
                }
                i--;
            }
            // Could not find explicit character before the cursor
            // Note: This case should never happen (means that isExplicit
            // returned true when it should have been false)
            return -1;

        } else {

            // For implicit tokens, we need to go back a certain number of words to find the start
            // of the token (with the max number of words to go back defined in the config)
            int maxNumKeywords = mConfig.MAX_NUM_KEYWORDS;

            // Go back to the start of the word that the cursor is currently in
            while (i > start && !isWordBreakingChar(text.charAt(i - 1))) {
                i--;
            }

            // Cursor is at beginning of current word, go back MaxNumKeywords - 1 now
            for (int j = 0; j < maxNumKeywords - 1; j++) {
                // Decrement through only one word-breaking character, if it exists
                if (i > start && isWordBreakingChar(text.charAt(i - 1))) {
                    i--;
                }
                // If there is more than one word-breaking space, break out now
                // Do not consider queries with words separated by more than one word-breaking char
                if (i > start && isWordBreakingChar(text.charAt(i - 1))) {
                    break;
                }
                // Decrement until the next space
                while (i > start && !isWordBreakingChar(text.charAt(i - 1))) {
                    i--;
                }
            }

            // Ensures that text.char(i) is not a word-breaking or explicit char (i.e. cursor must have a
            // word-breaking char in front of it and a non-word-breaking char behind it)
            while (i < cursor && (isWordBreakingChar(text.charAt(i)) || isExplicitChar(text.charAt(i)))) {
                i++;
            }

            return i;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int findTokenEnd(final @NonNull Spanned text, final int cursor) {
        int i = cursor;
        int end = getSearchEndIndex(text, cursor);

        // Starting from the cursor, increment i until it reaches the first word-breaking char
        while (i >= 0 && i < end) {
            if (isWordBreakingChar(text.charAt(i))) {
                return i;
            } else {
                i++;
            }
        }

        return i;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValidMention(final @NonNull Spanned text, final int start, final int end) {
        // Get the token
        CharSequence token = text.subSequence(start, end);

        // Null or empty string is not a valid mention
        if (TextUtils.isEmpty(token)) {
            return false;
        }

        // Handle explicit mentions first, then implicit mentions
        final int threshold = mConfig.THRESHOLD;
        boolean multipleWords = containsWordBreakingChar(token);
        boolean containsExplicitChar = containsExplicitChar(token);

        if (!multipleWords && containsExplicitChar) {

            // If it is one word and has an explicit char, the explicit char must be the first char
            if (!isExplicitChar(token.charAt(0))) {
                return false;
            }

            // Ensure that the character before the explicit character is a word-breaking character
            // Note: Checks the explicit character closest in front of the cursor
            if (!hasWordBreakingCharBeforeExplicitChar(text, end)) {
                return false;
            }

            // Return true if string is just an explicit character
            if (token.length() == 1) {
                return true;
            }

            // If input has length greater than one, the second character must be a letter or digit
            // Return true if and only if second character is a letter or digit, i.e. "@d"
            return Character.isLetterOrDigit(token.charAt(1));

        } else if (token.length() >= threshold) {

            // Change behavior depending on if keywords is one or more words
            if (!multipleWords) {
                // One word, no explicit characters
                // input is only one word, i.e. "u41"
                return onlyLettersOrDigits(token, threshold, 0);
            } else if (containsExplicitChar) {
                // Multiple words, has explicit character
                // Must have a space, the explicit character, then a letter or digit
                return hasWordBreakingCharBeforeExplicitChar(text, end)
                        && isExplicitChar(token.charAt(0))
                        && Character.isLetterOrDigit(token.charAt(1));
            } else {
                // Multiple words, no explicit character
                // Either the first or last couple of characters must be letters/digits
                boolean firstCharactersValid = onlyLettersOrDigits(token, threshold, 0);
                boolean lastCharactersValid = onlyLettersOrDigits(token, threshold, token.length() - threshold);
                return firstCharactersValid || lastCharactersValid;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Spanned terminateToken(final @NonNull Spanned text) {
        // Note: We do not need to modify the text to terminate it
        return text;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExplicitChar(final char c) {
        final String explicitChars = mConfig.EXPLICIT_CHARS;
        for (int i = 0; i < explicitChars.length(); i++) {
            char explicitChar = explicitChars.charAt(i);
            if (c == explicitChar) {
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------
    // Public Methods
    // --------------------------------------------------

    /**
     * Returns true if and only if there is an explicit character before the cursor
     * but after any previous mentions. There must be a word-breaking character before the
     * explicit character.
     *
     * @param text   String to determine if it is explicit or not
     * @param cursor position of the cursor in text
     *
     * @return true if the current keywords are explicit (i.e. explicit character typed before cursor)
     */
    public boolean isExplicit(final @NonNull CharSequence text, final int cursor) {
        return getExplicitChar(text, cursor) != (char) 0;
    }

    /**
     * Returns the explicit character if appropriate (i.e. within the keywords).
     * If not currently explicit, then returns the null character (i.e. '/0').
     *
     * @param text   String to get the explicit character from
     * @param cursor position of the cursor in text
     *
     * @return the current explicit character or the null character if not currently explicit
     */
    public char getExplicitChar(final @NonNull CharSequence text, final int cursor) {
        if (cursor < 0 || cursor > text.length()) {
            return (char) 0;
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        int start = getSearchStartIndex(ssb, cursor);
        int i = cursor - 1;
        int numWordBreakingCharsSeen = 0;
        while (i >= start) {
            char currentChar = text.charAt(i);
            if (isExplicitChar(currentChar)) {
                // Explicit character must have a word-breaking character before it
                if (i == 0 || isWordBreakingChar(text.charAt(i - 1))) {
                    return currentChar;
                } else {
                    // Otherwise, explicit character is not in a valid position, return null char
                    return (char) 0;
                }
            } else if (isWordBreakingChar(currentChar)) {
                // Do not allow the explicit mention to exceed
                numWordBreakingCharsSeen++;
                if (numWordBreakingCharsSeen == mConfig.MAX_NUM_KEYWORDS) {
                    // No explicit char in maxNumKeywords, so return null char
                    return (char) 0;
                }
            }
            i--;
        }
        return (char) 0;
    }

    /**
     * Returns true if the input string contains an explicit character.
     *
     * @param input a {@link CharSequence} to test
     *
     * @return true if input contains an explicit character
     */
    public boolean containsExplicitChar(final @NonNull CharSequence input) {
        if (!TextUtils.isEmpty(input)) {
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (isExplicitChar(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if given character is a word-breaking character according to the current settings
     * within the {@link Configuration}.
     *
     * @param c character to test
     *
     * @return true if c is an word-breaking character
     */
    public boolean isWordBreakingChar(final char c) {
        final String wordBreakChars = mConfig.WORD_BREAK_CHARS;
        for (int i = 0; i < wordBreakChars.length(); i++) {
            char wordBreakChar = wordBreakChars.charAt(i);
            if (c == wordBreakChar) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the input string contains a word-breaking character.
     *
     * @param input a {@link CharSequence} to test
     *
     * @return true if input contains a word-breaking character
     */
    public boolean containsWordBreakingChar(final @NonNull CharSequence input) {
        if (!TextUtils.isEmpty(input)) {
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (isWordBreakingChar(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Given a string and starting index, return true if the first "numCharsToCheck" characters at
     * the starting index are either a letter or a digit.
     *
     * @param input           a {@link CharSequence} to test
     * @param numCharsToCheck number of characters to examine at starting position
     * @param start           starting position within the input string
     *
     * @return true if the first "numCharsToCheck" at the starting index are either letters or digits
     */
    public boolean onlyLettersOrDigits(final @NonNull CharSequence input, final int numCharsToCheck, final int start) {

        // Starting position must be within the input string
        if (start < 0 || start > input.length()) {
            return false;
        }

        // Check the first "numCharsToCheck" characters to ensure they are a letter or digit
        for (int i = 0; i < numCharsToCheck; i++) {
            int positionToCheck = start + i;
            // Return false if we would throw an Out-of-Bounds exception
            if (positionToCheck >= input.length()) {
                return false;
            }
            // Return false early if current character is not a letter or digit
            char charToCheck = input.charAt(positionToCheck);
            if (!Character.isLetterOrDigit(charToCheck)) {
                return false;
            }
        }

        // First "numCharsToCheck" characters are either letters or digits, so return true
        return true;
    }

    // --------------------------------------------------
    // Protected Helper Methods
    // --------------------------------------------------

    /**
     * Returns the index of the end of the last span before the cursor or
     * the start of the current line if there are no spans before the cursor.
     *
     * @param text   the {@link Spanned} to examine
     * @param cursor position of the cursor in text
     *
     * @return the furthest in front of the cursor to search for the current keywords
     */
    protected int getSearchStartIndex(final @NonNull Spanned text, int cursor) {
        if (cursor < 0 || cursor > text.length()) {
            cursor = 0;
        }

        // Get index of the end of the last span before the cursor (or 0 if does not exist)
        MentionSpan[] spans = text.getSpans(0, text.length(), MentionSpan.class);
        int closestToCursor = 0;
        for (MentionSpan span : spans) {
            int end = text.getSpanEnd(span);
            if (end > closestToCursor && end <= cursor) {
                closestToCursor = end;
            }
        }

        // Get the index of the start of the line
        String textString = text.toString().substring(0, cursor);
        int lineStartIndex = 0;
        if (textString.contains(mConfig.LINE_SEPARATOR)) {
            lineStartIndex = textString.lastIndexOf(mConfig.LINE_SEPARATOR) + 1;
        }

        // Return whichever is closer before to the cursor
        return Math.max(closestToCursor, lineStartIndex);
    }

    /**
     * Returns the index of the beginning of the first span after the cursor or
     * length of the text if there are no spans after the cursor.
     *
     * @param text   the {@link Spanned} to examine
     * @param cursor position of the cursor in text
     *
     * @return the furthest behind the cursor to search for the current keywords
     */
    protected int getSearchEndIndex(final @NonNull Spanned text, int cursor) {
        if (cursor < 0 || cursor > text.length()) {
            cursor = 0;
        }

        // Get index of the start of the first span after the cursor (or text.length() if does not exist)
        MentionSpan[] spans = text.getSpans(0, text.length(), MentionSpan.class);
        int closestAfterCursor = text.length();
        for (MentionSpan span : spans) {
            int start = text.getSpanStart(span);
            if (start < closestAfterCursor && start >= cursor) {
                closestAfterCursor = start;
            }
        }

        // Get the index of the end of the line
        String textString = text.toString().substring(cursor, text.length());
        int lineEndIndex = text.length();
        if (textString.contains(mConfig.LINE_SEPARATOR)) {
            lineEndIndex = cursor + textString.indexOf(mConfig.LINE_SEPARATOR);
        }

        // Return whichever is closest after the cursor
        return Math.min(closestAfterCursor, lineEndIndex);
    }

    /**
     * Ensure the the character before the explicit character is a word-breaking character.
     * Note that the function only uses the input string to determine which explicit character was
     * typed. It uses the complete contents of the {@link EditText} to determine if there is a
     * word-breaking character before the explicit character, as the input string may start with an
     * explicit character, i.e. with an input string of "@John Doe" and an {@link EditText} containing
     * the string "Hello @John Doe", this should return true.
     *
     * @param text   the {@link Spanned} to check for a word-breaking character before the explicit character
     * @param cursor position of the cursor in text
     *
     * @return true if there is a space before the explicit character, false otherwise
     */
    protected boolean hasWordBreakingCharBeforeExplicitChar(final @NonNull Spanned text, final int cursor) {
        CharSequence beforeCursor = text.subSequence(0, cursor);
        // Get the explicit character closest before the cursor and make sure it
        // has a word-breaking character in front of it
        int i = cursor - 1;
        while (i >= 0 && i < beforeCursor.length()) {
            char c = beforeCursor.charAt(i);
            if (isExplicitChar(c)) {
                return i == 0 || isWordBreakingChar(beforeCursor.charAt(i - 1));
            }
            i--;
        }
        return false;
    }

}
