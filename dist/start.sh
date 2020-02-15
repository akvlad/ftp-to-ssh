#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )";
pushd $DIR;
java -jar ftp2Ssh-0.0.5.jar
popd;
