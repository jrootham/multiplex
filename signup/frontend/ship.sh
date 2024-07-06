#! /usr/bin/bash

cp -RT plain target
cp plain/style.css ../server/signup/resources/public/
cp map/*.png ../server/signup/resources/public/tiles/
cd src
m4 signup.html > ../target/signup.html
cd ..
