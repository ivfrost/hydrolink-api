#!/bin/bash

# Get the latest hydro-backend image tag
IMAGE_TAG=$(docker images --format '{{.Repository}}:{{.Tag}} {{.CreatedAt}}' | grep '^hydro-backend:' | sort -k2 -r | head -n1 | awk '{print $1}')

# Save the Docker image to a tar file
docker save "$IMAGE_TAG" > /tmp/hydro-backend.tar

# Copy the tar file to the remote server
scp /tmp/hydro-backend.tar admin@netoasis.app:/home/admin/hydro-backend.tar

# SSH into the remote server and execute commands with a pseudo-terminal
ssh -t admin@netoasis.app "docker load -i /home/admin/hydro-backend.tar && cd /home/admin/ && docker-compose up"
