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

package com.linkedin.android.spyglass.suggestions;

import com.linkedin.android.spyglass.BuildConfig;
import com.linkedin.android.spyglass.mentions.TestMention;
import com.linkedin.android.spyglass.suggestions.interfaces.Suggestible;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import com.linkedin.android.spyglass.ui.MentionsEditText;
import com.linkedin.android.spyglass.ui.RichEditorView;
import com.linkedin.android.spyglass.ui.wrappers.RichEditorFragment;
import com.linkedin.android.utils.SpyglassRobolectricRunner;
import com.linkedin.android.utils.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.linkedin.android.utils.SpyglassRobolectricRunner.startFragment;
import static junit.framework.Assert.assertEquals;

@Config(constants = BuildConfig.class, sdk = 18)
@RunWith(SpyglassRobolectricRunner.class)
public class SuggestionsAdapterTest {

    private RichEditorFragment mRichEditorFragment;
    private RichEditorView mRichEditor;
    private SuggestionsAdapter mAdapter;

    @Before
    public void setUp() throws Exception {
        mRichEditorFragment = new RichEditorFragment();
        startFragment(mRichEditorFragment);
        mRichEditor = mRichEditorFragment.getRichEditor();
        mAdapter = TestUtils.getPrivateField(mRichEditor, "mSuggestionsAdapter");
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
        MentionsEditText mentionsEditText = TestUtils.getPrivateField(mRichEditor, "mMentionsEditText");
        mAdapter.addSuggestions(queryResult, bucket, mentionsEditText);
    }

}
