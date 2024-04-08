FROM nginx:stable-alpine3.17-slim

COPY nginx.conf /etc/nginx/nginx.conf

# Ignore undefined FROM alias - hadolint can't detect additional contexts
# hadolint ignore=DL3022
COPY --from=static html /data/www

RUN chown -R nginx:nginx /data && chmod -R 755 /data && \
        chown -R nginx:nginx /var/cache/nginx && \
        chown -R nginx:nginx /var/log/nginx && \
        chown -R nginx:nginx /etc/nginx/conf.d
RUN touch /var/run/nginx.pid && \
        chown -R nginx:nginx /var/run/nginx.pid

USER nginx

HEALTHCHECK CMD wget -O - http://127.0.0.1:80 || exit 1
