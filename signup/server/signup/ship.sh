#! /usr/bin/bash

lein uberjar
scp target/uberjar/signup.jar jrootham@jrootham.ca:multiplexes/request
