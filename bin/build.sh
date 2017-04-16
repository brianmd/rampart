#!/bin/sh
lein uberjar
docker build -t rampart .

