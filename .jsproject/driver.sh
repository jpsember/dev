#!/usr/bin/env sh
set -eu

MVN=$HOME/.m2/repository
java -Dfile.encoding=UTF-8 -classpath $MVN/com/jsbase/dev/1.0/dev-1.0.jar:$MVN/commons-io/commons-io/2.6/commons-io-2.6.jar:$MVN/com/jsbase/graphics/1.0/graphics-1.0.jar:$MVN/com/jsbase/base/1.0/base-1.0.jar:$MVN/com/jsbase/webtools/1.0/webtools-1.0.jar:$MVN/org/apache/httpcomponents/httpclient/4.5.6/httpclient-4.5.6.jar:$MVN/org/apache/httpcomponents/httpcore/4.4.10/httpcore-4.4.10.jar:$MVN/commons-logging/commons-logging/1.2/commons-logging-1.2.jar:$MVN/commons-codec/commons-codec/1.10/commons-codec-1.10.jar:$MVN/org/apache/httpcomponents/httpmime/4.5.6/httpmime-4.5.6.jar dev.Main "$@"
