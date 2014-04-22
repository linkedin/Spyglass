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

package com.linkedin.android.spyglass.suggestions.interfaces;

/**
 * Interface for a class to handle when the suggestions are visible.
 */
public interface SuggestionsVisibilityManager {

    /**
     * Displays or hides the mentions suggestions list.
     *
     * @param display whether the mentions suggestions should be displayed
     */
    public void displaySuggestions(boolean display);

    /**
     * @return true if the mention suggestions list is currently being displayed
     */
    public boolean isDisplayingSuggestions();

}
