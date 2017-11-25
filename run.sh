#!/usr/bin/env bash
SERVICE_NAME=leonard \
SERVICE_ROOT=hi \
PORT=8081 \
SERVICE_PORT=8081 \
REDIS_URL=redis://127.0.0.1:6379 \
java  -jar target/leonard-1.0-SNAPSHOT-fat.jar
