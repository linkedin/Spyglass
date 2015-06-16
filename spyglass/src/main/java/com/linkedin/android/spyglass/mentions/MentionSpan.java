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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.EditText;

import com.linkedin.android.spyglass.R;
import com.linkedin.android.spyglass.mentions.Mentionable.MentionDisplayMode;
import com.linkedin.android.spyglass.ui.MentionsEditText;

/**
 * Class representing a spannable {@link Mentionable} in an {@link EditText}. This class is
 * specifically used by the {@link MentionsEditText}.
 */
public class MentionSpan extends ClickableSpan implements Parcelable {

    private int NORMAL_TEXT_COLOR;
    private int NORMAL_BG_COLOR;
    private int SELECTED_TEXT_COLOR;
    private int SELECTED_BG_COLOR;

    private Mentionable mMention;
    private boolean mIsSelected = false;

    private MentionDisplayMode mDisplayMode = MentionDisplayMode.FULL;

    public MentionSpan(Context context, Mentionable mention) {
        super();
        mMention = mention;
        // Setup colors from resources
        Resources resources = context.getResources();
        NORMAL_TEXT_COLOR = resources.getColor(R.color.mention_unselected);
        NORMAL_BG_COLOR = Color.TRANSPARENT;
        SELECTED_TEXT_COLOR = Color.WHITE;
        SELECTED_BG_COLOR = resources.getColor(R.color.mention_selected);
    }

    @Override
    public void onClick(View widget) {

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
    public void updateDrawState(TextPaint tp) {
        if (isSelected()) {
            tp.setColor(SELECTED_TEXT_COLOR);
            tp.bgColor = SELECTED_BG_COLOR;
        } else {
            tp.setColor(NORMAL_TEXT_COLOR);
            tp.bgColor = NORMAL_BG_COLOR;
        }
        tp.setUnderlineText(false);
    }

    public Mentionable getMention() {
        return mMention;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean selected) {
        mIsSelected = selected;
    }

    public MentionDisplayMode getDisplayMode() {
        return mDisplayMode;
    }

    public void setDisplayMode(MentionDisplayMode mode) {
        mDisplayMode = mode;
    }

    public String getDisplayString() {
        return mMention.getTextForDisplayMode(mDisplayMode);
    }

    public void setNormalTextColor(int color) {
        NORMAL_TEXT_COLOR = color;
    }

    public void setNormalBackgroundColor(int color) {
        NORMAL_BG_COLOR = color;
    }

    public void setSelectedTextColor(int color) {
        SELECTED_TEXT_COLOR = color;
    }

    public void setSelectedBackgroundColor(int color) {
        SELECTED_BG_COLOR = color;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(NORMAL_TEXT_COLOR);
        dest.writeInt(NORMAL_BG_COLOR);
        dest.writeInt(SELECTED_TEXT_COLOR);
        dest.writeInt(SELECTED_BG_COLOR);

        dest.writeInt(getMention().getDeleteStyle().ordinal());
        dest.writeInt(getDisplayMode().ordinal());
        dest.writeString(getMention().getTextForDisplayMode(MentionDisplayMode.FULL));
        dest.writeString(getMention().getTextForDisplayMode(MentionDisplayMode.PARTIAL));
        dest.writeInt(isSelected() ? 1 : 0);
        dest.writeInt(getMention().getId());
        dest.writeString(getMention().getPrimaryText());
    }

    public MentionSpan(Parcel in) {
        NORMAL_TEXT_COLOR = in.readInt();
        NORMAL_BG_COLOR = in.readInt();
        SELECTED_TEXT_COLOR = in.readInt();
        SELECTED_BG_COLOR = in.readInt();

        final Mentionable.MentionDeleteStyle deleteStyle = Mentionable.MentionDeleteStyle.values()[in.readInt()];
        mDisplayMode = MentionDisplayMode.values()[in.readInt()];
        final String fullDisplayModeText = in.readString();
        final String partialDisplayModeText = in.readString();
        setSelected((in.readInt() == 1));
        final int id = in.readInt();
        final String primaryText = in.readString();

        mMention = new Mentionable() {
            @Override
            public String getTextForDisplayMode(MentionDisplayMode mentionDisplayMode) {
                String text = "";
                switch (mentionDisplayMode) {
                    case FULL:
                        text = fullDisplayModeText;
                        break;
                    case PARTIAL:
                        text = partialDisplayModeText;
                        break;
                    default:
                        break;
                }

                return text;
            }

            @Override
            public MentionDeleteStyle getDeleteStyle() {
                return deleteStyle;
            }

            @Override
            public int getId() {
                return id;
            }

            @Override
            public String getPrimaryText() {
                return primaryText;
            }
        };
    }

    public static final Parcelable.Creator<MentionSpan> CREATOR
            = new Parcelable.Creator<MentionSpan>() {
        public MentionSpan createFromParcel(Parcel in) {
            return new MentionSpan(in);
        }

        public MentionSpan[] newArray(int size) {
            return new MentionSpan[size];
        }
    };
}
