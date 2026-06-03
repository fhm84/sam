# Frontend image — builds Angular with Node and serves via nginx.
# Backend image is built by Jib: ./mvnw package -Dquarkus.container-image.build=true -pl server -am

FROM node:22-alpine AS build
WORKDIR /app
COPY ui/src/main/webui/package*.json ./
RUN npm ci
COPY ui/src/main/webui/ .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist/sam/browser /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
