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

import android.text.Spanned;
import com.linkedin.android.spyglass.BuildConfig;
import com.linkedin.android.spyglass.mentions.MentionSpan;
import com.linkedin.android.spyglass.mentions.TestMention;
import com.linkedin.android.spyglass.ui.RichEditorView;
import com.linkedin.android.spyglass.ui.wrappers.RichEditorFragment;
import com.linkedin.android.utils.SpyglassRobolectricRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.linkedin.android.utils.SpyglassRobolectricRunner.startFragment;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@Config(constants = BuildConfig.class, sdk = 18)
@RunWith(SpyglassRobolectricRunner.class)
public class WordTokenizerTest {

    private RichEditorFragment mRichEditorFragment;
    private RichEditorView mRichEditor;
    private WordTokenizer mTokenizer;

    @Before
    public void setUp() throws Exception {
        // Setup the RichEditorView
        mRichEditorFragment = new RichEditorFragment();
        startFragment(mRichEditorFragment);
        mRichEditor = mRichEditorFragment.getRichEditor();
        mRichEditor.setText("Test mentions Shoulong Li and @Nathan Hi @Mi");
        mRichEditor.setSelection(0);

        // Configure the tokenizer with custom settings
        WordTokenizerConfig config = new WordTokenizerConfig.Builder()
                                                            .setThreshold(3)
                                                            .setMaxNumKeywords(2)
                                                            .build();
        mTokenizer = new WordTokenizer(config);
        mRichEditor.setTokenizer(mTokenizer);
    }

    @Test
    public void testFindTokenStart() throws Exception {

        // Cursor in first word, "Test"
        findTokenStartChecker(0, 0);
        findTokenStartChecker(1, 0);
        findTokenStartChecker(2, 0);
        findTokenStartChecker(3, 0);

        // Cursor at end of first word, space, and starting next word, "mentions"
        findTokenStartChecker(4, 0);
        findTokenStartChecker(5, 0);
        findTokenStartChecker(6, 0);

        // Cursor at end of second word, space, and starting third word, "Shoulong"
        findTokenStartChecker(13, 0);
        findTokenStartChecker(14, 5);
        findTokenStartChecker(15, 5);

        // Cursor at end of third word, space, and starting fourth word, "Li"
        findTokenStartChecker(22, 5);
        findTokenStartChecker(23, 14);
        findTokenStartChecker(24, 14);

        // Cursor at end of fourth word, space, and starting fifth word, "and"
        findTokenStartChecker(25, 14);
        findTokenStartChecker(26, 23);
        findTokenStartChecker(27, 23);

        // Set a test span in the editor around "Shoulong Li"
        setTestSpan("Shoulong Li", 14);

        // Cursor at end of fourth word, space, and starting fifth word, "and" (after span added around "Shoulong Li")
        findTokenStartChecker(25, 25);
        findTokenStartChecker(26, 26);
        findTokenStartChecker(27, 26);

        // Before first '@' character
        findTokenStartChecker(30, 26);

        // After first '@' character
        findTokenStartChecker(31, 30);
        findTokenStartChecker(38, 30);

        // Before second '@' character
        findTokenStartChecker(41, 38);

        // After second '@' character
        findTokenStartChecker(42, 41);
        findTokenStartChecker(43, 41);
    }

    private void findTokenStartChecker(int cursor, int expectedStartIndex) {
        mRichEditor.setSelection(cursor);
        assertEquals(expectedStartIndex, mTokenizer.findTokenStart(mRichEditor.getText(), cursor));
    }

    @Test
    public void testFindTokenEnd() throws Exception {

        // Cursor in first word, "Test"
        findTokenEndChecker(0, 4);
        findTokenEndChecker(1, 4);
        findTokenEndChecker(2, 4);
        findTokenEndChecker(3, 4);

        // Cursor at end of first word, space, and starting next word, "mentions"
        findTokenEndChecker(4, 4);
        findTokenEndChecker(5, 13);
        findTokenEndChecker(6, 13);

        // Cursor at end of second word, space, and starting third word, "Shoulong"
        findTokenEndChecker(13, 13);
        findTokenEndChecker(14, 22);
        findTokenEndChecker(15, 22);

        // Cursor at end of third word, space, and starting fourth word, "Li"
        findTokenEndChecker(22, 22);
        findTokenEndChecker(23, 25);
        findTokenEndChecker(24, 25);

        // Cursor at end of fourth word, space, and starting fifth word, "and"
        findTokenEndChecker(25, 25);
        findTokenEndChecker(26, 29);
        findTokenEndChecker(27, 29);

        // Set a test span in the editor around "Shoulong Li"
        setTestSpan("Shoulong Li", 14);

        // Cursor at end of first word, space, and starting next word, "mentions" (after adding span around "Shoulong Li")
        findTokenEndChecker(4, 4);
        findTokenEndChecker(5, 13);
        findTokenEndChecker(6, 13);

        // Cursor at end of second word, space, and starting third word, "Shoulong" (after adding span around "Shoulong Li")
        findTokenEndChecker(13, 13);
        findTokenEndChecker(14, 14);
        findTokenEndChecker(15, 22);

        // Before first '@' character
        findTokenEndChecker(30, 37);

        // After first '@' character
        findTokenEndChecker(31, 37);
        findTokenEndChecker(38, 40);

        // Before second '@' character
        findTokenEndChecker(41, 44);

        // After second '@' character
        findTokenEndChecker(42, 44);
        findTokenEndChecker(43, 44);
    }

    private void findTokenEndChecker(int cursor, int expectedEndIndex) {
        mRichEditor.setSelection(cursor);
        assertEquals(expectedEndIndex, mTokenizer.findTokenEnd(mRichEditor.getText(), cursor));
    }

    @Test
    public void testIsExplicit() throws Exception {
        CharSequence text = mRichEditor.getText();

        // Testing before the first '@' character
        assertFalse(mTokenizer.isExplicit(text, 0));
        assertFalse(mTokenizer.isExplicit(text, 5));
        assertFalse(mTokenizer.isExplicit(text, 6));
        assertFalse(mTokenizer.isExplicit(text, 14));
        assertFalse(mTokenizer.isExplicit(text, 15));
        assertFalse(mTokenizer.isExplicit(text, 30));

        // Testing after the first '@' character
        assertTrue(mTokenizer.isExplicit(text, 31));
        assertTrue(mTokenizer.isExplicit(text, 32));
        assertTrue(mTokenizer.isExplicit(text, 39));
        assertTrue(mTokenizer.isExplicit(text, 40));
        assertTrue(mTokenizer.isExplicit(text, 43));

        // Change text
        mRichEditor.setText("@Test mentions Shoulong Li and @Nathan Hi @Mi");
        text = mRichEditor.getText();

        // Explicit mention should occur earlier now
        assertFalse(mTokenizer.isExplicit(text, 0));
        assertTrue(mTokenizer.isExplicit(text, 1));
        assertTrue(mTokenizer.isExplicit(text, 2));
        assertTrue(mTokenizer.isExplicit(text, 5));
        assertTrue(mTokenizer.isExplicit(text, 6));
        assertTrue(mTokenizer.isExplicit(text, 7));
        assertTrue(mTokenizer.isExplicit(text, 14));
        assertFalse(mTokenizer.isExplicit(text, 15));
        assertFalse(mTokenizer.isExplicit(text, 16));
        assertFalse(mTokenizer.isExplicit(text, 27));

        // Set a test span in the editor around "Shoulong Li"
        setTestSpan("Shoulong Li", 14);

        // Testing after the span is added
        // Edge case: Ensure that tokenizer does not cross an added span and consider it explicit
        assertFalse(mTokenizer.isExplicit(text, 27));

        // Explicit mentions on a new line should work just fine
        text = "\n@";
        assertFalse(mTokenizer.isExplicit(text, 0));
        assertFalse(mTokenizer.isExplicit(text, 1));
        assertTrue(mTokenizer.isExplicit(text, 2));

        // Two consecutive explicit characters should not work when cursor is at the end, see MOB-37907
        text = "@ @@";
        assertFalse(mTokenizer.isExplicit(text, 0));
        assertTrue(mTokenizer.isExplicit(text, 1));
        assertFalse(mTokenizer.isExplicit(text, text.length()));

        // An explicit mention can have one space in it (but not two), see MOB-38287
        text = "@first second third";
        assertFalse(mTokenizer.isExplicit(text, 0));
        assertTrue(mTokenizer.isExplicit(text, 1));
        assertTrue(mTokenizer.isExplicit(text, 6));
        assertTrue(mTokenizer.isExplicit(text, 7));
        assertTrue(mTokenizer.isExplicit(text, 8));
        assertTrue(mTokenizer.isExplicit(text, 13));
        assertFalse(mTokenizer.isExplicit(text, 14));
        assertFalse(mTokenizer.isExplicit(text, 15));
    }

    @Test
    public void testGetStartIndex() throws Exception {
        setTestSpan("Shoulong Li", 14);
        Spanned text = mRichEditor.getText();

        // Run checks with cursor before span
        assertEquals(0, mTokenizer.getSearchStartIndex(text, 0));
        assertEquals(0, mTokenizer.getSearchStartIndex(text, 6));
        assertEquals(0, mTokenizer.getSearchStartIndex(text, 14));

        // Run checks with cursor after span
        assertEquals(25, mTokenizer.getSearchStartIndex(text, 26));
        assertEquals(25, mTokenizer.getSearchStartIndex(text, 30));
        assertEquals(25, mTokenizer.getSearchStartIndex(text, 35));
        assertEquals(25, mTokenizer.getSearchStartIndex(text, 40));
    }

    @Test
    public void testGetStartIndexWithMultipleLines() throws Exception {
        String ls = System.getProperty("line.separator");
        String textString = String.format("Test %smentions Shoulong Li a%snd @Nathan Hi %s@Mi", ls, ls, ls);
        mRichEditor.setText(textString);
        setTestSpan("Shoulong Li", 15);
        Spanned text = mRichEditor.getText();

        // Before first line separator
        assertEquals(0, mTokenizer.getSearchStartIndex(text, 0));
        assertEquals(0, mTokenizer.getSearchStartIndex(text, 5));

        // After first line separator
        assertEquals(6, mTokenizer.getSearchStartIndex(text, 6));
        assertEquals(6, mTokenizer.getSearchStartIndex(text, 14));
        assertEquals(6, mTokenizer.getSearchStartIndex(text, 15));

        // Before second line separator
        assertEquals(26, mTokenizer.getSearchStartIndex(text, 28));

        // After second line separator
        assertEquals(29, mTokenizer.getSearchStartIndex(text, 29));
        assertEquals(29, mTokenizer.getSearchStartIndex(text, 30));
        assertEquals(29, mTokenizer.getSearchStartIndex(text, 32));
        assertEquals(29, mTokenizer.getSearchStartIndex(text, 33));

        // Before third line separator
        assertEquals(29, mTokenizer.getSearchStartIndex(text, 43));

        // After third line separator
        assertEquals(44, mTokenizer.getSearchStartIndex(text, 44));
        assertEquals(44, mTokenizer.getSearchStartIndex(text, 45));
        assertEquals(44, mTokenizer.getSearchStartIndex(text, 47));
    }

    @Test
    public void testGetEndIndex() throws Exception {
        setTestSpan("Shoulong Li", 14);
        Spanned text = mRichEditor.getText();

        // Run checks with cursor before span
        assertEquals(14, mTokenizer.getSearchEndIndex(text, 0));
        assertEquals(14, mTokenizer.getSearchEndIndex(text, 6));
        assertEquals(14, mTokenizer.getSearchEndIndex(text, 14));

        // Run checks with cursor after span
        int len = text.length();
        assertEquals(len, mTokenizer.getSearchEndIndex(text, 26));
        assertEquals(len, mTokenizer.getSearchEndIndex(text, 30));
        assertEquals(len, mTokenizer.getSearchEndIndex(text, 35));
        assertEquals(len, mTokenizer.getSearchEndIndex(text, 40));
    }

    @Test
    public void testGetEndIndexWithMultipleLines() throws Exception {
        String ls = System.getProperty("line.separator");
        String textString = String.format("Test %smentions Shoulong Li a%snd @Nathan Hi %s@Mi", ls, ls, ls);
        mRichEditor.setText(textString);
        setTestSpan("Shoulong Li", 15);
        Spanned text = mRichEditor.getText();

        // Before first line separator
        assertEquals(5, mTokenizer.getSearchEndIndex(text, 0));
        assertEquals(5, mTokenizer.getSearchEndIndex(text, 5));

        // After first line separator
        assertEquals(15, mTokenizer.getSearchEndIndex(text, 6));
        assertEquals(15, mTokenizer.getSearchEndIndex(text, 14));
        assertEquals(15, mTokenizer.getSearchEndIndex(text, 15));

        // Before second line separator
        assertEquals(28, mTokenizer.getSearchEndIndex(text, 28));

        // After second line separator
        assertEquals(43, mTokenizer.getSearchEndIndex(text, 29));
        assertEquals(43, mTokenizer.getSearchEndIndex(text, 30));
        assertEquals(43, mTokenizer.getSearchEndIndex(text, 32));
        assertEquals(43, mTokenizer.getSearchEndIndex(text, 33));

        // Before third line separator
        assertEquals(43, mTokenizer.getSearchEndIndex(text, 43));

        // After third line separator
        assertEquals(47, mTokenizer.getSearchEndIndex(text, 44));
        assertEquals(47, mTokenizer.getSearchEndIndex(text, 45));
        assertEquals(47, mTokenizer.getSearchEndIndex(text, 47));
    }

    private void setTestSpan(String name, int start) throws Exception {
        // Set a test span in the editor around a person's name
        TestMention mention = new TestMention(name);
        MentionSpan span = new MentionSpan(mention);
        mRichEditor.getText().setSpan(span, start, start + mention.getSuggestiblePrimaryText().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Test
    public void testIsValidMention() {
        isValidMentionHelper(null, false);
        isValidMentionHelper("", false);
        isValidMentionHelper("ab ", false);
        isValidMentionHelper("  ", false);
        isValidMentionHelper("a b", false);
        isValidMentionHelper("a%b", false);
        isValidMentionHelper("@", true);
        isValidMentionHelper("@%b", false);
        isValidMentionHelper("mic", true);
        isValidMentionHelper("mic", true);
        isValidMentionHelper("@m", true);
        isValidMentionHelper("@_", false);
        isValidMentionHelper("@ ", false);
        isValidMentionHelper("@,", false);
        isValidMentionHelper("@%", false);
        isValidMentionHelper("Jon@", false);
    }

    private void isValidMentionHelper(String text, boolean expected) {
        mRichEditor.setText(text);
        int end = (text != null) ? text.length() : 0;
        mRichEditor.setSelection(end);
        assertEquals(expected, mTokenizer.isValidMention(mRichEditor.getText(), 0, end));
    }

    @Test
    public void testHasThreeCharactersOrDigits() {
        assertFalse(mTokenizer.onlyLettersOrDigits("123", 3, -1));
        assertTrue(mTokenizer.onlyLettersOrDigits("1b3", 3, 0));
        assertFalse(mTokenizer.onlyLettersOrDigits("1b3", 3, 1));
        assertFalse(mTokenizer.onlyLettersOrDigits("1b3", 3, 2));
        assertFalse(mTokenizer.onlyLettersOrDigits("1b3", 3, 4));
        assertFalse(mTokenizer.onlyLettersOrDigits("1 a", 3, 0));
        assertFalse(mTokenizer.onlyLettersOrDigits("1a ", 3, 0));
        assertFalse(mTokenizer.onlyLettersOrDigits(" 1a", 3, 1));
        assertFalse(mTokenizer.onlyLettersOrDigits("a'1", 3, 0));
        assertTrue(mTokenizer.onlyLettersOrDigits("Jon Caveman", 3, 4));
    }

    @Test
    public void testHasWordBreakingCharBeforeExplicitChar() throws Exception {
        // Input has explicit char, editText has space, should return true
        String text = "Hi @John Doe";
        hasWordBreakingCharBeforeExplicitCharHelper(text, true);

        // Input has explicit char, editText has no space, should return false
        text = "Hi@John Doe";
        hasWordBreakingCharBeforeExplicitCharHelper(text, false);

        // Input does not have explicit char (not explicit), editText has no space, return false
        text = "John Doe";
        hasWordBreakingCharBeforeExplicitCharHelper(text, false);
    }

    private void hasWordBreakingCharBeforeExplicitCharHelper(String text, boolean expected) {
        mRichEditor.setText(text);
        mRichEditor.setSelection(text.length());
        boolean result = mTokenizer.hasWordBreakingCharBeforeExplicitChar(mRichEditor.getText(), text.length());
        assertEquals(expected, result);
    }

}
