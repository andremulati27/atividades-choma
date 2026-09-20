package br.edu.ifpr.pedidos;

import static br.edu.ifpr.pedidos.ProcessadorSimulado.aprova;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.comRoteiro;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.indisponivel;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.recusa;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.sempre;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Base de caminhos linearmente independentes.
 *
 * Esta classe e a contrapartida executavel dos grafos de fluxo de controle do
 * relatorio. Para cada metodo com complexidade ciclomatica V(G), ha exatamente
 * V(G) testes, um por caminho da base, e o comentario de cada teste traz a
 * sequencia de nos exatamente como ela aparece no desenho.
 *
 * A numeracao dos nos e a mesma do relatorio. Os demais arquivos de teste cobrem
 * as regras de negocio; aqui o objetivo e outro: documentar que a base escolhida
 * e alcancavel com dados reais, e nao apenas no papel.
 *
 * Valores em centavos.
 */
@DisplayName("Base de caminhos independentes")
class BaseDeCaminhosTest {

    // ------------------------------------------------------------- atalhos comuns

    private static Cliente cliente(boolean vip, boolean bloqueado, int compras) {
        return new Cliente(vip, bloqueado, compras);
    }

    private static Pedido pedidoDeFrete(String uf, int peso, boolean expresso, boolean fragil) {
        return new Pedido(List.of(new ItemPedido("SKU", 100, 1, 1, peso, fragil)), uf, expresso, null);
    }

    @Nested
    @DisplayName("PoliticaDesconto.calcular — V(G) = 12")
    class DescontoComDozeCaminhos {

        private final PoliticaDesconto politica = new PoliticaDesconto();

        @Test
        @DisplayName("C1: 1-2-4-6-8-9-11-23 | comum, subtotal 0, sem cupom")
        void descontoCaminho01() {
            assertEquals(0L, politica.calcular(cliente(false, false, 1), 0L, null));
        }

        @Test
        @DisplayName("C2: 1-2-4-5-9-11-23 | VIP, subtotal 10000, sem cupom")
        void descontoCaminho02() {
            assertEquals(1_000L, politica.calcular(cliente(true, false, 1), 10_000L, null));
        }

        @Test
        @DisplayName("C3: 1-2-4-6-7-9-11-23 | comum, subtotal 50000, sem cupom")
        void descontoCaminho03() {
            assertEquals(2_500L, politica.calcular(cliente(false, false, 1), 50_000L, null));
        }

        @Test
        @DisplayName("C4: 1-2-4-6-8-9-10-11-23 | comum, subtotal 0, cupom em branco")
        void descontoCaminho04() {
            assertEquals(0L, politica.calcular(cliente(false, false, 1), 0L, "   "));
        }

        @Test
        @DisplayName("C5: 1-2-4-6-8-9-10-12-13-14-15-19-20-22-23 | primeira compra, 10000, BEMVINDO")
        void descontoCaminho05() {
            assertEquals(2_000L, politica.calcular(cliente(false, false, 0), 10_000L, "BEMVINDO"));
        }

        @Test
        @DisplayName("C6: 1-2-4-6-8-9-10-12-13-14-19-20-22-23 | primeira compra, 9999, BEMVINDO")
        void descontoCaminho06() {
            assertEquals(0L, politica.calcular(cliente(false, false, 0), 9_999L, "BEMVINDO"));
        }

        @Test
        @DisplayName("C7: 1-2-4-6-8-9-10-12-13-19-20-22-23 | ja comprou antes, 10000, BEMVINDO")
        void descontoCaminho07() {
            assertEquals(0L, politica.calcular(cliente(false, false, 1), 10_000L, "BEMVINDO"));
        }

        @Test
        @DisplayName("C8: 1-2-4-6-8-9-10-12-16-17-19-20-22-23 | comum, 20000, EXTRA10")
        void descontoCaminho08() {
            assertEquals(2_000L, politica.calcular(cliente(false, false, 1), 20_000L, "EXTRA10"));
        }

        @Test
        @DisplayName("C9: 1-2-4-6-8-9-10-12-16-19-20-22-23 | comum, 19999, EXTRA10")
        void descontoCaminho09() {
            assertEquals(0L, politica.calcular(cliente(false, false, 1), 19_999L, "EXTRA10"));
        }

        @Test
        @DisplayName("C10: 1-2-4-6-8-9-10-12-18-23 | comum, 10000, cupom desconhecido")
        void descontoCaminho10() {
            assertThrows(IllegalArgumentException.class,
                () -> politica.calcular(cliente(false, false, 1), 10_000L, "CUPOM-X"));
        }

        @Test
        @DisplayName("C11: 1-2-4-5-9-10-12-13-14-15-19-20-21-23 | VIP na primeira compra, 10000, BEMVINDO")
        void descontoCaminho11() {
            // Desconto de 3000 cortado pelo teto de 20% do subtotal.
            assertEquals(2_000L, politica.calcular(cliente(true, false, 0), 10_000L, "BEMVINDO"));
        }

        @Test
        @DisplayName("C12: 1-2-3-23 | subtotal negativo")
        void descontoCaminho12() {
            assertThrows(IllegalArgumentException.class,
                () -> politica.calcular(cliente(false, false, 1), -1L, null));
        }
    }

    @Nested
    @DisplayName("CalculadoraFrete.calcular — V(G) = 11")
    class FreteComOnzeCaminhos {

        private final CalculadoraFrete calculadora = new CalculadoraFrete();

        private long calcular(String uf, int peso, long liquido, boolean expresso, boolean vip, boolean fragil) {
            return calculadora.calcular(pedidoDeFrete(uf, peso, expresso, fragil), cliente(vip, false, 1), liquido);
        }

        @Test
        @DisplayName("C1: 1-2-4-5-9-10-12-15-17-19-21-22 | PR, 2000 g, liquido 29999")
        void freteCaminho01() {
            assertEquals(1_200L, calcular("PR", 2_000, 29_999L, false, false, false));
        }

        @Test
        @DisplayName("C2: 1-2-4-6-9-10-12-15-17-19-21-22 | SP, 2000 g, liquido 29999")
        void freteCaminho02() {
            assertEquals(2_000L, calcular("SP", 2_000, 29_999L, false, false, false));
        }

        @Test
        @DisplayName("C3: 1-2-4-7-9-10-12-15-17-19-21-22 | RJ, 2000 g, liquido 29999")
        void freteCaminho03() {
            assertEquals(2_000L, calcular("RJ", 2_000, 29_999L, false, false, false));
        }

        @Test
        @DisplayName("C4: 1-2-4-8-9-10-12-15-17-19-21-22 | MG, 2000 g, liquido 29999")
        void freteCaminho04() {
            assertEquals(3_000L, calcular("MG", 2_000, 29_999L, false, false, false));
        }

        @Test
        @DisplayName("C5: 1-2-4-5-9-10-11-10-12-15-17-19-21-22 | PR, 2001 g: uma repeticao do laco")
        void freteCaminho05() {
            assertEquals(1_500L, calcular("PR", 2_001, 29_999L, false, false, false));
        }

        @Test
        @DisplayName("C6: 1-2-4-5-9-10-12-15-16-17-19-21-22 | PR, cliente VIP")
        void freteCaminho06() {
            assertEquals(600L, calcular("PR", 2_000, 100L, false, true, false));
        }

        @Test
        @DisplayName("C7: 1-2-4-5-9-10-12-15-17-18-19-21-22 | PR, entrega expressa")
        void freteCaminho07() {
            assertEquals(2_700L, calcular("PR", 2_000, 100L, true, false, false));
        }

        @Test
        @DisplayName("C8: 1-2-4-5-9-10-12-15-17-19-20-21-22 | PR, item fragil")
        void freteCaminho08() {
            assertEquals(1_700L, calcular("PR", 2_000, 100L, false, false, true));
        }

        @Test
        @DisplayName("C9: 1-2-4-5-9-10-12-13-14-15-17-19-21-22 | PR, liquido 30000 em entrega normal")
        void freteCaminho09() {
            assertEquals(0L, calcular("PR", 2_000, 30_000L, false, false, false));
        }

        @Test
        @DisplayName("C10: 1-2-4-5-9-10-12-13-15-17-18-19-21-22 | PR, liquido 30000 em entrega expressa")
        void freteCaminho10() {
            assertEquals(2_700L, calcular("PR", 2_000, 30_000L, true, false, false));
        }

        @Test
        @DisplayName("C11: 1-2-3-22 | valor liquido negativo")
        void freteCaminho11() {
            assertThrows(IllegalArgumentException.class,
                () -> calcular("PR", 2_000, -1L, false, false, false));
        }
    }

    @Nested
    @DisplayName("AnaliseRisco.avaliar — V(G) = 8")
    class RiscoComOitoCaminhos {

        private final AnaliseRisco analise = new AnaliseRisco();

        @Test
        @DisplayName("C1: 1-2-4-6-7-8-12-13 | primeira compra, total 0, entrega normal")
        void riscoCaminho01() {
            assertEquals("APROVADO", analise.avaliar(cliente(false, false, 0), 0L, false));
        }

        @Test
        @DisplayName("C2: 1-2-4-6-7-9-13 | primeira compra, total 100001")
        void riscoCaminho02() {
            assertEquals("REVISAO", analise.avaliar(cliente(false, false, 0), 100_001L, false));
        }

        @Test
        @DisplayName("C3: 1-2-4-6-7-8-9-13 | primeira compra, total 0, entrega expressa")
        void riscoCaminho03() {
            assertEquals("REVISAO", analise.avaliar(cliente(false, false, 0), 0L, true));
        }

        @Test
        @DisplayName("C4: 1-2-4-6-10-12-13 | ja comprou antes, total 0")
        void riscoCaminho04() {
            assertEquals("APROVADO", analise.avaliar(cliente(false, false, 1), 0L, false));
        }

        @Test
        @DisplayName("C5: 1-2-4-6-10-11-9-13 | ja comprou antes, total 500001, sem VIP")
        void riscoCaminho05() {
            assertEquals("REVISAO", analise.avaliar(cliente(false, false, 1), 500_001L, false));
        }

        @Test
        @DisplayName("C6: 1-2-4-6-10-11-12-13 | ja comprou antes, total 500001, cliente VIP")
        void riscoCaminho06() {
            assertEquals("APROVADO", analise.avaliar(cliente(true, false, 1), 500_001L, false));
        }

        @Test
        @DisplayName("C7: 1-2-4-5-13 | cliente bloqueado")
        void riscoCaminho07() {
            assertEquals("RECUSADO", analise.avaliar(cliente(false, true, 0), 0L, false));
        }

        @Test
        @DisplayName("C8: 1-2-3-13 | total negativo")
        void riscoCaminho08() {
            assertThrows(IllegalArgumentException.class,
                () -> analise.avaliar(cliente(false, false, 0), -1L, false));
        }
    }

    @Nested
    @DisplayName("PagamentoService.pagar — V(G) = 7")
    class PagamentoComSeteCaminhos {

        @Test
        @DisplayName("C1: 1-2-4-5-7-8-9-10-15 | processador aprova na primeira chamada")
        void pagamentoCaminho01() {
            ProcessadorSimulado processador = sempre(aprova());

            assertTrue(new PagamentoService(processador).pagar(100L, 3));
            assertEquals(1, processador.chamadas());
        }

        @Test
        @DisplayName("C2: 1-2-4-5-7-8-9-11-12-13-15 | indisponivel com limite de uma tentativa")
        void pagamentoCaminho02() {
            ProcessadorSimulado processador = sempre(indisponivel());

            assertFalse(new PagamentoService(processador).pagar(100L, 1));
            assertEquals(1, processador.chamadas());
        }

        @Test
        @DisplayName("C3: 1-2-4-5-7-8-9-11-12-8-9-10-15 | indisponivel e aprovacao na segunda tentativa")
        void pagamentoCaminho03() {
            ProcessadorSimulado processador = comRoteiro(indisponivel(), aprova());

            assertTrue(new PagamentoService(processador).pagar(100L, 2));
            assertEquals(2, processador.chamadas());
        }

        @Test
        @DisplayName("C4: 1-2-4-5-7-8-9-14-15 | excecao fora do contrato sobe para quem chamou")
        void pagamentoCaminho04() {
            ProcessadorSimulado processador =
                sempre(ProcessadorSimulado.falhaGrave(new UnsupportedOperationException()));

            assertThrows(UnsupportedOperationException.class,
                () -> new PagamentoService(processador).pagar(100L, 3));
            assertEquals(1, processador.chamadas());
        }

        @Test
        @DisplayName("C5: 1-2-3-15 | total igual a zero")
        void pagamentoCaminho05() {
            PagamentoService servico = new PagamentoService(sempre(aprova()));

            assertThrows(IllegalArgumentException.class, () -> servico.pagar(0L, 3));
        }

        @Test
        @DisplayName("C6: 1-2-4-6-15 | limite de tentativas abaixo de um")
        void pagamentoCaminho06() {
            PagamentoService servico = new PagamentoService(sempre(aprova()));

            assertThrows(IllegalArgumentException.class, () -> servico.pagar(100L, 0));
        }

        @Test
        @DisplayName("C7: 1-2-4-5-6-15 | limite de tentativas acima de tres")
        void pagamentoCaminho07() {
            PagamentoService servico = new PagamentoService(sempre(aprova()));

            assertThrows(IllegalArgumentException.class, () -> servico.pagar(100L, 4));
        }
    }

    @Nested
    @DisplayName("PedidoService.fechar — V(G) = 6")
    class FechamentoComSeisCaminhos {

        private static final String SKU = "SKU";

        private Pedido pedido(int estoque, boolean expresso) {
            return new Pedido(List.of(new ItemPedido(SKU, 10_000, 1, estoque, 1_000, false)), "PR", expresso, null);
        }

        @Test
        @DisplayName("C1: 1-2-4-5-7-9-10-11-13-14-16-17 | fechamento aprovado e pago")
        void fechamentoCaminho01() {
            ProcessadorSimulado processador = sempre(aprova());

            ResultadoPedido resultado =
                new PedidoService(processador).fechar(pedido(5, false), cliente(false, false, 1));

            assertEquals(new ResultadoPedido("PAGO", 10_000, 0, 1_200, 11_200), resultado);
            assertEquals(1, processador.chamadas());
        }

        @Test
        @DisplayName("C2: 1-2-4-5-7-9-10-11-13-15-16-17 | pagamento recusado")
        void fechamentoCaminho02() {
            ProcessadorSimulado processador = sempre(recusa());

            ResultadoPedido resultado =
                new PedidoService(processador).fechar(pedido(5, false), cliente(false, false, 1));

            assertEquals(new ResultadoPedido("PAGAMENTO_RECUSADO", 10_000, 0, 1_200, 11_200), resultado);
            assertEquals(1, processador.chamadas());
        }

        @Test
        @DisplayName("C3: 1-2-4-5-7-9-10-11-12-17 | analise de risco em revisao")
        void fechamentoCaminho03() {
            ProcessadorSimulado processador = sempre(aprova());

            ResultadoPedido resultado =
                new PedidoService(processador).fechar(pedido(5, true), cliente(false, false, 0));

            assertEquals(new ResultadoPedido("REVISAO", 10_000, 0, 2_700, 12_700), resultado);
            assertEquals(0, processador.chamadas());
        }

        @Test
        @DisplayName("C4: 1-2-4-5-7-8-17 | estoque insuficiente")
        void fechamentoCaminho04() {
            ProcessadorSimulado processador = sempre(aprova());

            ResultadoPedido resultado =
                new PedidoService(processador).fechar(pedido(0, false), cliente(false, false, 1));

            assertEquals(new ResultadoPedido("SEM_ESTOQUE", 0, 0, 0, 0), resultado);
            assertEquals(0, processador.chamadas());
        }

        @Test
        @DisplayName("C5: 1-2-4-5-6-17 | pedido sem itens ativos")
        void fechamentoCaminho05() {
            ProcessadorSimulado processador = sempre(aprova());
            PedidoService servico = new PedidoService(processador);
            Pedido vazio = new Pedido(List.of(), "PR", false, null);

            assertThrows(IllegalArgumentException.class,
                () -> servico.fechar(vazio, cliente(false, false, 1)));
            assertEquals(0, processador.chamadas());
        }

        @Test
        @DisplayName("C6: 1-2-3-17 | cliente bloqueado")
        void fechamentoCaminho06() {
            ProcessadorSimulado processador = sempre(aprova());

            ResultadoPedido resultado =
                new PedidoService(processador).fechar(pedido(5, false), cliente(false, true, 1));

            assertEquals(new ResultadoPedido("BLOQUEADO", 0, 0, 0, 0), resultado);
            assertEquals(0, processador.chamadas());
        }
    }
}
