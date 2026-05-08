# HealthApp — Remote Teleassistance Data Ingestion & Monitoring

HealthApp is a Spring Boot backend that ingests telemetry from low-cost IoT patient sensors, analyzes measurements for critical conditions (alarms), and persists readings for dashboarding and clinician review. It supports MQTT ingestion from devices (ESP8266), REST endpoints for historical queries, and a simple JWT-based authentication flow.

## Business value (for employers)
- Real-time ingestion of patient telemetry from IoT devices using MQTT.
- Automatic alarm detection (gas presence, abnormal pulse ranges, high ambient temperature, low blood glucose) to accelerate interventions.
- REST API for frontend dashboards and reporting; H2-based local setup for fast demos.

## Key features
- MQTT ingestion via Spring Integration (configurable broker and topic).
- JSON → domain mapping (Jackson) and dispatch to `TrasabilitateService` for analysis and persistence.
- Alarm detection logic inside `TrasabilitateService` (logged and ready to be extended for WebSocket/push notifications).
- JPA entities + repositories for persistence. H2 is used by default for local development.
- Seeded demo accounts (Medic and Dispatcher) created at startup by `DataInitializer`.

## Architecture (high-level)
IoT device (ESP8266) → MQTT Broker → Spring Integration MQTT inbound adapter → Message handler → `TrasabilitateService` (process + alarm checks) → JPA repository → H2 DB

REST API: controllers expose endpoints for creating and querying telemetry and patient data.

## Tech stack
- Java 11+ / 17+
- Spring Boot (Web, Data JPA, Security)
- Spring Integration MQTT
- Eclipse Paho MQTT client
- Jackson (JSON parsing)
- Lombok
- H2 (development)
- Maven

---

## Quick start — build & run

1. Build

```powershell
./mvnw.cmd clean package
```

2. Run

```powershell
./mvnw.cmd spring-boot:run
```

Or run the fat jar produced in `target/`:

```powershell
java -jar target/*.jar
```

## Configuration
Edit `src/main/resources/application.properties` to change MQTT and DB settings. Example properties used by the project:

```properties
# MQTT
mqtt.broker.url=tcp://broker.hivemq.com:1883
mqtt.client.id=health-app-client
mqtt.topic=spital/teleasistenta/senzori
mqtt.qos=2

# H2 / JPA
spring.datasource.url=jdbc:h2:mem:healthapp;DB_CLOSE_DELAY=-1
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

## Seeded demo accounts
Created by `DataInitializer` at first startup:
- Medic: `medic@spital.ro` / `parola123`
- Dispatcher (Supraveghetor): `dispecerat@spital.ro` / `parola123`

> Note: passwords are plain text for development only.

## REST API
Base path: `/api/v1`

- POST /api/v1/trasabilitate — create telemetry (JSON body matching `Trasabilitate` entity)
- GET /api/v1/trasabilitate/pacient/{pacientId} — get patient history

Example (HTTP POST payload — same as MQTT message):

```json
{
  "pacientId": 1,
  "dataInregistrare": "2026-05-06T18:45:00",
  "ta": "160/100",
  "puls": 135,
  "tempC": 37.1,
  "greutate": 75.5,
  "glicemie": 45,
  "lumina": true,
  "gaz": true,
  "umiditate": 40,
  "proximitate": true,
  "temperaturaAm": 38
}
```

## MQTT — where to publish
- Topic (default): `spital/teleasistenta/senzori`
- Broker (default): `tcp://broker.hivemq.com:1883`

Example publish (mosquitto_pub):

```powershell
mosquitto_pub -h broker.hivemq.com -p 1883 -t "spital/teleasistenta/senzori" -m '{"pacientId":1,"dataInregistrare":"2026-05-06T18:45:00","ta":"160/100","puls":135,"tempC":37.1,"greutate":75.5,"glicemie":45,"lumina":true,"gaz":true,"umiditate":40,"proximitate":true,"temperaturaAm":38}'
```

## Troubleshooting — common issues
- Application fails to start because MQTT broker is unreachable
  - Option: use a local broker (mosquitto) or configure the MQTT adapter to not block application startup. The project includes a Spring Integration MQTT inbound adapter that attempts to connect at startup by default.

- `Dispatcher has no subscribers for channel 'mqttInputChannel'`
  - Ensure `MqttConfig` contains a `@ServiceActivator(inputChannel = "mqttInputChannel")` message handler.

- `class file for org.eclipse.paho.client.mqttv3.MqttCallbackExtended not found`
  - Add the Paho dependency to `pom.xml` and refresh Maven:

```xml
<dependency>
  <groupId>org.eclipse.paho</groupId>
  <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
  <version>1.2.5</version>
</dependency>
```

- JSON parsing errors
  - Handler tries to convert message payload to String — if sender publishes bytes/other types, adapt handler to accept byte[] and convert safely (String.valueOf(payload) or new String((byte[])payload)).

## Production recommendations
- Replace H2 with a managed database (Postgres, MySQL) and use migrations (Flyway/Liquibase).
- Use BCrypt for password hashing and secure JWT configuration.
- Use a secured MQTT broker (TLS, authentication).
- Implement push notifications / WebSocket for alarm delivery and monitoring.

## How to evaluate quickly (for interviews)
1. Run the app locally with `./mvnw.cmd spring-boot:run`.
2. Publish the sample MQTT payload to the configured topic.
3. Inspect logs to confirm alarm detection and DB inserts.
4. Use the REST GET endpoint to verify persistence.

---

If you want, I can also add example scripts for MQTT publishing, harden MQTT startup behavior, or commit additional documentation. 

