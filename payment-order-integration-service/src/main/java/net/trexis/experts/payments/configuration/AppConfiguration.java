package net.trexis.experts.payments.configuration;

import net.trexis.experts.backbaseapi.configuration.ArrangementManagerServiceRestClientConfiguration;
import net.trexis.experts.finite.FiniteConfiguration;
import net.trexis.experts.ingestion.configuration.TrexisBackbaseIngestionRestClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import({FiniteConfiguration.class, TrexisBackbaseIngestionRestClientConfiguration.class,
        ArrangementManagerServiceRestClientConfiguration.class})
public class AppConfiguration {

    @Primary
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}