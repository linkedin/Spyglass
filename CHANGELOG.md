Change Log
==========

3.0.1 *(2021-02-22)*
--------------------
* Fix for IndexOutOfBoundsException when trimming MentionsEditable

3.0.0 *(2021-02-19)*
--------------------
 * Migrated artifact publishing from Bintray back to Maven Central

2.2.0 *(2020-08-21)*
--------------------------
 * Improve handling of text deletion
 * Add check to prevent IndexOutOfBoundsException

2.1.1 *(2020-05-20)*
--------------------------
 * Added additional nullability annotations, improving Kotlin interoperability

2.1.0 *(2020-01-12)*
--------------------------
 * Bump min sdk to 21
 * Avoid ClassCastException when retrieving mentions text
 * Fix issue with text counter visibility

2.0.1 *(2019-06-07)*
--------------------------
 * Fix bug when deleting mentions ending in an emoticon character
 * Add the ability to add or remove MentionWatchers in the RichEditorView
 * Fix issue when comparing TextWatchers within the MentionsEditText

2.0.0 *(2018-11-27)*
--------------------------
 * Migrate from Android support library to AndroidX libraries

 1.5.0 *(2018-11-11)*
--------------------------
 * Update versions for nearly all dependencies
 * Fix an internal NPE occurring on Android 6
 * Migrate to publishing versions on jCenter. This will be the first release on jCenter.

1.4.0 *(2017-01-12)*
--------------------------
 * add support for cut/copy/paste of text containing mentions
 * update dependencies
 * fix edge case when inserting a mention

1.3.1 *(2016-04-05)*
--------------------------
 * add support for listening to partial mention deletions

1.3.0 *(2015-09-11)*
--------------------------
 * added custom XML attributes to control the color of the mentions
 * renamed functions within the Suggestible interface
 * added factory to customize how MentionSpans are generated
 * other minor improvements

1.2.2 *(2015-08-10)*
--------------------------
 * prevent IndexOutOfBoundsException when copying/pasting part of a mention
 * fix crash when accessibility is enabled

1.2.1 *(2015-07-21)*
--------------------------
 * better handling of multi-word mentions

1.2.0 *(2015-07-20)*
--------------------------
 * added isWordBreakingChar method to Tokenizer interface
 * better handling of edge cases when typing around a mention

1.1.3 *(2015-07-14)*
--------------------------
 * fix NPE when inserting mention with MentionWatchers interface

1.1.2 *(2015-07-14)*
--------------------------
 * major bug fix for cursor not appearing initially
 * added MentionWatcher interface

1.1.1 *(2015-07-04)*
--------------------------
 * updated dependencies and minor bugs fixes

1.1.0 *(2015-06-17)*
--------------------------
 * support saving/restoring mentions in onSaveInstanceState/onRestoreInstanceState

1.0.3 *(2015-05-27)*
--------------------------
 * improved stability
 * better handling of names in mentions

1.0.2 *(2015-02-23)*
--------------------------
 * switch to using a builder to configure the WordTokenizer

1.0.1 *(2015-02-21)*
--------------------------
 * disable spelling suggestions during mention suggestions
 * change target SDK to latest version (21) for spyglass.

1.0.0 *(2015-02-11)*
--------------------------
 * first public release
