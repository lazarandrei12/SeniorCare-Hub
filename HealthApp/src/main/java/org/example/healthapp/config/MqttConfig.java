package org.example.healthapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.healthapp.models.Trasabilitate;
import org.example.healthapp.services.TrasabilitateService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;

@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String clientId;

    @Value("${mqtt.topic}")
    private String topic;

    @Value("${mqtt.qos:2}")
    private int qos;

    @Bean(name = "mqttInputChannel")
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        return new DefaultMqttPahoClientFactory();
    }

    @Bean
    public MessageProducer inbound(MessageChannel mqttInputChannel) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(brokerUrl, clientId + "_" + System.currentTimeMillis(),
                        mqttClientFactory(), topic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(qos);
        adapter.setOutputChannel(mqttInputChannel);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttMessageHandler(ObjectMapper objectMapper,
                                             TrasabilitateService trasabilitateService) {
        return message -> {
            try {
                String payload = convertPayloadToString(message);
                Trasabilitate masuratoare = objectMapper.readValue(payload, Trasabilitate.class);
                trasabilitateService.salvareMasuratoare(masuratoare);
            } catch (Exception e) {
                System.out.println("Eroare la procesarea mesajului MQTT: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    private String convertPayloadToString(Message<?> message) {
        Object payload = message.getPayload();

        if (payload instanceof String text) {
            return text;
        }

        if (payload instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        return String.valueOf(payload);
    }
}
