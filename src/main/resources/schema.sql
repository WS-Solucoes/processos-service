CREATE SCHEMA IF NOT EXISTS processos;

ALTER TABLE IF EXISTS public.processo_modelo SET SCHEMA processos;
ALTER TABLE IF EXISTS public.processo_documento_modelo SET SCHEMA processos;
ALTER TABLE IF EXISTS public.processo_etapa_modelo SET SCHEMA processos;
ALTER TABLE IF EXISTS public.processo_campo_modelo SET SCHEMA processos;
ALTER TABLE IF EXISTS public.processo SET SCHEMA processos;
ALTER TABLE IF EXISTS public.processo_documento SET SCHEMA processos;
ALTER TABLE IF EXISTS public.processo_mensagem SET SCHEMA processos;
ALTER TABLE IF EXISTS public.processo_historico SET SCHEMA processos;
ALTER TABLE IF EXISTS public.processo_complementacao SET SCHEMA processos;
ALTER TABLE IF EXISTS public.processo_complementacao_item SET SCHEMA processos;

ALTER SEQUENCE IF EXISTS public.processo_modelo_id_seq SET SCHEMA processos;
ALTER SEQUENCE IF EXISTS public.processo_documento_modelo_id_seq SET SCHEMA processos;
ALTER SEQUENCE IF EXISTS public.processo_etapa_modelo_id_seq SET SCHEMA processos;
ALTER SEQUENCE IF EXISTS public.processo_campo_modelo_id_seq SET SCHEMA processos;
ALTER SEQUENCE IF EXISTS public.processo_id_seq SET SCHEMA processos;
ALTER SEQUENCE IF EXISTS public.processo_documento_id_seq SET SCHEMA processos;
ALTER SEQUENCE IF EXISTS public.processo_mensagem_id_seq SET SCHEMA processos;
ALTER SEQUENCE IF EXISTS public.processo_historico_id_seq SET SCHEMA processos;
ALTER SEQUENCE IF EXISTS public.processo_complementacao_id_seq SET SCHEMA processos;
ALTER SEQUENCE IF EXISTS public.processo_complementacao_item_id_seq SET SCHEMA processos;

ALTER TABLE IF EXISTS processos.processo DROP CONSTRAINT IF EXISTS fk65py5vjq0wi2atouciudcoavm;
ALTER TABLE IF EXISTS processos.processo DROP CONSTRAINT IF EXISTS fkn6pbxhbllk9ay3jnedsu7a8ar;

DROP TABLE IF EXISTS processos.acesso CASCADE;
DROP TABLE IF EXISTS processos.agente_politico CASCADE;
DROP TABLE IF EXISTS processos.endereco CASCADE;
DROP TABLE IF EXISTS processos.gestor_ordenador CASCADE;
DROP TABLE IF EXISTS processos.log CASCADE;
DROP TABLE IF EXISTS processos.log_authentication CASCADE;
DROP TABLE IF EXISTS processos.municipio CASCADE;
DROP TABLE IF EXISTS processos.servidor CASCADE;
DROP TABLE IF EXISTS processos.unidade_gestora CASCADE;
DROP TABLE IF EXISTS processos.usuario CASCADE;
DROP TABLE IF EXISTS processos.usuario_permissao CASCADE;
DROP TABLE IF EXISTS processos.usuario_unidade_gestora CASCADE;
DROP TABLE IF EXISTS processos.vinculo_funcional CASCADE;

ALTER TABLE IF EXISTS processos.processo
    ADD COLUMN IF NOT EXISTS servidor_nome VARCHAR(255),
    ADD COLUMN IF NOT EXISTS servidor_cpf VARCHAR(32),
    ADD COLUMN IF NOT EXISTS vinculo_funcional_matricula VARCHAR(64),
    ADD COLUMN IF NOT EXISTS unidade_gestora_nome VARCHAR(255),
    ADD COLUMN IF NOT EXISTS municipio_nome VARCHAR(255);

CREATE TABLE IF NOT EXISTS processos.processo_outbox_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(80) NOT NULL UNIQUE,
    aggregate_id BIGINT NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    routing_key VARCHAR(160) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL
);
