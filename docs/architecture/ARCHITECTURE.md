# Arquitetura — Spring Batch Performance Optimization

## Visão Geral

Este projeto é uma aplicação de benchmark comparativo que executa o mesmo
pipeline de processamento batch com 7 configurações progressivas de otimização,
medindo o impacto de cada técnica sobre 1 milhão de transações financeiras.

---

## Diagrama C4 — Nível de Contexto

```
┌──────────────────────────────────────────────────────────────────┐
│                         Usuário / Desenvolvedor                  │
│                  (executa benchmarks localmente ou em CI)        │
└───────────────────────────────┬──────────────────────────────────┘
                                │
                    docker-compose up / ./mvnw run
                                │
                    ┌───────────▼───────────┐
                    │   Spring Batch App    │
                    │  (Java 21 / SB 3.3)   │
                    └───────────┬───────────┘
                                │ JDBC
                    ┌───────────▼───────────┐
                    │     PostgreSQL 16      │
                    │  (transações + batch   │
                    │   metadata tables)     │
                    └───────────────────────┘
```

---

## Diagrama C4 — Nível de Container

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Spring Batch App                            │
│                                                                     │
│  ┌──────────────┐    ┌───────────────┐    ┌─────────────────────┐  │
│  │  BenchmarkRunner   │ ← CommandLineRunner (@Profile benchmark) │  │
│  │  (orquestrador) │    └───────────────┘                        │  │
│  └──────┬───────┘                                                │  │
│         │ lança jobs via JobLauncher                             │  │
│         ▼                                                        │  │
│  ┌──────────────────────────────────────────────────────────┐    │  │
│  │                   Spring Batch JobRepository              │    │  │
│  │           (metadados em BATCH_JOB_EXECUTION etc.)         │    │  │
│  └──────────────────────────────────────────────────────────┘    │  │
│         │                                                        │  │
│  ┌──────▼───────────────────────────────────────────────────┐    │  │
│  │               Cenários (S1 a S7) — @Configuration        │    │  │
│  │                                                           │    │  │
│  │  S1: chunk=1      S2: chunk=500    S3: chunk+threads      │    │  │
│  │  S4: partitioning  S5: JDBC batch  S6: HikariCP tuned     │    │  │
│  │  S7: all combined                                         │    │  │
│  └──────┬────────────────────────────────────────────────────┘    │  │
│         │ read/write                                              │  │
│         ▼                                                        │  │
│  ┌──────────────────┐  ┌──────────────────┐                      │  │
│  │ JpaPagingItemReader│  │ JdbcBatchItemWriter│ (S5, S7)          │  │
│  │ (todos cenários)  │  │  RepositoryWriter  │ (S1..S4, S6)      │  │
│  └──────────────────┘  └──────────────────┘                      │  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐    │  │
│  │        TransactionItemProcessor (@Component)              │    │  │
│  │   PENDING → PROCESSED  |  não-PENDING → null (skip)       │    │  │
│  └──────────────────────────────────────────────────────────┘    │  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Diagrama C4 — Nível de Componente (Domínio)

```
com.wesleytaumaturgo.batch
│
├── domain/
│   ├── model/
│   │   ├── Transaction      @Entity — entidade central do domínio
│   │   ├── TransactionType  PAYMENT | TRANSFER | REFUND
│   │   └── TransactionStatus PENDING | PROCESSED | FAILED
│   └── repository/
│       └── TransactionRepository  JpaRepository + resetAllToPending()
│
├── generator/
│   └── TransactionDataGenerator  DataFaker, lotes de 1000, log de progresso
│
├── processor/
│   └── TransactionItemProcessor  ItemProcessor<T,T> — PENDING → PROCESSED
│
├── partitioner/
│   └── TransactionRangePartitioner  GridPartitioner por count total
│
├── scenario/
│   ├── S1NaiveConfig       chunk=1, single-thread, JpaWriter
│   ├── S2ChunkConfig       chunk=500, single-thread, JpaWriter
│   ├── S3MultiThreadConfig chunk=500, 8 threads, SynchronizedReader
│   ├── S4PartitionConfig   8 partições, SynchronizedReader @StepScope
│   ├── S5SqlBatchConfig    chunk=500, JdbcBatchItemWriter (UPDATE nativo)
│   ├── S6PoolTuningConfig  chunk=500, HikariCP otimizado (application.yml)
│   └── S7AllCombinedConfig S2+S3+S4+S5+S6 combinados
│
├── benchmark/
│   ├── BenchmarkRunner   CommandLineRunner @Profile(benchmark)
│   ├── BenchmarkResult   DTO imutável: nome, registros, ms, heap, speedup
│   └── BenchmarkReport   Markdown + CSV
│
└── config/
    └── BatchProperties   @ConfigurationProperties(benchmark.*)
```

---

## Decisões Arquiteturais

| ADR | Decisão | Impacto |
|-----|---------|---------|
| ADR-001 | Spring Batch 5 como framework | Suporte nativo a chunk, partitioning, multi-thread |
| ADR-002 | Benchmark sequencial com reset | Isolamento do efeito de cada otimização |
| ADR-003 | PostgreSQL + H2 para testes | Realismo em prod, velocidade em CI |
| ADR-004 | HikariCP tuning via YAML | Configurável por ambiente sem recompilação |

---

## Fluxo de Execução do Benchmark

```
1. docker-compose up -d          → PostgreSQL 16 pronto
2. ./mvnw spring-boot:run \
   -Dspring.profiles.active=generate  → insere 1M transações PENDING
3. ./mvnw spring-boot:run \
   -Dspring.profiles.active=benchmark → executa S1..S7, gera relatório
```

### Estrutura de um Job (exemplo S2)

```
Job: s2ChunkJob
  └── Step: s2ChunkStep (chunk=500)
        ├── Reader:    JpaPagingItemReader<Transaction>
        │              SELECT t FROM Transaction t ORDER BY t.createdAt
        │              (pageSize=500)
        ├── Processor: TransactionItemProcessor
        │              PENDING → markProcessed(now) → PROCESSED
        │              não-PENDING → null (filtrado pelo Spring Batch)
        └── Writer:    RepositoryItemWriter
                       repository.save(transaction)
```

### Por que "ler tudo e filtrar no processor"?

O `JpaPagingItemReader` com filtro `WHERE status = 'PENDING'` sofre de
**page-shift problem**: conforme itens são marcados como PROCESSED, as
páginas subsequentes pulam registros (o OFFSET desloca sobre um conjunto
menor). A solução adotada é ler todos os registros e deixar o processor
filtrar os não-PENDING (retornando `null`, que o Spring Batch ignora).

---

## Propriedades de Configuração

```yaml
benchmark:
  record-count: 1000000   # total de registros a gerar/processar
  chunk-size: 500         # tamanho do chunk (S2..S7)
  thread-count: 8         # threads do TaskExecutor (S3, S7)
  grid-size: 8            # partições (S4, S7)
```

Todos os parâmetros são sobrescritíveis via variáveis de ambiente ou
profiles Spring (`application-generate.yml`, `application-benchmark.yml`).
