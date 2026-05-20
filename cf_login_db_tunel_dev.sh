#!/bin/zsh

cf login -a api.cf.dev.datev.de --sso -o dalo-migration-platform -s test && cf ssh -L 4712:pgd76182b-psql-master-alias.node.dc1.consul.a9s.dev:5432 flowmetrix-ai-chat