# Processos — Workflows de RH

Domínio: processos e fluxos do módulo de RH (complementação, documentos, portal do servidor).
Registrado no Eureka como `processos-service`. Porta: `8084`. Prefixo gateway: `/processos/`.

## Arquitetura

Microserviço isolado com AbstractTenantService + **Outbox pattern** para mensageria confiável.

```
ws/erh/
├── cadastro/
│   ├── processo/
│   │   ├── controller/
│   │   ├── service/       # ProcessoComplementacaoService (extends AbstractTenantService)
│   │   ├── repository/    # JpaRepository
│   │   └── dto/
│   └── portal/
│       ├── security/
│       └── service/
├── core/
│   ├── base/              # AbstractTenantService — base para todos os services
│   ├── enums/             # processo/, portal/
│   ├── exception/
│   ├── storage/           # Integração com file-storage-service
│   └── tenant/
└── event/
    ├── ProcessoOutboxPublisher.java         # @Scheduled — publica eventos pendentes
    ├── ProcessoOutboxEvent.java             # Entidade de outbox (PENDING → PUBLISHED/ERROR)
    └── ProcessoIntegracaoResultadoListener.java  # @RabbitListener
```

## AbstractTenantService

Classe base que injeta automaticamente via reflection:
- `unidadeGestoraId` → do `TenantContext`
- `usuarioId` → do `SecurityContext`
- `usuarioLog` → do `TenantContext.currentUnidadeGestoraRole()`

Todos os services devem estender esta classe.

## Outbox Pattern (Mensageria Confiável)

Garante entrega mesmo com falhas de rede:
1. Service salva evento na tabela `ProcessoOutboxEvent` com status `PENDING`
2. `ProcessoOutboxPublisher` (`@Scheduled`) busca pendentes e publica via `RabbitTemplate`
3. Status atualizado para `PUBLISHED` ou `ERROR` com retry automático

## RabbitMQ

- **Exchange:** `processos.exchange` (var: `${processos.rabbit.exchange}`)
- **Routing keys:** `processo.lifecycle`, `processo.integracao.solicitada`, `processo.integracao.resultado`
- **Queues:** `processos.lifecycle.q`, `processos.integracao.solicitada.q`, `processos.integracao.resultado.q`
- **Listener:** `ProcessoIntegracaoResultadoListener` — consome resultados do `eRH-Service`

## Aliases de Rota no Gateway

- `/processos/**` → este serviço (StripPrefix=1)
- `/erh/api/v1/processo/**` → este serviço (alias para compatibilidade)

## Banco de Dados

PostgreSQL, banco `erh`. Credenciais locais: `postgres` / `postgres`.

## Swagger / OpenAPI

Obrigatório expor spec. Usar springdoc-openapi-starter-webmvc-ui.
