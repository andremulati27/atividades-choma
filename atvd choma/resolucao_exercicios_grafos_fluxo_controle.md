# Resolução dos exercícios — Grafo de Fluxo de Controle

Fonte dos enunciados: `exercicios_grafos_fluxo_controle.md`.

Certo. Vou resolver os **2 exercícios completos**, respondendo todas as tarefas e também as questões para discussão, seguindo exatamente os códigos e critérios do arquivo.

# Exercício 1 — Classificação de pedido

O método possui três decisões: `valor >= 500`, `clienteVip` e `!pagamentoAprovado`.

## 1. Blocos básicos

Podemos dividir o código nos seguintes nós:

**N1 — Início / inicialização**
```text
desconto = 0
```

**N2 — Decisão**
```text
valor >= 500?
```

**N3 — Desconto por valor**
```text
desconto = 10
```

**N4 — Decisão**
```text
clienteVip?
```

**N5 — Desconto VIP**
```text
desconto += 5
```

**N6 — Decisão**
```text
!pagamentoAprovado?
```

**N7 — Pagamento recusado**
```text
return "PAGAMENTO RECUSADO"
```

**N8 — Cálculo e retorno**
```text
valorFinal = valor - (valor * desconto / 100)
return "PEDIDO APROVADO: " + valorFinal
```

**N9 — Fim**

---

## 2. Decisões

Existem **3 decisões**:

1. `valor >= 500`
2. `clienteVip`
3. `!pagamentoAprovado`

---

## 3 e 4. Grafo de Fluxo de Controle

```text
              ┌──────────────┐
              │ N1 - Início  │
              │ desconto = 0 │
              └──────┬───────┘
                     │
                     ▼
              ┌──────────────┐
              │ N2           │
              │ valor >= 500?│
              └───┬──────┬───┘
                V │      │ F
                  ▼      │
           ┌──────────┐  │
           │ N3       │  │
           │desc. = 10│  │
           └────┬─────┘  │
                │        │
                └────┬───┘
                     ▼
              ┌─────────────┐
              │ N4          │
              │ clienteVip? │
              └───┬─────┬───┘
                V │     │ F
                  ▼     │
             ┌────────┐ │
             │ N5     │ │
             │desc.+=5│ │
             └───┬────┘ │
                 │      │
                 └──┬───┘
                    ▼
          ┌───────────────────┐
          │ N6                │
          │!pagamentoAprovado?│
          └────┬─────────┬────┘
             V │         │ F
               ▼         ▼
       ┌────────────┐ ┌───────────────┐
       │ N7         │ │ N8            │
       │ PAGAMENTO  │ │ calcula valor │
       │ RECUSADO   │ │ final/retorna │
       └──────┬─────┘ └───────┬───────┘
              │               │
              └───────┬───────┘
                      ▼
                 ┌────────┐
                 │N9 - Fim│
                 └────────┘
```

O `return "PAGAMENTO RECUSADO"` encerra antecipadamente a execução e vai diretamente para o fim, sem passar pelo cálculo de `valorFinal`.

---

## 5. Número de nós e arestas

Temos:

**N = 9 nós**

Arestas:

```text
N1 → N2
N2 → N3
N2 → N4
N3 → N4
N4 → N5
N4 → N6
N5 → N6
N6 → N7
N6 → N8
N7 → N9
N8 → N9
```

Portanto:

**E = 11 arestas**

---

## 6. Complexidade ciclomática

Usando:

```text
V(G) = E - N + 2
```

Temos:

```text
V(G) = 11 - 9 + 2
V(G) = 4
```

**Complexidade ciclomática = 4.**

---

## 7. Conferência pelas decisões

São 3 decisões:

```text
V(G) = decisões + 1
V(G) = 3 + 1
V(G) = 4
```

Os dois métodos chegam ao mesmo resultado, conforme esperado pelo exercício.

---

## 8, 9 e 10. Caminhos independentes e testes

### Caminho 1

```text
N1 → N2(F) → N4(F) → N6(F) → N8 → N9
```

Entradas:

```text
valor = 100
clienteVip = false
pagamentoAprovado = true
```

Desconto: `0%`

Valor final:

```text
100 - (100 × 0 / 100) = 100
```

**Resultado:** `PEDIDO APROVADO: 100.0`

### Caminho 2

```text
N1 → N2(V) → N3 → N4(F) → N6(F) → N8 → N9
```

Entradas:

```text
valor = 500
clienteVip = false
pagamentoAprovado = true
```

Desconto: `10%`

```text
500 - 50 = 450
```

**Resultado:** `PEDIDO APROVADO: 450.0`

### Caminho 3

```text
N1 → N2(F) → N4(V) → N5 → N6(F) → N8 → N9
```

Entradas:

```text
valor = 100
clienteVip = true
pagamentoAprovado = true
```

Desconto: `5%`

```text
100 - 5 = 95
```

**Resultado:** `PEDIDO APROVADO: 95.0`

### Caminho 4

```text
N1 → N2(F) → N4(F) → N6(V) → N7 → N9
```

Entradas:

```text
valor = 100
clienteVip = false
pagamentoAprovado = false
```

**Resultado:** `PAGAMENTO RECUSADO`

---

## Questões para discussão

**1. Quantas combinações entre as três condições são possíveis?**

Cada condição pode ser verdadeira ou falsa. Portanto:

```text
2³ = 8 combinações
```

São **8 combinações possíveis**.

**2. O número de combinações é igual à complexidade ciclomática? Explique.**

Não. Existem 8 combinações possíveis das condições, mas a complexidade ciclomática é **4**. Ela representa a quantidade de caminhos linearmente independentes do CFG, e não todas as combinações possíveis de valores verdadeiros e falsos.

**3. Como o `return` dentro da terceira condição altera o grafo?**

Quando `!pagamentoAprovado` é verdadeiro, o fluxo vai diretamente para o fim do método. Assim, o cálculo de `valorFinal` não é executado.

**4. É possível executar o cálculo de `valorFinal` quando o pagamento não foi aprovado?**

Não. Quando `pagamentoAprovado == false`, ocorre:

```java
return "PAGAMENTO RECUSADO";
```

e o método termina imediatamente.

---

# Exercício 2 — Análise de leituras de temperatura

O segundo método percorre o vetor usando `while` e classifica cada temperatura em negativa, superior a 35 ou entre 0 e 35.

## 1. Blocos básicos

**N1 — Início / inicialização**
```text
alertas = 0
i = 0
```

**N2 — Decisão do while**
```text
i < temperaturas.length?
```

**N3 — Primeira decisão**
```text
temperaturas[i] < 0?
```

**N4 — Temperatura negativa**
```text
alertas += 2
```

**N5 — Segunda decisão**
```text
temperaturas[i] > 35?
```

**N6 — Temperatura alta**
```text
alertas++
```

**N7 — Incremento**
```text
i++
```

**N8 — Retorno**
```text
return alertas
```

**N9 — Fim**

---

## 2. Decisões

Existem três decisões:

1. `i < temperaturas.length`
2. `temperaturas[i] < 0`
3. `temperaturas[i] > 35`

---

## 3 e 4. Grafo de Fluxo de Controle

```text
            ┌────────────────┐
            │ N1 - Início    │
            │ alertas=0, i=0 │
            └───────┬────────┘
                    ▼
          ┌──────────────────┐
      ┌──►│ N2               │
      │   │ i < tamanho?     │
      │   └────┬────────┬────┘
      │      V │        │ F
      │        ▼        ▼
      │  ┌───────────┐ ┌────────────┐
      │  │ N3        │ │ N8         │
      │  │ temp < 0? │ │return      │
      │  └──┬─────┬──┘ │alertas     │
      │   V │     │ F   └─────┬──────┘
      │     ▼     ▼           ▼
      │ ┌──────┐ ┌───────────┐ ┌──────┐
      │ │ N4   │ │ N5        │ │N9 Fim│
      │ │+= 2  │ │ temp >35? │ └──────┘
      │ └──┬───┘ └──┬─────┬──┘
      │    │        V│     │F
      │    │         ▼     │
      │    │      ┌──────┐ │
      │    │      │ N6   │ │
      │    │      │ += 1 │ │
      │    │      └──┬───┘ │
      │    │         │     │
      │    └────┬────┴─────┘
      │         ▼
      │     ┌────────┐
      │     │ N7     │
      │     │ i++    │
      │     └───┬────┘
      │         │
      └─────────┘
```

A aresta `N7 → N2` é essencial porque representa a repetição do `while`.

---

## 5. Número de nós e arestas

**N = 9 nós**

Arestas:

```text
N1 → N2
N2 → N3
N2 → N8
N3 → N4
N3 → N5
N4 → N7
N5 → N6
N5 → N7
N6 → N7
N7 → N2
N8 → N9
```

Portanto:

**E = 11**

---

## 6. Complexidade ciclomática

Primeira fórmula:

```text
V(G) = E - N + 2
V(G) = 11 - 9 + 2
V(G) = 4
```

Segunda fórmula:

```text
V(G) = decisões + 1
V(G) = 3 + 1
V(G) = 4
```

Logo:

**V(G) = 4**

Isso coincide com a verificação mínima indicada no arquivo.

---

## 7, 8 e 9. Caminhos independentes e vetores de teste

### Caminho 1 — Nenhuma iteração

Entrada:

```text
[]
```

Caminho:

```text
N1 → N2(F) → N8 → N9
```

Como o vetor está vazio:

```text
0 < 0 = falso
```

**Retorno: 0**

### Caminho 2 — Temperatura negativa

Entrada:

```text
[-5]
```

Caminho principal:

```text
N1 → N2(V) → N3(V) → N4 → N7
→ N2(F) → N8 → N9
```

Como `-5 < 0`:

```text
alertas += 2
```

**Retorno: 2**

### Caminho 3 — Temperatura superior a 35

Entrada:

```text
[40]
```

Fluxo:

```text
N1 → N2(V) → N3(F) → N5(V)
→ N6 → N7 → N2(F) → N8 → N9
```

Como `40 > 35`:

```text
alertas++
```

**Retorno: 1**

### Caminho 4 — Temperatura normal

Entrada:

```text
[20]
```

Fluxo:

```text
N1 → N2(V) → N3(F) → N5(F)
→ N7 → N2(F) → N8 → N9
```

Nenhum alerta é acrescentado.

**Retorno: 0**

---

## 10. Por que o retorno do laço precisa aparecer no CFG?

Porque depois de executar `i++`, o programa não continua diretamente para o `return`.

Ele volta para:

```java
while (i < temperaturas.length)
```

e verifica novamente se existe outra temperatura a ser processada. A aresta `N7 → N2` representa justamente essa repetição.

---

# Questões para discussão

**1. Um vetor com várias temperaturas percorre um único caminho ou pode repetir partes do grafo?**

Pode repetir partes do grafo. A cada posição do vetor, o fluxo retorna à condição do `while` e executa novamente os nós internos.

**2. Qual entrada permite sair do método sem acessar uma posição do vetor?**

Um vetor vazio:

```text
[]
```

Como seu tamanho é `0`, a condição inicial `0 < 0` é falsa e nenhuma posição é acessada.

**3. Os testes dos valores `0` e `35` ajudam a avaliar quais fronteiras?**

Eles verificam os limites das duas condições:

```java
temperaturas[i] < 0
temperaturas[i] > 35
```

`0` testa a fronteira da condição `< 0`, enquanto `35` testa a fronteira da condição `> 35`. Nos dois casos, o valor pertence ao intervalo normal.

**4. Por que o `else if` deve ser representado como uma nova decisão?**

Porque ele realiza uma segunda avaliação lógica:

```java
temperaturas[i] > 35
```

Essa condição também possui duas saídas, verdadeira e falsa. Por isso, constitui outro nó de decisão no CFG.

Assim, os dois exercícios atendem ao critério do arquivo de que cada decisão tenha saídas verdadeira e falsa, os `return` levem ao encerramento e o laço tenha sua aresta de retorno.

