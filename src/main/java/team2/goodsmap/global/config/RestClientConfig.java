package team2.goodsmap.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient kakaoLocalRestClient(
            RestClient.Builder builder,
            KakaoLocalProperties properties
    ) {
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(
                        "Authorization",
                        "KakaoAK " + properties.restApiKey()
                )
                .build();
    }
}