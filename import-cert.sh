echo 'password is "changeit"'
keytool -importcert -alias api.paymill.com -file paymill-api-cert.pem -keystore resources/cacerts -import
