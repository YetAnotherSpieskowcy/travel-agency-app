FROM rabbitmq:3.13-management-alpine

# No special configuration needed yet...
#
# COPY rabbitmq.conf /etc/rabbitmq/rabbitmq.conf

USER rabbitmq

HEALTHCHECK --interval=3s CMD rabbitmq-diagnostics -q ping || exit 1
