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
# lein run ../src ../site plan.outline banner $2

scp ../site/* jrootham.ca_130yzdh9615@jrootham.ca:multi-$1.jrootham.ca
