#! /usr/bin/bash

# Before running copy the 4 images into the static directory
# Copy stuff.clj

cd signup/server/signup/
./ship.sh $1

cd ../../../
mkdir -p site
cp static/* site

cd makesite
lein run ../src ../site site.outline banner $2

