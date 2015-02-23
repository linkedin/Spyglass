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

import android.support.annotation.NonNull;

/**
 * Class used to configure various parsing options for the {@link WordTokenizer}. Instantiate using the
 * {@link WordTokenizerConfig.Builder} class.
 */
public class WordTokenizerConfig {

    public final String LINE_SEPARATOR;

    // Number of characters required in a word before returning a mention suggestion starting with the word
    // Note: These characters are required to be either letters or digits
    public int THRESHOLD;

    // Max number of words to consider as keywords in a query
    public int MAX_NUM_KEYWORDS;

    // Characters to use as explicit mention indicators
    public final String EXPLICIT_CHARS;

    // Characters to use to separate words
    public final String WORD_BREAK_CHARS;

    private WordTokenizerConfig(final @NonNull String lineSeparator,
                                final int threshold,
                                final int maxNumKeywords,
                                final @NonNull String explicitChars,
                                final @NonNull String wordBreakChars) {
        LINE_SEPARATOR = lineSeparator;
        THRESHOLD = threshold;
        MAX_NUM_KEYWORDS = maxNumKeywords;
        EXPLICIT_CHARS = explicitChars;
        WORD_BREAK_CHARS = wordBreakChars;
    }

    public static class Builder {

        // Default values for configuration
        private String lineSeparator = System.getProperty("line.separator");
        private int threshold = 4;
        private int maxNumKeywords = 1;
        private String explicitChars = "@";
        private String wordBreakChars = " ." + System.getProperty("line.separator");

        public Builder setLineSeparator(String lineSeparator) {
            this.lineSeparator = lineSeparator;
            return this;
        }

        public Builder setThreshold(int threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder setMaxNumKeywords(int maxNumKeywords) {
            this.maxNumKeywords = maxNumKeywords;
            return this;
        }

        public Builder setExplicitChars(String explicitChars) {
            this.explicitChars = explicitChars;
            return this;
        }

        public Builder setWordBreakChars(String wordBreakChars) {
            this.wordBreakChars = wordBreakChars;
            return this;
        }

        public WordTokenizerConfig build() {
            return new WordTokenizerConfig(lineSeparator, threshold, maxNumKeywords, explicitChars, wordBreakChars);
        }
    }
}
