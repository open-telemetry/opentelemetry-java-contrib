#!/usr/bin/env python3
"""Classify each PR in build/changelog-bundle/prs/ into a CHANGELOG section.

For every PR bundle produced by .github/scripts/draft-release-notes/fetch.py,
this script writes a per-PR decision artifact. The artifact forces a one-PR-
at-a-time diff-based decision before any CHANGELOG text is written, which
is the design intent of the draft-release-notes skill.

Outputs per PR (under build/changelog-bundle/prs/<N>/):
  - prompt.md             — LLM prompt with the diff embedded
  - decision.json             — structured classification (schema below)
  - decision.md               — human-readable rendering
  - cli-response.jsonl / .txt — raw copilot stdout (forensic; always written
                               on non-preclassify runs regardless of outcome)

decision.json schema:
  {
    "pr": <int>,
    "module": <string (the user-facing module name inferred from the file paths)>,
    "decision": "include" | "omit",
    "section": "breaking" | "deprecations" | "new-module"
             | "enhancements" | "bug-fixes" | null,
    "surface": <short phrase>,
    "user_visible_effect": <one sentence or "none">,
    "bullet": <final CHANGELOG sentence without PR link> | null,
    "evidence": <2-4 line verbatim quote from the diff>,
    "source": "preclassify" | "llm"
  }

Invokes `copilot` (must be on PATH) per PR. Response is expected on stdout
as a JSON object matching the schema above (markdown code fences tolerated).
Model is overridable via $CLASSIFY_MODEL (default: gpt-5.4-mini).

Run with --jobs N for parallelism (default 4).
Idempotent: skips PRs whose decision.json already exists unless --force.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from pathlib import Path

BUNDLE_ROOT = Path("build/changelog-bundle/prs")
RULES_PATH = Path(__file__).resolve().parent / "rules.md"
CHANGELOG_PATH = Path("CHANGELOG.md")
# `### ` headings that are change-type sections rather than module names.
# Initial diff cap. The build_prompt() function further trims the diff if the
# full prompt would exceed MAX_PROMPT_UTF16_UNITS.
MAX_DIFF_CHARS = 20_000
# Hard cap on total prompt length. Windows CreateProcess rejects command
# lines longer than 32767 UTF-16 code units, and copilot has no stdin/@file
# prompt input, so the entire prompt must fit in a single argv token. Count
# UTF-16 units rather than Python characters, since non-BMP characters in a
# diff occupy two units each. Leaves headroom for flags and argv quoting.
MAX_PROMPT_UTF16_UNITS = 24_000
WINDOWS_COMMAND_LINE_UTF16_LIMIT = 32_767
# Smallest excerpt worth spending budget on: enough for a file's `diff --git`
# header plus the first hunk. When the budget cannot give every changed file at
# least this much, the lowest-priority files are dropped entirely rather than
# starving the user-facing ones down to bare headers.
MIN_DIFF_SECTION_CHARS = 400
DIFF_SECTION_TRUNCATION_MARKER = "\n...[remaining hunks truncated for length]...\n"
# Retry a PR once before declaring it failed; most failures are transient
# (timeout, truncated response, malformed JSON).
MAX_LLM_ATTEMPTS = 2
# Bump to invalidate every cached decision.json.
CLASSIFIER_VERSION = 1


VALID_SECTIONS = {
    "breaking",
    "deprecations",
    "new-module",
    "enhancements",
    "bug-fixes",
    None,
}

PROMPT_TEMPLATE = """You are classifying a single PR from the \
opentelemetry-java-contrib repository for inclusion in CHANGELOG.md.

Apply the classification rules below. Respond with a single JSON object \
matching the schema described in those rules and nothing else (no prose, \
no code fences).

---BEGIN RULES---
{rules}
---END RULES---

PR number: {pr}
Title (for link bookkeeping only, not evidence): {title}

Changed files:
{files_summary}

Module names already used in this repository's CHANGELOG (reuse the exact
spelling and capitalization when the PR touches one of these modules; only
invent a new name for a genuinely new module):
{known_modules}

---BEGIN DIFF---
{diff}
---END DIFF---
"""


def known_modules() -> list[str]:
    """Module names already used as `### ` headings in CHANGELOG.md.

    Contrib groups the changelog by module, so the model must reuse the
    established spelling ("GCP authentication extension", not "GCP auth
    extension") or a module's entries fragment across releases.
    """
    try:
        text = CHANGELOG_PATH.read_text(encoding="utf-8")
    except OSError:
        return []
    # CHANGELOG.md is newest-first, so the first spelling encountered for a
    # given module is the most recent one. The file has historical casing
    # drift ("JMX Metrics" vs "JMX metrics"); offering both would just move
    # the ambiguity into the prompt, so keep only the newest spelling.
    seen: dict[str, str] = {}
    for line in text.splitlines():
        if not line.startswith("### "):
            continue
        name = line[4:].strip()
        name = re.sub(r"\s*-\s*New\s*\U0001F31F\s*$", "", name).strip()
        # Skip change-type headings (":warning: Breaking changes", emoji ones).
        if name.startswith(":") or not re.match(r"^[A-Za-z]", name):
            continue
        seen.setdefault(name.lower(), name)
    return sorted(seen.values())


def load_rules() -> str:
    try:
        return RULES_PATH.read_text(encoding="utf-8")
    except FileNotFoundError:
        sys.exit(f"rules file not found: {RULES_PATH}")


@dataclass
class PrBundle:
    pr: int
    dir: Path
    meta: dict
    diff: str


def iter_bundles() -> list[PrBundle]:
    if not BUNDLE_ROOT.is_dir():
        sys.exit(f"{BUNDLE_ROOT} not found; run .github/scripts/draft-release-notes/fetch.py first")
    out = []
    for d in sorted(BUNDLE_ROOT.iterdir(), key=lambda p: int(p.name) if p.name.isdigit() else 0):
        if not d.is_dir() or not d.name.isdigit():
            continue
        meta_path = d / "meta.json"
        diff_path = d / "patch.diff"
        if not meta_path.exists() or not diff_path.exists():
            continue
        meta = json.loads(meta_path.read_text(encoding="utf-8"))
        diff = diff_path.read_text(encoding="utf-8", errors="replace")
        out.append(PrBundle(pr=int(d.name), dir=d, meta=meta, diff=diff))
    return out


# --- preclassifier ---------------------------------------------------------


def utf16_units(text: str) -> int:
    return len(text.encode("utf-16-le")) // 2


def command_line_utf16_units(argv: list[str]) -> int:
    return utf16_units(subprocess.list2cmdline(argv))


def effective_model() -> str:
    return os.environ.get("CLASSIFY_MODEL", "gpt-5.4-mini")


def changed_paths(bundle: PrBundle) -> list[str]:
    """Changed file paths, tolerating both dict and bare-string entries."""
    paths = []
    for item in bundle.meta.get("files", []):
        path = item.get("path") if isinstance(item, dict) else item
        if isinstance(path, str):
            paths.append(path)
    return paths


def split_diff_sections(diff: str) -> list[str]:
    """Split a unified diff into one string per changed file."""
    return [
        section
        for section in re.split(r"(?=^diff --git )", diff, flags=re.MULTILINE)
        if section.startswith("diff --git ")
    ]


def strip_changelog_diff(diff: str) -> str:
    """Drop CHANGELOG.md from the diff so prior entries are not read as evidence."""
    return "".join(
        section
        for section in split_diff_sections(diff)
        if not section.startswith("diff --git a/CHANGELOG.md b/CHANGELOG.md")
    )


def section_path(section: str) -> str:
    """The post-image path of a diff section, or "" if the header is unparseable."""
    match = re.match(r"diff --git a/(?:\S+) b/(\S+)", section)
    return match.group(1) if match else ""


def section_priority(section: str) -> int:
    """Rank a diff section by how likely it is to carry changelog evidence.

    Lower sorts first. User-facing runtime sources decide the section and the
    bullet, and a `@Deprecated` delta inside one is the only signal for the
    deprecations section, so those excerpts must survive the budget even when a
    PR also churns hundreds of test or generated files.
    """
    path = section_path(section)
    user_facing = "/src/main/" in path
    if user_facing and "@Deprecated" in section:
        return 0
    if user_facing:
        return 1
    if "/src/" in path:
        return 2
    return 3


def compact_diff(diff: str, budget: int) -> str:
    """Trim `diff` to `budget` characters, excerpting across all changed files.

    Head-truncating the combined patch drops every hunk for the files that
    happen to sort last, which silently hides breaking changes. Instead, give
    each file section an even share of the budget in priority order, let slack
    from sections smaller than their share flow to clipped ones, and reassemble
    in the original file order. Each clipped section keeps its `diff --git`
    header and leading hunks, so the model always sees which file it is reading.

    The result can exceed `budget` slightly once truncation markers are added;
    build_prompt() re-runs with a lower budget if the prompt still overflows.
    """
    if budget <= 0:
        return ""
    if len(diff) <= budget:
        return diff
    sections = split_diff_sections(diff)
    if not sections:
        # No parseable file headers (e.g. a bare `git show` preamble); fall back
        # to head truncation since there are no sections to spread across.
        return diff[:budget].rstrip("\n") + DIFF_SECTION_TRUNCATION_MARKER

    order = sorted(
        range(len(sections)), key=lambda i: (section_priority(sections[i]), i)
    )
    keep = order[: max(1, budget // MIN_DIFF_SECTION_CHARS)]

    allocations = dict.fromkeys(range(len(sections)), 0)
    remaining = budget
    for position, i in enumerate(keep):
        share = remaining // (len(keep) - position)
        take = min(len(sections[i]), share)
        allocations[i] = take
        remaining -= take
    # Sections clipped before the slack appeared get a second pass at it.
    for i in keep:
        if remaining <= 0:
            break
        deficit = len(sections[i]) - allocations[i]
        if deficit <= 0:
            continue
        extra = min(deficit, remaining)
        allocations[i] += extra
        remaining -= extra

    out = []
    for i, section in enumerate(sections):
        take = allocations[i]
        if take >= len(section):
            out.append(section)
        elif take > 0:
            out.append(section[:take].rstrip("\n") + DIFF_SECTION_TRUNCATION_MARKER)
    return "".join(out)


def classifier_fingerprint(bundle: PrBundle, rules: str) -> str:
    """Identity of the inputs that produced a decision.

    Cached decisions are reused only while this matches, so editing rules.md,
    changing $CLASSIFY_MODEL, or refetching a PR all invalidate the cache.
    """
    digest = hashlib.sha256()
    digest.update(str(CLASSIFIER_VERSION).encode("ascii"))
    digest.update(effective_model().encode("utf-8"))
    digest.update(build_prompt(bundle, rules).encode("utf-8"))
    digest.update(
        json.dumps(bundle.meta, sort_keys=True, separators=(",", ":")).encode("utf-8")
    )
    return digest.hexdigest()


def preclassify(bundle: PrBundle) -> dict | None:
    """Return a decision dict if we can decide without the LLM, else None."""
    if not bundle.meta.get("touches_src_main"):
        files = changed_paths(bundle)
        return {
            "decision": "omit",
            "module": None,
            "section": None,
            "surface": "test/build/docs only",
            "user_visible_effect": "none",
            "bullet": None,
            "evidence": "no changed paths are user-facing /src/main/: "
            + ", ".join(files[:5])
            + ("..." if len(files) > 5 else ""),
            "source": "preclassify",
        }
    return None


# --- prompt + invocation ---------------------------------------------------

def build_prompt(bundle: PrBundle, rules: str) -> str:
    """Render the classification prompt, trimming the diff to fit the argv cap.

    Upstream's excerpt selection keys on the API-diff snapshot markers that
    opentelemetry-java-instrumentation emits; contrib has no such snapshots, so
    compact_diff() ranks file sections by path instead, and the authoritative
    file list is carried in files_summary.
    """
    diff = strip_changelog_diff(bundle.diff)
    files = bundle.meta.get("files", [])
    file_lines = []
    for item in files[:50]:
        if isinstance(item, dict):
            path = item.get("path")
            if isinstance(path, str):
                file_lines.append(
                    f"  - {path} (+{item.get('additions', 0)}/-{item.get('deletions', 0)})"
                )
        elif isinstance(item, str):
            file_lines.append(f"  - {item}")
    files_summary = "\n".join(file_lines)
    if len(files) > 50:
        files_summary += f"\n  ... and {len(files) - 50} more"

    modules_list = "\n".join(f"  - {m}" for m in known_modules()) or "  (none found)"

    def render(diff_text: str, summary: str) -> str:
        return PROMPT_TEMPLATE.format(
            rules=rules,
            pr=bundle.pr,
            title=bundle.meta.get("title", ""),
            files_summary=summary,
            known_modules=modules_list,
            diff=diff_text,
        )

    truncated_note = "\n  (diff excerpted; changed files list above is authoritative)"
    base_units = utf16_units(render("", files_summary + truncated_note))
    budget = min(MAX_DIFF_CHARS, MAX_PROMPT_UTF16_UNITS - base_units - 200)
    if budget < 0:
        raise RuntimeError("classification rules and PR metadata exceed the prompt size limit")
    while budget >= 0:
        compacted = compact_diff(diff, budget)
        if compacted == diff:
            prompt = render(diff, files_summary)
        else:
            prompt = render(compacted, files_summary + truncated_note)
        excess = utf16_units(prompt) - MAX_PROMPT_UTF16_UNITS
        if excess <= 0:
            return prompt
        budget -= excess + 100
    raise RuntimeError("classification prompt exceeds the prompt size limit")


def invoke_cli(prompt_text: str, timeout: int) -> tuple[int, str, str]:
    """Run `copilot -p` with the prompt as a single argv token.

    --output-format json emits JSONL whose final `result` event carries
    premiumRequests, which we record in decision.json.

    Tool permissions: the prompt embeds contributor-controlled text (PR title,
    body, comments, reviews and the full diff), so the model is granted no
    tools. Classification is a pure text->JSON transform and needs none.
    --allow-all-tools is mandatory in non-interactive mode, but denial rules
    take precedence over it, so the --deny-tool flags still apply. The
    github-mcp-server builtin is loaded by default and would otherwise expose
    an auto-approved GitHub API surface to a step holding a GitHub token, so it
    is disabled explicitly.

    Model is overridable via $CLASSIFY_MODEL. The default bills zero premium
    requests; larger models bill one per PR.
    """
    argv = [
        "copilot",
        "-p", prompt_text,
        "--output-format", "json",
        "--allow-all-tools",
        "--deny-tool", "shell",
        "--deny-tool", "write",
        "--disable-builtin-mcps",
        "--secret-env-vars", "GH_TOKEN,COPILOT_GITHUB_TOKEN",
        "--model", effective_model(),
    ]
    if command_line_utf16_units(argv) >= WINDOWS_COMMAND_LINE_UTF16_LIMIT:
        return 1, "", "command line exceeds the Windows CreateProcess limit"
    proc = subprocess.run(
        argv,
        capture_output=True,
        text=True,
        encoding="utf-8",
        timeout=timeout,
    )
    return proc.returncode, proc.stdout, proc.stderr


def parse_copilot_jsonl(s: str) -> tuple[str, dict]:
    """Extract concatenated assistant message text and usage from copilot JSONL.

    Returns (response_text, usage) where usage is:
      {"premium_requests": <int or None>}
    """
    parts: list[str] = []
    premium_requests: int | None = None
    for line in s.splitlines():
        line = line.strip()
        if not line or not line.startswith("{"):
            continue
        try:
            evt = json.loads(line)
        except json.JSONDecodeError:
            continue
        et = evt.get("type")
        data = evt.get("data") or {}
        if et == "assistant.message":
            content = data.get("content")
            if isinstance(content, str):
                parts.append(content)
        elif et == "result":
            usage = evt.get("usage") or {}
            if isinstance(usage.get("premiumRequests"), int):
                premium_requests = usage["premiumRequests"]
    return "\n".join(parts), {"premium_requests": premium_requests}


def parse_response(s: str) -> dict:
    s = s.strip()
    s = re.sub(r"^```(?:json)?\s*", "", s, flags=re.I)
    s = re.sub(r"\s*```$", "", s)
    # The model sometimes emits scratchpad objects (e.g. {"intent": "..."})
    # before the real decision object. Walk all top-level JSON objects in
    # the string and return the last one that has a "decision" key, falling
    # back to the last object if none match.
    decoder = json.JSONDecoder()
    objects: list[dict] = []
    i = 0
    n = len(s)
    while i < n:
        # Skip to the next object start.
        j = s.find("{", i)
        if j == -1:
            break
        try:
            obj, end = decoder.raw_decode(s, j)
        except json.JSONDecodeError:
            i = j + 1
            continue
        if isinstance(obj, dict):
            objects.append(obj)
        i = end
    if not objects:
        # Force the original error path for callers that expect JSONDecodeError.
        return json.loads(s)
    for obj in reversed(objects):
        if "decision" in obj:
            return obj
    return objects[-1]


def validate(decision: dict) -> list[str]:
    errors = []
    decision_value = decision.get("decision")
    section = decision.get("section")
    bullet_value = decision.get("bullet")
    evidence = decision.get("evidence")
    if not isinstance(decision_value, str) or decision_value not in ("include", "omit"):
        errors.append("decision must be include or omit")
    if decision_value == "include":
        if not isinstance(decision.get("module"), str) or not decision["module"].strip():
            errors.append("module required for include")
        if not isinstance(section, str) or section not in VALID_SECTIONS - {None}:
            errors.append("section required for include")
        if not isinstance(bullet_value, str) or not bullet_value.strip():
            errors.append("bullet required for include")
    else:
        if section not in (None, "", "null"):
            errors.append("section must be null for omit")
    for field in ("surface", "user_visible_effect"):
        if not isinstance(decision.get(field), str):
            errors.append(f"{field} must be a string")
    if not isinstance(evidence, str) or not evidence.strip():
        errors.append("evidence required")
    bullet = bullet_value if isinstance(bullet_value, str) else ""
    if "\n" in bullet:
        errors.append("bullet must be a single line")
    if re.search(r"https://github\.com/.+/pull/\d+", bullet):
        errors.append("bullet must not include a PR link")
    return errors


def render_markdown(pr: int, decision: dict) -> str:
    lines = [
        f"# PR #{pr}",
        "",
        f"- module: {decision.get('module')}",
        f"- decision: **{decision.get('decision')}**",
        f"- section: {decision.get('section')}",
        f"- source: {decision.get('source', 'llm')}",
        f"- surface: {decision.get('surface')}",
        f"- user-visible effect: {decision.get('user_visible_effect')}",
        "",
        "## bullet",
        "",
        decision.get("bullet") or "_(none)_",
        "",
        "## evidence",
        "",
        "```",
        (decision.get("evidence") or "").strip(),
        "```",
        "",
    ]
    return "\n".join(lines)


# --- main ------------------------------------------------------------------


def process_one(bundle: PrBundle, args) -> tuple[str, str | None, dict | None]:
    """Classify one PR. Returns (status, error_or_None, decision_or_None)."""
    decision_path = bundle.dir / "decision.json"
    md_path = bundle.dir / "decision.md"
    prompt_path = bundle.dir / "prompt.md"
    fingerprint = classifier_fingerprint(bundle, args.rules)

    if decision_path.exists() and not args.force:
        try:
            existing = json.loads(decision_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            existing = {}
        if existing.get("classifier_fingerprint") == fingerprint and not validate(existing):
            return "skip", None, None

    # Preclassify first. Deterministic: path-pattern and metadata rules only.
    pre = preclassify(bundle)
    if pre is not None:
        pre["pr"] = bundle.pr
        pre["classifier_fingerprint"] = fingerprint
        decision_path.write_text(json.dumps(pre, indent=2), encoding="utf-8")
        md_path.write_text(render_markdown(bundle.pr, pre), encoding="utf-8")
        return f"pre:{pre['decision']}", None, pre

    if args.preclassify_only:
        return "needs-llm", None, None

    # Write prompt so it is inspectable alongside the decision.
    prompt = build_prompt(bundle, args.rules)
    prompt_path.write_text(prompt, encoding="utf-8")

    # Most failures here are transient (timeout, truncated response, malformed
    # JSON), so retry once before giving up on the PR.
    last_error = "no attempt made"
    decision = None
    usage = None
    for attempt in range(1, MAX_LLM_ATTEMPTS + 1):
        try:
            rc, out, err = invoke_cli(prompt, args.timeout)
        except subprocess.TimeoutExpired:
            last_error = f"timeout after {args.timeout}s"
            continue
        if rc != 0:
            last_error = f"cli rc={rc}: {err.strip()[:500]}"
            continue
        # Always persist the raw CLI stdout for forensic inspection, regardless
        # of format or success/failure. File extension reflects the content
        # format the copilot CLI returned.
        is_jsonl = out.lstrip().startswith('{"type":')
        suffix = "jsonl" if is_jsonl else "txt"
        name = f"cli-response.{suffix}" if attempt == 1 else f"cli-response-retry-{attempt}.{suffix}"
        raw_path = bundle.dir / name
        raw_path.write_text(out, encoding="utf-8")
        response_text = out
        attempt_usage = None
        if is_jsonl:
            response_text, attempt_usage = parse_copilot_jsonl(out)
        try:
            candidate = parse_response(response_text)
        except (json.JSONDecodeError, ValueError) as e:
            last_error = f"parse failure ({e}); raw saved to {raw_path}"
            continue
        errs = validate(candidate)
        if errs:
            last_error = "validation: " + "; ".join(errs) + f"; raw saved to {raw_path}"
            continue
        decision = candidate
        usage = attempt_usage
        break
    if decision is None:
        return "error", f"{last_error} (after {MAX_LLM_ATTEMPTS} attempts)", None
    decision["pr"] = bundle.pr
    decision["classifier_fingerprint"] = fingerprint
    decision.setdefault("source", "llm")
    if usage is not None:
        decision["usage"] = usage
    decision_path.write_text(json.dumps(decision, indent=2), encoding="utf-8")
    md_path.write_text(render_markdown(bundle.pr, decision), encoding="utf-8")
    return f"llm:{decision['decision']}", None, decision


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--jobs", type=int, default=4, help="parallel CLI invocations (default 4)")
    ap.add_argument("--timeout", type=int, default=900, help="per-PR CLI timeout seconds")
    ap.add_argument("--force", action="store_true", help="re-classify PRs with existing decision.json")
    ap.add_argument("--only", type=int, nargs="*", help="restrict to these PR numbers")
    ap.add_argument(
        "--preclassify-only",
        action="store_true",
        help="Run deterministic preclassifier only; skip LLM calls. "
        "PRs that need LLM classification are reported but left without a decision.json.",
    )
    args = ap.parse_args()
    args.rules = "" if args.preclassify_only else load_rules()

    bundles = iter_bundles()
    if args.only:
        wanted = set(args.only)
        bundles = [b for b in bundles if b.pr in wanted]
    if not bundles:
        print("No PR bundles to process.")
        return 0

    counts: dict[str, int] = {}
    errors: list[str] = []
    premium_requests = 0
    prs_with_usage = 0
    total = len(bundles)

    with ThreadPoolExecutor(max_workers=max(1, args.jobs)) as ex:
        futures = {ex.submit(process_one, b, args): b for b in bundles}
        for done, fut in enumerate(as_completed(futures), start=1):
            bundle = futures[fut]
            status, err, decision = fut.result()
            counts[status] = counts.get(status, 0) + 1
            if err:
                errors.append(f"#{bundle.pr}: {err}")
                print(f"[{done}/{total}] #{bundle.pr}: ERROR {err}", file=sys.stderr)
                continue
            print(f"[{done}/{total}] #{bundle.pr}: {status}")
            usage = (decision or {}).get("usage")
            if isinstance(usage, dict):
                prs_with_usage += 1
                v = usage.get("premium_requests")
                if isinstance(v, int):
                    premium_requests += v

    print()
    print("Summary:")
    for k, v in sorted(counts.items()):
        print(f"  {k}: {v}")
    if prs_with_usage:
        print()
        print("LLM usage (from copilot --output-format json):")
        print(f"  PRs with usage data: {prs_with_usage}")
        print(f"  premium requests:    {premium_requests}")
    if errors:
        print(f"\n{len(errors)} errors; rerun with --force on those PRs after fixing.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
