#!/usr/bin/env python

import errno
import json
import os
import sys

DEFAULT_PATH = '/Applications/Firefox.app/Contents/Resources/distribution/policies.json'
DEFAULT_DATA = '{ "policies": { "Certificates": { "ImportEnterpriseRoots": true } } }'
DEFAULT_OVERWRITE = False

path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_PATH
merge = json.loads(sys.argv[2]) if len(sys.argv) > 2 else json.loads(DEFAULT_DATA)
overwrite = sys.argv[3].lower() == 'true' if len(sys.argv) > 3 else DEFAULT_OVERWRITE


def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError as e:  # Python >2.5
        if e.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise


def load_json(path):
    data = json.loads("{}")
    if os.path.isfile(path):
        try:
            stream = open(path, "r")
            data = json.load(stream)
            stream.close()
        except ValueError as e:
            print("Warning: Not a valid JSON file: " + path)

    return data


def merge_json(base, append):
    """ Writes values from append to base, deep copying if necessary """
    for key, val in append.items():
        if base.get(key) is None:
            base[key] = val
        elif type(base.get(key)) is dict and type(val) is dict:
            merge_json(base.get(key), val)
        elif overwrite:
            # only forces overwrite if no deeper objects exist first
            base[key] = val


policy = load_json(path)
merge_json(policy, merge)

mkdir_p(os.path.dirname(path))
stream = open(path, "w+")
stream.write(json.dumps(policy, sort_keys=True, indent=2))
stream.close()
