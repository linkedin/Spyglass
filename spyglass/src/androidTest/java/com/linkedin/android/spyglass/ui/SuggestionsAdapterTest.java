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

import android.support.test.InstrumentationRegistry;
import com.linkedin.android.spyglass.mentions.TestMention;
import com.linkedin.android.spyglass.suggestions.SuggestionsAdapter;
import com.linkedin.android.spyglass.suggestions.SuggestionsResult;
import com.linkedin.android.spyglass.suggestions.interfaces.Suggestible;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class SuggestionsAdapterTest {
    private RichEditorView mRichEditor;
    private SuggestionsAdapter mAdapter;

    @Before
    public void setUp() throws Exception {
        mRichEditor = new RichEditorView(InstrumentationRegistry.getTargetContext());
        mAdapter = mRichEditor.mSuggestionsAdapter;
    }

    @Test
    public void testAddSuggestions() throws Exception {
        // Add a single TestMention
        TestMention testMention1 = new TestMention("FirstName LastName");
        addMentionToEditor(testMention1, "First", "Person");
        assertEquals(1, mAdapter.getCount());

        // Add another TestMention with different keyword, ensure adapter only has second test mention
        TestMention testMention2 = new TestMention("Second Name");
        addMentionToEditor(testMention2, "Second", "Group");
        assertEquals(1, mAdapter.getCount());

        // Add another TestMention with same keyword, ensure that adapter has both mentions
        TestMention testMention3 = new TestMention("SecondName LastName");
        addMentionToEditor(testMention3, "Second", "Person");
        assertEquals(2, mAdapter.getCount());
    }

    private void addMentionToEditor(Suggestible mention, String typedText, String bucket) throws Exception {
        QueryToken query = new QueryToken(typedText);
        mRichEditor.setText(typedText);
        List<String> buckets = Arrays.asList(bucket);
        mAdapter.notifyQueryTokenReceived(query, buckets);
        ArrayList<Suggestible> mentions = new ArrayList<>();
        mentions.add(mention);
        SuggestionsResult queryResult = new SuggestionsResult(query, mentions);
        mAdapter.addSuggestions(queryResult, bucket, mRichEditor.mMentionsEditText);
    }

}
