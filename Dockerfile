# Frontend/edge image — builds Angular with Node and serves it with Caddy, which also
# terminates TLS and proxies the API and the OIDC provider (see docker/Caddyfile).
# Backend image is built by Jib: ./mvnw package -Dquarkus.container-image.build=true -pl server -am

# The build output is static JS/CSS, so build on the host platform even for an arm64
# image (no emulated npm build).
FROM --platform=$BUILDPLATFORM node:26-alpine AS build
WORKDIR /app
COPY ui/src/main/webui/package*.json ./
RUN npm ci
COPY ui/src/main/webui/ .
RUN npm run build

FROM caddy:2
COPY --from=build /app/dist/sam/browser /srv
COPY docker/Caddyfile /etc/caddy/Caddyfile
EXPOSE 80 443
