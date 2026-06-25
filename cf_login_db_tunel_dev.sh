#!/bin/zsh

cf login -a <url> --sso -o <org> -s <space> && cf ssh -L 4712:<service_url>:5432 flowmetrix-ai-chat