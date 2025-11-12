set -a
source ./src/main/resources/.env
set +a
./mvnw -Pnative spring-boot:build-image

