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

package com.linkedin.android.spyglass.ui.wrappers;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.linkedin.android.spyglass.R;
import com.linkedin.android.spyglass.ui.RichEditorView;

/**
 * Convenient fragment wrapper around a {@link RichEditorView}.
 */
public class RichEditorFragment extends Fragment {

    private static final String FRAGMENT_TAG = "fragment_rich_editor";

    private RichEditorView mRichEditor;
    private OnCreateViewListener mOnCreateViewListener;

    public static interface OnCreateViewListener {
        public void onFragmentCreateView(RichEditorFragment fragment);
    }

    public RichEditorFragment() {
        // Required empty public constructor
    }

    public static RichEditorFragment newInstance(Bundle args) {
        RichEditorFragment fragment = new RichEditorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static RichEditorFragment getInstance(FragmentManager fragmentManager, Bundle args) {
        RichEditorFragment instance;
        Fragment fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            instance = RichEditorFragment.newInstance(args);
        } else {
            instance = (RichEditorFragment) fragment;
        }
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.editor_fragment, container, false);
        if (rootView == null) {
            return null;
        }

        mRichEditor = (RichEditorView) rootView.findViewById(R.id.rich_editor);
        if (mOnCreateViewListener != null) {
            mOnCreateViewListener.onFragmentCreateView(this);
        }

        return rootView;
    }

    @Nullable
    public RichEditorView getRichEditor() {
        return mRichEditor;
    }

    @NonNull
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    public void setOnCreateViewListener(OnCreateViewListener listener) {
        mOnCreateViewListener = listener;
    }

}
