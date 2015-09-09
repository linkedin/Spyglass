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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Concrete implementation of the {@link Mentionable} interface for testing purposes.
 */
public class TestMention implements Mentionable {

	private final String mText;

	public TestMention(String text) {
		mText = text;
	}

	@Override
	public int getSuggestibleId() {
		return mText.hashCode();
	}

	@Override
	public String getSuggestiblePrimaryText() {
		return mText;
	}

	@Override
	public String getTextForDisplayMode(MentionDisplayMode mode) {
		switch (mode) {
			case FULL:
				return mText;
			case PARTIAL:
				// Return the first word
				return mText.split(" ")[0];
			case NONE:
			default:
				return "";
		}
	}

	@Override
	public MentionDeleteStyle getDeleteStyle() {
		return MentionDeleteStyle.PARTIAL_NAME_DELETE;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mText);
	}

	public TestMention(Parcel in) {
		mText = in.readString();
	}

	public static final Parcelable.Creator<TestMention> CREATOR
			= new Parcelable.Creator<TestMention>() {
		public TestMention createFromParcel(Parcel in) {
			return new TestMention(in);
		}

		public TestMention[] newArray(int size) {
			return new TestMention[size];
		}
	};


}
