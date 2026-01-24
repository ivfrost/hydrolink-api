#!/bin/bash
export $(grep -v '^#' .env | xargs)
# Build the Docker image
# to skip tests: -Dmaven.test.skip
./mvnw spring-boot:build-image