package cau.capstone.backend.voice.aiserver;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${aiserver.url}")
    private String serverUrl;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        final int size = 16*1024*1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();


        return builder.baseUrl(serverUrl)
                .exchangeStrategies(strategies)
                .build();
    }
}
