FROM linkedin/datahub-gms
USER root
ADD ca.crt /usr/local/share/ca-certificates/ca.crt
ADD tls.crt /usr/local/share/ca-certificates/tls.crt
RUN update-ca-certificates
