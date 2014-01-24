openssl ca -config etc/ca.conf -revoke certs/05.crt -crl_reason unspecified
openssl ca -gencrl -config etc/ca.conf -out crl/ca.crl