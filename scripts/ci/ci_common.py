"""Helpers shared by the CI scripts: GitHub workflow annotations and the job summary."""
from __future__ import annotations

import os


def in_github_actions() -> bool:
    return os.environ.get("GITHUB_ACTIONS") == "true"


def _escape_data(value: str) -> str:
    # Same escaping as @actions/core, so text from reports can't break or inject workflow commands.
    return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def _escape_property(value: str) -> str:
    return _escape_data(value).replace(":", "%3A").replace(",", "%2C")


def annotation(level: str, title: str, message: str, file: str | None = None, line: str | None = None) -> str:
    """Return a GitHub `::error`/`::warning` workflow command for the message."""
    props = [f"title={_escape_property(title)}"]
    if file:
        props.insert(0, f"file={_escape_property(file)}")
        if line:
            props.insert(1, f"line={_escape_property(str(line))}")
    return f"::{level} {','.join(props)}::{_escape_data(message)}"


def annotate(level: str, title: str, message: str, file: str | None = None, line: str | None = None) -> None:
    """Print a workflow annotation when running in GitHub Actions (no-op locally)."""
    if in_github_actions():
        print(annotation(level, title, message, file, line))


def append_summary(markdown: str) -> None:
    """Append Markdown to the job summary when GITHUB_STEP_SUMMARY is set."""
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        with open(path, "a", encoding="utf-8") as summary:
            summary.write(markdown)


def md_cell(text: str) -> str:
    """Make text safe inside a Markdown table cell."""
    return text.replace("|", "\\|").replace("\n", " ")
