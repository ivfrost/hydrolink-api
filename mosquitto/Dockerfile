FROM eclipse-mosquitto:2.0

RUN apk add --no-cache \
    rust \
    cargo \
    build-base \
    openssl-dev \
    mosquitto-dev \
    git

RUN git clone https://github.com/wiomoc/mosquitto-jwt-auth.git /jwt-auth
WORKDIR /jwt-auth
RUN cargo build --release

RUN mkdir -p /usr/lib/mosquitto && \
    cp target/release/libmosquitto_jwt_auth.so /usr/lib/mosquitto/

RUN apk del build-base git

COPY mosquitto.conf /mosquitto/config/mosquitto.conf
COPY public_key.pem /mosquitto/config/public_key.pem
RUN chown -R mosquitto:mosquitto /mosquitto/config
