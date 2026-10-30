import unittest

import safe_xml
from tests.helpers import ScriptTestCase

BILLION_LAUGHS = b"""<?xml version="1.0"?>
<!DOCTYPE lolz [<!ENTITY lol "lol"><!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">]>
<testsuite><testcase name="&lol2;"/></testsuite>"""

EXTERNAL_ENTITY = b"""<?xml version="1.0"?>
<!DOCTYPE r [<!ENTITY x SYSTEM "file:///etc/passwd">]>
<testsuite><testcase name="&x;"/></testsuite>"""

EXTERNAL_DTD = b"""<?xml version="1.0"?>
<!DOCTYPE testsuite SYSTEM "http://example.invalid/evil.dtd">
<testsuite/>"""


class SafeXmlTest(ScriptTestCase):
    def test_parses_elements_attributes_and_text(self):
        root = safe_xml.fromstring(b'<a x="1"><b>hi</b><b>there</b><c/></a>')
        self.assertEqual(root.tag, "a")
        self.assertEqual(root.get("x"), "1")
        self.assertEqual([b.text for b in root.findall("b")], ["hi", "there"])
        self.assertIsNotNone(root.find("c"))
        self.assertEqual([e.tag for e in root.iter()], ["a", "b", "b", "c"])

    def test_rejects_entity_expansion(self):
        with self.assertRaises(safe_xml.UnsafeXmlError):
            safe_xml.fromstring(BILLION_LAUGHS)

    def test_rejects_external_entities_and_dtds(self):
        for payload in (EXTERNAL_ENTITY, EXTERNAL_DTD):
            with self.subTest(payload=payload[:60]), self.assertRaises(safe_xml.UnsafeXmlError):
                safe_xml.fromstring(payload)

    def test_rejects_dtd_in_utf16_documents(self):
        with self.assertRaises(safe_xml.UnsafeXmlError):
            safe_xml.fromstring(EXTERNAL_ENTITY.decode().replace('version="1.0"', 'version="1.0" encoding="UTF-16"')
                                .encode("utf-16"))

    def test_rejects_malformed_and_empty_documents(self):
        for payload in (b"<a><b></a>", b"", b"not xml"):
            with self.subTest(payload=payload), self.assertRaises(safe_xml.UnsafeXmlError):
                safe_xml.fromstring(payload)

    def test_rejects_oversized_files(self):
        path = self.write("big.xml", b"<a>" + b"x" * 2048 + b"</a>")
        with self.assertRaises(safe_xml.UnsafeXmlError):
            safe_xml.parse(path, max_bytes=1024)
        self.assertEqual(safe_xml.parse(path).tag, "a")


if __name__ == "__main__":
    unittest.main()
