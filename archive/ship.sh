#! /usr/bin/bash

# Before running copy the 4 images into the plan static directory
# Copy stuff.clj

cd signup/server/signup/
./ship.sh $1

cd ../../../
mkdir -p site
rm site/*
cp static/* site

mkdir -p planSite
rm planSite/*
cp planStatic/* planSite

cd makesite
lein run ../src ../site site.outline banner $2
lein run ../planSrc ../planSite plan.outline banner $2

scp ../site/* jrootham.ca_130yzdh9615@jrootham.ca:multi-$1.jrootham.ca
scp ../planSite/* jrootham.ca_130yzdh9615@jrootham.ca:multi-$1.jrootham.ca/plan
