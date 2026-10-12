ARG NODE_IMAGE=node:24.11.0-alpine
ARG NGINX_IMAGE=nginxinc/nginx-unprivileged:1.29.2-alpine

FROM ${NODE_IMAGE} AS build
WORKDIR /workspace
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM ${NGINX_IMAGE}
COPY deploy/local/nginx/storefront.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/out /usr/share/nginx/html
EXPOSE 8080
USER nginx
