#!/usr/bin/env bash
set -eu

# Compile rxp files to dfa
#
DEST="src/main/resources/dev/"
TOK="src/main/java/dev/"

dfa input dfas/collect_errors.rxp output ${DEST}/collect_errors.dfa ids ${TOK}/CollectErrorsOper.java  example_text dfas/src1.txt example_update "$@"


