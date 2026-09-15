"""Unit tests for classify.py.

Run: python3 -m unittest discover -s .github/scripts/draft-release-notes
"""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent


def _load(name: str):
    spec = importlib.util.spec_from_file_location(name, HERE / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    # Register before exec so @dataclass can resolve the module namespace.
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


classify = _load("classify")


def bundle(**meta):
    return classify.PrBundle(pr=1, dir=HERE, meta=meta, diff=meta.pop("_diff", ""))


class PreclassifyTest(unittest.TestCase):
    def test_omits_when_no_src_main_touched(self):
        decision = classify.preclassify(
            bundle(touches_src_main=False, files=[{"path": "README.md"}])
        )
        self.assertIsNotNone(decision)
        self.assertEqual(decision["decision"], "omit")
        self.assertIsNone(decision["section"])

    def test_defers_to_llm_when_src_main_touched(self):
        self.assertIsNone(
            classify.preclassify(
                bundle(touches_src_main=True, files=[{"path": "a/src/main/java/A.java"}])
            )
        )


class ChangedPathsTest(unittest.TestCase):
    def test_accepts_dict_and_string_entries(self):
        b = bundle(files=[{"path": "a.java"}, "b.java", {"no_path": 1}, 7])
        self.assertEqual(classify.changed_paths(b), ["a.java", "b.java"])


class KnownModulesTest(unittest.TestCase):
    def test_prefers_newest_spelling_and_skips_change_type_headings(self):
        changelog = HERE / "_test_changelog.md"
        changelog.write_text(
            "# Changelog\n\n"
            "## Unreleased\n\n"
            "### :warning: Breaking changes\n\n"
            "### JMX metrics\n\n"          # newest spelling wins
            "### Azure resources - New \U0001F31F\n\n"
            "## Version 1.0.0\n\n"
            "### JMX Metrics\n\n"          # older casing must lose
            "### \U0001F6E0️ Bug fixes\n",
            encoding="utf-8",
        )
        original = classify.CHANGELOG_PATH
        classify.CHANGELOG_PATH = changelog
        try:
            self.assertEqual(
                classify.known_modules(), ["Azure resources", "JMX metrics"]
            )
        finally:
            classify.CHANGELOG_PATH = original
            changelog.unlink()


class BuildPromptTest(unittest.TestCase):
    def test_renders_without_format_errors(self):
        # Regression: the prompt template once embedded a literal JSON schema,
        # so str.format() raised KeyError and every run crashed on the first PR.
        prompt = classify.build_prompt(
            bundle(title="T", files=[{"path": "a/src/main/java/A.java"}], _diff="diff --git a/x b/x\n+x\n"),
            "RULES",
        )
        self.assertIn("RULES", prompt)
        self.assertIn("a/src/main/java/A.java", prompt)

    def test_strips_changelog_from_diff(self):
        diff = (
            "diff --git a/CHANGELOG.md b/CHANGELOG.md\n+- old entry\n"
            "diff --git a/a/src/main/java/A.java b/a/src/main/java/A.java\n+code\n"
        )
        self.assertNotIn("old entry", classify.strip_changelog_diff(diff))
        self.assertIn("code", classify.strip_changelog_diff(diff))

    def test_trims_oversized_diff_within_utf16_budget(self):
        big = "diff --git a/a b/a\n" + ("+x\n" * 200_000)
        prompt = classify.build_prompt(
            bundle(title="T", files=[{"path": "a"}], _diff=big), "RULES"
        )
        self.assertLessEqual(
            classify.utf16_units(prompt), classify.MAX_PROMPT_UTF16_UNITS
        )

    def test_oversized_diff_keeps_hunks_from_a_late_user_facing_file(self):
        # Regression: head-truncating the combined patch dropped every hunk for
        # the files sorting last, hiding breaking changes from the classifier.
        early = "diff --git a/z/src/test/java/T.java b/z/src/test/java/T.java\n" + (
            "+filler\n" * 100_000
        )
        late = (
            "diff --git a/m/src/main/java/Api.java b/m/src/main/java/Api.java\n"
            "@@ -1,3 +1,3 @@\n"
            "-  public void removedMethod();\n"
        )
        prompt = classify.build_prompt(
            bundle(
                title="T",
                files=[{"path": "z/src/test/java/T.java"}, {"path": "m/src/main/java/Api.java"}],
                _diff=early + late,
            ),
            "RULES",
        )
        self.assertLessEqual(
            classify.utf16_units(prompt), classify.MAX_PROMPT_UTF16_UNITS
        )
        self.assertIn("removedMethod", prompt)


class CompactDiffTest(unittest.TestCase):
    SRC_MAIN = "diff --git a/m/src/main/java/A.java b/m/src/main/java/A.java\n" + "+a\n" * 500
    DEPRECATED = (
        "diff --git a/m/src/main/java/B.java b/m/src/main/java/B.java\n"
        "+  @Deprecated\n" + "+b\n" * 500
    )
    TEST = "diff --git a/m/src/test/java/C.java b/m/src/test/java/C.java\n" + "+c\n" * 500
    DOCS = "diff --git a/README.md b/README.md\n" + "+d\n" * 500

    def test_returns_input_unchanged_when_it_fits(self):
        diff = self.SRC_MAIN
        self.assertEqual(classify.compact_diff(diff, len(diff)), diff)

    def test_every_kept_file_contributes_an_excerpt(self):
        diff = self.SRC_MAIN + self.DEPRECATED + self.TEST + self.DOCS
        out = classify.compact_diff(diff, 2_000)
        for path in ("A.java", "B.java", "C.java", "README.md"):
            self.assertIn(path, out)

    def test_preserves_original_file_order(self):
        diff = self.DOCS + self.SRC_MAIN
        out = classify.compact_diff(diff, 1_000)
        self.assertLess(out.index("README.md"), out.index("A.java"))

    def test_drops_lowest_priority_files_before_starving_user_facing_ones(self):
        # Budget only affords two sections at MIN_DIFF_SECTION_CHARS each, so the
        # deprecation and src/main excerpts must win over the test and docs ones.
        diff = self.DOCS + self.TEST + self.SRC_MAIN + self.DEPRECATED
        out = classify.compact_diff(diff, 2 * classify.MIN_DIFF_SECTION_CHARS)
        self.assertIn("@Deprecated", out)
        self.assertIn("A.java", out)
        self.assertNotIn("README.md", out)
        self.assertNotIn("C.java", out)

    def test_head_truncates_when_there_are_no_file_headers(self):
        out = classify.compact_diff("commit abcdef\n" + "x" * 5_000, 100)
        self.assertIn(classify.DIFF_SECTION_TRUNCATION_MARKER.strip(), out)

    def test_zero_budget_yields_empty_output(self):
        self.assertEqual(classify.compact_diff(self.SRC_MAIN, 0), "")


class SectionPriorityTest(unittest.TestCase):
    def test_ranks_deprecation_above_plain_src_main_above_test_above_rest(self):
        self.assertEqual(
            [
                classify.section_priority(CompactDiffTest.DEPRECATED),
                classify.section_priority(CompactDiffTest.SRC_MAIN),
                classify.section_priority(CompactDiffTest.TEST),
                classify.section_priority(CompactDiffTest.DOCS),
            ],
            [0, 1, 2, 3],
        )

    def test_reads_the_post_image_path(self):
        section = "diff --git a/old/src/main/java/A.java b/new/src/main/java/A.java\n"
        self.assertEqual(classify.section_path(section), "new/src/main/java/A.java")


class ValidateTest(unittest.TestCase):
    def ok(self, **over):
        d = {
            "decision": "include",
            "module": "Disk buffering",
            "section": "enhancements",
            "bullet": "Add a thing.",
            "evidence": "+code",
            "surface": "api",
            "user_visible_effect": "adds a thing",
        }
        d.update(over)
        return d

    def test_accepts_valid_include(self):
        self.assertEqual(classify.validate(self.ok()), [])

    def test_requires_module_for_include(self):
        self.assertIn("module required for include", classify.validate(self.ok(module="")))

    def test_rejects_multiline_bullet(self):
        self.assertIn("bullet must be a single line", classify.validate(self.ok(bullet="a\nb")))

    def test_rejects_embedded_pr_link(self):
        bad = self.ok(bullet="Add a thing. https://github.com/o/r/pull/12")
        self.assertIn("bullet must not include a PR link", classify.validate(bad))

    def test_omit_requires_null_section(self):
        d = self.ok(decision="omit", section="enhancements", bullet=None)
        self.assertIn("section must be null for omit", classify.validate(d))


if __name__ == "__main__":
    unittest.main()
