#!/usr/bin/env python3
"""Merge per-PR decision.json files into a CHANGELOG Unreleased section.

Reads build/changelog-bundle/prs/<N>/decision.json for every PR that has
one, groups kept entries by module name, sorts each module's entries by
ascending PR number, and prints the Unreleased markdown block to stdout.

The output contains only the `## Unreleased` heading and module sections;
the SDK-version preamble is inserted at release time by
.github/scripts/update-changelog-for-release.sh.

Any entry in state other than `include`/`omit`, or `include` without a
summary/bullet, is reported on stderr and excluded.

By default writes to stdout. Use --splice to rewrite CHANGELOG.md in
place, replacing the entire `## Unreleased` block. Any hand-written
content in that block is discarded; review the resulting diff to recover
anything worth keeping.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import textwrap
from pathlib import Path

BUNDLE_ROOT = Path("build/changelog-bundle/prs")
CHANGELOG = Path("CHANGELOG.md")

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

    modules: dict[str, list[dict]] = {}
    errors = 0
    for d in decisions:
        pr = d.get("pr")
        decision = d.get("decision")
        section = d.get("section")
        
        if decision == "omit":
            continue
            
        reason: str | None = None
        # Fallback to 'bullet' if the older output format is somehow retained
        text = d.get("summary") or d.get("bullet")
        mod_name = d.get("module", "Other")

        if decision != "include":
            reason = f"unknown decision {decision!r}"
        elif not text:
            reason = "empty summary"
            
        if reason is not None:
            print(f"#{pr}: skipping, {reason}", file=sys.stderr)
            errors += 1
            continue
            
        if mod_name not in modules:
            modules[mod_name] = []
        modules[mod_name].append(d)

    out_lines = [
        "## Unreleased",
        "",
    ]

    for mod_name in sorted(modules.keys()):
        mod_entries = modules[mod_name]
        
        is_new_module = any(e.get("section") == "new-module" for e in mod_entries)
        
        header = f"### {mod_name}"
        if is_new_module:
            header += " - New 🌟"
            
        out_lines.append(header)
        out_lines.append("")
        
        items = sorted(mod_entries, key=lambda d: d["pr"])
        for entry in items:
            section = entry.get("section")
            prefix = ""
            if section == "breaking":
                prefix = "**[Breaking]** "
            elif section == "deprecations":
                prefix = "**[Deprecation]** "
            elif section == "bug-fixes":
                prefix = "**[Bug Fix]** "
                
            text = entry.get("summary") or entry.get("bullet")
            full_text = f"{prefix}{text}"
            
            out_lines.append(format_bullet(full_text, entry["pr"]))
        out_lines.append("")

    block = "\n".join(out_lines)
    if not block.endswith("\n"):
        block += "\n"

    if args.splice:
        if not CHANGELOG.exists():
            sys.exit(f"{CHANGELOG} not found")
        text = CHANGELOG.read_text(encoding="utf-8")
        # Match `## Unreleased` through the next `## ` heading, or end of file
        # if Unreleased is the final heading.
        m = re.search(r"^## Unreleased\n.*?(?=^## |\Z)", text, re.S | re.M)
        if not m:
            sys.exit("## Unreleased section not found in CHANGELOG.md")
        new_text = text[: m.start()] + block + "\n" + text[m.end():]
        CHANGELOG.write_text(new_text, encoding="utf-8")
        bullet_count = sum(len(v) for v in modules.values())
        print(f"Rewrote {CHANGELOG} ({bullet_count} PR-linked bullets)", file=sys.stderr)
    else:
        sys.stdout.write(block)

    if args.report:
        print("Module counts:", file=sys.stderr)
        for mod_name in sorted(modules.keys()):
            print(f"  {mod_name}: {len(modules[mod_name])}", file=sys.stderr)

    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
