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
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.linkedin.android.spyglass.R;
import com.linkedin.android.spyglass.mentions.MentionSpan;
import com.linkedin.android.spyglass.mentions.Mentionable;
import com.linkedin.android.spyglass.mentions.MentionsEditable;
import com.linkedin.android.spyglass.suggestions.SuggestionsAdapter;
import com.linkedin.android.spyglass.suggestions.SuggestionsResult;
import com.linkedin.android.spyglass.suggestions.impl.BasicSuggestionsListBuilder;
import com.linkedin.android.spyglass.suggestions.interfaces.OnSuggestionsVisibilityChangeListener;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsListBuilder;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsResultListener;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsVisibilityManager;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizer;
import com.linkedin.android.spyglass.tokenization.impl.WordTokenizerConfig;
import com.linkedin.android.spyglass.tokenization.interfaces.QueryTokenReceiver;
import com.linkedin.android.spyglass.tokenization.interfaces.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom view for the RichEditor. Manages three subviews:
 * <p>
 * 1. EditText - contains text typed by user
 * 2. TextView - displays count of the number of characters in the EditText
 * 3. ListView - displays mention suggestions when relevant
 */
public class RichEditorView extends RelativeLayout implements TextWatcher, QueryTokenReceiver, SuggestionsResultListener, SuggestionsVisibilityManager {

    private MentionsEditText mMentionsEditText;
    private int mOriginalInputType = InputType.TYPE_CLASS_TEXT; // Default to plain text
    private TextView mTextCounterView;
    private ListView mSuggestionsList;

    private QueryTokenReceiver mHostQueryTokenReceiver;
    private SuggestionsAdapter mSuggestionsAdapter;
    private OnSuggestionsVisibilityChangeListener mActionListener;

    private ViewGroup.LayoutParams mPrevEditTextParams;
    private boolean mEditTextShouldWrapContent = false; // Default to match parent in height
    private int mPrevEditTextBottomPadding;

    private int mTextCountLimit = -1;
    private int mWithinCountLimitTextColor = Color.BLACK;
    private int mBeyondCountLimitTextColor = Color.RED;

    private boolean mWaitingForFirstResult = false;

    // --------------------------------------------------
    // Constructors & Initialization
    // --------------------------------------------------

    public RichEditorView(Context context) {
        super(context);
        init(context, null);
    }

    public RichEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public void init(Context context, AttributeSet attrs) {

        // Inflate view from XML layout file
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.editor_view, this, true);

        // Get the views for the fragment
        mMentionsEditText = (MentionsEditText) findViewById(R.id.text_editor);
        mTextCounterView = (TextView) findViewById(R.id.text_counter);
        mSuggestionsList = (ListView) findViewById(R.id.suggestions_list);

        // Create the tokenizer to use for the editor
        // TODO: Allow customization of configuration via XML attributes
        WordTokenizerConfig config = new WordTokenizerConfig.Builder().build();
        WordTokenizer tokenizer = new WordTokenizer(config);
        mMentionsEditText.setTokenizer(tokenizer);

        // Set various delegates on the MentionEditText to the RichEditorView
        mMentionsEditText.setSuggestionsVisibilityManager(this);
        mMentionsEditText.addTextChangedListener(this);
        mMentionsEditText.setQueryTokenReceiver(this);
        mMentionsEditText.setAvoidPrefixOnTap(true);

        // Set the suggestions adapter
        SuggestionsListBuilder listBuilder = new BasicSuggestionsListBuilder();
        mSuggestionsAdapter = new SuggestionsAdapter(context, this, listBuilder);
        mSuggestionsList.setAdapter(mSuggestionsAdapter);

        // Set the item click listener
        mSuggestionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Mentionable mention = (Mentionable) mSuggestionsAdapter.getItem(position);
                if (mMentionsEditText != null) {
                    mMentionsEditText.insertMention(mention);
                    mSuggestionsAdapter.clear();
                }
            }
        });

        // Display and update the editor text counter (starts it at 0)
        updateEditorTextCount();

        // Wrap the EditText content height if necessary (ideally, allow this to be controlled via custom XML attribute)
        setEditTextShouldWrapContent(mEditTextShouldWrapContent);
        mPrevEditTextBottomPadding = mMentionsEditText.getPaddingBottom();
    }

    // --------------------------------------------------
    // Public Span & UI Methods
    // --------------------------------------------------

	/**
	 * Allows filters in the input element.
	 *
	 * Example: obj.setInputFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
	 *
	 * @param filters
	 */
    public void setInputFilters(InputFilter... filters) {
		mMentionsEditText.setFilters(filters);

    }

    /**
     * @return a list of {@link MentionSpan} objects currently in the editor
     */
    @NonNull
    public List<MentionSpan> getMentionSpans() {
        return (mMentionsEditText != null) ? mMentionsEditText.getMentionsText().getMentionSpans() : new ArrayList<MentionSpan>();
    }

    /**
     * Determine whether the internal {@link EditText} should match the full height of the {@link RichEditorView}
     * initially or if it should wrap its content in height and expand to fill it as the user types.
     * <p>
     * Note: The {@link EditText} will always match the parent (i.e. the {@link RichEditorView} in width.
     * Additionally, the {@link ListView} containing mention suggestions will always fill the rest
     * of the height in the {@link RichEditorView}.
     *
     * @param enabled true if the {@link EditText} should wrap its content in height
     */
    public void setEditTextShouldWrapContent(boolean enabled) {
        mEditTextShouldWrapContent = enabled;
        if (mMentionsEditText == null) {
            return;
        }
        mPrevEditTextParams = mMentionsEditText.getLayoutParams();
        int wrap = (enabled) ? LayoutParams.WRAP_CONTENT : LayoutParams.MATCH_PARENT;
        if (mPrevEditTextParams != null && mPrevEditTextParams.height == wrap) {
            return;
        }
        ViewGroup.LayoutParams newParams = new LayoutParams(LayoutParams.MATCH_PARENT, wrap);
        mMentionsEditText.setLayoutParams(newParams);
        requestLayout();
        invalidate();
    }

    /**
     * @return current line number of the cursor, or -1 if no cursor
     */
    public int getCurrentCursorLine() {
        int selectionStart = mMentionsEditText.getSelectionStart();
        Layout layout = mMentionsEditText.getLayout();
        if (layout != null && !(selectionStart == -1)) {
            return layout.getLineForOffset(selectionStart);
        }
        return -1;
    }

    /**
     * Show or hide the text counter view.
     *
     * @param display true to display the text counter view
     */
    public void displayTextCounter(boolean display) {
        if (display) {
            mTextCounterView.setVisibility(TextView.VISIBLE);
        } else {
            mTextCounterView.setVisibility(TextView.GONE);
        }
    }

    /**
     * @return true if the text counter view is currently visible to the user
     */
    public boolean isDisplayingTextCounter() {
        return mTextCounterView != null && mTextCounterView.getVisibility() == TextView.VISIBLE;
    }

    // --------------------------------------------------
    // TextWatcher Implementation
    // --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTextChanged(Editable s) {
        updateEditorTextCount();
    }

    // --------------------------------------------------
    // QueryTokenReceiver Implementation
    // --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> onQueryReceived(@NonNull QueryToken queryToken) {
        List<String> buckets = null;
        // Pass the query token to a host receiver
        if (mHostQueryTokenReceiver != null) {
            buckets = mHostQueryTokenReceiver.onQueryReceived(queryToken);
            mSuggestionsAdapter.notifyQueryTokenReceived(queryToken, buckets);
        }
        return buckets;
    }


    // --------------------------------------------------
    // SuggestionsResultListener Implementation
    // --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceiveSuggestionsResult(final @NonNull SuggestionsResult result, final @NonNull String bucket) {
        // Add the mentions and notify the editor/dropdown of the changes on the UI thread
        post(new Runnable() {
            @Override
            public void run() {
                if (mSuggestionsAdapter != null) {
                    mSuggestionsAdapter.addSuggestions(result, bucket, mMentionsEditText);
                }
                // Make sure the list is scrolled to the top once you receive the first query result
                if (mWaitingForFirstResult && mSuggestionsList != null) {
                    mSuggestionsList.setSelection(0);
                    mWaitingForFirstResult = false;
                }
            }
        });
    }

    // --------------------------------------------------
    // SuggestionsManager Implementation
    // --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void displaySuggestions(boolean display) {

        // If nothing to change, return early
        if (display == isDisplayingSuggestions() || mMentionsEditText == null) {
            return;
        }

        // Change view depending on whether suggestions are being shown or not
        if (display) {
            disableSpellingSuggestions(true);
            mTextCounterView.setVisibility(View.GONE);
            mSuggestionsList.setVisibility(View.VISIBLE);
            mPrevEditTextParams = mMentionsEditText.getLayoutParams();
            mPrevEditTextBottomPadding = mMentionsEditText.getPaddingBottom();
            mMentionsEditText.setPadding(mMentionsEditText.getPaddingLeft(), mMentionsEditText.getPaddingTop(), mMentionsEditText.getPaddingRight(), mMentionsEditText.getPaddingTop());
            int height = mMentionsEditText.getPaddingTop() + mMentionsEditText.getLineHeight() + mMentionsEditText.getPaddingBottom();
            mMentionsEditText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, height));
            mMentionsEditText.setVerticalScrollBarEnabled(false);
            int cursorLine = getCurrentCursorLine();
            Layout layout = mMentionsEditText.getLayout();
            if (layout != null) {
                int lineTop = layout.getLineTop(cursorLine);
                mMentionsEditText.scrollTo(0, lineTop);
            }
            // Notify action listener that list was shown
            if (mActionListener != null) {
                mActionListener.onSuggestionsDisplayed();
            }
        } else {
            disableSpellingSuggestions(false);
            mTextCounterView.setVisibility(View.VISIBLE);
            mSuggestionsList.setVisibility(View.GONE);
            mMentionsEditText.setPadding(mMentionsEditText.getPaddingLeft(), mMentionsEditText.getPaddingTop(), mMentionsEditText.getPaddingRight(), mPrevEditTextBottomPadding);
            if (mPrevEditTextParams == null) {
                mPrevEditTextParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            }
            mMentionsEditText.setLayoutParams(mPrevEditTextParams);
            mMentionsEditText.setVerticalScrollBarEnabled(true);
            // Notify action listener that list was hidden
            if (mActionListener != null) {
                mActionListener.onSuggestionsHidden();
            }
        }

        requestLayout();
        invalidate();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDisplayingSuggestions() {
        return mSuggestionsList.getVisibility() == View.VISIBLE;
    }

    /**
     * Disables spelling suggestions from the user's keyboard.
     * This is necessary because some keyboards will replace the input text with
     * spelling suggestions automatically, which changes the suggestion results.
     * This results in a confusing user experience.
     *
     * @param disable {@code true} if spelling suggestions should be disabled, otherwise {@code false}
     */
    private void disableSpellingSuggestions(boolean disable) {
        // toggling suggestions often resets the cursor location, but we don't want that to happen
        int start = mMentionsEditText.getSelectionStart();
        int end = mMentionsEditText.getSelectionEnd();
        // -1 means there is no selection or cursor.
        if (start == -1 || end == -1) {
            return;
        }
        if (disable) {
            // store the previous input type
            mOriginalInputType = mMentionsEditText.getInputType();
        }
        mMentionsEditText.setRawInputType(disable ? InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS : mOriginalInputType);
        mMentionsEditText.setSelection(start, end);
    }

    // --------------------------------------------------
    // Private Methods
    // --------------------------------------------------

    /**
     * Updates the TextView counting the number of characters in the editor. Sets not only the content
     * of the TextView, but also the color of the text depending if the limit has been reached.
     */
    private void updateEditorTextCount() {
        if (mMentionsEditText != null && mTextCounterView != null) {
            int textCount = mMentionsEditText.getMentionsText().length();
            mTextCounterView.setText(String.valueOf(textCount));

            if (mTextCountLimit > 0 && textCount > mTextCountLimit) {
                mTextCounterView.setTextColor(mBeyondCountLimitTextColor);
            } else {
                mTextCounterView.setTextColor(mWithinCountLimitTextColor);
            }
        }
    }

    // --------------------------------------------------
    // Pass-Through Methods to the MentionsEditText
    // --------------------------------------------------

    /**
     * Convenience method for {@link MentionsEditText#getCurrentTokenString()}.
     *
     * @return a string representing currently being considered for a possible query, as the user typed it
     */
    @NonNull
    public String getCurrentTokenString() {
        if (mMentionsEditText == null) {
            return "";
        }
        return mMentionsEditText.getCurrentTokenString();
    }

    /**
     * Convenience method for {@link MentionsEditText#getCurrentKeywordsString()}.
     *
     * @return a String representing current keywords in the underlying {@link EditText}
     */
    @NonNull
    public String getCurrentKeywordsString() {
        if (mMentionsEditText == null) {
            return "";
        }
        return mMentionsEditText.getCurrentKeywordsString();
    }

    /**
     * Resets the given {@link MentionSpan} in the editor, forcing it to redraw with its latest drawable state.
     *
     * @param span the {@link MentionSpan} to update
     */
    public void updateSpan(MentionSpan span) {
        if (mMentionsEditText != null) {
            mMentionsEditText.updateSpan(span);
        }
    }

    /**
     * Deselects any spans in the editor that are currently selected.
     */
    public void deselectAllSpans() {
        if (mMentionsEditText != null) {
            mMentionsEditText.deselectAllSpans();
        }
    }

    /**
     * Adds an {@link TextWatcher} to the internal {@link MentionsEditText}.
     *
     * @param hostTextWatcher the {TextWatcher} to add
     */
    public void addTextChangedListener(final @Nullable TextWatcher hostTextWatcher) {
        if (mMentionsEditText != null) {
            mMentionsEditText.addTextChangedListener(hostTextWatcher);
        }
    }

    /**
     * @return the {@link MentionsEditable} within the embedded {@link MentionsEditText}
     */
    @NonNull
    public MentionsEditable getText() {
        return (mMentionsEditText != null) ? ((MentionsEditable) mMentionsEditText.getText()) : new MentionsEditable("");
    }

    /**
     * @return the {@link Tokenizer} in use
     */
    @Nullable
    public Tokenizer getTokenizer() {
        return (mMentionsEditText != null) ? mMentionsEditText.getTokenizer() : null;
    }

    /**
     * Sets the text being displayed within the {@link RichEditorView}. Note that this removes the
     * {@link TextWatcher} temporarily to avoid changing the text while listening to text changes
     * (which could result in an infinite loop).
     *
     * @param text the text to display
     */
    public void setText(final @NonNull CharSequence text) {
        if (mMentionsEditText != null) {
            mMentionsEditText.setText(text);
        }
    }

    /**
     * Sets the text hint to use within the embedded {@link MentionsEditText}.
     *
     * @param hint the text hint to use
     */
    public void setHint(final @NonNull CharSequence hint) {
        if (mMentionsEditText != null) {
            mMentionsEditText.setHint(hint);
        }
    }

    /**
     * Sets the input type of the embedded {@link MentionsEditText}.
     *
     * @param type the input type of the {@link MentionsEditText}
     */
    public void setInputType(final int type) {
        if (mMentionsEditText != null) {
            mMentionsEditText.setInputType(type);
        }
    }

    /**
     * Sets the selection within the embedded {@link MentionsEditText}.
     *
     * @param index the index of the selection within the embedded {@link MentionsEditText}
     */
    public void setSelection(final int index) {
        if (mMentionsEditText != null) {
            mMentionsEditText.setSelection(index);
        }
    }

    /**
     * Sets the {@link Tokenizer} for the {@link MentionsEditText} to use.
     *
     * @param tokenizer the {@link Tokenizer} to use
     */
    public void setTokenizer(final @NonNull Tokenizer tokenizer) {
        if (mMentionsEditText != null) {
            mMentionsEditText.setTokenizer(tokenizer);
        }
    }

    // --------------------------------------------------
    // RichEditorView-specific Setters
    // --------------------------------------------------

    /**
     * Sets the limit on the maximum number of characters allowed to be entered into the
     * {@link MentionsEditText} before the text counter changes color.
     *
     * @param limit the maximum number of characters allowed before the text counter changes color
     */
    public void setTextCountLimit(final int limit) {
        mTextCountLimit = limit;
    }

    /**
     * Sets the color of the text within the text counter while the user has entered fewer than the
     * limit of characters.
     *
     * @param color the color of the text within the text counter below the limit
     */
    public void setWithinCountLimitTextColor(final int color) {
        mWithinCountLimitTextColor = color;
    }

    /**
     * Sets the color of the text within the text counter while the user has entered more text than
     * the current limit.
     *
     * @param color the color of the text within the text counter beyond the limit
     */
    public void setBeyondCountLimitTextColor(final int color) {
        mBeyondCountLimitTextColor = color;
    }

    /**
     * Sets the receiver of any tokens generated by the embedded {@link MentionsEditText}. The
     * receive should act on the queries as they are received and call
     * {@link #onReceiveSuggestionsResult(SuggestionsResult, String)} once the suggestions are ready.
     *
     * @param client the object that can receive {@link QueryToken} objects and generate suggestions from them
     */
    public void setQueryTokenReceiver(final @Nullable QueryTokenReceiver client) {
        mHostQueryTokenReceiver = client;
    }

    /**
     * Sets a listener for anyone interested in specific actions of the {@link RichEditorView}.
     *
     * @param listener the object that wants to listen to specific actions of the {@link RichEditorView}
     */
    public void setOnRichEditorActionListener(final @Nullable OnSuggestionsVisibilityChangeListener listener) {
        mActionListener = listener;
    }

    /**
     * Sets the {@link com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsVisibilityManager} to use (determines which and how the suggestions are displayed).
     *
     * @param suggestionsVisibilityManager the {@link com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsVisibilityManager} to use
     */
    public void setSuggestionsManager(final @NonNull SuggestionsVisibilityManager suggestionsVisibilityManager) {
        if (mMentionsEditText != null && mSuggestionsAdapter != null) {
            mMentionsEditText.setSuggestionsVisibilityManager(suggestionsVisibilityManager);
            mSuggestionsAdapter.setSuggestionsManager(suggestionsVisibilityManager);
        }
    }

    /**
     * Sets the {@link SuggestionsListBuilder} to use.
     *
     * @param suggestionsListBuilder the {@link SuggestionsListBuilder} to use
     */
    public void setSuggestionsListBuilder(final @NonNull SuggestionsListBuilder suggestionsListBuilder) {
        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter.setSuggestionsListBuilder(suggestionsListBuilder);
        }
    }

}
