package br.edu.ifpr.pedidos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes de {@link PoliticaDesconto}.
 *
 * O metodo calcula o desconto em duas etapas: primeiro a regra do cliente (VIP ou
 * faixa de valor) e depois o bonus do cupom, sempre limitado a um teto de 20% do
 * subtotal. Os cenarios abaixo isolam cada etapa e, no fim, verificam a interacao
 * entre elas — inclusive o caso em que o teto corta o bonus.
 *
 * Todos os valores estao em centavos.
 */
@DisplayName("PoliticaDesconto")
class PoliticaDescontoTest {

    private final PoliticaDesconto politica = new PoliticaDesconto();

    /** Monta um cliente informando apenas o que a regra de desconto observa. */
    private Cliente cliente(boolean vip, int comprasAnteriores) {
        return new Cliente(vip, false, comprasAnteriores);
    }

    @Nested
    @DisplayName("desconto de base, sem cupom")
    class DescontoDeBase {

        @ParameterizedTest(name = "VIP={0}, subtotal={1} resulta em {2}")
        @CsvSource({
            "false, 0,     0",      // cliente comum, pedido nulo
            "false, 49999, 0",      // um centavo abaixo da faixa de 5%
            "false, 50000, 2500",   // limite da faixa de 5%
            "false, 50001, 2500",   // vizinho interno do limite, com truncamento
            "true,  101,   10",     // VIP: 10% com truncamento da divisao inteira
            "true,  50000, 5000"    // VIP recebe 10% mesmo na faixa dos 5%
        })
        void deveAplicarARegraDoCliente(boolean vip, long subtotal, long esperado) {
            assertEquals(esperado, politica.calcular(cliente(vip, 1), subtotal, null));
        }

        @ParameterizedTest(name = "cupom [{0}] nao altera o desconto de base")
        @NullAndEmptySource
        @ValueSource(strings = { " ", "   " })
        void deveIgnorarCupomAusenteOuEmBranco(String cupom) {
            assertEquals(1_000, politica.calcular(cliente(true, 1), 10_000, cupom));
        }
    }

    @Nested
    @DisplayName("cupom BEMVINDO: bonus fixo na primeira compra")
    class CupomBemvindo {

        @ParameterizedTest(name = "compras={0}, subtotal={1} resulta em {2}")
        @CsvSource({
            "0, 9999,  0",      // primeira compra, mas abaixo do minimo de 10000
            "0, 10000, 2000",   // primeira compra no valor minimo: bonus liberado
            "0, 10001, 2000",   // vizinho interno do minimo
            "1, 10000, 0"       // ja comprou antes: o bonus nao se aplica
        })
        void deveExigirPrimeiraCompraEValorMinimo(int compras, long subtotal, long esperado) {
            assertEquals(esperado, politica.calcular(cliente(false, compras), subtotal, "BEMVINDO"));
        }

        @Test
        @DisplayName("soma o bonus ao desconto de base do cliente VIP")
        void deveSomarOBonusFixoAoDescontoDoVip() {
            // 10% de 100000 = 10000, mais 2000 do cupom.
            assertEquals(12_000, politica.calcular(cliente(true, 0), 100_000, "BEMVINDO"));
        }

        @ParameterizedTest(name = "o cupom {0} e reconhecido")
        @ValueSource(strings = { "BEMVINDO", "bemvindo", "  BemVindo  ", "\tBEMVINDO\t" })
        void deveNormalizarEspacosEMaiusculas(String cupom) {
            assertEquals(2_000, politica.calcular(cliente(false, 0), 10_000, cupom));
        }
    }

    @Nested
    @DisplayName("cupom EXTRA10: bonus proporcional")
    class CupomExtra10 {

        @ParameterizedTest(name = "subtotal={0} resulta em {1}")
        @CsvSource({
            "19999, 0",      // um centavo abaixo do minimo de 20000
            "20000, 2000",   // limite do cupom
            "20001, 2000",   // vizinho interno do limite, com truncamento
            "50000, 7500"    // 5% de base mais 10% do cupom
        })
        void deveExigirValorMinimo(long subtotal, long esperado) {
            assertEquals(esperado, politica.calcular(cliente(false, 1), subtotal, "EXTRA10"));
        }

        @Test
        @DisplayName("soma os 10% do cupom aos 10% do cliente VIP")
        void deveSomarOPercentualAoDescontoDoVip() {
            assertEquals(4_000, politica.calcular(cliente(true, 1), 20_000, "EXTRA10"));
        }
    }

    @Nested
    @DisplayName("teto de 20% do subtotal")
    class TetoDoDesconto {

        @Test
        @DisplayName("corta o desconto que ultrapassa o teto")
        void deveCortarNoTeto() {
            // VIP com primeira compra: 1000 de base mais 2000 do cupom = 3000,
            // acima do teto de 2000 (20% de 10000).
            assertEquals(2_000, politica.calcular(cliente(true, 0), 10_000, "BEMVINDO"));
        }

        @Test
        @DisplayName("mantem o desconto exatamente igual ao teto")
        void deveManterODescontoIgualAoTeto() {
            // 10% de base mais 10% do cupom fecham exatamente os 20% permitidos.
            assertEquals(4_000, politica.calcular(cliente(true, 1), 20_000, "EXTRA10"));
        }

        @Test
        @DisplayName("mantem o desconto que fica abaixo do teto")
        void deveManterODescontoAbaixoDoTeto() {
            assertEquals(2_500, politica.calcular(cliente(false, 1), 50_000, null));
        }
    }

    @Nested
    @DisplayName("entradas recusadas")
    class EntradasRecusadas {

        @Test
        @DisplayName("recusa subtotal negativo antes de qualquer calculo")
        void deveRecusarSubtotalNegativo() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> politica.calcular(cliente(false, 1), -1, null));

            assertEquals("Subtotal negativo", erro.getMessage());
        }

        @ParameterizedTest(name = "o cupom {0} e desconhecido")
        @ValueSource(strings = { "NENHUM", "EXTRA20", "BEM VINDO", "X" })
        void deveRecusarCupomDesconhecido(String cupom) {
            assertThrows(IllegalArgumentException.class,
                () -> politica.calcular(cliente(false, 0), 10_000, cupom));
        }
    }
}
