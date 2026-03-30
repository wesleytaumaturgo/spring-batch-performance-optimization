# ADR-003 — PostgreSQL como banco de dados de benchmark

**Status:** ACEITO
**Data:** 2026-03-29
**Autor:** Wesley Taumaturgo

---

## Contexto

O projeto precisa de um banco de dados que:
- Seja representativo de ambientes de produção enterprise
- Suporte volumes de 1M+ registros com boa performance
- Permita demonstrar otimizações de JDBC (batch inserts, connection pool)
- Seja facilmente provisionado via Docker para reprodutibilidade

## Decisão

Usar **PostgreSQL 16** em produção/benchmark e **H2 em modo compatibilidade
PostgreSQL** para testes unitários e de integração.

## Justificativa

**PostgreSQL:**
- Banco relacional mais popular em fintechs e enterprise (onde Spring Batch
  é mais utilizado)
- Suporte a `reWriteBatchedInserts=true` na URL JDBC, que é a otimização
  demonstrada no cenário S5
- HikariCP tem integração nativa com PostgreSQL, relevante para S6
- UUID como tipo nativo (melhor performance que VARCHAR(36))
- Extensão `pg_isready` facilita healthchecks no docker-compose

**H2 para testes:**
- Sem necessidade de container externo nos testes unitários
- Modo `MODE=PostgreSQL` garante compatibilidade de SQL suficiente para
  os casos de uso do projeto
- Inicialização em memória: testes completam em < 10 segundos

**Por que não MySQL/MariaDB?**
PostgreSQL é o banco primário na Sem Parar (Grupo Corpay) e o mais
relevante para o contexto do projeto.

## Consequências

- Testes unitários rodam sem Docker (usam H2 em memória)
- `docker-compose up -d` provisiona PostgreSQL 16 com healthcheck
- A URL JDBC de produção deve incluir `reWriteBatchedInserts=true`
  para que o cenário S5 demonstre o ganho máximo
- Enum `TransactionStatus` é armazenado como `VARCHAR(10)` (portável entre
  H2 e PostgreSQL sem necessidade de tipo `ENUM` nativo do PostgreSQL)
