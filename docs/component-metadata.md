# Component metadata

Component metadata describes contrib components for discovery in the
[Ecosystem Explorer](https://github.com/open-telemetry/opentelemetry-ecosystem-explorer).

For each component, place a `metadata.yaml` in the source directory. Use a YAML mapping with
the fields below. Keep values synchronized with the component's README, source, tests, and
build configuration. Metadata describes the current checkout, not a specific released version.

## Fields

| Field              | Required | Type             | Meaning                                                                                                                                         |
| ------------------ | -------- | ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `name`             | Yes      | String           | Persistent component identifier, unique within this repository.                                                                                 |
| `display_name`     | Yes      | String           | Human-readable component name.                                                                                                                  |
| `description`      | Yes      | String           | Purpose and effect on telemetry.                                                                                                                |
| `source_path`      | Yes      | String           | Directory relative to the repository root, without a leading or trailing slash.                                                                 |
| `component_types`  | Yes      | List of strings  | One or more distinct usage categories described below.                                                                                          |
| `docs_url`         | Yes      | String           | Absolute HTTPS URL to the component's usage documentation.                                                                                      |
| `usage`            | Yes      | String           | How to activate or integrate the component, including relevant prerequisites.                                                                   |
| `status`           | Yes      | Mapping          | Contains required `stability`: `development`, `alpha`, `beta`, `stable`, or `deprecated`; optional `replaced_by` names a replacement component. |
| `artifact`         | No       | Mapping          | Maven coordinates; required when the component publishes a Maven artifact. Contains `group_id` and `artifact_id`.                               |
| `min_java_version` | Yes      | Integer          | Minimum supported runtime Java major version, not the build JDK.                                                                                |
| `distributions`    | Yes      | List of mappings | Other projects' distributions that bundle the component; use `[]` when none do.                                                                 |
| `configurations`   | Yes      | List of mappings | Supported configuration methods, each with inline instructions, an example, and settings; use `[]` when none apply.                             |

Strings must be nonempty. The only nullable value is a configuration `default`.

Descriptions (including method and setting descriptions) and `usage` support inline Markdown:
code, emphasis, and links. Block Markdown and raw HTML are not supported. Names, identifiers,
URLs, units, and other machine-readable fields are plain text. `example.code` is literal code.

Stability follows the repository's component maturity listing in the root README, not the Maven
version suffix or a declarative component identifier. An explicit deprecation notice takes
precedence over the maturity listing. `status.replaced_by` is allowed only with `deprecated`
stability and must name another component with metadata in this repository.

## Component types

`component_types` describes how users integrate a component. Available values are:

* `library`: included as an application dependency, used programmatically or through SDK
  autoconfiguration.
* `agent-extension`: explicitly supported for loading as a Java agent extension.
* `standalone`: run as a separate application or process.

Select every verified usage category. Describe the specific activation mode in `usage`.

## Distributions

`distributions` lists distributions published outside this repository that include the
component's artifact, so their users get it without adding a dependency or extension.
Each entry requires these fields:

| Field            | Type   | Meaning                                                                                       |
| ---------------- | ------ | --------------------------------------------------------------------------------------------- |
| `name`           | String | Distribution identifier from the list below, unique within `distributions`.                   |
| `repository_url` | String | Absolute HTTPS URL of the GitHub repository that publishes the distribution, as listed below. |

For example, a component bundled in the OpenTelemetry Java agent uses:

```yaml
distributions:
  - name: javaagent
    repository_url: https://github.com/open-telemetry/opentelemetry-java-instrumentation
```

Available distributions are:

* `javaagent`: the OpenTelemetry Java agent, published from
  <https://github.com/open-telemetry/opentelemetry-java-instrumentation>.

Entries reflect each distribution's main branch when last verified, and those projects
can change what they bundle in any release.

Being bundled does not mean being enabled. Describe any distribution-specific activation,
such as an enabling property, in `usage`. Distributions are independent of
`component_types`: `agent-extension` means users load the component themselves as an extension.

## Configuration methods

`configurations` is a list of supported methods.

Each method requires these fields:

| Field          | Type             | Meaning                                                                                 |
| -------------- | ---------------- | --------------------------------------------------------------------------------------- |
| `type`         | String           | Method identifier, unique within the list: `sdk-autoconfiguration` or `declarative`.    |
| `display_name` | String           | Human-readable heading for the method.                                                  |
| `description`  | String           | Activation instructions and method-specific prerequisites or limitations.               |
| `example`      | Mapping          | Contains required strings `language` (code-block language) and `code` (inline example). |
| `settings`     | List of mappings | Component-owned settings for this method; use `[]` when no settings apply.              |

Examples must show where settings belong. A declarative example includes the
containing SDK configuration structure; a system-property example shows JVM
arguments. Explain placeholders and any setup the example assumes in the method's
`description`. Examples illustrate one choice of values, which may differ from
defaults. Methods are alternatives; do not imply that their settings are merged.

`example.language` must be one of `yaml`, `shell`, `properties`, `java`, `kotlin`, or `xml`.
Declarative examples must use `yaml`. Shell examples should show a complete command, including
`java` when illustrating JVM arguments.

Declarative methods may also include these two fields. Supply both or neither; they are not
allowed on other methods. Consumers that build configuration require both fields.

| Field           | Type   | Meaning                                                                                |
| --------------- | ------ | -------------------------------------------------------------------------------------- |
| `component_key` | String | Literal component mapping key, including any slash, such as `stacktrace/development`.  |
| `location`      | String | Path to the mapping containing that key, with `[]` marking sequence-element traversal. |

A location is one or more dot-separated mapping keys, each optionally followed by `[]`.
Keys in this path must be nonempty and cannot contain dots or square brackets. For example,
`tracer_provider.processors[]` traverses the root `tracer_provider` mapping and an element of
its `processors` sequence. `component_key` is a literal key, not a path expression.
For the pilot this gives:

```yaml
component_key: stacktrace/development
location: tracer_provider.processors[]
```

A setting named `min_duration` then has the display path
`tracer_provider.processors[].stacktrace/development.min_duration`.
Omitting placement means it is unknown to tooling; consumers must not guess it from the method type.
The example must contain the declared placement. Examples may omit settings, but any keys shown
inside the component mapping must match listed setting names. Other SDK settings may appear outside
that mapping. This placement format does not yet express multiple alternative locations.

Each setting requires:

| Field         | Type                       | Meaning                                                                                                                      |
| ------------- | -------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `name`        | String                     | Full SDK property name, or declarative key relative to the component mapping shown in the example; unique within the method. |
| `type`        | String                     | Value type: `string`, `boolean`, `int`, `double`, `duration`, `list`, or `map`.                                              |
| `default`     | Scalar, list, map, or null | Default value when unset; null means no configured value.                                                                    |
| `description` | String                     | Behavior, accepted values, units, constraints, and what an unset value means.                                                |

The setting fields are based on the structured entries in
[Java instrumentation metadata](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/documenting-instrumentation.md#metadatayaml-files).
This schema groups them by method and defines its own typed defaults rather than using that schema's
flat property list.

Optional setting fields:

| Field            | Type            | Meaning                                                                                    |
| ---------------- | --------------- | ------------------------------------------------------------------------------------------ |
| `allowed_values` | List of strings | Nonempty list of distinct accepted values; allowed only for `string` settings.             |
| `unit`           | String          | Implicit unit of an `int` or `double` value, for example `ms`; not allowed on other types. |

A non-null default for a string enum must be one of its `allowed_values`.
Units describe the configured number, not a display conversion. Duration strings carry their own
units, so they do not use `unit`.

Represent defaults as YAML strings for `string` and `duration`, booleans for `boolean`, integers
for `int`, and finite numbers for `double` (integral numbers are also valid double defaults).
Quote duration defaults, such as `"5ms"`.

For SDK autoconfiguration, these types describe parsed values even though properties are supplied
as text. Describe collection serialization (including separators and escaping) in the setting's
`description`. For declarative methods, types must describe the actual YAML input: a setting that
accepts a comma-separated string is `string`, not `list`. A declarative `list` accepts a YAML
sequence and a `map` accepts a YAML mapping. A null default must have its fallback behavior
explained in `description`. List only component-owned settings, not all inherited SDK options.
