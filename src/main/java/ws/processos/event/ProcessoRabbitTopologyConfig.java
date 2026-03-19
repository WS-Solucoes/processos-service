package ws.processos.event;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessoRabbitTopologyConfig {

    @Bean
    Declarables processoRabbitDeclarables(
            @Value("${processos.rabbit.exchange}") String exchangeName,
            @Value("${processos.rabbit.lifecycle-queue}") String lifecycleQueueName,
            @Value("${processos.rabbit.integracao-queue}") String integracaoQueueName,
            @Value("${processos.rabbit.integracao-resultado-queue}") String integracaoResultadoQueueName,
            @Value("${processos.rabbit.lifecycle-routing-key}") String lifecycleRoutingKey,
            @Value("${processos.rabbit.integracao-routing-key}") String integracaoRoutingKey,
            @Value("${processos.rabbit.integracao-resultado-routing-key}") String integracaoResultadoRoutingKey) {
        DirectExchange exchange = new DirectExchange(exchangeName, true, false);
        Queue lifecycleQueue = new Queue(lifecycleQueueName, true);
        Queue integracaoQueue = new Queue(integracaoQueueName, true);
        Queue integracaoResultadoQueue = new Queue(integracaoResultadoQueueName, true);

        Binding lifecycleBinding = BindingBuilder.bind(lifecycleQueue).to(exchange).with(lifecycleRoutingKey);
        Binding integracaoBinding = BindingBuilder.bind(integracaoQueue).to(exchange).with(integracaoRoutingKey);
        Binding integracaoResultadoBinding = BindingBuilder.bind(integracaoResultadoQueue).to(exchange).with(integracaoResultadoRoutingKey);

        return new Declarables(
                exchange,
                lifecycleQueue,
                integracaoQueue,
                integracaoResultadoQueue,
                lifecycleBinding,
                integracaoBinding,
                integracaoResultadoBinding
        );
    }
}
