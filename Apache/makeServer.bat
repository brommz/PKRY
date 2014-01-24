openssl req -new -config etc/server.conf -out certs/server.csr -keyout certs/server.key

openssl ca -config etc/ca.conf -in certs/server.csr -out certs/server.crt -extensions server_ext

openssl pkcs12 -export -inkey certs/server.key -in certs/server.crt -certfile ca/02.pem -out certs/server.p12  