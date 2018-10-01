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

package com.linkedin.android.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import org.junit.runners.model.InitializationError;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.Fs;

public class SpyglassRobolectricRunner extends RobolectricTestRunner {

    public SpyglassRobolectricRunner(Class testClass) throws InitializationError {
        super(testClass);
    }

    public static void startFragment(Fragment fragment) {
        FragmentActivity activity = getActivity(FragmentActivity.class);
        startFragment(fragment, activity, null);
    }

    @TargetApi(11)
    public static void startFragment(Fragment fragment, FragmentActivity activity, String tag) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(fragment, tag);
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
        activity.invalidateOptionsMenu();
    }

    public static <T extends Activity> T getActivity(Class<T> clazz) {
        return Robolectric.buildActivity(clazz)
                .create()
                .start()
                .resume()
                .get();
    }

}
