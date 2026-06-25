#!/bin/zsh

mvn clean package -Pproduction -DskipTests && cf login -a <url> --sso -o <org> -s <space> && cf push