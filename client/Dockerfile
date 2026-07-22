# Build static files
# Node Bullseye has npm
FROM node:22.14-bookworm-slim AS build
ENV NODE_OPTIONS="--max-old-space-size=3072"

# Build
WORKDIR /app
COPY *.html *.json *.ts ./
COPY ./src ./src
COPY ./public ./public
RUN npm i --ignore-scripts --no-update-notifier --omit=dev && \
    npm run build && \
    rm -rf node_modules

# Deploy container
# Caddy serves static files
FROM caddy:2.11.4-alpine
RUN apk add --no-cache ca-certificates curl

# Receive build number as argument, retain as environment variable
ARG VITE_BUILD
ARG SOURCE_PR
ENV VITE_BUILD=${VITE_BUILD}
ENV SOURCE_PR=${SOURCE_PR}

# Add metadata to the final image
LABEL org.opencontainers.image.authors="Ricardo Campos <ricardompcampos@gmail.com>" \
      org.opencontainers.image.vendor="Ricardo Campos Org" \
      org.opencontainers.image.title="TaskNoteApp client" \
      org.opencontainers.image.description="React Web app application" \
      org.opencontainers.image.version="${SOURCE_PR}" \
      org.opencontainers.image.source="https://lightroasted.vps-kinghost.net/rmcampos/tasknote"

# Copy files and run formatting
COPY --from=build /app/dist/ /app/dist
COPY Caddyfile /etc/caddy/Caddyfile
RUN caddy fmt --overwrite /etc/caddy/Caddyfile

# User, port and healthcheck
EXPOSE 5000
HEALTHCHECK CMD curl -f http://localhost:5000
USER 1001
