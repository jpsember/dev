#!/usr/bin/env sh
set -eu

echo "Installing driver for dev program"

mvn package "$@"

BINDIR=/usr/local/bin
JARSDIR=${BINDIR}/jpsember_jars
JARNAME=dev-1.0-jar-with-dependencies.jar
OUTFILE=${BINDIR}/dev

mkdir -p ${JARSDIR}
cp -rf target/${JARNAME} ${JARSDIR}
cp -rf driver.sh.txt ${OUTFILE}
chmod 755 ${OUTFILE}
