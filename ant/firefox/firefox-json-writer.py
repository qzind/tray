#!/usr/bin/env python

import os
import sys
import json
import errno

DEFAULT_PATH = '/Applications/Firefox.app/Contents/Resources/distribution/policies.json'
# DEFAULT_DATA = '{ "policies": { "Certificates": { "ImportEnterpriseRoots": true } } }'
# DEFAULT_OVERWRITE = False

path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_PATH
# merge = json.loads(sys.argv[2]) if len(sys.argv) > 2 else json.loads(DEFAULT_DATA)
# overwrite = sys.argv[3].lower() == 'true' if len(sys.argv) > 3 else DEFAULT_OVERWRITE

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

policy = load_json(path)

# Add only missing keys
#{
#  "policies": {
#    "Certificates": {
#      "ImportEnterpriseRoots": true
#    }
#  }
#}

# FIXME: Replace this with a deep merge function
# print(json.dumps(merge, sort_keys=True, indent=2))

if policy.get('policies') == None:
    policy['policies'] = {}

if policy['policies'].get('Certificates') == None:
    policy['policies']['Certificates'] = {}

if policy['policies']['Certificates'].get('ImportEnterpriseRoots') == None:
    policy['policies']['Certificates']['ImportEnterpriseRoots'] = True

mkdir_p(os.path.dirname(path))
stream = open(path, "w+")
stream.write(json.dumps(policy, sort_keys=True, indent=2))
stream.close()