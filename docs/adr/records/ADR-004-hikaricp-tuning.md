# ADR-004 — Estratégia de tuning do HikariCP

**Status:** ACEITO
**Data:** 2026-03-29
**Autor:** Wesley Taumaturgo

---

## Contexto

O cenário S6 demonstra o impacto do tuning de connection pool. As configurações
padrão do HikariCP são conservadoras e sub-ótimas para workloads batch intensivos.

É necessário definir quais parâmetros ajustar, quais valores usar como baseline
vs. otimizado, e como demonstrar o impacto de forma reproduzível.

## Decisão

Configurar o HikariCP via `application.yml` com os seguintes parâmetros
para o cenário S6:

| Parâmetro | Padrão HikariCP | Configurado S6 | Motivo |
|-----------|----------------|----------------|--------|
| `maximumPoolSize` | 10 | 20 | Suporta multi-threading S3/S4/S7 |
| `minimumIdle` | igual ao max | 5 | Evita criação dinâmica em pico |
| `connectionTimeout` | 30.000 ms | 30.000 ms | Mantém padrão seguro |
| `idleTimeout` | 600.000 ms | 600.000 ms | Mantém padrão |
| `maxLifetime` | 1.800.000 ms | 1.800.000 ms | Mantém padrão |

## Justificativa

**Por que 20 conexões máximas?**
Para um job com 8 threads (S3/S4) + Spring Batch metadata + gerador de dados,
o pool precisa de pelo menos 10-15 conexões simultâneas. 20 dá margem sem
exagero que causaria contenção no PostgreSQL.

**Por que minimumIdle = 5 e não igual ao max?**
Manter `minimumIdle = maximumPoolSize` garante que todas as conexões estejam
pré-aquecidas, mas desperdiça recursos. Para benchmark, 5 idle é suficiente
para absorver picos sem overhead de criação de conexão durante a execução.

**O que NÃO configuramos:**
- `connectionTestQuery`: PostgreSQL valida conexões via `isValid()` JDBC, mais
  eficiente que uma query SQL
- `dataSourceClassName`: HikariCP detecta automaticamente pelo driver no classpath

## Consequências

- As configurações de pool ficam em `application.yml` (não em código),
  tornando fácil comparar baseline vs. otimizado via arquivos de perfil
- Os cenários S1 a S5 usam o mesmo pool (baseline), S6 usa pool otimizado
- O impacto real do pool tuning é mais evidente com 1M+ registros e
  múltiplas threads concorrentes (S7 combina S4+S5+S6)
