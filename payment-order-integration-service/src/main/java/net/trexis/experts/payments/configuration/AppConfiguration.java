package net.trexis.experts.payments.configuration;

import net.trexis.experts.finite.FiniteConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({FiniteConfiguration.class})
public class AppConfiguration {

}