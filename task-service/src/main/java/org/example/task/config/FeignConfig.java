package org.example.task.config;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Client feignClient(LoadBalancerClient loadBalancerClient,
                              LoadBalancerClientFactory loadBalancerClientFactory) {
        Client delegate = new ApacheHttp5Client();
        return new FeignBlockingLoadBalancerClient(delegate, loadBalancerClient, loadBalancerClientFactory);
    }
}
