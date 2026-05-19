#!/bin/zsh

mvn clean package -Pproduction -DskipTests && cf login -a api.cf.dev.datev.de --sso -o dalo-migration-platform -s test && cf push