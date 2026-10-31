"""
Tests for scripts/ci.

Run with: python3 -m unittest discover -s scripts/ci/tests -t scripts/ci
"""
import os
import sys

# Make the scripts importable when a single test file is run directly.
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
