package br.edu.ifpr.pedidos;

import static br.edu.ifpr.pedidos.ProcessadorSimulado.aprova;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.comRoteiro;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.falhaGrave;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.indisponivel;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.recusa;
import static br.edu.ifpr.pedidos.ProcessadorSimulado.sempre;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes de {@link PagamentoService}.
 *
 * O servico envolve o processador externo em um laco do tipo do/while, e o que
 * determina o comportamento nao e apenas o valor de retorno, mas a forma como a
 * chamada termina:
 *
 * <ul>
 *   <li>retorno true ou false encerra o pagamento na hora;</li>
 *   <li>IllegalStateException significa indisponibilidade e autoriza nova tentativa;</li>
 *   <li>qualquer outra excecao sobe para quem chamou.</li>
 * </ul>
 *
 * Como essas tres saidas nao aparecem como desvios no codigo-fonte, cada teste
 * confere tambem o numero de chamadas ao processador — e a contagem que prova qual
 * caminho foi percorrido.
 */
@DisplayName("PagamentoService")
class PagamentoServiceTest {

    private static final long TOTAL = 11_200;
    private static final int LIMITE_DE_TENTATIVAS = 3;

    @Nested
    @DisplayName("desfecho na primeira tentativa")
    class DesfechoImediato {

        @Test
        @DisplayName("aprova e cobra o valor informado uma unica vez")
        void deveAprovarNaPrimeiraChamada() {
            ProcessadorSimulado processador = sempre(aprova());

            boolean pago = new PagamentoService(processador).pagar(TOTAL, LIMITE_DE_TENTATIVAS);

            assertAll(
                () -> assertTrue(pago),
                () -> assertEquals(1, processador.chamadas()),
                () -> assertEquals(List.of(TOTAL), processador.cobrancas())
            );
        }

        @Test
        @DisplayName("uma recusa definitiva nao gera nova tentativa")
        void deveEncerrarNaRecusaDefinitiva() {
            ProcessadorSimulado processador = sempre(recusa());

            boolean pago = new PagamentoService(processador).pagar(TOTAL, LIMITE_DE_TENTATIVAS);

            assertAll(
                () -> assertFalse(pago),
                () -> assertEquals(1, processador.chamadas())
            );
        }
    }

    @Nested
    @DisplayName("repeticao por indisponibilidade temporaria")
    class RepeticaoPorIndisponibilidade {

        @ParameterizedTest(name = "aprova na tentativa {0}")
        @ValueSource(ints = { 1, 2, 3 })
        void deveRepetirAteAAprovacao(int tentativaQueAprova) {
            ProcessadorSimulado processador = switch (tentativaQueAprova) {
                case 1 -> comRoteiro(aprova());
                case 2 -> comRoteiro(indisponivel(), aprova());
                default -> comRoteiro(indisponivel(), indisponivel(), aprova());
            };

            boolean pago = new PagamentoService(processador).pagar(TOTAL, LIMITE_DE_TENTATIVAS);

            assertAll(
                () -> assertTrue(pago),
                () -> assertEquals(tentativaQueAprova, processador.chamadas())
            );
        }

        @ParameterizedTest(name = "desiste apos {0} tentativa(s)")
        @ValueSource(ints = { 1, 2, 3 })
        void deveDesistirAoEsgotarOLimite(int limite) {
            ProcessadorSimulado processador = sempre(indisponivel());

            boolean pago = new PagamentoService(processador).pagar(TOTAL, limite);

            assertAll(
                () -> assertFalse(pago),
                () -> assertEquals(limite, processador.chamadas())
            );
        }

        @Test
        @DisplayName("uma recusa apos a indisponibilidade encerra o laco")
        void deveEncerrarQuandoARepeticaoTerminaEmRecusa() {
            ProcessadorSimulado processador = comRoteiro(indisponivel(), recusa());

            boolean pago = new PagamentoService(processador).pagar(TOTAL, LIMITE_DE_TENTATIVAS);

            assertAll(
                () -> assertFalse(pago),
                () -> assertEquals(2, processador.chamadas())
            );
        }

        @Test
        @DisplayName("o mesmo valor e reapresentado em cada tentativa")
        void deveReapresentarOMesmoValor() {
            ProcessadorSimulado processador = comRoteiro(indisponivel(), indisponivel(), aprova());

            new PagamentoService(processador).pagar(TOTAL, LIMITE_DE_TENTATIVAS);

            assertEquals(List.of(TOTAL, TOTAL, TOTAL), processador.cobrancas());
        }
    }

    @Nested
    @DisplayName("falha fora do contrato")
    class FalhaForaDoContrato {

        @Test
        @DisplayName("propaga a excecao inalterada e nao tenta de novo")
        void devePropagarOutrasExcecoes() {
            RuntimeException erroOriginal = new UnsupportedOperationException("meio de pagamento desativado");
            ProcessadorSimulado processador = sempre(falhaGrave(erroOriginal));
            PagamentoService servico = new PagamentoService(processador);

            RuntimeException capturado = assertThrows(UnsupportedOperationException.class,
                () -> servico.pagar(TOTAL, LIMITE_DE_TENTATIVAS));

            assertAll(
                () -> assertSame(erroOriginal, capturado),
                () -> assertEquals(1, processador.chamadas())
            );
        }
    }

    @Nested
    @DisplayName("validacao dos argumentos")
    class ValidacaoDosArgumentos {

        @Test
        @DisplayName("exige um processador de pagamento")
        void deveExigirProcessador() {
            assertThrows(NullPointerException.class, () -> new PagamentoService(null));
        }

        @ParameterizedTest(name = "total {0} e recusado")
        @ValueSource(longs = { 0, -1 })
        void deveExigirTotalPositivo(long total) {
            ProcessadorSimulado processador = sempre(aprova());
            PagamentoService servico = new PagamentoService(processador);

            assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                    () -> servico.pagar(total, LIMITE_DE_TENTATIVAS)),
                () -> assertEquals(0, processador.chamadas(), "o processador nao deve ser chamado")
            );
        }

        @ParameterizedTest(name = "limite de {0} tentativa(s) e recusado")
        @ValueSource(ints = { 0, -1, 4 })
        void deveExigirLimiteEntreUmETres(int limite) {
            ProcessadorSimulado processador = sempre(aprova());
            PagamentoService servico = new PagamentoService(processador);

            assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> servico.pagar(TOTAL, limite)),
                () -> assertEquals(0, processador.chamadas(), "o processador nao deve ser chamado")
            );
        }
    }
}
