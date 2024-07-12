FROM alpine:latest

RUN apk add nginx bash

ADD nginx.conf /etc/nginx/nginx.conf

ADD entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

COPY --from=openpolicyagent/opa:latest-static /opa /usr/bin/opa

ENTRYPOINT ["/entrypoint.sh"]

