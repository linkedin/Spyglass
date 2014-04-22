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
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.linkedin.android.spyglass.mentions.MentionSpan;
import com.linkedin.android.spyglass.mentions.Mentionable;
import com.linkedin.android.spyglass.mentions.MentionsEditable;
import com.linkedin.android.spyglass.suggestions.interfaces.SuggestionsVisibilityManager;
import com.linkedin.android.spyglass.tokenization.QueryToken;
import com.linkedin.android.spyglass.tokenization.interfaces.QueryTokenReceiver;
import com.linkedin.android.spyglass.tokenization.interfaces.TokenSource;
import com.linkedin.android.spyglass.tokenization.interfaces.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that overrides {@link EditText} in order to have more control over touch events and selection ranges for use in
 * the {@link RichEditorView}.
 */
public class MentionsEditText extends EditText implements TokenSource {

    private Tokenizer mTokenizer;
    private QueryTokenReceiver mQueryTokenReceiver;
    private SuggestionsVisibilityManager mSuggestionsVisibilityManager;

    private List<TextWatcher> mExternalTextWatchers;
    private final MyWatcher mInternalTextWatcher = new MyWatcher();
    private final Object mTextWatcherLock = new Object();
    private boolean mIsWatchingText = false;
    private boolean mAvoidPrefixOnTap = false;
    private String mAvoidedPrefix;

    public MentionsEditText(Context context) {
        super(context);
        init();
    }

    public MentionsEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MentionsEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Initialization method called by all constructors.
     */
    private void init() {
        // Must set movement method in order for MentionSpans to be clickable
        setMovementMethod(MentionsMovementMethod.getInstance());

        // Use MentionsEditable instead of default Editable
        setEditableFactory(MentionsEditableFactory.getInstance());

        // Start watching itself for text changes
        addTextChangedListener(mInternalTextWatcher);
    }

    // --------------------------------------------------
    // TokenSource Interface Implementation
    // --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String getCurrentTokenString() {
        // Get the text and ensure a valid tokenizer is set
        Editable text = getText();
        if (mTokenizer == null || text == null) {
            return "";
        }

        // Use current text to determine token string
        int cursor = Math.max(getSelectionStart(), 0);
        int start = mTokenizer.findTokenStart(text, cursor);
        int end = mTokenizer.findTokenEnd(text, cursor);
        String contentString = text.toString();
        return TextUtils.isEmpty(contentString) ? "" : contentString.substring(start, end);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public QueryToken getQueryTokenIfValid() {

        if (mTokenizer == null) {
            return null;
        }

        // Use current text to determine the start and end index of the token
        MentionsEditable text = getMentionsText();
        int cursor = Math.max(getSelectionStart(), 0);
        int start = mTokenizer.findTokenStart(text, cursor);
        int end = mTokenizer.findTokenEnd(text, cursor);

        if (!mTokenizer.isValidMention(text, start, end)) {
            return null;
        }
        String tokenString = text.subSequence(start, end).toString();
        char firstChar = tokenString.charAt(0);
        boolean isExplicit = mTokenizer.isExplicitChar(tokenString.charAt(0));
        return (isExplicit ? new QueryToken(tokenString, firstChar) : new QueryToken(tokenString));
    }

    // --------------------------------------------------
    // Touch Event Methods
    // --------------------------------------------------

    /**
     * Called whenever the user touches this {@link EditText}. This was one of the primary reasons for overriding
     * EditText in this library. This method ensures that when a user taps on a {@link MentionSpan} in this EditText,
     * {@link MentionSpan#onClick(View)} is called before the onClick method of this {@link EditText}.
     *
     * @param event the given {@link MotionEvent}
     *
     * @return true if the {@link MotionEvent} has been handled
     */
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        // If user tapped a span
        MentionSpan touchedSpan = getTouchedSpan(event);
        if (touchedSpan != null) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Manually click span and show soft keyboard
                touchedSpan.onClick(this);
                Context context = getContext();
                if (context != null) {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(this, 0);
                }
            }
            return true;
        }

        // Check if user clicked on the EditText while showing the suggestions list
        // If so, avoid the current prefix
        if (mAvoidPrefixOnTap
                && mSuggestionsVisibilityManager != null
                && mSuggestionsVisibilityManager.isDisplayingSuggestions()) {
            mSuggestionsVisibilityManager.displaySuggestions(false);
            String keywords = getCurrentKeywordsString();
            String[] words = keywords.split(" ");
            if (words.length > 0) {
                String prefix = words[words.length - 1];
                // Note that prefix == "" when user types an explicit character and taps the EditText
                // We must not allow the user to avoid suggestions for the empty string prefix
                // Otherwise, explicit mentions would be broken, see MOB-38080
                if (prefix.length() > 0) {
                    setAvoidedPrefix(prefix);
                }
            }
        }

        return super.onTouchEvent(event);
    }

    /**
     * Gets the {@link MentionSpan} from the {@link MentionsEditText} that was tapped.
     * <p/>
     * Note: Almost all of this code is taken directly from the Android source code, see:
     * {@link LinkMovementMethod#onTouchEvent(TextView, Spannable, MotionEvent)}
     *
     * @param event the given (@link MotionEvent}
     *
     * @return the tapped {@link MentionSpan}, or null if one was not tapped
     */
    @Nullable
    protected MentionSpan getTouchedSpan(MotionEvent event) {
        Layout layout = getLayout();
        // Note: Layout can be null if text or width has recently changed, see MOB-38193
        if (event == null || layout == null) {
            return null;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        x -= getTotalPaddingLeft();
        y -= getTotalPaddingTop();

        x += getScrollX();
        y += getScrollY();

        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);
        Editable text = getText();
        if (text != null && off >= getText().length()) {
            return null;
        }

        // Get the MentionSpans in the area that the user tapped
        // If one exists, call the onClick method manually
        MentionSpan[] spans = getText().getSpans(off, off, MentionSpan.class);
        if (spans.length > 0) {
            MentionSpan span = spans[0];
            // Do not return the span if user typed at the end of it
            // (user may have tapped much further away in the EditText)
            if (getText().getSpanEnd(span) == off) {
                return null;
            } else {
                return span;
            }
        }

        return null;
    }

    // --------------------------------------------------
    // Cursor & Selection Event Methods
    // --------------------------------------------------

    /**
     * Called whenever the selection within the {@link EditText} has changed.
     *
     * @param selStart starting position of the selection
     * @param selEnd   ending position of the selection
     */
    @Override
    protected void onSelectionChanged(final int selStart, final int selEnd) {

        // Handle case where there is only one cursor (i.e. not selecting a range, just moving cursor)
        if (selStart == selEnd) {
            if (!onCursorChanged(selStart)) {
                super.onSelectionChanged(selStart, selEnd);
            }
            return;
        }

        super.onSelectionChanged(selStart, selEnd);
    }

    /**
     * Method to handle the cursor changing positions. Returns true if handled, false if it should
     * be passed to the super method.
     *
     * @param index int position of cursor within the text
     *
     * @return true if handled
     */
    private boolean onCursorChanged(final int index) {
        Editable text = getText();
        if (text == null) {
            return false;
        }

        // Deselect any spans if the cursor is not at the end of it
        MentionSpan[] allSpans = text.getSpans(0, text.length(), MentionSpan.class);
        for (MentionSpan span : allSpans) {
            if (span.isSelected() && text.getSpanEnd(span) != index) {
                span.setSelected(false);
                updateSpan(span);
            }
        }

        // Do not allow the user to set the cursor within a span. If the user tries to do so, select
        // move the cursor to the end of it.
        MentionSpan[] currentSpans = text.getSpans(index, index, MentionSpan.class);
        if (currentSpans.length != 0) {
            MentionSpan span = currentSpans[0];
            int start = text.getSpanStart(span);
            int end = text.getSpanEnd(span);
            if (index > start && index < end) {
                super.setSelection(end);
                return true;
            }
        }

        return false;
    }

    // --------------------------------------------------
    // TextWatcher Implementation
    // --------------------------------------------------

    private class MyWatcher implements TextWatcher {

        /**
         * {@inheritDoc}
         */
        @Override
        public void beforeTextChanged(CharSequence text, int start, int before, int after) {

            // Mark a span for deletion later if necessary
            markSpans(start, before, after);

            // Call any watchers for text changes
            sendBeforeTextChanged(text, start, before, after);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onTextChanged(CharSequence text, int start, int before, int after) {
            // Call any watchers for text changes
            sendOnTextChanged(text, start, before, after);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void afterTextChanged(Editable text) {
            if (text == null) {
                return;
            }

            synchronized (mTextWatcherLock) {
                // Detach the TextWatcher to prevent infinite loops
                // (i.e. changing text here would call afterTextChanged again)
                detachTextWatcher();
                // Handle the change in text
                // We can freely modify the text here
                handleTextChanged(text);
                // Allow class to listen for changes to the text again
                attachTextWatcher();
            }

            // Call any watchers for text changes after we have handled it
            sendAfterTextChanged(text);
        }

    }

    /**
     * Notify external text watchers that the text is about to change.
     * See {@link TextWatcher#beforeTextChanged(CharSequence, int, int, int)}.
     */
    private void sendBeforeTextChanged(CharSequence text, int start, int before, int after) {
        if (mExternalTextWatchers != null) {
            final List<TextWatcher> list = mExternalTextWatchers;
            final int count = list.size();
            for (int i = 0; i < count; i++) {
                TextWatcher watcher = list.get(i);
                if (watcher != this) {
                    watcher.beforeTextChanged(text, start, before, after);
                }
            }
        }
    }

    /**
     * Notify external text watchers that the text is changing.
     * See {@link TextWatcher#onTextChanged(CharSequence, int, int, int)}.
     */
    private void sendOnTextChanged(CharSequence text, int start, int before, int after) {
        if (mExternalTextWatchers != null) {
            final List<TextWatcher> list = mExternalTextWatchers;
            final int count = list.size();
            for (int i = 0; i < count; i++) {
                TextWatcher watcher = list.get(i);
                if (watcher != this) {
                    watcher.onTextChanged(text, start, before, after);
                }
            }
        }
    }

    /**
     * Notify external text watchers that the text has changed.
     * See {@link TextWatcher#afterTextChanged(Editable)}.
     */
    private void sendAfterTextChanged(Editable text) {
        if (mExternalTextWatchers != null) {
            final List<TextWatcher> list = mExternalTextWatchers;
            final int count = list.size();
            for (int i = 0; i < count; i++) {
                TextWatcher watcher = list.get(i);
                if (watcher != this) {
                    watcher.afterTextChanged(text);
                }
            }
        }
    }

    /**
     * Marks a span for deletion later if necessary by checking if the last character in a MentionSpan
     * is deleted by this change. If so, mark the span to be deleted later when
     * {@link #ensureMentionSpanIntegrity(Editable)} is called in {@link #handleTextChanged(Editable)}.
     *
     * @param start int index in text where change begins
     * @param count length of affected text before change starting at start in text
     * @param after length of affected text after change
     */
    private void markSpans(int start, int count, int after) {

        int cursor = getSelectionStart();
        MentionsEditable text = getMentionsText();
        MentionSpan prevSpan = text.getMentionSpanEndingAt(cursor);

        if (count == (after + 1) && prevSpan != null) {

            // Cursor was directly behind a span and was moved back one, so delete it if selected,
            // or select it if not already selected
            if (prevSpan.isSelected()) {
                Mentionable mention = prevSpan.getMention();
                Mentionable.MentionDeleteStyle deleteStyle = mention.getDeleteStyle();
                Mentionable.MentionDisplayMode displayMode = prevSpan.getDisplayMode();
                // Determine new DisplayMode given previous DisplayMode and MentionDeleteStyle
                if (deleteStyle == Mentionable.MentionDeleteStyle.PARTIAL_NAME_DELETE
                        && displayMode == Mentionable.MentionDisplayMode.FULL) {
                    prevSpan.setDisplayMode(Mentionable.MentionDisplayMode.PARTIAL);
                } else {
                    prevSpan.setDisplayMode(Mentionable.MentionDisplayMode.NONE);
                }
            } else {
                // Span was not selected, so select it
                prevSpan.setSelected(true);
            }
        }
    }

    /**
     * Called after the {@link Editable} text within the {@link EditText} has been changed. Note that
     * editing text in this function is guaranteed to be safe and not cause an infinite loop.
     *
     * @param text the {@link Editable} that has changed
     */
    private void handleTextChanged(Editable text) {

        // Ensure that the text in all the MentionSpans remains unchanged
        ensureMentionSpanIntegrity(text);

        // Ignore requests if the last word in keywords is prefixed by the currently avoided prefix
        if (mAvoidedPrefix != null) {
            String[] keywords = getCurrentKeywordsString().split(" ");
            // Add null and length check to avoid the ArrayIndexOutOfBoundsException
            if (keywords.length == 0) {
                return;
            }
            String lastKeyword = keywords[keywords.length - 1];
            if (lastKeyword.startsWith(mAvoidedPrefix)) {
                return;
            } else {
                setAvoidedPrefix(null);
            }
        }

        // Request suggestions from the QueryClient
        QueryToken queryToken = getQueryTokenIfValid();
        if (queryToken != null && mQueryTokenReceiver != null) {
            // Valid token, so send query to the app for processing
            mQueryTokenReceiver.onQueryReceived(queryToken);
        } else {
            // Ensure that the suggestions are hidden
            if (mSuggestionsVisibilityManager != null) {
                mSuggestionsVisibilityManager.displaySuggestions(false);
            }
        }
    }

    /**
     * Ensures that the text within each {@link MentionSpan} in the {@link Editable} correctly
     * matches what it should be outputting. If not, replace it with the correct value.
     *
     * @param text the {@link Editable} to examine
     */
    private void ensureMentionSpanIntegrity(Editable text) {
        if (text == null) {
            return;
        }

        MentionSpan[] spans = text.getSpans(0, text.length(), MentionSpan.class);
        boolean spanAltered = false;
        for (MentionSpan span : spans) {
            int start = text.getSpanStart(span);
            int end = text.getSpanEnd(span);
            CharSequence spanText = text.subSequence(start, end).toString();
            Mentionable.MentionDisplayMode displayMode = span.getDisplayMode();

            switch (displayMode) {

                case PARTIAL:
                case FULL:
                    String name = span.getDisplayString();
                    if (!name.equals(spanText) && start >= 0 && start < end && end <= text.length()) {
                        // Mention display name does not match what is being shown,
                        // replace text in span with proper display name
                        text.removeSpan(span);
                        text.replace(start, end, name);
                        if (name.length() > 0) {
                            text.setSpan(span, start, start + name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        setSelection(start + name.length());
                        spanAltered = true;
                    }
                    break;

                case NONE:
                default:
                    // Mention with DisplayMode == NONE should be deleted from the text
                    text.delete(start, end);
                    setSelection(start);
                    spanAltered = true;
                    break;

            }
        }

        // Reset input method if spans have been changed (updates suggestions)
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && spanAltered) {
            imm.restartInput(this);
        }
    }

    /**
     * Attaches internal text watcher to intercept changes.
     */
    private void attachTextWatcher() {
        addTextChangedListener(mInternalTextWatcher);
    }

    /**
     * Removes internal text watcher.
     */
    private void detachTextWatcher() {
        removeTextChangedListener(mInternalTextWatcher);
    }

    // --------------------------------------------------
    // Public Methods
    // --------------------------------------------------

    /**
     * Gets the keywords that the {@link Tokenizer} is currently considering for mention suggestions. Note that this is
     * the keywords string and will not include any explicit character, if present.
     *
     * @return a String representing current keywords in the {@link EditText}
     */
    @NonNull
    public String getCurrentKeywordsString() {
        String keywordsString = getCurrentTokenString();
        if (keywordsString.length() > 0 && mTokenizer.isExplicitChar(keywordsString.charAt(0))) {
            keywordsString = keywordsString.substring(1);
        }
        return keywordsString;
    }

    /**
     * Resets the given {@link MentionSpan} in the editor, forcing it to redraw with its latest drawable state.
     *
     * @param span the {@link MentionSpan} to update
     */
    public void updateSpan(MentionSpan span) {
        detachTextWatcher();
        Editable text = getText();
        int start = text.getSpanStart(span);
        int end = text.getSpanEnd(span);
        if (start >= 0 && end > start && end <= text.length()) {
            text.removeSpan(span);
            text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        attachTextWatcher();
    }

    /**
     * Deselects any spans in the editor that are currently selected.
     */
    public void deselectAllSpans() {
        detachTextWatcher();
        Editable text = getText();
        MentionSpan[] spans = text.getSpans(0, text.length(), MentionSpan.class);
        for (MentionSpan span : spans) {
            if (span.isSelected()) {
                span.setSelected(false);
                updateSpan(span);
            }
        }
        attachTextWatcher();
    }

    /**
     * Inserts a mention into the token being considered currently.
     *
     * @param mention {@link Mentionable} to insert a span for
     */
    public void insertMention(Mentionable mention) {

        if (mTokenizer == null) {
            return;
        }

        // Setup variables and ensure they are valid
        Editable text = getEditableText();
        int cursor = getSelectionStart();
        int start = mTokenizer.findTokenStart(text, cursor);
        int end = mTokenizer.findTokenEnd(text, cursor);
        if (start < 0 || start >= end || end > text.length()) {
            return;
        }

        // Must ensure that the starting index to insert the span matches the name of the mention if implicit
        // Note: If explicit, then do not change the start index (must replace the explicit character)

        if (!isCurrentlyExplicit()) {
            int initialStart = start;
            String tokenString = getCurrentTokenString().toLowerCase();
            String[] tsNames = tokenString.split(" ");
            String[] mentionNames = mention.getPrimaryText().split(" ");
            String firstName = mentionNames[0].toLowerCase();
            for (String tsName : tsNames) {
                if (firstName.startsWith(tsName)) {
                    start = initialStart + tokenString.indexOf(tsName);
                    break;
                }
            }
        }

        // Insert the span into the editor
        MentionSpan mentionSpan = new MentionSpan(getContext(), mention);
        String name = mention.getPrimaryText();

        detachTextWatcher();
        text.replace(start, end, name);
        text.setSpan(mentionSpan, start, start + name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Selection.setSelection(text, start + name.length());
        ensureMentionSpanIntegrity(text);
        attachTextWatcher();

        // Hide the suggestions and clear adapter
        if (mSuggestionsVisibilityManager != null) {
            mSuggestionsVisibilityManager.displaySuggestions(false);
        }

        // Reset input method since text has been changed (updates mention draw states)
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.restartInput(this);
        }
    }

    /**
     * Determines if the {@link Tokenizer} is looking at an explicit token right now.
     *
     * @return true if the {@link Tokenizer} is currently considering an explicit query
     */
    public boolean isCurrentlyExplicit() {
        String tokenString = getCurrentTokenString();
        return tokenString.length() > 0 && mTokenizer != null && mTokenizer.isExplicitChar(tokenString.charAt(0));
    }

    /**
     * Allows a class to watch for text changes. Note that adding this class to itself will add it
     * to the super class. Other instances of {@link TextWatcher} will be notified by this class
     * as appropriate (helps prevent infinite loops when text keeps changing).
     *
     * @param watcher the {@link TextWatcher} to add
     */
    @Override
    public void addTextChangedListener(TextWatcher watcher) {
        if (watcher == mInternalTextWatcher) {
            if (!mIsWatchingText) {
                super.addTextChangedListener(mInternalTextWatcher);
                mIsWatchingText = true;
            }
        } else {
            if (mExternalTextWatchers == null) {
                mExternalTextWatchers = new ArrayList<>();
            }
            mExternalTextWatchers.add(watcher);
        }
    }

    /**
     * Removes a {@link TextWatcher} from this class. Note that this function servers as the
     * counterpart to {@link #addTextChangedListener(TextWatcher)}).
     *
     * @param watcher the {@link TextWatcher} to remove
     */
    @Override
    public void removeTextChangedListener(TextWatcher watcher) {
        if (watcher == mInternalTextWatcher) {
            if (mIsWatchingText) {
                super.removeTextChangedListener(mInternalTextWatcher);
                mIsWatchingText = false;
            }
        } else {
            // Other watchers are added
            if (mExternalTextWatchers != null) {
                int i = mExternalTextWatchers.indexOf(watcher);
                if (i >= 0) {
                    mExternalTextWatchers.remove(i);
                }
            }
        }

    }

    // --------------------------------------------------
    // MentionsEditable Factory
    // --------------------------------------------------

    /**
     * Custom EditableFactory designed so that we can use the customized {@link MentionsEditable} in place of the
     * default {@link Editable}.
     */
    public static class MentionsEditableFactory extends Editable.Factory {

        private static MentionsEditableFactory sInstance = new MentionsEditableFactory();

        @NonNull
        public static MentionsEditableFactory getInstance() {
            return sInstance;
        }

        @Override
        public Editable newEditable(CharSequence source) {
            return new MentionsEditable(source);
        }
    }

    // --------------------------------------------------
    // MentionsMovementMethod Class
    // --------------------------------------------------

    /**
     * Custom {@link MovementMethod} for this class used to override specific behavior in {@link ArrowKeyMovementMethod}.
     */
    public static class MentionsMovementMethod extends ArrowKeyMovementMethod {

        private static MentionsMovementMethod sInstance;

        public static MovementMethod getInstance() {
            if (sInstance == null)
                sInstance = new MentionsMovementMethod();

            return sInstance;
        }

        @Override
        public void initialize(TextView widget, Spannable text) {
            // Do nothing. Note that ArrowKeyMovementMethod calls setSelection(0) here, but we would
            // like to override that behavior (otherwise, cursor is set to beginning of EditText when
            // this method is called).
        }

    }

    // --------------------------------------------------
    // Getters & Setters
    // --------------------------------------------------

    /**
     * Convenience method for getting the {@link MentionsEditable} associated with this class.
     *
     * @return the current text as a {@link MentionsEditable} specifically
     */
    @NonNull
    public MentionsEditable getMentionsText() {
        return (MentionsEditable) super.getText();
    }

    /**
     * @return the {@link Tokenizer} in use
     */
    @Nullable
    public Tokenizer getTokenizer() {
        return mTokenizer;
    }

    /**
     * Sets the tokenizer used by this class. The tokenizer determines how {@link QueryToken} objects
     * are generated.
     *
     * @param tokenizer the {@link Tokenizer} to use
     */
    public void setTokenizer(final @Nullable Tokenizer tokenizer) {
        mTokenizer = tokenizer;
    }

    /**
     * Sets the receiver of query tokens used by this class. The query token receiver will use the
     * tokens to generate suggestions, which can then be inserted back into this edit text.
     *
     * @param queryTokenReceiver the {@link QueryTokenReceiver} to use
     */
    public void setQueryTokenReceiver(final @Nullable QueryTokenReceiver queryTokenReceiver) {
        mQueryTokenReceiver = queryTokenReceiver;
    }

    /**
     * Sets the suggestions manager used by this class.
     *
     * @param suggestionsVisibilityManager the {@link SuggestionsVisibilityManager} to use
     */
    public void setSuggestionsVisibilityManager(final @Nullable SuggestionsVisibilityManager suggestionsVisibilityManager) {
        mSuggestionsVisibilityManager = suggestionsVisibilityManager;
    }

    /**
     * Sets the string prefix to avoid creating and displaying suggestions.
     *
     * @param prefix prefix to avoid suggestions
     */
    public void setAvoidedPrefix(String prefix) {
        mAvoidedPrefix = prefix;
    }

    /**
     * Determines whether the edit text should avoid the current prefix if the user taps on it while
     * it is displaying suggestions (defaults to false).
     *
     * @param avoidPrefixOnTap true if the prefix should be avoided after a tap
     */
    public void setAvoidPrefixOnTap(boolean avoidPrefixOnTap) {
        mAvoidPrefixOnTap = avoidPrefixOnTap;
    }

}
