#!/bin/bash
export $(grep -v '^#' ./src/main/resources/.env | xargs)
# Build the Docker image
# to skip tests: -Dmaven.test.skip
./mvnw spring-boot:build-image