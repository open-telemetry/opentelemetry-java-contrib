"""Unit tests for fetch.py."""

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


fetch = _load("fetch")


def refs(text):
    return {m.group(1) or m.group(2) for m in fetch.ISSUE_REF_RE.finditer(text)}


class IssueRefTest(unittest.TestCase):
    def test_matches_bare_hash_reference(self):
        self.assertEqual(refs("fixes #123"), {"123"})

    def test_matches_this_repository_url(self):
        url = "https://github.com/open-telemetry/opentelemetry-java-contrib/issues/42"
        self.assertEqual(refs(url), {"42"})

    def test_ignores_other_repositories(self):
        # Regression: the unanchored pattern resolved foreign issue numbers
        # as if they belonged to this repository.
        url = "https://github.com/open-telemetry/opentelemetry-java/issues/999"
        self.assertNotIn("999", refs(url))

    def test_ignores_zero_and_leading_zero(self):
        self.assertEqual(refs("see #0"), set())

    def test_ignores_hash_inside_identifiers(self):
        self.assertEqual(refs("abc#123"), set())


class VersionRangeTest(unittest.TestCase):
    def test_minor_release_ranges_from_previous_minor(self):
        self.assertEqual(fetch.compute_default_range("1.60.0"), "v1.59.0..HEAD")

    def test_rejects_patch_version(self):
        with self.assertRaises(RuntimeError):
            fetch.compute_default_range("1.59.1")


if __name__ == "__main__":
    unittest.main()
