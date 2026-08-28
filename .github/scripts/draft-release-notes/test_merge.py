"""Unit tests for merge.py."""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent


def _load(name: str):
    spec = importlib.util.spec_from_file_location(name, HERE / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


merge = _load("merge")


def entry(pr, section="enhancements", module="Disk buffering", bullet="Add a thing."):
    return {"pr": pr, "decision": "include", "section": section,
            "module": module, "bullet": bullet}


class GroupDecisionsTest(unittest.TestCase):
    def test_separates_breaking_from_modules(self):
        breaking, modules, errors = merge.group_decisions(
            [entry(1, section="breaking"), entry(2)]
        )
        self.assertEqual([e["pr"] for e in breaking], [1])
        self.assertEqual(list(modules), ["Disk buffering"])
        self.assertEqual(errors, 0)

    def test_counts_unusable_entries_without_including_them(self):
        _, modules, errors = merge.group_decisions(
            [entry(1, bullet=None), {"pr": 2, "decision": "maybe"}]
        )
        self.assertEqual(modules, {})
        self.assertEqual(errors, 2)

    def test_omit_is_not_an_error(self):
        _, _, errors = merge.group_decisions([{"pr": 1, "decision": "omit"}])
        self.assertEqual(errors, 0)


class RenderGeneratedBlockTest(unittest.TestCase):
    def test_breaking_section_leads_and_modules_are_alphabetical(self):
        breaking, modules, _ = merge.group_decisions([
            entry(3, module="Zebra"),
            entry(1, section="breaking"),
            entry(2, module="Alpha"),
        ])
        block = merge.render_generated_block(breaking, modules)
        self.assertLess(block.index(merge.BREAKING_HEADING), block.index("### Alpha"))
        self.assertLess(block.index("### Alpha"), block.index("### Zebra"))

    def test_new_module_marker(self):
        breaking, modules, _ = merge.group_decisions([entry(1, section="new-module")])
        self.assertIn("### Disk buffering - New \U0001F31F",
                      merge.render_generated_block(breaking, modules))

    def test_bullets_sorted_by_pr_number(self):
        breaking, modules, _ = merge.group_decisions([
            entry(9, bullet="Later."), entry(4, bullet="Earlier.")
        ])
        block = merge.render_generated_block(breaking, modules)
        self.assertLess(block.index("Earlier."), block.index("Later."))

    def test_empty_input_still_emits_unreleased_heading(self):
        self.assertEqual(merge.render_generated_block([], {}), "## Unreleased\n")


class SpliceUnreleasedTest(unittest.TestCase):
    ORIGINAL = (
        "# Changelog\n\n## Unreleased\n\n### Old\n\n- stale\n\n"
        "## Version 1.0.0 (2020-01-01)\n\n### Kept\n\n- keep me\n"
    )

    def test_replaces_only_the_unreleased_block(self):
        out = merge.splice_unreleased(self.ORIGINAL, "## Unreleased\n\n### New\n\n- fresh\n")
        self.assertIn("### New", out)
        self.assertNotIn("stale", out)
        self.assertIn("## Version 1.0.0 (2020-01-01)", out)
        self.assertIn("keep me", out)

    def test_is_idempotent(self):
        block = "## Unreleased\n\n### New\n\n- fresh\n"
        once = merge.splice_unreleased(self.ORIGINAL, block)
        self.assertEqual(once, merge.splice_unreleased(once, block))

    def test_preserves_the_literal_unreleased_heading(self):
        # prepare-release-branch.yml greps for '^## Unreleased$'.
        out = merge.splice_unreleased(self.ORIGINAL, "## Unreleased\n")
        self.assertIn("\n## Unreleased\n", out)

    def test_handles_unreleased_as_final_heading(self):
        out = merge.splice_unreleased("# C\n\n## Unreleased\n\n- old\n",
                                      "## Unreleased\n\n- new\n")
        self.assertIn("- new", out)
        self.assertNotIn("- old", out)

    def test_raises_when_heading_missing(self):
        with self.assertRaises(ValueError):
            merge.splice_unreleased("# Changelog\n\n## Version 1.0.0\n", "## Unreleased\n")


if __name__ == "__main__":
    unittest.main()
