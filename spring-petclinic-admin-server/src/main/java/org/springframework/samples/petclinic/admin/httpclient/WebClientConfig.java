package org.springframework.samples.petclinic.api.httpclient;

import java.util.LinkedList;
import java.util.List;


import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

import jakarta.annotation.PostConstruct;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Configuration
@Slf4j
public class WebClientConfig {

    @PostConstruct
    public void postContruct() {
        log.info("Setting up webclients beans");
    }

    @Bean
    HttpClient httpClient() {
        SslContext nettySslContext;
        try {
            log.info("Creating Netty SSL Context using JDK SSL Provider");
            nettySslContext = SslContextBuilder.forClient().sslProvider(SslProvider.JDK).build();
            return HttpClient.create()
                .followRedirect(true)
                .disableRetry(true)
                .wiretap(false)
                .secure(t -> t.sslContext(nettySslContext)
                    .handlerConfigurator(handler -> {
                            SSLEngine engine = handler.engine();
                            SSLParameters params = new SSLParameters();
                            List<SNIMatcher> matchers = new LinkedList<>();
                            SNIMatcher matcher = new SNIMatcher(0) {
                                @Override
                                public boolean matches(SNIServerName serverName) {
                                    return true;
                                }
                            };
                            matchers.add(matcher);
                            params.setSNIMatchers(matchers);
                            engine.setSSLParameters(params);
                        }
                    ));
        } catch (SSLException excp) {
            log.error("Unable to create reactive http client with azure keyvault certs", excp);
        }
        return HttpClient.create();
    }

    @Bean
    @LoadBalanced
    WebClient.Builder builder() {
        log.info("Creating reactive load balancer spring webclient builder");
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient()));
    }

    @Bean
    WebClient webClient(WebClient.Builder builder) {
        log.info("Creating reactive load balancer spring webclient");
        return builder.build();
    }

    @Bean
    WebClient.Builder externalWebClientBuilder() {
        log.info("Creating reactive spring webclient builder for external calls");
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient()));
    }

    // @Bean
    // public RestTemplate restTemplate() throws Exception {
    //     KeyStore ks = KeyStore.getInstance("AzureKeyVault");
    //     SSLContext sslContext = SSLContexts.custom()
    //         .loadTrustMaterial(ks, new TrustSelfSignedStrategy())
    //         .build();

    //     HostnameVerifier allowAll = (String hostName, SSLSession session) -> true;
    //     SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, allowAll);

    //     CloseableHttpClient httpClient = HttpClients.custom()
    //         .setSSLSocketFactory(csf)
    //         .build();

    //     HttpComponentsClientHttpRequestFactory requestFactory
    //         = new HttpComponentsClientHttpRequestFactory();

    //     requestFactory.setHttpClient(httpClient);
    //     RestTemplate restTemplate = new RestTemplate(requestFactory);
    //     return restTemplate;
    // }

}
