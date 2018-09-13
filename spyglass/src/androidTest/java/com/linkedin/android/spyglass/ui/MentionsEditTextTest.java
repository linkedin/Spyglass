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

package com.linkedin.android.spyglass.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.text.Editable;
import android.view.MotionEvent;
import com.linkedin.android.spyglass.mentions.MentionSpan;
import com.linkedin.android.spyglass.mentions.Mentionable;
import com.linkedin.android.spyglass.mentions.TestMention;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizer;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * This is a series of tests for the MentionsEditText. It will use hard
 * implementations of it to test some of the functionality of the MentionsEditText.
 * Placing this class in the same package as the class we're testing so we can
 * call protected methods in the test.
 */
public class MentionsEditTextTest {

    private static class TestMentionsEditText extends MentionsEditText
    {
        public boolean enableNullTouchSpans;
        public String overriddenCurrentKeyword;
        public boolean throwExceptionOnSetAvoidPrefix;
        public String expectedAvoidPrefix;

        public TestMentionsEditText(@NonNull Context context) {
            super(context);
        }

        @Nullable
        @Override
        protected MentionSpan getTouchedSpan(MotionEvent event) {
            return enableNullTouchSpans ? null : super.getTouchedSpan(event);
        }

        @NonNull
        @Override
        public String getCurrentKeywordsString() {
            return overriddenCurrentKeyword != null ? overriddenCurrentKeyword : super.getCurrentKeywordsString();
        }

        @Override
        public void setAvoidedPrefix(String prefix) {

            if (throwExceptionOnSetAvoidPrefix) {
                throw new IllegalStateException();
            }

            if (expectedAvoidPrefix != null && !expectedAvoidPrefix.equals(prefix)) {
                throw new IllegalStateException();
            }

            super.setAvoidedPrefix(prefix);
        }
    }

    private static class TestRichEditorView extends RichEditorView
    {
        public boolean forceSuggestionDisplay;

        public TestRichEditorView(@NonNull Context context) {
            super(context);
        }

        @Override
        public boolean isDisplayingSuggestions() {
            return forceSuggestionDisplay || super.isDisplayingSuggestions();
        }
    }

    private TestMentionsEditText mEditText;
    private TestRichEditorView mRichEditor;

    @Before
    public void setUp() throws Exception {
        mEditText = new TestMentionsEditText(InstrumentationRegistry.getTargetContext());
        mEditText.setAvoidPrefixOnTap(true);
        mRichEditor = new TestRichEditorView(InstrumentationRegistry.getTargetContext());
        mEditText.setSuggestionsVisibilityManager(mRichEditor);
        mEditText.setTokenizer(new WordTokenizer());
    }

    @Test
    public void testOnTouchEvent() throws Exception {
        MotionEvent event = MotionEvent.obtain(100, 100, MotionEvent.ACTION_DOWN, 0, 0, 0);
        mEditText.enableNullTouchSpans = true;
        mRichEditor.forceSuggestionDisplay = true;

        // Test that the MentionsEditText does not avoid "" as a prefix
        // Note: After typing "@", the keyword string is "", so avoiding "" would mean avoiding all
        // explicit mentions (keyword string is what the user typed minus explicit characters)
        mEditText.overriddenCurrentKeyword = "";
        mEditText.throwExceptionOnSetAvoidPrefix = true;
        mEditText.onTouchEvent(event);

        // Test that the MentionsEditText avoids a prefix as long as it has length > 0
        mEditText.overriddenCurrentKeyword = "a";
        mEditText.throwExceptionOnSetAvoidPrefix = false;
        mEditText.expectedAvoidPrefix = "a";
        mEditText.onTouchEvent(event);
    }

    @Test
    public void testSelectionAtIndexZeroOnInit() {
        MentionsEditText editText = new MentionsEditText(InstrumentationRegistry.getTargetContext());
        assertEquals(0, editText.getSelectionStart());
        assertEquals(0, editText.getSelectionEnd());
    }

    @Test
    public void testLastNameMention() throws Exception {
        // first name only
        testMention("Hello FirstName", new TestMention("FirstName"));

        // first+last name
        testMention("Hello LastName", new TestMention("FirstName LastName"));

        // first+middle+last name
        testMention("Hello LastName", new TestMention("FirstName MiddleName LastName"));
    }

    private void testMention(String hello, Mentionable mention) throws Exception {
        Editable editable = mEditText.getEditableText();
        editable.append(hello);
        mEditText.setSelection(hello.length() - 1);
        mEditText.insertMention(mention);

        // ensure mention does not clobber existing text
        assertTrue(mEditText.getText().toString().startsWith("Hello "));
    }

    @Test
    public void testInsertMentionWithoutToken() throws Exception {
        Mentionable mention = new TestMention("FirstName MiddleName LastName");
        mEditText.insertMentionWithoutToken(mention);

        // ensure mention adds correctly
        assertTrue(mEditText.getText().toString().equals("FirstName MiddleName LastName"));

        Editable editable = mEditText.getEditableText();
        editable.append(" hello ");
        mention = new TestMention("New Mention");
        mEditText.insertMentionWithoutToken(mention);

        // ensure mention does not clobber existing text
        assertTrue(mEditText.getText().toString().equals("FirstName MiddleName LastName hello New Mention"));
    }
}
