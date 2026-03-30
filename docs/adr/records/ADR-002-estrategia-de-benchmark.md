# ADR-002 — Estratégia de benchmark comparativo

**Status:** ACEITO
**Data:** 2026-03-29
**Autor:** Wesley Taumaturgo

---

## Contexto

Para que os benchmarks sejam válidos e reproduzíveis, é necessário definir:
- Como os dados de teste são gerados
- Como isolar o efeito de cada otimização
- Como medir e comparar os resultados

## Decisão

Adotar a estratégia de **benchmark sequencial com reset entre cenários**:

1. Gerar N transações PENDING no banco antes de iniciar
2. Para cada cenário (S1..S7):
   - Reset: `UPDATE transactions SET status = 'PENDING', processed_at = NULL`
   - Medir: iniciar timer, executar job, parar timer
   - Coletar: duração (ms), throughput (reg/s), heap usado (MB)
3. Calcular speedup de cada cenário vs. baseline (S1 Naive)
4. Gerar relatório em Markdown + CSV

## Justificativa

**Por que sequencial e não paralelo?**
Execução paralela introduziria contenção de recursos (CPU, I/O, conexões)
que distorceria os resultados. O objetivo é medir o impacto isolado de
cada técnica, não o comportamento sob carga concorrente entre jobs.

**Por que resetar o status em vez de recriar dados?**
Recriar 1M de registros entre cada cenário adicionaria latência de I/O
irrelevante para a comparação. O reset via UPDATE é em milissegundos.

**Por que DataFaker para geração?**
Dados realistas (contas com formato correto, valores numéricos distribuídos)
evitam otimizações de banco que não ocorrem em produção (ex: compression
de strings repetidas). A semente aleatória fixa (`Faker` com seed) garante
reprodutibilidade.

## Consequências

- O `BenchmarkRunner` é ativado apenas com o profile `benchmark`
  (`@Profile("benchmark")`), evitando execução acidental em testes.
- A classe `BenchmarkReport` gera `benchmark-results.csv` no diretório
  raiz, facilmente importável para análise em Excel/Sheets.
- O cenário S1 (Naive) é sempre a linha de base para cálculo de speedup.
