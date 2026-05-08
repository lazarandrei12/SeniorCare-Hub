package org.example.healthapp.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.healthapp.models.Trasabilitate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class MqttSubscriberService {

    private final TrasabilitateService trasabilitateService;
    // ObjectMapper este instanțiat direct. Nu este un bean Spring, ci o unealtă.
    // Aceasta este o abordare perfect validă și curată.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.topic}")
    private String topic;

    // Constructorul primește DOAR bean-urile manageriate de Spring.
    // ObjectMapper nu mai este aici.
    public MqttSubscriberService(TrasabilitateService trasabilitateService) {
        this.trasabilitateService = trasabilitateService;
    }

    @PostConstruct
    public void init() {
        log.info("MQTT Subscriber initializat pentru topicul: {} pe brokerul: {}", topic, brokerUrl);
        log.info("Notă: Conexiunea la MQTT se va stabili prin Spring Integration beans definite în MqttConfig");
    }

    @PreDestroy
    public void destroy() {
        log.info("MQTT Subscriber oprit");
    }

    public void procesareMesajMqtt(String payload) {
        try {
            log.debug("Mesaj primit pe MQTT: {}", payload);
            Trasabilitate masuratoare = parseJsonToTrasabilitate(payload);
            trasabilitateService.salvareMasuratoare(masuratoare);
            log.info("Masuratoare salvata cu succes din MQTT pentru pacientId: {}", masuratoare.getPacientId());
        } catch (IllegalArgumentException e) {
            log.error("Eroare de validare la parsarea mesajului MQTT: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Eroare neașteptata la procesarea mesajului MQTT: {}", e.getMessage(), e);
        }
    }

    private Trasabilitate parseJsonToTrasabilitate(String json) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);

            Trasabilitate masuratoare = new Trasabilitate();
            masuratoare.setPacientId(rootNode.get("pacientId").asLong());

            if (rootNode.has("dataInregistrare")) {
                String dataStr = rootNode.get("dataInregistrare").asText();
                masuratoare.setDataInregistrare(LocalDateTime.parse(dataStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                masuratoare.setDataInregistrare(LocalDateTime.now());
            }

            // Verificări 'has' pentru fiecare câmp pentru a preveni NullPointerException
            if (rootNode.has("ta")) {
                masuratoare.setTa(rootNode.get("ta").asText());
            }
            if (rootNode.has("puls")) {
                masuratoare.setPuls(rootNode.get("puls").asInt());
            }
            if (rootNode.has("tempC")) {
                masuratoare.setTempC(rootNode.get("tempC").asDouble());
            }
            if (rootNode.has("greutate")) {
                masuratoare.setGreutate(rootNode.get("greutate").asDouble());
            }
            if (rootNode.has("glicemie")) {
                masuratoare.setGlicemie(rootNode.get("glicemie").asInt());
            }
            if (rootNode.has("lumina")) {
                masuratoare.setLumina(rootNode.get("lumina").asBoolean());
            }
            if (rootNode.has("gaz")) {
                masuratoare.setGaz(rootNode.get("gaz").asBoolean());
            }
            if (rootNode.has("umiditate")) {
                masuratoare.setUmiditate(rootNode.get("umiditate").asInt());
            }
            if (rootNode.has("proximitate")) {
                masuratoare.setProximitate(rootNode.get("proximitate").asBoolean());
            }
            if (rootNode.has("temperaturaAm")) {
                masuratoare.setTemperaturaAm(rootNode.get("temperaturaAm").asInt());
            }

            return masuratoare;
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.error("Eroare la parsarea JSON-ului: nu este JSON valid");
            throw new IllegalArgumentException("JSON invalid: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Eroare la maparea JSON-ului la Trasabilitate: {}", e.getMessage());
            throw new IllegalArgumentException("Eroare mapare JSON: " + e.getMessage(), e);
        }
    }
}
