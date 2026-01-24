#!/bin/bash

# Build docker image
./mvnw spring-boot:build-image -Dmaven.test.skip

# Get the latest hydro-backend image tag
IMAGE_TAG=$(docker images --format '{{.Repository}}:{{.Tag}} {{.CreatedAt}}' | grep '^hydro-api:' | sort -k2 -r | head -n1 | awk '{print $1}')
IMAGE_PATH="/tmp/hydro-api.tar"
PRODUCTION_HOST="root@192.168.1.214"
PRODUCTION_PATH="/srv/hydro/"
PRODUCTION_IMAGE_PATH="/srv/hydro/hydro-api.tar"

# Save the Docker image to a tar file
docker save "$IMAGE_TAG" > $IMAGE_PATH

# Copy the tar file to the remote server
scp $IMAGE_PATH "$PRODUCTION_HOST:$PRODUCTION_PATH"

# SSH into the remote server and execute commands with a pseudo-terminal
ssh -t $PRODUCTION_HOST "docker load -i $PRODUCTION_IMAGE_PATH && cd $PRODUCTION_PATH && docker compose up"
