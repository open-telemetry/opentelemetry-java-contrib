# Component metadata (draft)

This pilot describes contrib components for discovery in the Ecosystem Explorer.
It starts with [span-stacktrace](../span-stacktrace/metadata.yaml); the schema and
usage categories are proposals, not a complete taxonomy of this repository.
See [the metadata discussion](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/2586).

Place `metadata.yaml` in the component's source directory. Use a YAML mapping with
the fields below. Keep values synchronized with the component's README, source,
tests, and build configuration. Metadata describes the current checkout, not a
specific released version.

## Fields

| Field              | Required | Type             | Meaning                                                                                                                            |
| ------------------ | -------- | ---------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `display_name`     | Yes      | String           | Human-readable component name.                                                                                                     |
| `description`      | Yes      | String           | Purpose and effect on telemetry.                                                                                                   |
| `source_path`      | Yes      | String           | Directory relative to the repository root, without a leading or trailing slash.                                                    |
| `component_type`   | Yes      | List of strings  | One or more distinct usage categories described below.                                                                             |
| `docs_url`         | Yes      | String           | Absolute HTTPS URL to the component's usage documentation.                                                                         |
| `usage`            | Yes      | String           | How to activate or integrate the component, including relevant prerequisites.                                                      |
| `status`           | Yes      | Mapping          | Contains required `stability`: `development`, `alpha`, `beta`, or `stable`.                                                        |
| `artifact`         | No       | Mapping          | Maven coordinates; required for the published library in this pilot. Contains required string fields `group_id` and `artifact_id`. |
| `min_java_version` | Yes      | Integer          | Minimum supported runtime Java major version, not the build JDK.                                                                   |
| `configurations`   | Yes      | List of mappings | Supported configuration methods, each with inline instructions, an example, and settings; use `[]` when none apply.                |

Strings must be nonempty. The only nullable value is a configuration `default`.
The pilot defines only the fields documented here; propose schema changes before
adding new fields. Stability follows the repository's component maturity listing,
not the Maven version suffix or a declarative component identifier.

## Component types

`component_type` describes how users integrate a component. Proposed values are:

* `library`: included as an application dependency, used programmatically or through SDK autoconfiguration.
* `agent-extension`: explicitly supported for loading as a Java agent extension.
* `standalone`: run as a separate application or process.

Select every verified usage category. SDK autoconfiguration support alone is not
evidence of Java agent extension support. Describe the specific activation mode in
`usage`; for example, adding a library to the runtime classpath still requires an
application using SDK autoconfiguration for its customizer to run.

The span-stacktrace pilot exercises only `library`. Validate the other categories
against further components before expanding the inventory. Features such as
sampling and context propagation, signal classification, multi-artifact modules,
and structured programmatic or standalone configuration remain open for later work.

## Configuration methods

`configurations` is a list of supported methods. Consumers can display each method
as a section or tab containing its instructions, example, and settings table,
without requiring users to follow documentation links. Top-level `usage` describes
shared integration prerequisites; each method explains its own activation.

Each method requires these fields:

| Field          | Type             | Meaning                                                                                             |
| -------------- | ---------------- | --------------------------------------------------------------------------------------------------- |
| `type`         | String           | Method identifier, unique within the list: `sdk-autoconfiguration` or `declarative` for this pilot. |
| `display_name` | String           | Human-readable heading for the method.                                                              |
| `description`  | String           | Activation instructions and method-specific prerequisites or limitations.                           |
| `example`      | Mapping          | Contains required strings `language` (code-block language) and `code` (inline example).             |
| `settings`     | List of mappings | Component-owned settings for this method; use `[]` when no settings apply.                          |

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
This draft groups them by method rather than using that schema's flat property list.

Represent defaults as YAML strings for `string` and `duration`, booleans for
`boolean`, and integers for `int`. Quote duration defaults, such as `"5ms"`.
These types describe parsed values even though system properties are supplied as
text. A null default must have its fallback behavior explained in `description`.
List only component-owned settings, not all inherited SDK options. Each listed
setting is optional to configure in this pilot; required or conditional settings
need a schema extension before adding a component that depends on them.

Describe each method independently, even where settings have equivalent purposes.
For span-stacktrace, the property duration uses strings such as `5ms`, whereas
declarative `min_duration` is an integer in milliseconds, defaulting to `5`.
A negative property duration disables registration through SDK autoconfiguration;
a negative declarative duration is rejected by the processor constructor.
These distinctions belong in the respective settings, so either method can be
rendered on its own. Additional methods, including programmatic configuration,
remain to be explored with further components.

## Review checklist

* Verify purpose, activation, and properties against implementation and tests.
* Verify stability against the root README and coordinates against publishing conventions.
* Verify runtime Java against shared build conventions and any module override.
* Check documentation links and YAML syntax, and run repository Markdown linting.

This draft adds no schema validator, generated documentation, or Explorer integration.
