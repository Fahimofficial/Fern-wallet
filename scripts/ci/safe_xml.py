"""Minimal, hardened XML reader for the CI reports these scripts read (JUnit, Android lint)."""

# Gradle and lint never write a document type declaration, so any DTD is rejected outright. That
# rules out entity expansion ("billion laughs") and external entities (XXE) regardless of the
# parser's own defaults. Input size is capped too. Standard library only, so the scripts run on a
# bare runner without installing packages.

from __future__ import annotations

import os
from dataclasses import dataclass, field
from typing import Iterator

# Hardened below: DTDs, entity declarations and external entities raise before anything is built.
from xml.parsers import expat  # nosemgrep: python.lang.security.use-defused-xml.use-defused-xml

MAX_BYTES = 64 * 1024 * 1024


class UnsafeXmlError(ValueError):
    """The document is too large, malformed, or declares a DTD or entities."""


@dataclass
class Element:
    """A parsed XML element: tag, attributes, child elements and directly contained text."""

    tag: str
    attrib: dict[str, str]
    children: list["Element"] = field(default_factory=list)
    text: str = ""

    def get(self, key: str, default: str | None = None) -> str | None:
        return self.attrib.get(key, default)

    def find(self, tag: str) -> "Element | None":
        return next((c for c in self.children if c.tag == tag), None)

    def findall(self, tag: str) -> list["Element"]:
        return [c for c in self.children if c.tag == tag]

    def iter(self, tag: str | None = None) -> Iterator["Element"]:
        if tag is None or self.tag == tag:
            yield self
        for child in self.children:
            yield from child.iter(tag)


def _forbidden(what: str):
    def handler(*_args):
        raise UnsafeXmlError(f"{what} are not allowed in CI report XML")
    return handler


def fromstring(data: bytes) -> Element:
    """Parse XML bytes into an Element tree, rejecting DTDs and entity declarations."""
    if len(data) > MAX_BYTES:
        raise UnsafeXmlError(f"XML document is larger than {MAX_BYTES} bytes")
    parser = expat.ParserCreate()
    parser.SetParamEntityParsing(expat.XML_PARAM_ENTITY_PARSING_NEVER)
    parser.StartDoctypeDeclHandler = _forbidden("document type declarations")
    parser.EntityDeclHandler = _forbidden("entity declarations")
    parser.UnparsedEntityDeclHandler = _forbidden("entity declarations")
    parser.NotationDeclHandler = _forbidden("notation declarations")
    parser.ExternalEntityRefHandler = _forbidden("external entities")

    root: list[Element] = []
    stack: list[Element] = []

    def start(tag: str, attrs: dict[str, str]) -> None:
        element = Element(tag, dict(attrs))
        if stack:
            stack[-1].children.append(element)
        else:
            root.append(element)
        stack.append(element)

    def end(_tag: str) -> None:
        stack.pop()

    def chars(text: str) -> None:
        if stack:
            stack[-1].text += text

    parser.StartElementHandler = start
    parser.EndElementHandler = end
    parser.CharacterDataHandler = chars
    try:
        parser.Parse(data, True)
    except expat.ExpatError as e:
        raise UnsafeXmlError(f"malformed XML: {e}") from e
    if not root:
        raise UnsafeXmlError("XML document has no root element")
    return root[0]


def parse(path: str, max_bytes: int = MAX_BYTES) -> Element:
    """Parse an XML file (see fromstring), refusing files larger than max_bytes."""
    if os.path.getsize(path) > max_bytes:
        raise UnsafeXmlError(f"{path} is larger than {max_bytes} bytes")
    with open(path, "rb") as f:
        return fromstring(f.read())
