# ADR-001 — Uso de Spring Batch para processamento em lote

**Status:** ACEITO
**Data:** 2026-03-29
**Autor:** Wesley Taumaturgo

---

## Contexto

O projeto precisa demonstrar técnicas de otimização de desempenho para
processamento batch em larga escala. A escolha do framework impacta
diretamente a legibilidade dos cenários, a facilidade de configuração
de concorrência e a comparabilidade dos resultados.

Alternativas consideradas:

| Opção | Prós | Contras |
|-------|------|---------|
| **Spring Batch** | Padrão enterprise, suporte a chunk, partitioning, multi-thread nativo | Curva de aprendizado, metadata tables |
| Quartz + JDBC manual | Mais controle baixo nível | Muito código boilerplate |
| Spring Integration | Bom para fluxos complexos | Excesso para uso batch puro |
| Java puro + ExecutorService | Simplicidade | Sem suporte a restart, sem métricas integradas |

## Decisão

Usar **Spring Batch 5 com Spring Boot 3.3**.

## Justificativa

1. **Cenários nativos**: Spring Batch fornece suporte de primeira classe para
   chunk-oriented processing, TaskExecutor multi-thread e partitioning —
   exatamente as técnicas que queremos comparar.

2. **Relevância enterprise**: Framework utilizado na Sem Parar (Grupo Corpay)
   e amplamente adotado em fintechs, bancos e seguradoras.

3. **Observabilidade integrada**: Metadados de execução (BATCH_JOB_EXECUTION,
   BATCH_STEP_EXECUTION) permitem comparar métricas reais entre cenários.

4. **Spring Boot 3.3 + Java 21**: Combinação moderna que habilita records,
   virtual threads e melhorias de GC relevantes para batch de alto volume.

## Consequências

- As tabelas de metadados do Spring Batch precisam existir no banco (criadas
  automaticamente com `spring.batch.jdbc.initialize-schema: always` em dev).
- Cada cenário de benchmark é um Spring Batch `Job` independente, o que
  facilita isolar e medir o impacto de cada otimização.
- A configuração via `@Configuration` classes torna cada cenário explícito
  e auditável.
