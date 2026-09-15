"""Unit tests for merge.py."""

from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
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


class UnrenderableCommitsTest(unittest.TestCase):
    """PR-less commits must never be dropped silently (bullets are PR-keyed)."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.original = merge.COMMITS_ROOT
        merge.COMMITS_ROOT = Path(self.tmp.name) / "commits"
        merge.COMMITS_ROOT.mkdir(parents=True)

    def tearDown(self):
        merge.COMMITS_ROOT = self.original
        self.tmp.cleanup()

    def write(self, name, meta):
        d = merge.COMMITS_ROOT / name
        d.mkdir()
        (d / "meta.json").write_text(json.dumps(meta), encoding="utf-8")

    def test_reports_pr_less_commit_touching_user_facing_source(self):
        self.write("commit-abc123abc123", {
            "commit_hash": "abc123abc123def", "pr": None,
            "touches_src_main": True, "subject": "Fix a thing",
        })
        self.assertEqual(
            [m["commit_hash"] for m in merge.unrenderable_commits()],
            ["abc123abc123def"],
        )

    def test_ignores_commit_with_no_user_facing_source(self):
        self.write("commit-abc123abc123", {
            "commit_hash": "abc", "pr": None,
            "touches_src_main": False, "subject": "Update docs",
        })
        self.assertEqual(merge.unrenderable_commits(), [])

    def test_reports_unreadable_meta(self):
        d = merge.COMMITS_ROOT / "commit-abc123abc123"
        d.mkdir()
        (d / "meta.json").write_text("{not json", encoding="utf-8")
        self.assertEqual(len(merge.unrenderable_commits()), 1)

    def test_missing_commits_dir_is_not_an_error(self):
        merge.COMMITS_ROOT = Path(self.tmp.name) / "absent"
        self.assertEqual(merge.unrenderable_commits(), [])


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
