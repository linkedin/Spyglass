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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.CharacterStyle;

import com.linkedin.android.spyglass.ui.MentionsEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Custom {@link Editable} containing methods specifically regarding mentions in a {@link Spanned} string object. Used
 * specifically within the {@link MentionsEditText}.
 */
public class MentionsEditable extends SpannableStringBuilder {

    public MentionsEditable(CharSequence text) {
        super(text);
    }

    public MentionsEditable(CharSequence text, int start, int end) {
        super(text, start, end);
    }

    // --------------------------------------------------
    // Overrides
    // --------------------------------------------------

    @Override
    public void setSpan(Object what, int start, int end, int flags) {
        // Do not add any spans that affect the character appearance of a mention (i.e. they overlap
        // with a MentionSpan). This helps prevent mentions from having a red underline due to the spell
        // checker. Note: SuggestionSpan was added in ICS, and different keyboards may use other kinds
        // of spans (i.e. the Motorola SpellCheckerMarkupSpan). Therefore, we cannot just filter out
        // SuggestionSpans, but rather, any span that would change the appearance of our MentionSpans.
        if (what instanceof CharacterStyle) {
            MentionSpan[] mentionSpans = getSpans(start, end, MentionSpan.class);
            if (mentionSpans != null && mentionSpans.length > 0) {
                return;
            }
        }

        // Ensure that the start and end points are set at zero initially
        // Note: This issue was seen on a Gingerbread device (start and end were both -1) and
        // prevents the device from crashing.
        if ((what == Selection.SELECTION_START || what == Selection.SELECTION_END) && length() == 0) {
            start = 0;
            end = 0;
        }

        super.setSpan(what, start, end, flags);
    }

    // --------------------------------------------------
    // Custom Public Methods
    // --------------------------------------------------

    /**
     * Implementation of {@link String#trim()} for an {@link Editable}.
     *
     * @return a new {@link MentionsEditable} with whitespace characters removed from the beginning and the end
     */
    public MentionsEditable trim() {
        // Delete beginning spaces
        while (length() > 0 && Character.isWhitespace(charAt(0))) {
            delete(0, 1);
        }
        // Delete ending spaces
        int len = length();
        while (len > 0 && Character.isWhitespace(charAt(len - 1))) {
            delete(len - 1, len);
            len--;
        }
        return new MentionsEditable(this, 0, length());
    }

    @NonNull
    public List<MentionSpan> getMentionSpans() {
        MentionSpan[] mentionSpans = getSpans(0, length(), MentionSpan.class);
        return (mentionSpans != null) ? Arrays.asList(mentionSpans) : new ArrayList<MentionSpan>();
    }

    /**
     * Given an integer offset, return the {@link MentionSpan} located at the offset in the text of the
     * {@link android.widget.EditText}, if it exists. Otherwise, return null.
     *
     * @param index integer offset in text
     *
     * @return a {@link MentionSpan} located at index in text, or null
     */
    @Nullable
    public MentionSpan getMentionSpanAtOffset(int index) {
        MentionSpan[] spans = getSpans(index, index, MentionSpan.class);
        return (spans != null && spans.length > 0) ? spans[0] : null;
    }

    /**
     * Get the {@link MentionSpan} starting at the given index in the text, or null if there is no {@link MentionSpan}
     * starting at that index.
     *
     * @param index integer offset in text
     *
     * @return a {@link MentionSpan} starting at index in text, or null
     */
    @Nullable
    public MentionSpan getMentionSpanStartingAt(int index) {
        MentionSpan[] spans = getSpans(0, length(), MentionSpan.class);
        if (spans != null) {
            for (MentionSpan span : spans) {
                if (getSpanStart(span) == index) {
                    return span;
                }
            }
        }
        return null;
    }

    /**
     * Get the {@link MentionSpan} ending at the given index in the text, or null if there is no {@link MentionSpan}
     * ending at that index.
     *
     * @param index integer offset in text
     *
     * @return a {@link MentionSpan} ending at index in text, or null
     */
    @Nullable
    public MentionSpan getMentionSpanEndingAt(int index) {
        MentionSpan[] spans = getSpans(0, length(), MentionSpan.class);
        if (spans != null) {
            for (MentionSpan span : spans) {
                if (getSpanEnd(span) == index) {
                    return span;
                }
            }
        }
        return null;
    }

}