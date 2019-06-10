# To run

1. Launch the Data Flow Shell and point it at your Data Flow Server
Docker-compose for Spring Cloud Data Flow and spring batch app+postgres DB
run.sh runs scdf, karka, grafana, postgresDb + pgadmin. -d key for docker-compose runs containers in background
For stop containers use stop.sh
For deploy batch task use spring app from folder /FileJdbc
Execute shell for scdf docker exec -it dataflow-server java -jar shell.jar logs are writing to /tmp directory

2. Register the application: 

    ```
    app register --name filejdbc --type task --uri maven://us.example:demo:2.1.1.RELEASE
    ```
3. Create the task definition

    ```
    task create --name filejdbcJob --definition "filejdbc"
    ```
4. Launch the task
    ```
    task launch --name filejdbcJob --arguments " --logging.level.org.springframework.cloud.task=debug --spring.datasource.url=jdbc:postgresql://dbpostgres:5432/postgres1 --spring.datasource.username=postgres  --spring.datasource.password=example --spring.datasource.driverClassName=org.postgresql.Driver --spring.datasource.initialization-mode=always --spring.batch.initialize-schema=always --spring.batch.job.enabled=false --spring.main.allow-bean-definition-overriding=true --spring.profiles.active=master"
    ```

NOTE: The job has the files hard coded in the code (In the `JobConfiguration` class) so you'll need to upload the files in the `/src/main/resources/input/` directory.


