package br.edu.ifpr.pedidos;

import java.util.List;

/**
 * Fabricas usadas pelos testes do pacote.
 *
 * Reunir a montagem dos objetos em um unico lugar deixa cada teste com apenas o
 * dado que realmente importa para o cenario descrito. A classe nao termina em
 * "Test", portanto o Surefire nao a interpreta como classe de teste.
 */
final class ApoioDePedidos {

    /** Peso padrao de um item, abaixo da franquia de 2 kg do frete. */
    static final int PESO_LEVE = 1_000;

    /** Preco padrao usado quando o valor do item nao e o foco do cenario. */
    static final long PRECO_PADRAO = 10_000;

    private ApoioDePedidos() {
        throw new AssertionError("Classe utilitaria");
    }

    // ---------------------------------------------------------------- clientes

    /** Cliente sem beneficios, com historico de compras. */
    static Cliente comum() {
        return new Cliente(false, false, 1);
    }

    /** Primeira compra: cai na subarvore de cliente novo da analise de risco. */
    static Cliente primeiraCompra() {
        return new Cliente(false, false, 0);
    }

    static Cliente vip() {
        return new Cliente(true, false, 1);
    }

    static Cliente bloqueado() {
        return new Cliente(false, true, 1);
    }

    // ------------------------------------------------------------------- itens

    /** Item disponivel, leve e nao fragil. */
    static ItemPedido item(long preco) {
        return new ItemPedido("SKU-PADRAO", preco, 1, 5, PESO_LEVE, false);
    }

    static ItemPedido item(long preco, int quantidade, int estoque) {
        return new ItemPedido("SKU-PADRAO", preco, quantidade, estoque, PESO_LEVE, false);
    }

    static ItemPedido itemComPeso(int pesoGramas) {
        return new ItemPedido("SKU-PESADO", 100, 1, 5, pesoGramas, false);
    }

    static ItemPedido itemFragil() {
        return new ItemPedido("SKU-FRAGIL", 100, 1, 5, PESO_LEVE, true);
    }

    // ------------------------------------------------------------------ pedido

    static Pedido pedido(String uf, ItemPedido... itens) {
        return new Pedido(List.of(itens), uf, false, null);
    }

    static Pedido pedidoExpresso(String uf, ItemPedido... itens) {
        return new Pedido(List.of(itens), uf, true, null);
    }

    static Pedido pedidoComCupom(String cupom, ItemPedido... itens) {
        return new Pedido(List.of(itens), "PR", false, cupom);
    }
}
