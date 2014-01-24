openssl req -new -config etc/ca.conf -out ca/ca.csr -keyout ca/private/ca.key

openssl ca -selfsign -config etc/ca.conf -in ca/ca.csr -out ca/ca.crt -extensions root_ca_ext -enddate 310101000000Z

openssl ca -gencrl -config etc/ca.conf -out crl/ca.crl