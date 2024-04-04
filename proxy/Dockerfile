FROM nginx:stable-alpine3.17-slim

COPY nginx.conf /etc/nginx/nginx.conf
# hadolint ignore=DL3022
COPY --from=static html /data/www

RUN chown -R nginx:nginx /data && chmod -R 755 /data && \
        chown -R nginx:nginx /var/cache/nginx && \
        chown -R nginx:nginx /var/log/nginx && \
        chown -R nginx:nginx /etc/nginx/conf.d
RUN touch /var/run/nginx.pid && \
        chown -R nginx:nginx /var/run/nginx.pid

RUN apk add --no-cache curl=8.5.0-r0

USER nginx

HEALTHCHECK CMD curl --fail http://localhost:80 || exit 1
