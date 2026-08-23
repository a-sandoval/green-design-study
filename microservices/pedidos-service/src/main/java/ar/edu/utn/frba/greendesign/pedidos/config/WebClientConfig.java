package ar.edu.utn.frba.greendesign.pedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient clientesWebClient(@Value("${services.clientes-url}") String baseUrl) {
        return buildWebClient(baseUrl);
    }

    @Bean
    public WebClient productosWebClient(@Value("${services.productos-url}") String baseUrl) {
        return buildWebClient(baseUrl);
    }

    private WebClient buildWebClient(String baseUrl) {
        ConnectionProvider provider = ConnectionProvider.builder("custom")
                .maxConnections(20)
                .maxIdleTime(Duration.ofSeconds(30))
                .build();
        HttpClient httpClient = HttpClient.create(provider);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
