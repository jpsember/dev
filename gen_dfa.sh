#!/usr/bin/env bash
set -eu

# Compile rxp files to dfa
#
DEST="src/main/resources/dev/"
TOK="src/main/java/dev/"

# Validates path elements in .filter files
#
dfa input dfas/filter_expr.rxp \
    output "${DEST}prep/filter_expr.dfa" \
    ids "${TOK}prep/FilterState.java" \
    "$@"
