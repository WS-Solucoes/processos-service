package ws.processos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import ws.common.config.ModelMapperBeanConfiguration;
import ws.common.config.SpringApplicationContext;

@SpringBootApplication(scanBasePackages = {"ws.erh", "ws.processos"})
@EnableScheduling
@EnableRabbit
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"ws.erh.core.storage", "ws.processos.client"})
@EntityScan(basePackages = {"ws.erh.model.cadastro.processo", "ws.erh.model.core.config", "ws.processos.event"})
@EnableJpaRepositories(basePackages = {"ws.erh.cadastro.processo.repository", "ws.processos.event"})
@Import({ModelMapperBeanConfiguration.class, SpringApplicationContext.class})
public class ProcessosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessosApplication.class, args);
    }
}
