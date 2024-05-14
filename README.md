This project aims to quickly spin up a monitoring setup for a spring-boot application in local environment. It will spin up the following containers:
- cAdvisor (for container metrics)
- Prometheus (for monitoring)
- Grafana (for visualization)
- vector (to collect, transform, and route all your logs and metrics)
- spring-boot app

The spring-boot app includes the following dependencies (and no code modification apart from what comes by default from [https://start.spring.io](https://start.spring.io)):
- spring-boot-devtools
- spring-boot-configuration-processor
- lombok
- spring-boot-starter-actuator
- micrometer-registry-prometheus
- spring-boot-starter-web

It also has one property defined in `application.properties` to expose metrics in a format Prometheus understands.

Grafana contains a dashboard which has graphs created for the metrics which are emitted by default from `micrometer` in the `spring-boot` app.
This will contain metrics related to:
- JVM:
    - **Memory**: Heap / NonHeap / Buffer <-> Used / Committed / Max
    - **GC**: Minor / Major GC Pauses with frequency
    - **Threads**: Thread States, Live, Daemon, Peak Threads
- Log Events - logback level frequencies

---

To start the application, run the following command (replace `nerdctl` with `docker` if needed):

```
./mvnw clean package && nerdctl compose up --build
```

Different dashboards will be available once the above commands successfully runs:
- Grafana dashboard: [http://localhost:3000](http://localhost:3000)
- Prometheus dashboard: [http://localhost:9090](http://localhost:9090)
- cAdvisor dashboard: [http://localhost:8090](http://localhost:8090)
- vector playground: [http://localhost:8686/playground](http://localhost:8686/playground)
- spring-boot app: [http://localhost:8080](http://localhost:8080)