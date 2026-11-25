# Component metadata

Component metadata describes contrib components for discovery in the Ecosystem Explorer.
See [the metadata discussion](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/2586)
and [span-stacktrace](../span-stacktrace/metadata.yaml) for an example.

For each component, place a `metadata.yaml` in the source directory. Use a YAML mapping with
the fields below. Keep values synchronized with the component's README, source, tests, and
build configuration. Metadata describes the current checkout, not a specific released version.

## Fields

| Field              | Required | Type             | Meaning                                                                                                             |
| ------------------ | -------- | ---------------- | ------------------------------------------------------------------------------------------------------------------- |
| `display_name`     | Yes      | String           | Human-readable component name.                                                                                      |
| `description`      | Yes      | String           | Purpose and effect on telemetry.                                                                                    |
| `source_path`      | Yes      | String           | Directory relative to the repository root, without a leading or trailing slash.                                     |
| `component_types`  | Yes      | List of strings  | One or more distinct usage categories described below.                                                              |
| `docs_url`         | Yes      | String           | Absolute HTTPS URL to the component's usage documentation.                                                          |
| `usage`            | Yes      | String           | How to activate or integrate the component, including relevant prerequisites.                                       |
| `status`           | Yes      | Mapping          | Contains required `stability`: `development`, `alpha`, `beta`, or `stable`.                                         |
| `artifact`         | No       | Mapping          | Maven coordinates; required when the component publishes a Maven artifact. Contains `group_id` and `artifact_id`.   |
| `min_java_version` | Yes      | Integer          | Minimum supported runtime Java major version, not the build JDK.                                                    |
| `configurations`   | Yes      | List of mappings | Supported configuration methods, each with inline instructions, an example, and settings; use `[]` when none apply. |

Strings must be nonempty. The only nullable value is a configuration `default`.
Propose schema changes before adding new fields. Stability follows the repository's
component maturity listing in the root README, not the Maven version suffix or a
declarative component identifier.

## Component types

`component_types` describes how users integrate a component. Available values are:

* `library`: included as an application dependency, used programmatically or through SDK autoconfiguration.
* `agent-extension`: explicitly supported for loading as a Java agent extension.
* `standalone`: run as a separate application or process.

Select every verified usage category. SDK autoconfiguration support alone is not
evidence of Java agent extension support. Describe the specific activation mode in
`usage`; for example, adding a library to the runtime classpath still requires an
application using SDK autoconfiguration for its customizer to run.

## Configuration methods

`configurations` is a list of supported methods. Consumers can display each method
with its instructions, example, and settings, without requiring users to follow
documentation links.

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

Each setting requires:

| Field         | Type           | Meaning                                                                                                                      |
| ------------- | -------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `name`        | String         | Full SDK property name, or declarative key relative to the component mapping shown in the example; unique within the method. |
| `type`        | String         | Logical value type: `string`, `boolean`, `int`, or `duration`.                                                               |
| `default`     | Scalar or null | Default value when unset; null means no configured value.                                                                    |
| `description` | String         | Behavior, accepted values, units, constraints, and what an unset value means.                                                |

The setting fields follow the structured entries in
[Java instrumentation metadata](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/documenting-instrumentation.md#metadatayaml-files).
This schema groups them by method rather than using that schema's flat property list.

Represent defaults as YAML strings for `string` and `duration`, booleans for
`boolean`, and integers for `int`. Quote duration defaults, such as `"5ms"`.
These types describe parsed values even though system properties are supplied as
text. A null default must have its fallback behavior explained in `description`.
List only component-owned settings, not all inherited SDK options. Every listed
setting is optional to configure; the schema cannot yet express required settings.

## Review checklist

* Verify purpose, activation, and settings against implementation and tests.
* Verify stability against the root README and coordinates against publishing conventions.
* Verify runtime Java against shared build conventions and any module override.
* Check documentation links and YAML syntax, and run repository Markdown linting.
