# Spyglass [![Build Status](https://img.shields.io/github/workflow/status/linkedin/Spyglass/Merge%20checks)](https://img.shields.io/github/workflow/status/linkedin/Spyglass/Merge%20checks)

A powerful Android library that provides highly-customizable widgets (with smart defaults) to easily add social-media-esque mention (aka tag) support to your app

*For a broad overview, check out our [blog post](https://engineering.linkedin.com/android/open-sourcing-spyglass-flexible-library-implementing-mentions-android) at the LinkedIn engineering blog.*

## Features

- A subclass of `EditText` that contains enhanced functionality in order to tokenize user input and display mentions
- A custom view, similar to a `MultiAutoCompleteTextView`, that displays suggestions in an embedded `ListView` rather than a `Popup`
- Custom tokenizer interface, including a default implementation containing several options for customization
- Designed to handle suggestions dynamically as are retrieved from multiple data sources
- Supports both implicit and explicit (i.e. "@User Name") mentions

## Getting Started

Grab via Maven:
```xml
<dependency>
  <groupId>com.linkedin.android.spyglass</groupId>
  <artifactId>spyglass</artifactId>
  <version>3.0.1</version>
</dependency>
```
or Gradle:
```groovy
api 'com.linkedin.android.spyglass:spyglass:3.0.1'
```

## Overview

Spyglass is divided into three, customizable layers: *tokenization*, *suggestions*, and *mentions*. Together, these layers form the update lifecycle used within Spyglass:

1. User types a character
2. Tokenize input
3. Generate and display suggestions
4. User taps a suggestion
5. Insert and display mention

Let's talk about each layer individually:

### Tokenization

After the user types a character, Spyglass will use a tokenizer to determine which part of the inputted text it should be considering and whether it could be a valid mention. While you may create your own tokenizer, most people should use the highly-configurable default tokenizer, the `WordTokenizer`. Here are some of the features you can define and customize:

- Define characters that will trigger an explicit mention (defaults to the '@' character)
- Set the number of characters required to have been typed in the current word before displaying any suggestions (defaults to 3)
- Define the maximum number of words to return as part of your current token (defaults to 1). This option would allow you to recommend suggestions based on more than just the currently-typed word (i.e. if value is 2 and user has typed "New Mex", you could recommend "New Mexico" before just "Mexico")
- Set which characters are considered word-breaking and line-breaking characters by the tokenizer (defaults to spaces, periods, and newline characters)

For more information about implementation and configuration options, see the documentation in the `WordTokenizer` class.

If the input being inspected by the tokenizer is currently valid, it will generate a `QueryToken` containing the keywords that will ultimately be used to generate suggestions.

### Suggestions

Once the tokenizer has generated a valid `QueryToken`, Spyglass must now determine which suggestions to display using that token. It will call an implementation of `QueryTokenReceiver`. This is the only interface you are required to implement to use Spyglass. This interface defines one method that takes in the generated `QueryToken`. Your app can then use the token to query data from any number of data sources (i.e. servers, databases, caches, etc.) asynchronously. The function must return a list of strings, where each string is as an identifier for one of the data sources used for the given `QueryToken`. Each data source must then call the `SuggestionsResultListener` with the resulting suggestions and the same string identifier representing the data source for the suggestions.

As the suggestions come in from multiple data sources, the suggestions must be displayed. If you use the `RichEditorView`, the suggestions will be displayed via a default view without any special ordering. You may customize the view and the order of its suggestions by providing your own implementation of `SuggestionsListBuilder`. If you are using the `MentionsEditText`, you will need to implement your own `SuggestionsResultListener` and use the given suggestions to build your own views (typically using either a `ListView`, `GridView`, or more recently, a `RecyclerView`). When a suggestion is selected, you will also need to call the `insertMention` method on the `MentionsEditText` with the suggestion to insert as a mention.

### Mentions

All mentions that Spyglass insert must be a subclass of the `MentionSpan`. By default, you can easily tweak how your mentions appear (i.e. highlight and text colors). Additionally, you may alter how they behave when they are deleted via multi-stage deletions. We use this extensively in the LinkedIn app to allow users to delete only the last name of a mentioned member (leaving just the first name).

## Usage

To use Spyglass, you have two options:  the `MentionsEditText` and the `RichEditorView`.

The `MentionsEditText` is a subclass of `EditText`. It contains extra functionality for tokenizing user input into query tokens and inserting new mentions to be displayed. Note that it does not have any view associated with displaying suggestions. You must provide that. This gives you the most power to customize how your mentions-enabled text box feels and behaves. See the "Grid Mentions" example in the sample app for a demo.

The `RichEditorView` is the quickest way to add mentions into your app. It is built on top of the aforementioned `MentionsEditText` and displays suggestions in an embedded `ListView`. It serves a similar functionality as Android's `MultiAutoCompleteTextView`. Note that you can still alter how suggestion items are displayed in the list, and you can still alter the tokenization and mention displaying options used by the underlying `MentionsEditText`.

## Sample App

The ''spyglass-sample'' app contains several examples of using the library. For more detailed information, see the documentation [here](spyglass-sample/README.md).

## Testing

We use the Robolectric framework coupled with Mockito for our unit tests. You can run them via the `gradle clean test` command.

## Snapshots

You can use snapshot builds to test the latest unreleased changes. A new snapshot is published
after every merge to the main branch by the [Deploy Snapshot Github Action workflow](.github/workflows/deploy-snapshot.yml).

Just add the Sonatype snapshot repository to your Gradle scripts:
```gradle
repositories {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
    }
}
```

You can find the latest snapshot version to use in the [gradle.properties](gradle.properties) file.
