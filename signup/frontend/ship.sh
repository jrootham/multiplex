#! /usr/bin/bash

cp -RT plain target
cd src
m4 signup.html > ../target/signup.html
cd ..
