#!/usr/bin/env bash

set -x
set -e

# delete old version
rm -rf tmp

# create our structure if need be
mkdir -p js

# clone ourselves
git clone . tmp --branch "master"
pushd tmp

# build
lein cljsbuild once min

# copy the new js
popd
cp tmp/dist/js/app.js js

cp -r tmp/resources/public/css .
cp -r tmp/resources/public/tz .

git add css tz js
git commit -m "$(date)"

git push
