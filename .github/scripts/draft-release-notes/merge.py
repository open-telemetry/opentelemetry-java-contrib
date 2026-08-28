#!/usr/bin/env python3
"""Merge per-PR decision.json files into a CHANGELOG Unreleased section.

Reads build/changelog-bundle/prs/<N>/decision.json for every PR that has one
and renders the `## Unreleased` block in this repo's changelog format:

  - entries classified `breaking` go into a single leading
    `### :warning: Breaking changes` section (matching Version 1.59.0);
  - every other entry is grouped under `### <Module>`, sorted alphabetically,
    with `- New 🌟` appended for a module's first appearance;
  - bullets within a section are sorted by ascending PR number.

Any entry in a state other than `include`/`omit`, or an `include` without a
bullet, is reported on stderr and excluded.

Exits non-zero if any candidate cannot be rendered, which includes a PR-less
commit bundle under build/changelog-bundle/commits/ that touches user-facing
`/src/main/` sources. Bullets are PR-keyed, so such a commit has no link to
render and must be added to the `## Unreleased` block by hand.

The block contains only the `## Unreleased` heading and its sections. Release
time rewrites that heading into `## Version X.Y.Z (date)` via the sed in
.github/workflows/prepare-release-branch.yml, which requires the literal
`## Unreleased` line to be present.

By default writes to stdout. Use --splice to rewrite CHANGELOG.md in place,
replacing the entire `## Unreleased` block. Any hand-written content in that
block is discarded; review the resulting diff to recover anything worth
keeping.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import textwrap
from pathlib import Path

BUNDLE_DIR = Path("build/changelog-bundle")
BUNDLE_ROOT = BUNDLE_DIR / "prs"
COMMITS_ROOT = BUNDLE_DIR / "commits"
CHANGELOG = Path("CHANGELOG.md")

BREAKING_HEADING = "### :warning: Breaking changes"

PR_URL = "https://github.com/open-telemetry/opentelemetry-java-contrib/pull/{pr}"


def load_decisions() -> list[dict]:
    out = []
    if not BUNDLE_ROOT.is_dir():
        sys.exit(f"{BUNDLE_ROOT} not found")
    for d in sorted(BUNDLE_ROOT.iterdir(), key=lambda p: int(p.name) if p.name.isdigit() else 0):
        if not d.is_dir() or not d.name.isdigit():
            continue
        p = d / "decision.json"
        if not p.exists():
            continue
        try:
            obj = json.loads(p.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as e:
            print(f"#{d.name}: decision.json unreadable: {e}", file=sys.stderr)
            continue
        obj.setdefault("pr", int(d.name))
        out.append(obj)
    return out


def unrenderable_commits() -> list[dict]:
    """PR-less commit bundles carrying user-facing changes.

    fetch.py bundles commits whose subject has no `(#NNN)` suffix under
    commits/, but classification and rendering are PR-keyed: every bullet ends
    in a PR link. main is protected, so such a commit is rare enough that it is
    not worth a second link format — but it must never be dropped silently, so
    report it and fail. Commits that touch no user-facing /src/main/ source
    would be omitted by the classifier anyway, so they are ignored here.
    """
    out: list[dict] = []
    if not COMMITS_ROOT.is_dir():
        return out
    for d in sorted(COMMITS_ROOT.iterdir()):
        meta_path = d / "meta.json"
        if not d.is_dir() or not meta_path.exists():
            continue
        try:
            meta = json.loads(meta_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as e:
            out.append({"commit_hash": d.name, "subject": f"meta.json unreadable: {e}"})
            continue
        if meta.get("pr") is None and meta.get("touches_src_main"):
            out.append(meta)
    return out


def format_bullet(bullet: str, pr: int) -> str:
    bullet = bullet.rstrip()
    # Wrap the bullet text to match repo style (see .editorconfig
    # max_line_length = 100). First line starts with "- " (2-char prefix);
    # continuation lines indent 2 spaces so they align with the bullet text.
    # textwrap preserves inline code spans and punctuation verbatim.
    #
    # Replace spaces inside `...` code spans with U+00A0 (non-breaking space)
    # so textwrap does not split the span across lines. Python textwrap treats
    # only ASCII whitespace as break opportunities, so NBSP survives the fill
    # and is swapped back to a regular space in the output.
    NBSP = "\u00a0"
    protected = re.sub(
        r"`[^`\n]+`",
        lambda m: m.group(0).replace(" ", NBSP),
        bullet,
    )
    wrapped = textwrap.fill(
        protected,
        width=100,
        initial_indent="- ",
        subsequent_indent="  ",
        break_long_words=False,
        break_on_hyphens=False,
    )
    wrapped = wrapped.replace(NBSP, " ")
    return f"{wrapped}\n  ([#{pr}]({PR_URL.format(pr=pr)}))"


def group_decisions(decisions: list[dict]) -> tuple[list[dict], dict[str, list[dict]], int]:
    """Split decisions into (breaking entries, module -> entries, error count)."""
    breaking: list[dict] = []
    modules: dict[str, list[dict]] = {}
    errors = 0
    for d in decisions:
        pr = d.get("pr")
        decision = d.get("decision")
        if decision == "omit":
            continue

        reason: str | None = None
        if decision != "include":
            reason = f"unknown decision {decision!r}"
        elif not d.get("bullet"):
            reason = "empty bullet"
        if reason is not None:
            print(f"#{pr}: skipping, {reason}", file=sys.stderr)
            errors += 1
            continue

        if d.get("section") == "breaking":
            breaking.append(d)
        else:
            modules.setdefault(d.get("module") or "Other", []).append(d)
    return breaking, modules, errors


def render_generated_block(breaking: list[dict], modules: dict[str, list[dict]]) -> str:
    """Render the `## Unreleased` block."""
    out_lines = ["## Unreleased", ""]

    if breaking:
        out_lines.append(BREAKING_HEADING)
        out_lines.append("")
        for entry in sorted(breaking, key=lambda d: d["pr"]):
            out_lines.append(format_bullet(entry["bullet"], entry["pr"]))
        out_lines.append("")

    for mod_name in sorted(modules):
        mod_entries = modules[mod_name]
        header = f"### {mod_name}"
        if any(e.get("section") == "new-module" for e in mod_entries):
            header += " - New 🌟"
        out_lines.append(header)
        out_lines.append("")
        for entry in sorted(mod_entries, key=lambda d: d["pr"]):
            out_lines.append(format_bullet(entry["bullet"], entry["pr"]))
        out_lines.append("")

    block = "\n".join(out_lines)
    if not block.endswith("\n"):
        block += "\n"
    return block


def splice_unreleased(text: str, block: str) -> str:
    """Replace the whole `## Unreleased` block in CHANGELOG text."""
    # Match `## Unreleased` through the next `## ` heading, or end of file
    # if Unreleased is the final heading.
    m = re.search(r"^## Unreleased\n.*?(?=^## |\Z)", text, re.S | re.M)
    if not m:
        raise ValueError("## Unreleased section not found in CHANGELOG.md")
    return text[: m.start()] + block + "\n" + text[m.end():]


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--missing-ok", action="store_true",
                    help="do not warn about PRs lacking decision.json")
    ap.add_argument("--report", action="store_true",
                    help="also print a module-count summary on stderr")
    ap.add_argument("--splice", action="store_true",
                    help="rewrite CHANGELOG.md in place (otherwise write to stdout)")
    args = ap.parse_args()

    decisions = load_decisions()

    if not args.missing_ok:
        # Warn about PR bundles with no decision artifact.
        bundles = {int(d.name) for d in BUNDLE_ROOT.iterdir() if d.is_dir() and d.name.isdigit()}
        decided = {d["pr"] for d in decisions}
        missing = sorted(bundles - decided)
        if missing:
            print(
                f"WARNING: {len(missing)} PR bundles have no decision.json: "
                + ", ".join(f"#{n}" for n in missing[:20])
                + (" ..." if len(missing) > 20 else ""),
                file=sys.stderr,
            )

    breaking, modules, errors = group_decisions(decisions)
    block = render_generated_block(breaking, modules)

    for meta in unrenderable_commits():
        commit_hash = str(meta.get("commit_hash") or "")[:12]
        print(
            f"ERROR: commit {commit_hash} touches user-facing src/main but has "
            f"no PR number; add its bullet by hand: {meta.get('subject')}",
            file=sys.stderr,
        )
        errors += 1

    if args.splice:
        if not CHANGELOG.exists():
            sys.exit(f"{CHANGELOG} not found")
        try:
            new_text = splice_unreleased(CHANGELOG.read_text(encoding="utf-8"), block)
        except ValueError as e:
            sys.exit(str(e))
        CHANGELOG.write_text(new_text, encoding="utf-8")
        bullet_count = len(breaking) + sum(len(v) for v in modules.values())
        print(f"Rewrote {CHANGELOG} ({bullet_count} PR-linked bullets)", file=sys.stderr)
    else:
        sys.stdout.write(block)

    if args.report:
        if breaking:
            print(f"Breaking changes: {len(breaking)}", file=sys.stderr)
        print("Module counts:", file=sys.stderr)
        for mod_name in sorted(modules.keys()):
            print(f"  {mod_name}: {len(modules[mod_name])}", file=sys.stderr)

    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
