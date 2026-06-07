package com.minilands.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;


@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        OtpProperties.class,
        GoogleProperties.class,
        CloudinaryProperties.class,
        AdminBootstrapProperties.class,
        RazorpayProperties.class,
        WalletProperties.class,
        NotificationProperties.class,
        OneSignalProperties.class,
        InvestmentProperties.class,
        MarketplaceProperties.class,
        BrandProperties.class,
        ReferralProperties.class
})
public class AppConfig {


    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }       

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
