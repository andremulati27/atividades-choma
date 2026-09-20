package br.edu.ifpr.pedidos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes de {@link AnaliseRisco}.
 *
 * A avaliacao separa o cliente em duas trilhas que nunca se cruzam: quem esta na
 * primeira compra e quem ja comprou antes. Cada trilha tem o proprio limite de
 * valor — 100000 para o cliente novo, 500000 para o antigo — e observa atributos
 * diferentes: a entrega expressa pesa apenas na primeira compra, e a condicao de
 * VIP apenas no historico antigo.
 *
 * Por isso os dois blocos abaixo sao independentes, e cada um percorre o limite,
 * o vizinho de baixo e o vizinho de cima. Valores em centavos.
 */
@DisplayName("AnaliseRisco")
class AnaliseRiscoTest {

    private static final String APROVADO = "APROVADO";
    private static final String REVISAO = "REVISAO";
    private static final String RECUSADO = "RECUSADO";

    private final AnaliseRisco analise = new AnaliseRisco();

    @Nested
    @DisplayName("cliente bloqueado")
    class ClienteBloqueado {

        @ParameterizedTest(name = "recusa o pedido de {0} centavos")
        @ValueSource(longs = { 0, 1_000, 900_000 })
        void deveRecusarAntesDeQualquerOutraRegra(long total) {
            assertEquals(RECUSADO, analise.avaliar(new Cliente(false, true, 0), total, false));
        }

        @Test
        @DisplayName("o bloqueio prevalece sobre a condicao de VIP")
        void deveRecusarAteMesmoOVip() {
            assertEquals(RECUSADO, analise.avaliar(new Cliente(true, true, 10), 100, false));
        }
    }

    @Nested
    @DisplayName("primeira compra: limite de 100000 e atencao a entrega expressa")
    class PrimeiraCompra {

        @ParameterizedTest(name = "total {0} com expresso={1} resulta em {2}")
        @CsvSource({
            "0,      false, APROVADO",   // pedido pequeno em entrega normal
            "99999,  false, APROVADO",   // um centavo abaixo do limite
            "100000, false, APROVADO",   // exatamente no limite
            "100001, false, REVISAO",    // um centavo acima do limite
            "0,      true,  REVISAO",    // entrega expressa basta para a revisao
            "100001, true,  REVISAO"     // as duas causas presentes ao mesmo tempo
        })
        void deveAvaliarValorEEntrega(long total, boolean expresso, String esperado) {
            assertEquals(esperado, analise.avaliar(new Cliente(false, false, 0), total, expresso));
        }

        @Test
        @DisplayName("a condicao de VIP nao livra o cliente novo da revisao")
        void deveRevisarVipNaPrimeiraCompra() {
            assertEquals(REVISAO, analise.avaliar(new Cliente(true, false, 0), 100_001, false));
        }
    }

    @Nested
    @DisplayName("cliente com historico: limite de 500000 e atencao ao VIP")
    class ClienteComHistorico {

        @ParameterizedTest(name = "total {0} com VIP={1} resulta em {2}")
        @CsvSource({
            "499999, false, APROVADO",   // um centavo abaixo do limite
            "500000, false, APROVADO",   // exatamente no limite
            "500001, false, REVISAO",    // um centavo acima do limite
            "500001, true,  APROVADO",   // o VIP nao passa por revisao
            "499999, true,  APROVADO"    // VIP abaixo do limite: nenhuma das causas
        })
        void deveAvaliarValorECondicaoVip(long total, boolean vip, String esperado) {
            assertEquals(esperado, analise.avaliar(new Cliente(vip, false, 1), total, false));
        }

        @Test
        @DisplayName("a entrega expressa nao interfere em quem ja comprou antes")
        void deveIgnorarEntregaExpressaNoClienteAntigo() {
            assertEquals(APROVADO, analise.avaliar(new Cliente(false, false, 5), 1_000, true));
        }
    }

    @Test
    @DisplayName("recusa total negativo antes de olhar o cliente")
    void deveRecusarTotalNegativo() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> analise.avaliar(new Cliente(false, true, 0), -1, false));

        assertEquals("Total negativo", erro.getMessage());
    }
}
