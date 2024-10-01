#! /usr/bin/bash

# Before running copy the 4 images into the static directory

cd signup/server/signup/
./ship.sh $1

cd ../../../
cp static/* site

cd makesite
lein run ../src ../site site.outline banner $2

