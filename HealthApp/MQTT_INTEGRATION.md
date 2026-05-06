# Integrare MQTT - Documentație

## Rezumat

Am integrat suportul MQTT în aplicația Spring Boot HealthApp pentru a recepționa date de la senzori hardware (ESP8266) printr-un Broker MQTT în loc de HTTP POST-uri directe.

Logica existentă din `TrasabilitateService` (care analizează datele și generează alarme) este păstrată și apelată din serviciul MQTT nou creat.

---

## Fișierele Modificate și Create

### 1. **pom.xml** - Dependențe Maven Adăugate

S-au adăugat următoarele dependențe:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-json</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-mqtt</artifactId>
</dependency>
```

**Explicație:**
- `jackson-databind`: Pentru parsarea JSON-ului din mesajele MQTT
- `spring-boot-starter-json`: JSON support din Spring Boot
- `spring-integration-mqtt`: Spring Integration pentru MQTT (include Eclipse Paho)

---

### 2. **application.properties** - Configurare MQTT

S-au adăugat variabilele de configurare:

```properties
# Configurare MQTT
mqtt.broker.url=tcp://broker.hivemq.com:1883
mqtt.client.id=health-app-client
mqtt.topic=spital/teleasistenta/senzori
mqtt.qos=2
```

**Explicație:**
- `mqtt.broker.url`: Adresa brokerului MQTT (HiveMQ public pentru testare)
- `mqtt.client.id`: ID-ul clientului MQTT (va fi sufixat cu timestamp pentru unicitate)
- `mqtt.topic`: Topicul MQTT pe care aplicația se va abona
- `mqtt.qos`: Quality of Service (0 = at most once, 1 = at least once, 2 = exactly once)

---

### 3. **MqttSubscriberService** - Serviciul de Prelucrare MQTT

Fișier: `src/main/java/org/example/healthapp/services/MqttSubscriberService.java`

**Responsabilități:**
- Parsarea mesajelor JSON în entități `Trasabilitate`
- Apelul serviciului de salvare și generare de alarme
- Tratarea erorilor de parsare cu log-uri descriptive

**Metodă Publică - procesareMesajMqtt(String payload):**
Se apelează din `MqttConfig` când sosesc mesaje pe topicul MQTT.

```java
public void procesareMesajMqtt(String payload) {
    // 1. Parsează JSON-ul în obiect Trasabilitate
    // 2. Apelează trasabilitateService.salvareMasuratoare()
    // 3. Logează succes sau eroare
}
```

**Metodă Privată - parseJsonToTrasabilitate(String json):**
Parsează manual JSON-ul folosind Jackson JsonNode pentru mai mult control.

```java
private Trasabilitate parseJsonToTrasabilitate(String json) {
    // Citește fiecare câmp din JSON
    // Construiește obiect Trasabilitate
    // Gestionează valori missing cu valori default
}
```

**Tratarea Erorilor:**
- `JsonParseException`: JSON invalid
- `IllegalArgumentException`: Mapare JSON eșuată sau validare
- `Exception`: Erori generale

---

### 4. **MqttConfig** - Configurație Spring Integration pentru MQTT

Fișier: `src/main/java/org/example/healthapp/config/MqttConfig.java`

**Beans Definiți:**

1. **mqttClientFactory()** - Factory pentru client MQTT
   ```java
   factory.setServerUri(brokerUrl);
   return factory;
   ```

2. **inbound()** - Message Producer pentru MQTT
   ```java
   MqttPahoMessageDrivenChannelAdapter adapter = 
       new MqttPahoMessageDrivenChannelAdapter(brokerUrl, clientId + timestamp, 
           mqttClientFactory(), topic);
   adapter.setQos(qos);
   ```

3. **mqttMessageHandler()** - Service Activator pentru procesare mesaje
   ```java
   return message -> mqttSubscriberService.procesareMesajMqtt((String) message.getPayload());
   ```

4. **objectMapper()** - Jackson ObjectMapper bean

5. **mqttInputChannel()** - Direct Channel pentru mesaje MQTT

---

## Flow-ul Datelor

```
ESP8266 (Senzori Hardware)
    ↓
Trimite JSON pe MQTT Topic: "spital/teleasistenta/senzori"
    ↓
Spring Integration MQTT adapter primește mesaj
    ↓
MessageHandler apelează MqttSubscriberService.procesareMesajMqtt()
    ↓
parseJsonToTrasabilitate() convertește JSON → Trasabilitate
    ↓
trasabilitateService.salvareMasuratoare()
    ↓
Verificare Alarme (gaz, puls, glicemie, temperatură)
    ↓
Salvare în H2/PostgreSQL + Generare Alarme (dacă necesar)
    ↓
Log-uri în Consolă
```

---

## Format JSON Așteptat

Mesajele pe topicul MQTT trebuie să conțină JSON valid cu următoarea structură:

```json
{
  "pacientId": 1,
  "dataInregistrare": "2026-05-05T14:30:00",
  "ta": "120/80",
  "puls": 75,
  "tempC": 36.5,
  "greutate": 70.5,
  "glicemie": 100,
  "lumina": false,
  "gaz": false,
  "umiditate": 55,
  "proximitate": false,
  "temperaturaAm": 22
}
```

**Notă:** Doar `pacientId` este obligatoriu; restul pot fi omise (vor fi null sau default).

---

## Testare MQTT

### Opțiunea 1: MQTTBox (Desktop)
1. Descarcă [MQTTBox](http://www.mqttbox.io/)
2. Se conectează la `tcp://broker.hivemq.com:1883`
3. Se abonează la `spital/teleasistenta/senzori` (pentru a vedea mesajele)
4. Trimite test JSON pe același topic

### Opțiunea 2: mosquitto_pub (Command Line)
```bash
mosquitto_pub -h broker.hivemq.com -p 1883 \
  -t spital/teleasistenta/senzori \
  -m '{"pacientId":1,"dataInregistrare":"2026-05-05T14:30:00","puls":75,"tempC":36.5,"glicemie":100}'
```

### Opțiunea 3: ESP8266 Arduino
```cpp
#include <PubSubClient.h>
#include <WiFi.h>

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  WiFi.begin("SSID", "PASSWORD");
  client.setServer("broker.hivemq.com", 1883);
}

void loop() {
  if (!client.connected()) {
    client.connect("ESP8266_HealthApp");
  }
  
  String json = "{\"pacientId\":1,\"dataInregistrare\":\"2026-05-05T14:30:00\",\"puls\":75,\"tempC\":36.5,\"glicemie\":100}";
  client.publish("spital/teleasistenta/senzori", json.c_str());
  
  delay(5000);
}
```

---

## Log-uri de Succes și Erori

### Startup (Normal)
```
INFO  - MQTT Subscriber initializat pentru topicul: spital/teleasistenta/senzori pe brokerul: tcp://broker.hivemq.com:1883
INFO  - Notă: Conexiunea la MQTT se va stabili prin Spring Integration beans definite în MqttConfig
```

### Primire Mesaj (Normal)
```
DEBUG - Mesaj primit pe MQTT: {"pacientId":1,"puls":75,...}
INFO  - Masuratoare salvata cu succes din MQTT pentru pacientId: 1
```

### Alarme Detectate
```
WARN  - [ALERTA] Parametri cardiaci aberanti: puls=150 bpm pentru pacientId=1
ERROR - [ALERTA] Glicemie scazuta cu risc: 50 mg/dL pentru pacientId=1
```

### Erori de Parsare
```
ERROR - Eroare la parsarea JSON-ului: nu este JSON valid
ERROR - Eroare la maparea JSON-ului la Trasabilitate: Structura JSON nu corespunde
```

---

## Păstrare Logicii Existente

**Toate alarme și logica din `TrasabilitateService` rămân identice.** Singurul lucru care s-a schimbat este sursa datelor:

- **Înainte**: HTTP POST `/api/v1/trasabilitate`
- **Acum**: Mesaje MQTT pe `spital/teleasistenta/senzori`

Metoda `salvareMasuratoare()` este apelată din `MqttSubscriberService` și funcționează exact la fel.

---

## Variante Brokerului MQTT

- **HiveMQ Public**: `tcp://broker.hivemq.com:1883` (recomandat pentru testare)
- **Mosquitto Local**: `tcp://localhost:1883` (dacă instalezi mosquitto local)
- **EMQ X**: `tcp://emqx.io:1883`
- **AWS IoT Core**: Necesită certificat SSL (pentru producție)

---

## Debugging și Troubleshooting

### Problema: Mesajele nu sosesc
1. Verifică că brokerul MQTT e accesibil
2. Verifică topicul din `application.properties`
3. Activează DEBUG logging: `logging.level.org.springframework.integration=DEBUG`

### Problema: JSON Parse Error
1. Validează structura JSON cu [jsonlint.com](https://www.jsonlint.com/)
2. Asigură-te că `pacientId` e număr (nu string)
3. DateTime trebuie în format ISO: `2026-05-05T14:30:00`

### Problema: Conexiune la MQTT eșuează
1. Testează conexiunea cu: `mosquitto_sub -h broker.hivemq.com -p 1883 -t spital/teleasistenta/senzori`
2. Dacă nu se conectează, brokerul poate fi offline
3. Comută la alt broker public (EMQ X, etc.)




