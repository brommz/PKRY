for /f "delims=" %%x in (ca/db/ca.crt.srl) do set Build=%%x
echo %Build%

openssl req -new -config etc/client.conf -out certs/%Build%.csr -keyout certs/%Build%.key

openssl ca -config etc/ca.conf -in certs/%Build%.csr -out certs/%Build%.crt -extensions client_ext

openssl pkcs12 -export -inkey certs/%Build%.key -in certs/%Build%.crt -certfile ca/%Build%.pem -out certs/%Build%.p12