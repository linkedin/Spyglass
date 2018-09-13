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
import android.support.test.InstrumentationRegistry;
import android.text.InputType;
import android.text.Spanned;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.linkedin.android.spyglass.mentions.MentionSpan;
import com.linkedin.android.spyglass.mentions.Mentionable;
import com.linkedin.android.spyglass.mentions.TestMention;
import com.linkedin.android.spyglass.suggestions.SuggestionsResult;
import com.linkedin.android.spyglass.suggestions.interfaces.Suggestible;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * This is a series of tests for the RichEditorView. It will use hard
 * implementations of it to test some of the functionality of the RichEditorView.
 * Placing this class in the same package as the class we're testing so we can
 * call protected methods in the test.
 */
public class RichEditorViewTest {

    private static class TestListView extends ListView
    {
        int mLastSelectedPosition = -1;

        public TestListView(Context context) {
            super(context);
        }

        @Override
        public void setSelection(int position) {
            mLastSelectedPosition = position;
            super.setSelection(position);
        }
    }

    private static class TestRichEditorView extends RichEditorView
    {

        public TestRichEditorView(@NonNull Context context) {
            super(context);
        }

        @Override
        public boolean post(Runnable action) {
            // Synchronous run to avoid flakiness in unit tests.
            action.run();
            return true;
        }
    }

    private RichEditorView mRichEditor;

    @Before
    public void setUp() throws Exception {
        mRichEditor = new TestRichEditorView(InstrumentationRegistry.getTargetContext());
        mRichEditor.setSelection(0);
    }

    @Test
    public void updateTextCount() throws Exception {
        String hello = "hello";
        mRichEditor.setText(hello);
        mRichEditor.updateEditorTextCount();

        TextView textCountView = mRichEditor.mTextCounterView;
        assertEquals("text count should be set", String.valueOf(hello.length()), textCountView.getText().toString());
    }

    @Test
    public void testGetMentionSpans() throws Exception {
        String hello = "Test mentions Shoulong Li and Deepank Gupta end test";
        mRichEditor.setText(hello);
        assertEquals(0, mRichEditor.getMentionSpans().size());

        Mentionable mention1 = new TestMention("Shoulong Li");
        MentionSpan span1 = new MentionSpan(mention1);
        Mentionable mention2 = new TestMention("Deepank Gupta");
        MentionSpan span2 = new MentionSpan(mention2);

        mRichEditor.getText().setSpan(span1, 14, 14 + mention1.getSuggestiblePrimaryText().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mRichEditor.getText().setSpan(span2, 30, 30 + mention2.getSuggestiblePrimaryText().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        List<MentionSpan> spans = mRichEditor.getMentionSpans();

        assertEquals(2, spans.size());
        Mentionable result1 = spans.get(0).getMention();
        Mentionable result2 = spans.get(1).getMention();

        assertEquals(mention1.getSuggestiblePrimaryText(), result1.getSuggestiblePrimaryText());
        assertEquals(mention1.getSuggestiblePrimaryText(), "Shoulong Li");
        assertEquals(mention2.getSuggestiblePrimaryText(), result2.getSuggestiblePrimaryText());
        assertEquals(mention2.getSuggestiblePrimaryText(), "Deepank Gupta");
    }

    @Test
    public void testSuggestionsListScrollsToTopOnReceivingFirstResults() throws Exception {
        TestListView suggestionsList = new TestListView(InstrumentationRegistry.getTargetContext());
        mRichEditor.mWaitingForFirstResult = true;
        mRichEditor.mSuggestionsList = suggestionsList;
        mRichEditor.mWaitingForFirstResult = true;
        mRichEditor.onReceiveSuggestionsResult(new SuggestionsResult(new QueryToken(""), new ArrayList<Suggestible>()), "");
        assertEquals(0, suggestionsList.mLastSelectedPosition);
    }

    @Test
    public void testSuggestionsListDisablesSpellingSuggestions() throws Exception {
        EditText input = mRichEditor.mMentionsEditText;
        int originalInputType = input.getInputType();

        mRichEditor.displaySuggestions(true);
        // should be no suggestions
        assertEquals(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, input.getInputType());

        mRichEditor.displaySuggestions(false);
        // should be back to original type
        assertEquals(originalInputType, input.getInputType());
    }
}
