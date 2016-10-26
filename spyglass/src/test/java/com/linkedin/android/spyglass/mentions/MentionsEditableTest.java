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

import android.annotation.TargetApi;
import android.text.Spanned;
import android.text.style.SuggestionSpan;
import com.linkedin.android.spyglass.BuildConfig;
import com.linkedin.android.utils.SpyglassRobolectricRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;

@Config(constants = BuildConfig.class, sdk = 18)
@RunWith(SpyglassRobolectricRunner.class)
public class MentionsEditableTest {

    private MentionsEditable mEditable;
    private MentionSpan mMentionSpan;

    private static final String NAME = "FirstName LastName";

    @Before
    public void setUp() {
        mEditable = new MentionsEditable("Hi @" + NAME + " bye");
        TestMention tm = new TestMention(NAME);
        mMentionSpan = new MentionSpan(tm);
    }

    @Test
    @TargetApi(14)
    public void testSetSpan() {
        // Should be able to add a MentionSpan
        final int spanStart = 3;
        final int spanEnd = 4 + NAME.length();
        mEditable.setSpan(mMentionSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertEquals(1, mEditable.getSpans(0, mEditable.length(), MentionSpan.class).length);

        // Can only add a SuggestionSpan (i.e. a subclass of CharacterStyle) if it does not overlap
        // at all with a MentionSpan (note: SuggestionSpan requires API 14)
        SuggestionSpan suggestionSpan = new SuggestionSpan(RuntimeEnvironment.application, new String[0], 0);
        mEditable.setSpan(suggestionSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertEquals(0, mEditable.getSpans(0, mEditable.length(), SuggestionSpan.class).length);
        mEditable.setSpan(suggestionSpan, 0, spanEnd - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertEquals(0, mEditable.getSpans(0, mEditable.length(), SuggestionSpan.class).length);
        mEditable.setSpan(suggestionSpan, spanStart, spanEnd + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertEquals(0, mEditable.getSpans(0, mEditable.length(), SuggestionSpan.class).length);
        mEditable.setSpan(suggestionSpan, spanEnd, spanEnd + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertEquals(1, mEditable.getSpans(0, mEditable.length(), SuggestionSpan.class).length);
    }

    @Test
    public void testTrim() {
        // Test editable beginning with spaces
        mEditable = new MentionsEditable("  " + NAME + "  ");
        mEditable.setSpan(mMentionSpan, 2, 2 + NAME.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mEditable = mEditable.trim();
        assertEquals(NAME, mEditable.toString());
        assertEquals(0, mEditable.getSpanStart(mMentionSpan));
        assertEquals(mEditable.length(), mEditable.getSpanEnd(mMentionSpan));

        // Test editable beginning with a new line and tab
        mEditable = new MentionsEditable("\n\t" + NAME);
        mEditable.setSpan(mMentionSpan, 2, 2 + NAME.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mEditable = mEditable.trim();
        assertEquals(NAME, mEditable.toString());
        assertEquals(0, mEditable.getSpanStart(mMentionSpan));
        assertEquals(mEditable.length(), mEditable.getSpanEnd(mMentionSpan));
    }

    @Test
    public void testMapToDeleteCharacterInsteadOfAppend() {
        mEditable = new MentionsEditable("Hello World");
        mEditable.replace(11, 11, "Worl");
        assertEquals("Hello Worl", mEditable.toString());
    }
}