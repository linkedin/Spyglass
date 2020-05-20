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

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.spyglass.R;
import com.linkedin.android.spyglass.mentions.MentionSpan;
import com.linkedin.android.spyglass.mentions.MentionSpanConfig;
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
 * <p/>
 * <b>XML attributes</b>
 * <p/>
 * See {@link R.styleable#MentionsEditText Attributes}
 *
 * @attr ref R.styleable#MentionsEditText_mentionTextColor
 * @attr ref R.styleable#MentionsEditText_mentionTextBackgroundColor
 * @attr ref R.styleable#MentionsEditText_selectedMentionTextColor
 * @attr ref R.styleable#MentionsEditText_selectedMentionTextBackgroundColor
 */
public class MentionsEditText extends EditText implements TokenSource {

    private static final String KEY_MENTION_SPANS = "mention_spans";
    private static final String KEY_MENTION_SPAN_STARTS = "mention_span_starts";

    private Tokenizer mTokenizer;
    private QueryTokenReceiver mQueryTokenReceiver;
    private SuggestionsVisibilityManager mSuggestionsVisibilityManager;

    private List<MentionWatcher> mMentionWatchers = new ArrayList<>();
    private List<TextWatcher> mExternalTextWatchers = new ArrayList<>();
    private final MyWatcher mInternalTextWatcher = new MyWatcher();
    private boolean mBlockCompletion = false;
    private boolean mIsWatchingText = false;
    private boolean mAvoidPrefixOnTap = false;
    @Nullable private String mAvoidedPrefix;

    private MentionSpanFactory mentionSpanFactory;
    private MentionSpanConfig mentionSpanConfig;
    private boolean isLongPressed;
    private CheckLongClickRunnable longClickRunnable;

    public MentionsEditText(@NonNull Context context) {
        super(context);
        init(null, 0);
    }

    public MentionsEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public MentionsEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Initialization method called by all constructors.
     */
    private void init(@Nullable AttributeSet attrs, int defStyleAttr) {
        // Get the mention span config from custom attributes
        mentionSpanConfig = parseMentionSpanConfigFromAttributes(attrs, defStyleAttr);

        // Must set movement method in order for MentionSpans to be clickable
        setMovementMethod(MentionsMovementMethod.getInstance());

        // Use MentionsEditable instead of default Editable
        setEditableFactory(MentionsEditableFactory.getInstance());

        // Start watching itself for text changes
        addTextChangedListener(mInternalTextWatcher);

        // Use default MentionSpanFactory initially
        mentionSpanFactory = new MentionSpanFactory();
    }

    private MentionSpanConfig parseMentionSpanConfigFromAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
        final Context context = getContext();
        MentionSpanConfig.Builder builder = new MentionSpanConfig.Builder();
        if (attrs == null) {
            return builder.build();
        }

        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MentionsEditText, defStyleAttr, 0);
        @ColorInt int normalTextColor = attributes.getColor(R.styleable.MentionsEditText_mentionTextColor, -1);
        builder.setMentionTextColor(normalTextColor);
        @ColorInt int normalBgColor = attributes.getColor(R.styleable.MentionsEditText_mentionTextBackgroundColor, -1);
        builder.setMentionTextBackgroundColor(normalBgColor);
        @ColorInt int selectedTextColor = attributes.getColor(R.styleable.MentionsEditText_selectedMentionTextColor, -1);
        builder.setSelectedMentionTextColor(selectedTextColor);
        @ColorInt int selectedBgColor = attributes.getColor(R.styleable.MentionsEditText_selectedMentionTextBackgroundColor, -1);
        builder.setSelectedMentionTextBackgroundColor(selectedBgColor);

        attributes.recycle();

        return builder.build();
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
        final MentionSpan touchedSpan = getTouchedSpan(event);

        // Android 6 occasionally throws a NullPointerException inside Editor.onTouchEvent()
        // for ACTION_UP when attempting to display (uninitialised) text handles.
        boolean superResult;
        if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.M &&
            event.getActionMasked() == MotionEvent.ACTION_UP) {
            try {
                superResult = super.onTouchEvent(event);
            } catch (NullPointerException ignored) {
                // Ignore this (see above) - since we're now in an unknown state let's clear all
                // selection (which is still better than an arbitrary crash that we can't control):
                clearFocus();
                superResult = true;
            }
        } else {
            superResult = super.onTouchEvent(event);
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            // Don't call the onclick on mention if MotionEvent.ACTION_UP is for long click action,
            if (!isLongPressed && touchedSpan != null) {
                // Manually click span and show soft keyboard
                touchedSpan.onClick(this);
                Context context = getContext();
                if (context != null) {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(this, 0);
                }
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            isLongPressed = false;
            if (isLongClickable() && touchedSpan != null) {
                if (longClickRunnable == null) {
                    longClickRunnable = new CheckLongClickRunnable();
                }
                longClickRunnable.touchedSpan = touchedSpan;
                removeCallbacks(longClickRunnable);
                postDelayed(longClickRunnable, ViewConfiguration.getLongPressTimeout());
            }
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            isLongPressed = false;
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
        return superResult;
    }

    @Override
    public boolean onTextContextMenuItem(@MenuRes int id) {
        MentionsEditable text = getMentionsText();
        int min = Math.max(0, getSelectionStart());
        int selectionEnd = getSelectionEnd();
        int max = selectionEnd >= 0 ? selectionEnd : text.length();
        // Ensuring that min is always less than or equal to max.
        min = Math.min(min, max);
        switch (id) {
            case android.R.id.cut:
                // First copy the span and then remove it from the current EditText
                copy(min, max);
                MentionSpan[] span = text.getSpans(min, max, MentionSpan.class);
                for (MentionSpan mentionSpan : span) {
                    text.removeSpan(mentionSpan);
                }
                text.delete(min, max);
                return true;
            case android.R.id.copy:
                copy(min, max);
                return true;
            case android.R.id.paste:
                paste(min, max);
                return true;
            default:
                return super.onTextContextMenuItem(id);
        }
    }

    /**
     * Copy the text between start and end in clipboard.
     * If no span is present, text is saved as plain text but if span is present
     * save it in Clipboard using intent.
     */
    private void copy(@IntRange(from = 0) int start, @IntRange(from = 0) int end) {
        MentionsEditable text = getMentionsText();
        SpannableStringBuilder copiedText = (SpannableStringBuilder) text.subSequence(start, end);
        MentionSpan[] spans = text.getSpans(start, end, MentionSpan.class);
        Intent intent = null;
        if (spans.length > 0) {
            // Save MentionSpan and it's start offset.
            intent = new Intent();
            intent.putExtra(KEY_MENTION_SPANS, spans);
            int[] spanStart = new int[spans.length];
            for (int i = 0; i < spans.length; i++) {
                spanStart[i] = copiedText.getSpanStart(spans[i]);
            }
            intent.putExtra(KEY_MENTION_SPAN_STARTS, spanStart);
        }
        saveToClipboard(copiedText, intent);
    }

    /**
     * Paste clipboard content between min and max positions.
     * If clipboard content contain the MentionSpan, set the span in copied text.
     */
    private void paste(@IntRange(from = 0) int min, @IntRange(from = 0) int max) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                ClipData.Item item = clip.getItemAt(i);
                String selectedText = item.coerceToText(getContext()).toString();
                MentionsEditable text = getMentionsText();
                MentionSpan[] spans = text.getSpans(min, max, MentionSpan.class);
                /*
                 * We need to remove the span between min and max. This is required because in
                 * {@link SpannableStringBuilder#replace(int, int, CharSequence)} existing spans within
                 * the Editable that entirely cover the replaced range are retained, but any that
                 * were strictly within the range that was replaced are removed. In our case the existing
                 * spans are retained if the selection entirely covers the span. So, we just remove
                 * the existing span and replace the new text with that span.
                 */
                for (MentionSpan span : spans) {
                    if (text.getSpanEnd(span) == min) {
                        // We do not want to remove the span, when we want to paste anything just next
                        // to the existing span. In this case "text.getSpanEnd(span)" will be equal
                        // to min.
                        continue;
                    }
                    text.removeSpan(span);
                }

                Intent intent = item.getIntent();
                // Just set the plain text if we do not have mentions data in the intent/bundle
                if (intent == null) {
                    text.replace(min, max, selectedText);
                    continue;
                }
                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    text.replace(min, max, selectedText);
                    continue;
                }
                bundle.setClassLoader(getContext().getClassLoader());
                int[] spanStart = bundle.getIntArray(KEY_MENTION_SPAN_STARTS);
                Parcelable[] parcelables = bundle.getParcelableArray(KEY_MENTION_SPANS);
                if (parcelables == null || parcelables.length <= 0 || spanStart == null || spanStart.length <= 0) {
                    text.replace(min, max, selectedText);
                    continue;
                }

                // Set the MentionSpan in text.
                SpannableStringBuilder s = new SpannableStringBuilder(selectedText);
                for (int j = 0; j < parcelables.length; j++) {
                    MentionSpan mentionSpan = (MentionSpan) parcelables[j];
                    s.setSpan(mentionSpan, spanStart[j], spanStart[j] + mentionSpan.getDisplayString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                text.replace(min, max, s);
            }
        }
    }

    /**
     * Save the selected text and intent in ClipboardManager
     */
    private void saveToClipboard(@NonNull CharSequence selectedText, @Nullable Intent intent) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData.Item item = new ClipData.Item(selectedText, intent, null);
        ClipData clip = new ClipData(null, new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
        clipboard.setPrimaryClip(clip);
    }

    /**
     * Gets the {@link MentionSpan} from the {@link MentionsEditText} that was tapped.
     * <p>
     * Note: Almost all of this code is taken directly from the Android source code, see:
     * {@link LinkMovementMethod#onTouchEvent(TextView, Spannable, MotionEvent)}
     *
     * @param event the given (@link MotionEvent}
     *
     * @return the tapped {@link MentionSpan}, or null if one was not tapped
     */
    @Nullable
    protected MentionSpan getTouchedSpan(@NonNull MotionEvent event) {
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
            return spans[0];
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
        } else {
            updateSelectionIfRequired(selStart, selEnd);
        }
        super.onSelectionChanged(selStart, selEnd);
    }

    /**
     * Don't allow user to set starting position or ending position of selection within the mention.
     */
    private void updateSelectionIfRequired(final int selStart, final int selEnd) {
        MentionsEditable text = getMentionsText();
        MentionSpan startMentionSpan = text.getMentionSpanAtOffset(selStart);
        MentionSpan endMentionSpan = text.getMentionSpanAtOffset(selEnd);
        boolean selChanged = false;
        int start = selStart;
        int end = selEnd;
        if (text.getSpanStart(startMentionSpan) < selStart && selStart < text.getSpanEnd(startMentionSpan)) {
            start = text.getSpanStart(startMentionSpan);
            selChanged = true;
        }
        if (text.getSpanStart(endMentionSpan) < selEnd && selEnd < text.getSpanEnd(endMentionSpan)) {
            end = text.getSpanEnd(endMentionSpan);
            selChanged = true;
        }
        if (selChanged) {
            setSelection(start, end);
        }
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

        MentionSpan[] allSpans = text.getSpans(0, text.length(), MentionSpan.class);
        for (MentionSpan span : allSpans) {
            // Deselect span if the cursor is not on the span.
            if (span.isSelected() && (index < text.getSpanStart(span) || index > text.getSpanEnd(span))) {
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
            if (mBlockCompletion) {
                return;
            }

            // Mark a span for deletion later if necessary
            boolean changed = markSpans(before, after);

            // If necessary, temporarily remove any MentionSpans that could potentially interfere with composing text
            if (!changed) {
                replaceMentionSpansWithPlaceholdersAsNecessary(text);
            }

            // Call any watchers for text changes
            sendBeforeTextChanged(text, start, before, after);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
            if (mBlockCompletion || !(text instanceof Editable) || getTokenizer() == null) {
                return;
            }

            // If the editor tries to insert duplicated text, mark the duplicated text for deletion later
            Editable editable = (Editable) text;
            int index = Selection.getSelectionStart(editable);
            Tokenizer tokenizer = getTokenizer();
            if (tokenizer != null) {
                markDuplicatedTextForDeletionLater((Editable) text, index, tokenizer);
            }

            // Call any watchers for text changes
            sendOnTextChanged(text, start, before, count);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void afterTextChanged(Editable text) {
            if (mBlockCompletion || text == null) {
                return;
            }

            // Block text change handling while we're changing the text (otherwise, may cause infinite loop)
            mBlockCompletion = true;

            // Text may have been marked to be removed in (before/on)TextChanged, remove that text now
            removeTextWithinDeleteSpans(text);

            // Some mentions may have been replaced by placeholders temporarily when altering the text, reinsert the
            // mention spans now
            replacePlaceholdersWithCorrespondingMentionSpans(text);

            // Ensure that the text in all the MentionSpans remains unchanged and valid
            ensureMentionSpanIntegrity(text);

            // Handle the change in text (can modify it freely here)
            handleTextChanged();

            // Allow class to listen for changes to the text again
            mBlockCompletion = false;

            // Call any watchers for text changes after we have handled it
            sendAfterTextChanged(text);
        }

        /**
         * Notify external text watchers that the text is about to change.
         * See {@link TextWatcher#beforeTextChanged(CharSequence, int, int, int)}.
         */
        private void sendBeforeTextChanged(CharSequence text, int start, int before, int after) {
            final List<TextWatcher> list = mExternalTextWatchers;
            final int count = list.size();
            for (int i = 0; i < count; i++) {
                TextWatcher watcher = list.get(i);
                // Self check to avoid infinite loop
                if (watcher != this) {
                    watcher.beforeTextChanged(text, start, before, after);
                }
            }
        }

        /**
         * Notify external text watchers that the text is changing.
         * See {@link TextWatcher#onTextChanged(CharSequence, int, int, int)}.
         */
        private void sendOnTextChanged(CharSequence text, int start, int before, int after) {
            final List<TextWatcher> list = mExternalTextWatchers;
            final int count = list.size();
            for (int i = 0; i < count; i++) {
                TextWatcher watcher = list.get(i);
                // Self check to avoid infinite loop
                if (watcher != this) {
                    watcher.onTextChanged(text, start, before, after);
                }
            }
        }

        /**
         * Notify external text watchers that the text has changed.
         * See {@link TextWatcher#afterTextChanged(Editable)}.
         */
        private void sendAfterTextChanged(Editable text) {
            final List<TextWatcher> list = mExternalTextWatchers;
            final int count = list.size();
            for (int i = 0; i < count; i++) {
                TextWatcher watcher = list.get(i);
                // Self check to avoid infinite loop
                if (watcher != this) {
                    watcher.afterTextChanged(text);
                }
            }
        }
    }

    /**
     * Marks a span for deletion later if necessary by checking if the last character in a MentionSpan
     * is deleted by this change. If so, mark the span to be deleted later when
     * {@link #ensureMentionSpanIntegrity(Editable)} is called in {@link #handleTextChanged()}.
     *
     * @param count length of affected text before change starting at start in text
     * @param after length of affected text after change
     *
     * @return  true if there is a span before the cursor that is going to change state
     */
    private boolean markSpans(int count, int after) {
        int cursor = getSelectionStart();
        MentionsEditable text = getMentionsText();
        MentionSpan prevSpan = text.getMentionSpanEndingAt(cursor);
        boolean isNeedToMarkSpan = (count == (after + 1) || after == 0) && prevSpan != null;
        if (isNeedToMarkSpan) {

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

            return true;
        }

        return false;
    }

    /**
     * Temporarily remove MentionSpans that may interfere with composing text. Note that software keyboards are allowed
     * to place arbitrary spans over the text. This was resulting in several bugs in edge cases while handling the
     * MentionSpans while composing text (with different issues for different keyboards). The easiest solution for this
     * is to remove any MentionSpans that could cause issues while the user is changing text.
     *
     * Note: The MentionSpans are added again in {@link #replacePlaceholdersWithCorrespondingMentionSpans(Editable)}
     *
     * @param text the current text before it changes
     */
    private void replaceMentionSpansWithPlaceholdersAsNecessary(@NonNull CharSequence text) {
        int index = getSelectionStart();
        int wordStart = findStartOfWord(text, index);
        Editable editable = getText();
        MentionSpan[] mentionSpansInCurrentWord = editable.getSpans(wordStart, index, MentionSpan.class);
        for (MentionSpan span : mentionSpansInCurrentWord) {
            if (span.getDisplayMode() != Mentionable.MentionDisplayMode.NONE) {
                int spanStart = editable.getSpanStart(span);
                int spanEnd = editable.getSpanEnd(span);
                editable.setSpan(new PlaceholderSpan(span, spanStart, spanEnd),
                        spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                editable.removeSpan(span);
            }
        }
    }

    /**
     * Helper utility to determine the beginning of a word using the current tokenizer.
     *
     * @param text  the text to examine
     * @param index index of the cursor in the text
     * @return  index of the beginning of the word in text (will be less than or equal to index)
     */
    private int findStartOfWord(@NonNull CharSequence text, int index) {
        int wordStart = index;
        while (wordStart > 0 && mTokenizer != null && !mTokenizer.isWordBreakingChar(text.charAt(wordStart - 1))) {
            wordStart--;
        }
        return wordStart;
    }

    /**
     * Mark text that was duplicated during text composition to delete it later.
     *
     * @param text          the given text
     * @param cursor        the index of the cursor in text
     * @param tokenizer     the {@link Tokenizer} to use
     */
    private void markDuplicatedTextForDeletionLater(@NonNull Editable text, int cursor, @NonNull Tokenizer tokenizer) {
        while (cursor > 0 && tokenizer.isWordBreakingChar(text.charAt(cursor - 1))) {
            cursor--;
        }
        int wordStart = findStartOfWord(text, cursor);
        PlaceholderSpan[] placeholderSpans = text.getSpans(wordStart, wordStart + 1, PlaceholderSpan.class);
        for (PlaceholderSpan span : placeholderSpans) {
            int spanEnd = span.originalEnd;
            int copyEnd = spanEnd + (spanEnd - wordStart);
            if (copyEnd > spanEnd && copyEnd <= text.length()) {
                CharSequence endOfMention = text.subSequence(wordStart, spanEnd);
                CharSequence copyOfEndOfMentionText = text.subSequence(spanEnd, copyEnd);
                // Note: Comparing strings since we do not want to compare any other aspects of spanned strings
                if (endOfMention.toString().equals(copyOfEndOfMentionText.toString())) {
                    text.setSpan(new DeleteSpan(),
                            spanEnd,
                            copyEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    /**
     * Removes any {@link com.linkedin.android.spyglass.ui.MentionsEditText.DeleteSpan}s and the text within them from
     * the given text.
     *
     * @param text the editable containing DeleteSpans to remove
     */
    private void removeTextWithinDeleteSpans(@NonNull Editable text) {
        DeleteSpan[] deleteSpans = text.getSpans(0, text.length(), DeleteSpan.class);
        for (DeleteSpan span : deleteSpans) {
            int spanStart = text.getSpanStart(span);
            int spanEnd = text.getSpanEnd(span);
            text.replace(spanStart, spanEnd, "");
            text.removeSpan(span);
        }
    }

    /**
     * Replaces any {@link com.linkedin.android.spyglass.ui.MentionsEditText.PlaceholderSpan} within the given text with
     * the {@link MentionSpan} it contains.
     *
     * Note: These PlaceholderSpans are added in {@link #replaceMentionSpansWithPlaceholdersAsNecessary(CharSequence)}
     *
     * @param text the final version of the text after it was changed
     */
    private void replacePlaceholdersWithCorrespondingMentionSpans(@NonNull Editable text) {
        PlaceholderSpan[] tempSpans = text.getSpans(0, text.length(), PlaceholderSpan.class);
        for (PlaceholderSpan span : tempSpans) {
            int spanStart = text.getSpanStart(span);
            String mentionDisplayString = span.holder.getDisplayString();
            int end = Math.min(spanStart + mentionDisplayString.length(), text.length());
            text.replace(spanStart, end, mentionDisplayString);
            text.setSpan(span.holder, spanStart, spanStart + mentionDisplayString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.removeSpan(span);
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
                    if (!name.contentEquals(spanText) && start >= 0 && start < end && end <= text.length()) {
                        // Mention display name does not match what is being shown,
                        // replace text in span with proper display name
                        int cursor = getSelectionStart();
                        int diff = cursor - end;
                        text.removeSpan(span);
                        text.replace(start, end, name);
                        if (diff > 0 && start + end + diff < text.length()) {
                            text.replace(start + end, start + end + diff, "");
                        }
                        if (name.length() > 0) {
                            text.setSpan(span, start, start + name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        // Notify for partially deleted mentions.
                        if (mMentionWatchers.size() > 0 && displayMode == Mentionable.MentionDisplayMode.PARTIAL) {
                            notifyMentionPartiallyDeletedWatchers(span.getMention(), name, start, end);
                        }
                        spanAltered = true;
                    }
                    break;

                case NONE:
                default:
                    // Mention with DisplayMode == NONE should be deleted from the text
                    boolean hasListeners = mMentionWatchers.size() > 0;
                    final String textBeforeDelete = hasListeners ? text.toString() : null;
                    text.delete(start, end);
                    setSelection(start);
                    if (hasListeners) {
                        notifyMentionDeletedWatchers(span.getMention(), textBeforeDelete, start, end);
                    }
                    spanAltered = true;
                    break;
            }
        }

        // Reset input method if spans have been changed (updates suggestions)
        if (spanAltered) {
            restartInput();
        }
    }

    /**
     * Called after the {@link Editable} text within the {@link EditText} has been changed. Note that
     * editing text in this function is guaranteed to be safe and not cause an infinite loop.
     */
    private void handleTextChanged() {
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
    public void updateSpan(@NonNull MentionSpan span) {
        mBlockCompletion = true;
        Editable text = getText();
        int start = text.getSpanStart(span);
        int end = text.getSpanEnd(span);
        if (start >= 0 && end > start && end <= text.length()) {
            text.removeSpan(span);
            text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        mBlockCompletion = false;
    }

    /**
     * Deselects any spans in the editor that are currently selected.
     */
    public void deselectAllSpans() {
        mBlockCompletion = true;
        Editable text = getText();
        MentionSpan[] spans = text.getSpans(0, text.length(), MentionSpan.class);
        for (MentionSpan span : spans) {
            if (span.isSelected()) {
                span.setSelected(false);
                updateSpan(span);
            }
        }
        mBlockCompletion = false;
    }

    /**
     * Inserts a mention into the token being considered currently.
     *
     * @param mention {@link Mentionable} to insert a span for
     */
    public void insertMention(@NonNull Mentionable mention) {
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

        insertMentionInternal(mention, text, start, end);
    }

    /**
     * Inserts a mention. This will not take any token into consideration. This method is useful
     * when you want to insert a mention which doesn't have a token.
     *
     * @param mention {@link Mentionable} to insert a span for
     */
    public void insertMentionWithoutToken(@NonNull Mentionable mention) {
        // Setup variables and ensure they are valid
        Editable text = getEditableText();
        int index = getSelectionStart();
        index = index > 0 ? index : 0;

        insertMentionInternal(mention, text, index, index);
    }

    private void insertMentionInternal(@NonNull Mentionable mention, @NonNull Editable text, int start, int end) {
        // Insert the span into the editor
        MentionSpan mentionSpan = mentionSpanFactory.createMentionSpan(mention, mentionSpanConfig);
        String name = mention.getSuggestiblePrimaryText();

        mBlockCompletion = true;
        text.replace(start, end, name);
        int endOfMention = start + name.length();
        text.setSpan(mentionSpan, start, endOfMention, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Selection.setSelection(text, endOfMention);
        ensureMentionSpanIntegrity(text);
        mBlockCompletion = false;

        // Notify listeners of added mention
        if (mMentionWatchers.size() > 0) {
            notifyMentionAddedWatchers(mention, text.toString(), start, endOfMention);
        }

        // Hide the suggestions and clear adapter
        if (mSuggestionsVisibilityManager != null) {
            mSuggestionsVisibilityManager.displaySuggestions(false);
        }

        // Reset input method since text has been changed (updates mention draw states)
        restartInput();
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
     * Populate an {@link AccessibilityEvent} with information about this text view. Note that this implementation uses
     * a copy of the text that is explicitly not an instance of {@link MentionsEditable}. This is due to the fact that
     * AccessibilityEvent will use the default system classloader when unparcelling the data within the event. This
     * results in a ClassNotFoundException. For more details, see: https://github.com/linkedin/Spyglass/issues/10
     *
     * @param event the populated AccessibilityEvent
     */
    @Override
    public void onPopulateAccessibilityEvent(@NonNull AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        List<CharSequence> textList = event.getText();
        CharSequence mentionLessText = getTextWithoutMentions();
        for (int i = 0; i < textList.size(); i++) {
            CharSequence text = textList.get(i);
            if (text instanceof MentionsEditable) {
                textList.set(i, mentionLessText);
            }
        }
    }

    /**
     * Allows a class to watch for text changes. Note that adding this class to itself will add it
     * to the super class. Other instances of {@link TextWatcher} will be notified by this class
     * as appropriate (helps prevent infinite loops when text keeps changing).
     *
     * @param watcher the {@link TextWatcher} to add
     */
    @Override
    public void addTextChangedListener(@NonNull TextWatcher watcher) {
        if (watcher == mInternalTextWatcher) {
            if (!mIsWatchingText) {
                super.addTextChangedListener(mInternalTextWatcher);
                mIsWatchingText = true;
            }
        } else {
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
    public void removeTextChangedListener(@NonNull TextWatcher watcher) {
        if (watcher == mInternalTextWatcher) {
            if (mIsWatchingText) {
                super.removeTextChangedListener(mInternalTextWatcher);
                mIsWatchingText = false;
            }
        } else {
            // Other watchers are added
            mExternalTextWatchers.remove(watcher);
        }
    }

    /**
     * Register a {@link com.linkedin.android.spyglass.ui.MentionsEditText.MentionWatcher} in order to receive callbacks
     * when mentions are changed.
     *
     * @param watcher the {@link com.linkedin.android.spyglass.ui.MentionsEditText.MentionWatcher} to add
     */
    @SuppressWarnings("unused")
    public void addMentionWatcher(@NonNull MentionWatcher watcher) {
        if (!mMentionWatchers.contains(watcher)) {
            mMentionWatchers.add(watcher);
        }
    }

    /**
     * Remove a {@link com.linkedin.android.spyglass.ui.MentionsEditText.MentionWatcher} from receiving anymore callbacks
     * when mentions are changed.
     *
     * @param watcher the {@link com.linkedin.android.spyglass.ui.MentionsEditText.MentionWatcher} to remove
     */
    @SuppressWarnings("unused")
    public void removeMentionWatcher(@NonNull MentionWatcher watcher) {
        mMentionWatchers.remove(watcher);
    }

    // --------------------------------------------------
    // Private Helper Methods
    // --------------------------------------------------

    private void restartInput() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.restartInput(this);
        }
    }

    /**
     * @return the text as an {@link Editable} (note: not a {@link MentionsEditable} and does not contain mentions)
     */
    private Editable getTextWithoutMentions() {
        Editable text = getText();
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        MentionSpan[] spans = sb.getSpans(0, sb.length(), MentionSpan.class);
        for (MentionSpan span: spans) {
            sb.removeSpan(span);
        }
        return sb;
    }

    private void notifyMentionAddedWatchers(@NonNull Mentionable mention, @NonNull String text, int start, int end) {
        for (MentionWatcher watcher : mMentionWatchers) {
            watcher.onMentionAdded(mention, text, start, end);
        }
    }

    private void notifyMentionDeletedWatchers(@NonNull Mentionable mention, @NonNull String text, int start, int end) {
        for (MentionWatcher watcher : mMentionWatchers) {
            watcher.onMentionDeleted(mention, text, start, end);
        }
    }

    private void notifyMentionPartiallyDeletedWatchers(@NonNull Mentionable mention, @NonNull String text, int start, int end) {
        for (MentionWatcher watcher : mMentionWatchers) {
            watcher.onMentionPartiallyDeleted(mention, text, start, end);
        }
    }

    // --------------------------------------------------
    // Private Classes
    // --------------------------------------------------

    /**
     * Simple class to hold onto a {@link MentionSpan} temporarily while the text is changing.
     */
    private class PlaceholderSpan {

        final MentionSpan holder;
        final int originalStart;
        final int originalEnd;

        PlaceholderSpan(MentionSpan holder, int originalStart, int originalEnd) {
            this.holder = holder;
            this.originalStart = originalStart;
            this.originalEnd = originalEnd;
        }
    }

    /**
     * Simple class to mark a span of text to delete later.
     */
    private class DeleteSpan {}

    /**
     * Runnable which detects the long click action.
     */
    private class CheckLongClickRunnable implements Runnable {
        private MentionSpan touchedSpan;

        @Override
        public void run() {
            if (isPressed()) {
                isLongPressed = true;
                if (touchedSpan == null) {
                    return;
                }
                MentionsEditable text = getMentionsText();
                // Set the selection anchor to start and end of the long clicked span and deselect all the span.
                setSelection(text.getSpanStart(touchedSpan), text.getSpanEnd(touchedSpan));
                deselectAllSpans();
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
        @NonNull
        public Editable newEditable(@NonNull CharSequence source) {
            MentionsEditable text = new MentionsEditable(source);
            Selection.setSelection(text, 0);
            return text;
        }
    }

    // --------------------------------------------------
    // MentionSpan Factory
    // --------------------------------------------------

    /**
     * Custom factory used when creating a {@link MentionSpan}.
     */
    public static class MentionSpanFactory {

        @NonNull
        public MentionSpan createMentionSpan(@NonNull Mentionable mention,
                                             @Nullable MentionSpanConfig config) {
            return (config != null) ? new MentionSpan(mention, config) : new MentionSpan(mention);
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

        @NonNull
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
        CharSequence text = super.getText();
        return text instanceof MentionsEditable ? (MentionsEditable) text : new MentionsEditable(text);
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
    public void setTokenizer(@Nullable final Tokenizer tokenizer) {
        mTokenizer = tokenizer;
    }

    /**
     * Sets the receiver of query tokens used by this class. The query token receiver will use the
     * tokens to generate suggestions, which can then be inserted back into this edit text.
     *
     * @param queryTokenReceiver the {@link QueryTokenReceiver} to use
     */
    public void setQueryTokenReceiver(@Nullable final QueryTokenReceiver queryTokenReceiver) {
        mQueryTokenReceiver = queryTokenReceiver;
    }

    /**
     * Sets the suggestions manager used by this class.
     *
     * @param suggestionsVisibilityManager the {@link SuggestionsVisibilityManager} to use
     */
    public void setSuggestionsVisibilityManager(@Nullable final SuggestionsVisibilityManager suggestionsVisibilityManager) {
        mSuggestionsVisibilityManager = suggestionsVisibilityManager;
    }

    /**
     * Sets the factory used to create MentionSpans within this class.
     *
     * @param factory the {@link MentionSpanFactory} to use
     */
    public void setMentionSpanFactory(@NonNull final MentionSpanFactory factory) {
        mentionSpanFactory = factory;
    }

    /**
     * Sets the configuration options used when creating MentionSpans.
     *
     * @param config the {@link MentionSpanConfig} to use
     */
    public void setMentionSpanConfig(@NonNull final MentionSpanConfig config) {
        mentionSpanConfig = config;
    }

    /**
     * Sets the string prefix to avoid creating and displaying suggestions.
     *
     * @param prefix prefix to avoid suggestions
     */
    public void setAvoidedPrefix(@Nullable String prefix) {
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

    // --------------------------------------------------
    // Save & Restore State
    // --------------------------------------------------

    @NonNull
    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        return new SavedState(parcelable, getMentionsText());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setText(savedState.mentionsEditable);
    }

    /**
     * Convenience class to save/restore the MentionsEditable state.
     */
    protected static class SavedState extends BaseSavedState {
        public MentionsEditable mentionsEditable;

        private SavedState(Parcelable superState, MentionsEditable mentionsEditable) {
            super(superState);
            this.mentionsEditable = mentionsEditable;
        }

        private SavedState(Parcel in) {
            super(in);
            mentionsEditable = in.readParcelable(MentionsEditable.class.getClassLoader());
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(mentionsEditable, flags);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    // --------------------------------------------------
    // MentionWatcher Interface & Simple Implementation
    // --------------------------------------------------

    /**
     * Interface to receive a callback for mention events.
     */
    public interface MentionWatcher {
        /**
         * Callback for when a mention is added.
         *
         * @param mention   the {@link Mentionable} that was added
         * @param text      the text after the mention was added
         * @param start     the starting index of where the mention was added
         * @param end       the ending index of where the mention was added
         */
        void onMentionAdded(@NonNull Mentionable mention, @NonNull String text, int start, int end);

        /**
         * Callback for when a mention is deleted.
         *
         * @param mention   the {@link Mentionable} that was deleted
         * @param text      the text before the mention was deleted
         * @param start     the starting index of where the mention was deleted
         * @param end       the ending index of where the mention was deleted
         */
        void onMentionDeleted(@NonNull Mentionable mention, @NonNull String text, int start, int end);

        /**
         * Callback for when a mention is partially deleted.
         *
         * @param mention   the {@link Mentionable} that was deleted
         * @param text      the text after the mention was partially deleted
         * @param start     the starting index of where the partial mention starts
         * @param end       the ending index of where the partial mention ends
         */
        void onMentionPartiallyDeleted(@NonNull Mentionable mention, @NonNull String text, int start, int end);
    }

    /**
     * Simple implementation of the {@link com.linkedin.android.spyglass.ui.MentionsEditText.MentionWatcher} interface
     * if you do not want to implement all methods.
     */
    @SuppressWarnings("unused")
    public class SimpleMentionWatcher implements MentionWatcher {
        @Override
        public void onMentionAdded(@NonNull Mentionable mention, @NonNull String text, int start, int end) {}

        @Override
        public void onMentionDeleted(@NonNull Mentionable mention, @NonNull String text, int start, int end) {}

        @Override
        public void onMentionPartiallyDeleted(@NonNull Mentionable mention, @NonNull String text, int start, int end) {}
    }
}
