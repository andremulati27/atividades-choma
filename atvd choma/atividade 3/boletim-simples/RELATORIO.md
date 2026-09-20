# Relatório do grupo — Boletim simples

Integrantes:

- **Pedro Massaki Carnelossi** — RA 24036481-2
- **André Filipe Caetano Mulati** — RA 24011149-2

**Resultado:** 28 execuções JUnit, nenhuma falha, nenhum erro, nenhum teste ignorado.
**Cobertura final (JaCoCo):** instruções 68/68, linhas 21/21, branches 12/12, métodos 6/6, classes 2/2 — todos em 100%.
**Reprodução:** `mvn clean test` nesta pasta; o relatório fica em `target/site/jacoco/index.html`.

Nenhuma regra de produção foi alterada.

## Organização dos testes

O projeto chega com quatro testes prontos em `BoletimTest` e com `ParticipacaoTest` vazio. Em vez de
criar uma classe nova à parte, completamos os dois arquivos existentes e agrupamos os casos em blocos
aninhados (`@Nested`), um por método sob teste. Assim a própria saída do Maven já indica qual método
falhou, antes mesmo de abrir o relatório de cobertura.

| Arquivo | Execuções |
| --- | --- |
| `BoletimTest` | 23 |
| `ParticipacaoTest` | 5 |
| **Total** | **28** |

## Casos de teste

| Método | Entradas exercitadas | Resultado esperado | Critério estrutural |
| --- | --- | --- | --- |
| `calcularMedia` | (0,0); (10,10); (5,8); (8,5); (0,10); (3.1,4.2); (7,6.9) | 0; 10; 6.5; 6.5; 5; 3.65; 6.95 | Método sem decisão, V(G) = 1. Extremos da escala, ordem trocada das parcelas e resultado decimal. Comparação com tolerância de 0.0001. |
| `verificarSituacao` | 0; 3.99; 4; 4.01; 6.99; 7; 7.01; 10 | `REPROVADO`; `REPROVADO`; `RECUPERACAO`; `RECUPERACAO`; `RECUPERACAO`; `APROVADO`; `APROVADO`; `APROVADO` | Os dois limites (4 e 7), cada um com o vizinho de baixo e o de cima. |
| `contarAprovados` | `[]`; `[7]`; `[6.99]`; `[8,5,7,0,10]`; `[7,8,9,10]`; `[1,2,3]`; `[7, 6.999, 7]` | 0; 1; 0; 3; 4; 0; 2 | Laço com zero, uma e várias repetições; os dois ramos do `if` interno; nota de corte inclusa. |
| `calcularPontos` | (false,false); (true,false); (false,true); (true,true) | 0; 2; 1; 3 | As quatro combinações das duas decisões independentes. |

## Evolução da cobertura

| Etapa | Execuções | Instruções | Linhas | Branches | Métodos | Classes |
| --- | --- | --- | --- | --- | --- | --- |
| Inicial — arquivos como recebidos | 4 | 27/68 (39,71%) | 9/21 (42,86%) | 4/12 (33,33%) | 3/6 (50,00%) | 1/2 (50,00%) |
| **Final** | **28** | **68/68 (100%)** | **21/21 (100%)** | **12/12 (100%)** | **6/6 (100%)** | **2/2 (100%)** |

O que cada contador significa: *linhas* indica que as instruções daquelas linhas foram executadas;
*branches*, que a alternativa verdadeira e a falsa de cada decisão foram percorridas; *métodos*, que os
quatro métodos de negócio e os dois construtores foram chamados; *classes*, apenas que ao menos um método
de cada classe rodou. Esse último é o indicador mais fraco dos quatro — e por isso já valia 50% com a
suíte inicial, que sequer tocava em `Participacao`.

## Complexidade e caminhos

- `calcularMedia` não tem decisão: V(G) = 1 e um único caminho basta.
- `verificarSituacao` tem dois `if` encadeados, V(G) = 3. A base é aprovação (7), recuperação (4) e
  reprovação (3.99).
- `contarAprovados` tem um `for` e um `if`, também V(G) = 3: vetor vazio, uma nota aprovada e uma nota
  reprovada já formam a base. As execuções com vários elementos não aumentam a base, mas verificam a
  acumulação do contador.
- `calcularPontos` tem V(G) = 3, porque são duas decisões.

Um detalhe de `calcularPontos` vale registro: as combinações `(true,true)` e `(false,false)` sozinhas já
cobrem **100% dos ramos** e, ainda assim, deixariam sem execução os dois casos em que apenas um benefício
é somado — justamente onde uma troca entre 2 e 1 ponto passaria despercebida. Por isso a tabela usa as
quatro combinações. É um exemplo pequeno, mas mostra que cobertura de ramos não implica cobertura de
comportamento.
