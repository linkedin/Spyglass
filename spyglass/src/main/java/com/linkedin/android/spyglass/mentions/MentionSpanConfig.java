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

package com.linkedin.android.spyglass.mentions;

import android.graphics.Color;
import android.graphics.Typeface;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/**
 * Class used to configure various options for the {@link MentionSpan}. Instantiate using the
 * {@link MentionSpanConfig.Builder} class.
 */
public class MentionSpanConfig {

    @ColorInt public final int NORMAL_TEXT_COLOR;
    @ColorInt public final int NORMAL_TEXT_BACKGROUND_COLOR;
    @ColorInt public final int SELECTED_TEXT_COLOR;
    @ColorInt public final int SELECTED_TEXT_BACKGROUND_COLOR;
    public final int NORMAL_TEXT_STYLE;
    public final int SELECTED_TEXT_STYLE;

    MentionSpanConfig(@ColorInt final int normalTextColor,
                      @ColorInt final int normalTextBackgroundColor,
                      @ColorInt final int selectedTextColor,
                      @ColorInt final int selectedTextBackgroundColor,
                      final int normalTextStyle,
                      final int selectedTextStyle) {
        this.NORMAL_TEXT_COLOR = normalTextColor;
        this.NORMAL_TEXT_BACKGROUND_COLOR = normalTextBackgroundColor;
        this.SELECTED_TEXT_COLOR = selectedTextColor;
        this.SELECTED_TEXT_BACKGROUND_COLOR = selectedTextBackgroundColor;
        this.NORMAL_TEXT_STYLE = normalTextStyle;
        this.SELECTED_TEXT_STYLE = selectedTextStyle;
    }

    public static class Builder {

        // Default colors
        @ColorInt private int normalTextColor = Color.parseColor("#00a0dc");
        @ColorInt private int normalTextBackgroundColor = Color.TRANSPARENT;
        @ColorInt private int selectedTextColor = Color.WHITE;
        @ColorInt private int selectedTextBackgroundColor = Color.parseColor("#0077b5");

        // Default text styles
        private int normalTextStyle = Typeface.NORMAL;
        private int selectedTextStyle = Typeface.NORMAL;

        @NonNull
        public Builder setMentionTextColor(@ColorInt int normalTextColor) {
            if (normalTextColor != -1) {
                this.normalTextColor = normalTextColor;
            }
            return this;
        }

        @NonNull
        public Builder setMentionTextBackgroundColor(@ColorInt int normalTextBackgroundColor) {
            if (normalTextBackgroundColor != -1) {
                this.normalTextBackgroundColor = normalTextBackgroundColor;
            }
            return this;
        }

        @NonNull
        public Builder setSelectedMentionTextColor(@ColorInt int selectedTextColor) {
            if (selectedTextColor != -1) {
                this.selectedTextColor = selectedTextColor;
            }
            return this;
        }

        @NonNull
        public Builder setSelectedMentionTextBackgroundColor(@ColorInt int selectedTextBackgroundColor) {
            if (selectedTextBackgroundColor != -1) {
                this.selectedTextBackgroundColor = selectedTextBackgroundColor;
            }
            return this;
        }

        @NonNull
        public Builder setMentionTextStyle(int normalTextStyle) {
            if (normalTextStyle != -1) {
                this.normalTextStyle = normalTextStyle;
            }
            return this;
        }

        @NonNull
        public Builder setSelectedMentionTextStyle(int selectedTextStyle) {
            if (selectedTextStyle != -1) {
                this.selectedTextStyle = selectedTextStyle;
            }
            return this;
        }

        @NonNull
        public MentionSpanConfig build() {
            return new MentionSpanConfig(normalTextColor, normalTextBackgroundColor,
                                         selectedTextColor, selectedTextBackgroundColor,
                                         normalTextStyle, selectedTextStyle);
        }
    }
}
