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

import android.text.Editable;
import android.view.MotionEvent;
import com.linkedin.android.spyglass.BuildConfig;
import com.linkedin.android.spyglass.mentions.Mentionable;
import com.linkedin.android.spyglass.mentions.TestMention;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizer;
import com.linkedin.android.utils.SpyglassRobolectricRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * This is a series of tests for the MentionsEditText. It will use hard
 * implementations of it to test some of the functionality of the MentionsEditText.
 * Placing this class in the same package as the class we're testing so we can
 * call protected methods in the test.
 */
@Config(constants = BuildConfig.class, sdk = 18)
@RunWith(SpyglassRobolectricRunner.class)
public class MentionsEditTextTest {

    private MentionsEditText mEditText;
    private RichEditorView mRichEditor;

    @Before
    public void setUp() throws Exception {
        mEditText = spy(new MentionsEditText(RuntimeEnvironment.application));
        mEditText.setAvoidPrefixOnTap(true);
        mRichEditor = mock(RichEditorView.class);
        mEditText.setSuggestionsVisibilityManager(mRichEditor);
        mEditText.setTokenizer(new WordTokenizer());
    }

    @Test
    public void testOnTouchEvent() throws Exception {
        MotionEvent event = mock(MotionEvent.class);
        doReturn(null).when(mEditText).getTouchedSpan(event);
        doReturn(true).when(mRichEditor).isDisplayingSuggestions();

        // Test that the MentionsEditText does not avoid "" as a prefix
        // Note: After typing "@", the keyword string is "", so avoiding "" would mean avoiding all
        // explicit mentions (keyword string is what the user typed minus explicit characters)
        doReturn("").when(mEditText).getCurrentKeywordsString();
        mEditText.onTouchEvent(event);
        verify(mEditText, never()).setAvoidedPrefix("");

        // Test that the MentionsEditText avoids a prefix as long as it has length > 0
        doReturn("a").when(mEditText).getCurrentKeywordsString();
        mEditText.onTouchEvent(event);
        verify(mEditText).setAvoidedPrefix("a");
    }

    @Test
    public void testSelectionAtIndexZeroOnInit() {
        MentionsEditText editText = new MentionsEditText(RuntimeEnvironment.application);
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
