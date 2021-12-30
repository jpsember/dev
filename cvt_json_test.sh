#!/usr/bin/env bash
set -eu


# Until we have proper unit tests for the json-convert operation,
# this script executes it with some sample data
#

mk

clear

dev json-convert unit_test/convert_json convert_output -v

cat convert_output/a.json
