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

import android.os.Parcelable;

import com.linkedin.android.spyglass.suggestions.SuggestionsAdapter;

/**
 * Interface for a model to implement in order for it to be able to be suggested.
 * <p/>
 * Note that the information gathered from the below methods are used in the default layout for the
 * {@link SuggestionsAdapter}.
 */
public interface Suggestible extends Parcelable {

	/**
	 * Must be unique (useful for eliminating duplicate suggestions)
	 *
	 * @return int the suggestible id
	 */
	public abstract int getSuggestibleId();

	/**
	 * Main text for the given suggestion, as will be shown to the user. Note other data fields can
	 * be added (other text, pictures), but this is the only required field, as it is used for the
	 * default layout.
	 *
	 * @return String the user visible suggestion
	 */
	public abstract String getSuggestiblePrimaryText();

}
