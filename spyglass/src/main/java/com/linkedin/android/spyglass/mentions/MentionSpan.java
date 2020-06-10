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

import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.NonNull;
import com.linkedin.android.spyglass.mentions.Mentionable.MentionDisplayMode;
import com.linkedin.android.spyglass.ui.MentionsEditText;

/**
 * Class representing a spannable {@link Mentionable} in an {@link EditText}. This class is
 * specifically used by the {@link MentionsEditText}.
 */
public class MentionSpan extends ClickableSpan implements Parcelable {

    private final Mentionable mention;
    private MentionSpanConfig config;

    private boolean isSelected = false;
    private MentionDisplayMode mDisplayMode = MentionDisplayMode.FULL;

    public MentionSpan(@NonNull Mentionable mention) {
        super();
        this.mention = mention;
        this.config = new MentionSpanConfig.Builder().build();
    }

    public MentionSpan(@NonNull Mentionable mention, @NonNull MentionSpanConfig config) {
        super();
        this.mention = mention;
        this.config = config;
    }

    @Override
    public void onClick(@NonNull final View widget) {
        if (!(widget instanceof MentionsEditText)) {
            return;
        }

        // Get reference to the MentionsEditText
        MentionsEditText editText = (MentionsEditText) widget;
        Editable text = editText.getText();

        if (text == null) {
            return;
        }

        // Set cursor behind span in EditText
        int newCursorPos = text.getSpanEnd(this);
        editText.setSelection(newCursorPos);

        // If we are going to select this span, deselect all others
        boolean isSelected = isSelected();
        if (!isSelected) {
            editText.deselectAllSpans();
        }

        // Toggle whether the view is selected
        setSelected(!isSelected());

        // Update the span (forces it to redraw)
        editText.updateSpan(this);
    }

    @Override
    public void updateDrawState(@NonNull final TextPaint tp) {
        if (isSelected()) {
            tp.setTypeface(Typeface.create((Typeface)null, config.SELECTED_TEXT_STYLE));
            tp.setColor(config.SELECTED_TEXT_COLOR);
            tp.bgColor = config.SELECTED_TEXT_BACKGROUND_COLOR;
        } else {
            tp.setTypeface(Typeface.create((Typeface)null, config.NORMAL_TEXT_STYLE));
            tp.setColor(config.NORMAL_TEXT_COLOR);
            tp.bgColor = config.NORMAL_TEXT_BACKGROUND_COLOR;
        }
        tp.setUnderlineText(false);
    }

    @NonNull
    public Mentionable getMention() {
        return mention;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @NonNull
    public MentionDisplayMode getDisplayMode() {
        return mDisplayMode;
    }

    public void setDisplayMode(@NonNull MentionDisplayMode mode) {
        mDisplayMode = mode;
    }

    @NonNull
    public String getDisplayString() {
        return mention.getTextForDisplayMode(mDisplayMode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, int flags) {
        dest.writeInt(config.NORMAL_TEXT_COLOR);
        dest.writeInt(config.NORMAL_TEXT_BACKGROUND_COLOR);
        dest.writeInt(config.SELECTED_TEXT_COLOR);
        dest.writeInt(config.SELECTED_TEXT_BACKGROUND_COLOR);
        dest.writeInt(config.NORMAL_TEXT_STYLE);
        dest.writeInt(config.SELECTED_TEXT_STYLE);

        dest.writeInt(getDisplayMode().ordinal());
        dest.writeInt(isSelected() ? 1 : 0);
        dest.writeParcelable(getMention(), flags);
    }

    public MentionSpan(@NonNull Parcel in) {
        int normalTextColor = in.readInt();
        int normalTextBackgroundColor = in.readInt();
        int selectedTextColor = in.readInt();
        int selectedTextBackgroundColor = in.readInt();
        int normalTextStyle = in.readInt();
        int selectedTextStyle = in.readInt();
        config = new MentionSpanConfig(normalTextColor, normalTextBackgroundColor,
                                       selectedTextColor, selectedTextBackgroundColor,
                                       normalTextStyle, selectedTextStyle);

        mDisplayMode = MentionDisplayMode.values()[in.readInt()];
        setSelected((in.readInt() == 1));
        mention = in.readParcelable(Mentionable.class.getClassLoader());
    }

    public static final Parcelable.Creator<MentionSpan> CREATOR
            = new Parcelable.Creator<MentionSpan>() {

        @NonNull
        public MentionSpan createFromParcel(@NonNull Parcel in) {
            return new MentionSpan(in);
        }

        @NonNull
        public MentionSpan[] newArray(int size) {
            return new MentionSpan[size];
        }
    };
}
