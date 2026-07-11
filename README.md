# Hbmartin's detekt rules

[![Join the chat at #detekt on KotlinLang](https://img.shields.io/badge/%23detekt-slack-red.svg?logo=slack)](https://kotlinlang.slack.com/archives/C88E12QH4)
[![PR Checks](https://github.com/hbmartin/hbmartin-detekt-rules/actions/workflows/pre-merge.yml/badge.svg)](https://github.com/hbmartin/hbmartin-detekt-rules/actions/workflows/pre-merge.yml)
[![codecov](https://codecov.io/github/hbmartin/hbmartin-detekt-rules/branch/main/graph/badge.svg?token=5CIMCMO3K3)](https://codecov.io/github/hbmartin/hbmartin-detekt-rules)
[![CodeFactor](https://www.codefactor.io/repository/github/hbmartin/hbmartin-detekt-rules/badge)](https://www.codefactor.io/repository/github/hbmartin/hbmartin-detekt-rules)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=hbmartin_hbmartin-detekt-rules&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=hbmartin_intellij-build-webhook-notifier)
[![Maven Central Version](https://img.shields.io/maven-central/v/me.haroldmartin/hbmartin-detekt-rules)](https://central.sonatype.com/artifact/me.haroldmartin/hbmartin-detekt-rules)
[![KDoc](https://img.shields.io/badge/KDoc-GitHub%20Pages-blue)](https://hbmartin.github.io/hbmartin-detekt-rules/)


These are my opinions. There are many like them but these are mine. 😄

## Quick Start

Inside of your `dependencies` block add the following: (for more details see [adding more rule sets](https://github.com/detekt/detekt#adding-more-rule-sets))
```kotlin 
detektPlugins("me.haroldmartin:hbmartin-detekt-rules:0.1.7")
```

Then add to your detekt configuration as in the section below to activate rules. Note that the AvoidFirstOrLastOnList, AvoidMutableCollections, and AvoidToIntOrThrowingConversions rules require [type resolution](https://detekt.dev/docs/gettingstarted/type-resolution) to be active.

## Type Resolution

Some of these rules require type resolution, which typically happens when running a `detekt*Main` task. For more details (including instructions for multiplatform) see [Using Type Resolution](https://detekt.dev/docs/gettingstarted/type-resolution/) in the detekt docs.

If you rely on the `check` task to run detekt, you can replace the default detekt check with a typed check using a configuration like:

```kotlin
tasks.named("check").configure {
     this.setDependsOn(
         this.dependsOn.filterNot {
             it is TaskProvider<*> && it.name == "detekt"
         } + tasks.named("detektMain"),
     )
 }
```



## Configuration

Add below to your `detekt.yml` and modify to suit your needs. In addition to the rule-specific options shown, every rule supports detekt's standard options such as `ignoreAnnotated` and `excludes` — e.g. `ignoreAnnotated: ['Test']` on `NoNotNullOperator` allows `!!` in annotated test functions.

```yaml
HbmartinRuleSet:
  AvoidFirstOrLastOnList:
    active: true
    # accessors to forbid; each must have an *OrNull equivalent
    methods:
      - 'first'
      - 'last'
      - 'single'
      - 'elementAt'
      - 'reduce'
      - 'reduceRight'
      - 'max'
      - 'min'
      - 'maxBy'
      - 'minBy'
  AvoidGlobalScope:
    active: true
  AvoidMutableCollections:
    active: true
    # set true to allow mutable collections in private declarations and function bodies
    allowPrivateAndLocal: false
  AvoidToIntOrThrowingConversions:
    active: true
    # String conversions to forbid; each must have an *OrNull equivalent
    methods:
      - 'toInt'
      - 'toLong'
      - 'toShort'
      - 'toByte'
      - 'toDouble'
      - 'toFloat'
      - 'toUInt'
      - 'toULong'
      - 'toUShort'
      - 'toUByte'
      - 'toBigDecimal'
      - 'toBigInteger'
      - 'toBooleanStrict'
  AvoidVarsExceptWithDelegate:
    active: true
    allowedDelegates:
      - 'remember\w*'
      - 'mutableState\w*'
  DontForceCast:
    active: true
  MutableTypeShouldBePrivate:
    active: true
    # regexes of mutable type names allowed to be exposed publicly
    allowedTypes: []
  NoCallbacksInFunctions:
    active: true
    ignoreAnnotated: ['Composable']
    allowExtensions: true
    allowReceivers: true
    allowInline: false
  NoDeferredResultInPublicApi:
    active: true
  NoLateinitVar:
    active: true
    # annotations that permit a lateinit var, e.g. ['Inject']
    allowedAnnotations: []
  NoNotNullOperator:
    active: true
  NoRunBlocking:
    active: true
  NoVarsInConstructor:
    active: true
  WhenBranchSingleLineOrBraces:
    active: true
```

## Rules

### AvoidFirstOrLastOnList

Finds uses of throwing accessors such as `.first()`, `.last()`, or `.single()` on a `List`, `Array`, or `Sequence`, including safe calls, calls with a predicate, and method references. These are dangerous calls since they will throw a `NoSuchElementException` if no matching element is present. Prefer the `*OrNull` variants, e.g. `.firstOrNull()` or `.lastOrNull()`, instead. The set of forbidden accessors is configurable via the `methods` option. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/AvoidFirstOrLastOnListTest.kt) for triggering and non-triggering examples.

### AvoidGlobalScope

Finds uses of `GlobalScope`. Coroutines launched in `GlobalScope` are not bound to any lifecycle, so they leak work, are not cancelled on failure, and swallow crashes. Prefer a structured `CoroutineScope`, e.g. `viewModelScope` or `lifecycleScope` on Android. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/AvoidGlobalScopeTest.kt) for triggering and non-triggering examples.

### AvoidMutableCollections

Finds uses of mutable collections e.g. `MutableList<>`. These are highly likely to lead to bugs, prefer to use functional patterns to create new lists modified as needed. Usages inside the standard library collection builders (`buildList`, `buildSet`, `buildMap`) and platform types from Java interop are not reported, and the `allowPrivateAndLocal` option can allow mutable collections in private declarations and function bodies. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/AvoidMutableCollectionsTest.kt) for triggering and non-triggering examples.

### AvoidToIntOrThrowingConversions

Finds `String` conversions such as `.toInt()` that throw `NumberFormatException` on malformed input. Prefer the `*OrNull` variants, e.g. `.toIntOrNull()`, and handle the `null` case. The set of forbidden conversions is configurable via the `methods` option. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/AvoidToIntOrThrowingConversionsTest.kt) for triggering and non-triggering examples.

### AvoidVarsExceptWithDelegate

Finds uses of mutable `var` fields. These are highly likely to lead to bugs, prefer to use a `Flow` or some reactive type for any mutable state. There is an exception made for `var`s which are implemented with the [delegation pattern](https://kotlinlang.org/docs/delegation.html), which is particularly common when using Compose. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/AvoidVarsExceptWithDelegateTest.kt) for triggering and non-triggering examples.

### DontForceCast

Finds uses of `as` to force cast. These are likely to lead to crashes, especially in unforeseen circumstances, prefer to safely cast with `as?` instead. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/DontForceCastTest.kt) for triggering and non-triggering examples.

### MutableTypeShouldBePrivate

Finds publicly exposed mutable types e.g. `MutableStateFlow<>`. These are likely to lead to bugs, prefer to expose a non-mutable `Flow` (e.g. with `_mutableStateFlow.asStateFlow()`) or other non-mutable type. Type names matching a regex in the `allowedTypes` option are permitted. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/MutableTypeShouldBePrivateTest.kt) for triggering and non-triggering examples.

### NoCallbacksInFunctions
Finds uses of callbacks in functions. This can lead to a mixed concurrency paradigm and are likely to lead to bugs or stalled threads, prefer to use a suspend function instead. Use the `ignoreAnnotated` configuration to allow callbacks in `@Composable` functions. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/NoCallbacksInFunctionsTest.kt) for triggering and non-triggering examples.

### NoDeferredResultInPublicApi

Finds public functions and properties with an explicitly declared `Deferred` type. Returning a `Deferred` leaks the concurrency implementation to callers and makes crashes hard to trace, prefer a suspend function returning the awaited value. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/NoDeferredResultInPublicApiTest.kt) for triggering and non-triggering examples.

### NoLateinitVar

Finds `lateinit var` properties. Accessing a `lateinit` property before initialization crashes with `UninitializedPropertyAccessException`, and `lateinit` forces mutability. Prefer constructor parameters, a nullable `val`, or lazy initialization. Annotations that legitimately require `lateinit`, e.g. `@Inject`, can be permitted with the `allowedAnnotations` option. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/NoLateinitVarTest.kt) for triggering and non-triggering examples.

### NoNotNullOperator

Finds uses of `!!` to force unwrap. These are likely to lead to crashes, prefer to safely unwrap with `?.` or `?:` instead. Otherwise the Kotlin docs will make fun of you for being an [NPE lover](https://kotlinlang.org/docs/null-safety.html#the-operator). To allow `!!` in tests, use `ignoreAnnotated: ['Test']` or an `excludes` glob on this rule. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/NoNotNullOperatorTest.kt) for triggering and non-triggering examples.

### NoRunBlocking

Finds calls to `runBlocking`, which blocks the current thread until its coroutine finishes and can deadlock or freeze the UI when used on a main thread. Prefer exposing suspend functions and launching coroutines from a structured `CoroutineScope`. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/NoRunBlockingTest.kt) for triggering and non-triggering examples.

### NoVarsInConstructor

Finds uses of `var` in a constructor. These are likely to lead to bugs, always use `val` instead. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/NoVarsInConstructorTest.kt) for triggering and non-triggering examples.

### WhenBranchSingleLineOrBraces

A stylistic rule that require that either a when expression be on a single line or use braces. Either case should have a single space after the arrow. [See here](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/src/test/kotlin/me/haroldmartin/detektrules/WhenBranchSingleLineOrBracesTest.kt) for triggering and non-triggering examples.

## Contributing

* Jump in and modify this project! Start by cloning it with `git clone git@github.com:hbmartin/hbmartin-detekt-rules.git`, then open it in IntelliJ and run the tests.
* Read the [detekt documentation](https://detekt.dev/docs/introduction/extensions/) to learn more about how to write rules.
* Each rule class carries KDoc with `<noncompliant>`/`<compliant>` examples — treat that KDoc as the canonical description when updating this README or the default `config.yml`.
* [PRs](https://github.com/hbmartin/hbmartin-detekt-rules/pulls) and [bug reports / feature requests](https://github.com/hbmartin/hbmartin-detekt-rules/issues) are all welcome!
* Checked with detekt, including the ruleauthors set and, of course, [running these rules on itself](https://github.com/hbmartin/hbmartin-detekt-rules/blob/main/build.gradle.kts#L20) 😏
* Treat other people with helpfulness, gratitude, and consideration! See the [JetBrains CoC](https://confluence.jetbrains.com/display/ALL/JetBrains+Open+Source+and+Community+Code+of+Conduct)

## Authors

* [Harold Martin](https://www.linkedin.com/in/harold-martin-98526971/) - harold.martin at gmail
* Significant inspiration from [kure-potlin by neeffect](https://github.com/neeffect/kure-potlin) and [Doist detekt-rules](https://github.com/Doist/detekt-rules)

