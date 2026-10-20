#!/usr/bin/env python3
"""Summarize a manually filled Play Console period export. Missing data stays unknown."""
import argparse
import json
import math
from pathlib import Path


def number(value):
    if value is None:
        return None
    if isinstance(value, bool) or not isinstance(value, (float, int)) or not math.isfinite(value) or value < 0:
        raise ValueError('Metrics must be nonnegative numbers or null.')
    return value


def conversion(visitors, acquisitions):
    visitors, acquisitions = number(visitors), number(acquisitions)
    if visitors is None or acquisitions is None:
        return None
    if acquisitions > visitors:
        raise ValueError('Acquisitions exceed visitors; check matching dates and report filters.')
    return acquisitions / visitors * 100 if visitors else None


def summarize(data):
    overall = data['overall']
    result = {'period_start': data['period_start'], 'period_end': data['period_end'],
              'overall_listing_conversion_percent': conversion(overall['store_listing_visitors'], overall['store_listing_acquisitions']),
              'campaigns': []}
    seen = set()
    for row in data['campaigns']:
        identity = (row['source'], row['campaign'])
        if identity in seen:
            raise ValueError('Duplicate source/campaign row; use one row per selected reporting period.')
        seen.add(identity)
        visitors = number(row['store_listing_visitors'])
        acquired = number(row['store_listing_acquisitions'])
        spend = number(row.get('spend_eur'))
        number(row.get('video_views'))
        result['campaigns'].append({
            'source': identity[0], 'campaign': identity[1],
            'visitors': visitors, 'acquisitions': acquired,
            'listing_conversion_percent': conversion(visitors, acquired),
            'cost_per_acquisition_eur': spend / acquired if spend is not None and acquired else None,
        })
    result['note'] = 'Null means not available. Campaign visitors are not summed into unique overall visitors. Video views are not unique people.'
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input', type=Path)
    args = parser.parse_args()
    try:
        print(json.dumps(summarize(json.loads(args.input.read_text())), indent=2))
    except (ValueError, KeyError, OSError) as error:
        parser.exit(1, f'{error}\n')
