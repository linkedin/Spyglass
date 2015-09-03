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

package com.linkedin.android.spyglass.sample.data.models;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.linkedin.android.spyglass.mentions.Mentionable;
import com.linkedin.android.spyglass.sample.R;
import com.linkedin.android.spyglass.sample.data.MentionsLoader;
import com.linkedin.android.spyglass.tokenization.QueryToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a person.
 */
public class Person implements Mentionable {

    private final String mFirstName;
    private final String mLastName;
    private final String mPictureURL;

    public Person(String firstName, String lastName, String pictureURL) {
        mFirstName = firstName;
        mLastName = lastName;
        mPictureURL = pictureURL;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    public String getPictureURL() {
        return mPictureURL;
    }

    // --------------------------------------------------
    // Mentionable/Suggestible Implementation
    // --------------------------------------------------

    @NonNull
    @Override
    public String getTextForDisplayMode(MentionDisplayMode mode) {
        switch (mode) {
            case FULL:
                return getFullName();
            case PARTIAL:
                String[] words = getFullName().split(" ");
                return (words.length > 1) ? words[0] : "";
            case NONE:
            default:
                return "";
        }
    }

    @Override
    public MentionDeleteStyle getDeleteStyle() {
        // People support partial deletion
        // i.e. "John Doe" -> DEL -> "John" -> DEL -> ""
        return MentionDeleteStyle.PARTIAL_NAME_DELETE;
    }

    @Override
    public int getSuggestibleId() {
        return getFullName().hashCode();
    }

    @Override
    public String getSuggestiblePrimaryText() {
        return getFullName();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mFirstName);
        dest.writeString(mLastName);
        dest.writeString(mPictureURL);
    }

    public Person(Parcel in) {
        mFirstName = in.readString();
        mLastName = in.readString();
        mPictureURL = in.readString();
    }

    public static final Parcelable.Creator<Person> CREATOR
            = new Parcelable.Creator<Person>() {
        public Person createFromParcel(Parcel in) {
            return new Person(in);
        }

        public Person[] newArray(int size) {
            return new Person[size];
        }
    };

    // --------------------------------------------------
    // PersonLoader Class (loads people from JSON file)
    // --------------------------------------------------

    public static class PersonLoader extends MentionsLoader<Person> {
        private static final String TAG = PersonLoader.class.getSimpleName();

        public PersonLoader(Resources res) {
            super(res, R.raw.people);
        }

        @Override
        public Person[] loadData(JSONArray arr) {
            Person[] data = new Person[arr.length()];
            try {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String first = obj.getString("first");
                    String last = obj.getString("last");
                    String url = obj.getString("picture");
                    data[i] = new Person(first, last, url);
                }
            } catch (Exception e) {
                Log.e(TAG, "Unhandled exception while parsing person JSONArray", e);
            }

            return data;
        }

        // Modified to return suggestions based on both first and last name
        @Override
        public List<Person> getSuggestions(QueryToken queryToken) {
            String prefix = queryToken.getKeywords().toLowerCase();
            List<Person> suggestions = new ArrayList<>();
            if (mData != null) {
                for (Person suggestion : mData) {
                    String firstName = suggestion.getFirstName().toLowerCase();
                    String lastName = suggestion.getLastName().toLowerCase();
                    if (firstName.startsWith(prefix) || lastName.startsWith(prefix)) {
                        suggestions.add(suggestion);
                    }
                }
            }
            return suggestions;
        }
    }
}
