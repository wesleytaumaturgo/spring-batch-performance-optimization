# Otimização de Performance — Spring Batch

> De minutos para segundos. Benchmark comparativo de 7 estratégias
> de otimização para processamento batch com Java 21.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Batch](https://img.shields.io/badge/Spring%20Batch-5-green)
![Licença](https://img.shields.io/badge/Licen%C3%A7a-MIT-blue)
![CI](https://github.com/wesleytaumaturgo/spring-batch-performance-optimization/actions/workflows/ci.yml/badge.svg)

## Resultados

| Cenário | Estratégia | Tempo | Speedup |
|:-------:|------------|:-----:|:-------:|
| 1 | Naive (sem otimização) | ~45 min | baseline |
| 2 | Chunk size otimizado | ~32 min | 1.4× |
| 3 | Multi-thread (8 threads) | ~12 min | 3.8× |
| 4 | Partitioning (8 partições) | ~7 min | 6.4× |
| 5 | SQL batch inserts | ~5 min | 9.0× |
| 6 | Connection pool tuning | ~4 min | 11.3× |
| 7 | **Todas combinadas** | **~2 min** | **22.5×** |

> *1M de registros. Gerador de dados incluso. Resultados reproduzíveis.*
> *Tempos variam conforme hardware.*

## Sobre

Processamento batch é o "segredo sujo" do enterprise — todo banco,
seguradora e fintech roda batches críticos, mas quase ninguém documenta
como otimizar. Este projeto preenche essa lacuna com benchmarks reais
mostrando o impacto de cada técnica isoladamente e combinada.

O framework gera dados de teste (100K a 1M registros), executa o
mesmo pipeline com 7 configurações progressivas e produz relatório
comparativo automaticamente.

## Funcionalidades

- 7 cenários progressivos de otimização com benchmark automatizado
- Gerador de dados configurável (100K a 1M+ registros)
- Multi-threading com TaskExecutor configurável
- Partitioning com GridSize dinâmico
- SQL batch inserts com rewriteBatchedStatements
- Connection pool tuning (HikariCP)
- Relatório comparativo gerado automaticamente

## Arquitetura

```
┌─────────────────────────────────────────────┐
│              Benchmark Runner               │
│  ┌─────────┐  ┌──────────┐  ┌───────────┐  │
│  │ DataGen │→ │ Scenario │→ │ Reporter  │  │
│  │ (1M)    │  │ 1..7     │  │ (compare) │  │
│  └─────────┘  └──────────┘  └───────────┘  │
│                    │                        │
│       ┌────────────┼────────────┐           │
│       ▼            ▼            ▼           │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐     │
│  │ Single  │ │ Multi-   │ │ Partitd  │     │
│  │ Thread  │ │ Thread   │ │ + Tuned  │     │
│  └─────────┘ └──────────┘ └──────────┘     │
└─────────────────────────────────────────────┘
```

## Tecnologias

- **Core:** Java 21 · Spring Boot 3.3 · Spring Batch 5
- **Banco:** PostgreSQL 16 · H2 (testes)
- **Pool:** HikariCP
- **Infra:** Docker · Docker Compose · GitHub Actions
- **Testes:** JUnit 5 · Testcontainers

## Como Rodar

```bash
git clone https://github.com/wesleytaumaturgo/spring-batch-performance-optimization.git
cd spring-batch-performance-optimization
docker-compose up -d
./mvnw spring-boot:run -Dspring.profiles.active=generate   # Gerar dados
./mvnw spring-boot:run -Dspring.profiles.active=benchmark   # Rodar benchmarks
```

## Estrutura do Projeto

```
src/main/java/com/wesleytaumaturgo/batch/
├── domain/        # Transaction entity e repository
├── generator/     # Gerador de dados fake
├── scenario/      # 7 configurações de cenário (S1..S7)
├── benchmark/     # Runner, Result e Report
├── processor/     # ItemProcessor compartilhado
├── partitioner/   # Partitioner customizado
└── config/        # DataSource, properties
```

## Contexto

Na Sem Parar (Grupo Corpay), otimizei um pipeline batch que processava
centenas de milhares de registros e travava por múltiplos dias,
comprometendo o fechamento financeiro. Aplicando as técnicas deste
benchmark — partitioning, multi-threading, SQL tuning e pool de
conexões — reduzi o tempo para poucas horas. Este repo traduz esse
aprendizado em benchmarks reproduzíveis. Projeto aplicando módulos
de Performance e Tuning do MBA em Arquitetura de Software (Full Cycle).

## English

Spring Batch performance optimization with benchmarks comparing
7 progressive strategies (up to 22.5x speedup). Generates test data,
runs each scenario, produces comparison reports. Based on real-world
optimization at Sem Parar (Corpay Group). Java 21 + Spring Boot 3.
Run: `docker-compose up -d`.

## Licença

MIT
