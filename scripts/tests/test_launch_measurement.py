import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('measurement', Path(__file__).resolve().parents[1] / 'measure-launch.py')
measurement = importlib.util.module_from_spec(spec)
spec.loader.exec_module(measurement)


class MeasurementTest(unittest.TestCase):
    def test_missing_is_not_zero(self):
        self.assertIsNone(measurement.conversion(100, None))
        self.assertIsNone(measurement.conversion(None, 5))
        self.assertIsNone(measurement.conversion(0, 0))

    def test_conversion_uses_listing_visitors(self):
        self.assertEqual(measurement.conversion(80, 20), 25)

    def test_inconsistent_report_filters_fail(self):
        with self.assertRaises(ValueError):
            measurement.conversion(5, 10)
        with self.assertRaises(ValueError):
            measurement.conversion(-10, 1)
