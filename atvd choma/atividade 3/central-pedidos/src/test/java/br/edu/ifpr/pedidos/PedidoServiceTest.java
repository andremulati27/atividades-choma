package br.edu.ifpr.pedidos;

import static br.edu.ifpr.pedidos.ProcessadorSimulado.aprova;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.comRoteiro;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.falhaGrave;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.indisponivel;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.recusa;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.sempre;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link PedidoService}.
 *
 * Este e o servico que reune as demais regras. O fechamento tem quatro pontos de
 * saida antecipada — cliente bloqueado, pedido sem itens ativos, estoque
 * insuficiente e analise de risco diferente de APROVADO — e so depois deles o
 * cliente e cobrado.
 *
 * A ordem dessas verificacoes importa tanto quanto o resultado: um pedido com
 * cupom invalido e cliente bloqueado deve responder BLOQUEADO, e nao estourar a
 * excecao do cupom. Por isso varios cenarios combinam dois problemas de proposito
 * e conferem qual deles prevalece.
 *
 * O dubl&ecirc; de pagamento registra as cobrancas, o que permite provar que os
 * caminhos interrompidos nao chegaram ao processador. Valores em centavos.
 */
@DisplayName("PedidoService")
class PedidoServiceTest {

    private static final String SKU = "LIVRO-JAVA";
    private static final long PRECO = 10_000;
    private static final int PESO_LEVE = 1_000;

    /** Pedido de uma linha no Parana, com a configuracao que o cenario exigir. */
    private Pedido pedido(long preco, int estoque, boolean expresso, String cupom) {
        ItemPedido item = new ItemPedido(SKU, preco, 1, estoque, PESO_LEVE, false);
        return new Pedido(List.of(item), "PR", expresso, cupom);
    }

    private Pedido pedidoSimples() {
        return pedido(PRECO, 5, false, null);
    }

    private void conferir(ResultadoPedido obtido, String status,
                          long subtotal, long desconto, long frete, long total) {
        assertAll(
            () -> assertEquals(status, obtido.status()),
            () -> assertEquals(subtotal, obtido.subtotalCentavos()),
            () -> assertEquals(desconto, obtido.descontoCentavos()),
            () -> assertEquals(frete, obtido.freteCentavos()),
            () -> assertEquals(total, obtido.totalCentavos())
        );
    }

    @Nested
    @DisplayName("fechamento concluido")
    class FechamentoConcluido {

        @Test
        @DisplayName("cobra o cliente comum e devolve o pedido como PAGO")
        void deveFecharOPedidoSimples() {
            // Preparar: cliente com historico, item disponivel de R$ 100,00 e entrega normal no PR.
            ProcessadorSimulado processador = sempre(aprova());
            PedidoService servico = new PedidoService(processador);

            // Executar: um caminho completo do fechamento, sem nenhuma saida antecipada.
            ResultadoPedido resultado = servico.fechar(pedidoSimples(), new Cliente(false, false, 1));

            // Verificar: sem desconto, frete de R$ 12,00 e uma unica cobranca de R$ 112,00.
            conferir(resultado, "PAGO", 10_000, 0, 1_200, 11_200);
            assertEquals(List.of(11_200L), processador.cobrancas());
        }

        @Test
        @DisplayName("combina desconto de VIP, cupom e entrega expressa")
        void deveCombinarDescontoEFrete() {
            // 10% de VIP e 10% do cupom sobre 35000 chegam ao teto de 20%, ou seja, 7000.
            // Sobre o liquido de 28000 incidem 600 de frete (metade da tarifa do PR) e
            // mais 1500 da entrega expressa.
            ProcessadorSimulado processador = sempre(aprova());
            PedidoService servico = new PedidoService(processador);

            ResultadoPedido resultado =
                servico.fechar(pedido(35_000, 5, true, "EXTRA10"), new Cliente(true, false, 1));

            conferir(resultado, "PAGO", 35_000, 7_000, 2_100, 30_100);
            assertEquals(List.of(30_100L), processador.cobrancas());
        }

        @Test
        @DisplayName("registra PAGAMENTO_RECUSADO preservando os valores calculados")
        void deveRegistrarPagamentoRecusado() {
            ProcessadorSimulado processador = sempre(recusa());
            PedidoService servico = new PedidoService(processador);

            ResultadoPedido resultado = servico.fechar(pedidoSimples(), new Cliente(false, false, 1));

            conferir(resultado, "PAGAMENTO_RECUSADO", 10_000, 0, 1_200, 11_200);
            assertEquals(1, processador.chamadas());
        }
    }

    @Nested
    @DisplayName("repeticao do pagamento delegada ao servico interno")
    class RepeticaoDoPagamento {

        @Test
        @DisplayName("duas indisponibilidades seguidas de aprovacao ainda fecham o pedido")
        void deveInsistirAteAAprovacao() {
            ProcessadorSimulado processador = comRoteiro(indisponivel(), indisponivel(), aprova());
            PedidoService servico = new PedidoService(processador);

            ResultadoPedido resultado = servico.fechar(pedidoSimples(), new Cliente(false, false, 1));

            conferir(resultado, "PAGO", 10_000, 0, 1_200, 11_200);
            assertEquals(3, processador.chamadas());
        }

        @Test
        @DisplayName("processador sempre indisponivel resulta em recusa apos tres tentativas")
        void deveDesistirAposTresTentativas() {
            ProcessadorSimulado processador = sempre(indisponivel());
            PedidoService servico = new PedidoService(processador);

            ResultadoPedido resultado = servico.fechar(pedidoSimples(), new Cliente(false, false, 1));

            conferir(resultado, "PAGAMENTO_RECUSADO", 10_000, 0, 1_200, 11_200);
            assertEquals(3, processador.chamadas());
        }

        @Test
        @DisplayName("falha fora do contrato sobe para quem chamou o fechamento")
        void devePropagarFalhaGrave() {
            PedidoService servico =
                new PedidoService(sempre(falhaGrave(new UnsupportedOperationException())));

            assertThrows(UnsupportedOperationException.class,
                () -> servico.fechar(pedidoSimples(), new Cliente(false, false, 1)));
        }
    }

    @Nested
    @DisplayName("saidas antecipadas, sem cobranca")
    class SaidasAntecipadas {

        /** Falha o teste se o processador for acionado em um caminho interrompido. */
        private ProcessadorSimulado processadorQueNaoDeveSerUsado() {
            return sempre(aprova());
        }

        @Test
        @DisplayName("cliente bloqueado responde BLOQUEADO antes de somar o pedido")
        void deveInterromperNoClienteBloqueado() {
            // O pedido esta vazio e o cupom e desconhecido: se a ordem das verificacoes
            // fosse outra, o fechamento estouraria uma excecao em vez de responder BLOQUEADO.
            ProcessadorSimulado processador = processadorQueNaoDeveSerUsado();
            PedidoService servico = new PedidoService(processador);

            ResultadoPedido resultado =
                servico.fechar(new Pedido(List.of(), "PR", false, "INEXISTENTE"), new Cliente(false, true, 0));

            conferir(resultado, "BLOQUEADO", 0, 0, 0, 0);
            assertEquals(0, processador.chamadas());
        }

        @Test
        @DisplayName("pedido sem itens ativos e recusado com excecao")
        void deveRecusarPedidoSemItensAtivos() {
            ProcessadorSimulado processador = processadorQueNaoDeveSerUsado();
            PedidoService servico = new PedidoService(processador);
            Cliente cliente = new Cliente(false, false, 1);

            // Duas formas de chegar a subtotal zero: nenhuma linha, ou linhas com quantidade zero.
            Pedido semLinhas = new Pedido(List.of(), "PR", false, null);
            Pedido somenteLinhasInativas = new Pedido(
                List.of(new ItemPedido(SKU, PRECO, 0, 0, PESO_LEVE, false)), "PR", false, null);

            assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> servico.fechar(semLinhas, cliente)),
                () -> assertThrows(IllegalArgumentException.class,
                    () -> servico.fechar(somenteLinhasInativas, cliente)),
                () -> assertEquals(0, processador.chamadas())
            );
        }

        @Test
        @DisplayName("estoque insuficiente e verificado antes do cupom")
        void deveInterromperNoEstoque() {
            ProcessadorSimulado processador = processadorQueNaoDeveSerUsado();
            PedidoService servico = new PedidoService(processador);

            ResultadoPedido resultado =
                servico.fechar(pedido(PRECO, 0, false, "INEXISTENTE"), new Cliente(false, false, 1));

            conferir(resultado, "SEM_ESTOQUE", 0, 0, 0, 0);
            assertEquals(0, processador.chamadas());
        }

        @Test
        @DisplayName("cupom desconhecido interrompe o fechamento antes da cobranca")
        void deveInterromperNoCupomDesconhecido() {
            ProcessadorSimulado processador = processadorQueNaoDeveSerUsado();
            PedidoService servico = new PedidoService(processador);

            assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                    () -> servico.fechar(pedido(PRECO, 5, false, "INEXISTENTE"), new Cliente(false, false, 1))),
                () -> assertEquals(0, processador.chamadas())
            );
        }

        @Test
        @DisplayName("analise em revisao devolve os valores calculados, mas nao cobra")
        void deveInterromperNaAnaliseDeRisco() {
            ProcessadorSimulado processador = processadorQueNaoDeveSerUsado();
            PedidoService servico = new PedidoService(processador);

            // Primeira compra com entrega expressa: a analise de risco pede revisao.
            ResultadoPedido resultado =
                servico.fechar(pedido(PRECO, 5, true, null), new Cliente(false, false, 0));

            conferir(resultado, "REVISAO", 10_000, 0, 2_700, 12_700);
            assertEquals(0, processador.chamadas());
        }
    }

    @Nested
    @DisplayName("contrato de referencias obrigatorias")
    class ReferenciasObrigatorias {

        @Test
        @DisplayName("exige processador, pedido e cliente")
        void deveExigirAsTresReferencias() {
            PedidoService servico = new PedidoService(sempre(aprova()));

            assertAll(
                () -> assertThrows(NullPointerException.class, () -> new PedidoService(null)),
                () -> assertThrows(NullPointerException.class,
                    () -> servico.fechar(null, new Cliente(false, false, 1))),
                () -> assertThrows(NullPointerException.class, () -> servico.fechar(pedidoSimples(), null))
            );
        }
    }

    @Nested
    @DisplayName("resultado devolvido")
    class ResultadoDevolvido {

        @Test
        @DisplayName("o registro preserva o status e os quatro valores")
        void devePreservarOsCamposDoResultado() {
            ResultadoPedido resultado = new ResultadoPedido("PAGO", 10_000, 1_000, 1_200, 10_200);

            conferir(resultado, "PAGO", 10_000, 1_000, 1_200, 10_200);
            assertEquals(new ResultadoPedido("PAGO", 10_000, 1_000, 1_200, 10_200), resultado);
        }
    }
}
