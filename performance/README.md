ocker-compose up -d -> detached mode

connect:

Postgres: psql -h localhost -U postgres

pgAdmin: http://localhost:9000

Redis: redis-cli -h localhost

---------------
jmeter test script no cache: [product-service-no-cache.jmx](product-service-no-cache.jmx)

jmeter test script with cache: [product-service-with-cache.jmx](product-service-with-cache.jmx)

Usually only run the test with GUI first time to warm up the server and verify the test plan
Then run the test in non-GUI mode for performance/load testing

jmeter -n(non-GUI) -t <test_script_path.jmx> -l <results_file_path.jtl>

