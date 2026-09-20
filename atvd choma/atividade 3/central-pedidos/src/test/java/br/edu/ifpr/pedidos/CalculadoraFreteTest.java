package br.edu.ifpr.pedidos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes de {@link CalculadoraFrete}.
 *
 * O calculo tem quatro etapas encadeadas: tarifa base pela UF (um switch), adicional
 * por peso excedente (um laco while), gratuidade para pedidos altos em entrega normal
 * e, por fim, os ajustes de VIP, entrega expressa e item fragil.
 *
 * Cada bloco abaixo trata de uma dessas etapas isoladamente; o ultimo bloco cuida das
 * combinacoes, que sao o ponto em que a cobertura de ramos deixa de ser suficiente.
 * Todos os valores estao em centavos.
 */
@DisplayName("CalculadoraFrete")
class CalculadoraFreteTest {

    private static final long LIQUIDO_ABAIXO_DA_GRATUIDADE = 29_999;
    private static final long LIQUIDO_NA_GRATUIDADE = 30_000;

    private final CalculadoraFrete calculadora = new CalculadoraFrete();

    /** Pedido de uma linha, com o peso e a fragilidade que o cenario exige. */
    private Pedido pedido(String uf, int pesoGramas, boolean expresso, boolean fragil) {
        ItemPedido item = new ItemPedido("SKU-FRETE", 100, 1, 1, pesoGramas, fragil);
        return new Pedido(List.of(item), uf, expresso, null);
    }

    private long frete(String uf, int peso, long liquido, boolean expresso, boolean vip, boolean fragil) {
        return calculadora.calcular(pedido(uf, peso, expresso, fragil), new Cliente(vip, false, 1), liquido);
    }

    @Nested
    @DisplayName("tarifa base conforme a UF")
    class TarifaBase {

        @ParameterizedTest(name = "UF {0} custa {1}")
        @CsvSource({
            "PR, 1200",   // alternativa dedicada do switch
            "SP, 2000",   // primeiro rotulo do bloco compartilhado
            "RJ, 2000",   // segundo rotulo do mesmo bloco
            "MG, 3000",   // cai no default
            "ZZ, 3000"    // UF valida no formato, mas sem tarifa propria
        })
        void deveCobrarATarifaDaRegiao(String uf, long esperado) {
            assertEquals(esperado, frete(uf, 2_000, LIQUIDO_ABAIXO_DA_GRATUIDADE, false, false, false));
        }
    }

    @Nested
    @DisplayName("adicional por peso excedente")
    class AdicionalPorPeso {

        @ParameterizedTest(name = "peso de {0} g cobra {1}")
        @CsvSource({
            "1,    1200",   // muito abaixo da franquia: o laco nao executa
            "1999, 1200",   // um grama abaixo da franquia
            "2000, 1200",   // exatamente na franquia de 2 kg
            "2001, 1500",   // um grama acima: uma repeticao do laco
            "3000, 1500",   // fecha o primeiro quilo excedente
            "3001, 1800",   // inicia o segundo quilo: duas repeticoes
            "5000, 2100"    // tres repeticoes do laco
        })
        void deveCobrarTrezentosPorQuiloIniciado(int peso, long esperado) {
            assertEquals(esperado, frete("PR", peso, LIQUIDO_ABAIXO_DA_GRATUIDADE, false, false, false));
        }
    }

    @Nested
    @DisplayName("gratuidade em entrega normal")
    class Gratuidade {

        @Test
        @DisplayName("isenta o frete a partir do valor liquido de 30000")
        void deveIsentarPedidoAltoEmEntregaNormal() {
            assertEquals(0, frete("PR", 2_000, LIQUIDO_NA_GRATUIDADE, false, false, false));
        }

        @Test
        @DisplayName("nao isenta um centavo abaixo do limite")
        void deveCobrarUmCentavoAbaixoDoLimite() {
            assertEquals(1_200, frete("PR", 2_000, LIQUIDO_ABAIXO_DA_GRATUIDADE, false, false, false));
        }

        @Test
        @DisplayName("nao isenta a entrega expressa, mesmo com pedido alto")
        void deveCobrarEntregaExpressaAcimaDoLimite() {
            assertEquals(2_700, frete("PR", 2_000, LIQUIDO_NA_GRATUIDADE, true, false, false));
        }
    }

    @Nested
    @DisplayName("ajustes finais")
    class AjustesFinais {

        @Test
        @DisplayName("cliente VIP paga metade da tarifa")
        void deveDividirATarifaDoVip() {
            assertEquals(600, frete("PR", 2_000, 100, false, true, false));
        }

        @Test
        @DisplayName("entrega expressa soma 1500")
        void deveSomarOAdicionalDeEntregaExpressa() {
            assertEquals(2_700, frete("PR", 2_000, 100, true, false, false));
        }

        @Test
        @DisplayName("item fragil soma 500")
        void deveSomarOAdicionalDeFragilidade() {
            assertEquals(1_700, frete("PR", 2_000, 100, false, false, true));
        }

        @Test
        @DisplayName("o adicional de fragilidade e cobrado uma unica vez")
        void deveCobrarAFragilidadeUmaVezSo() {
            Pedido comDoisFrageis = new Pedido(
                List.of(new ItemPedido("A", 100, 1, 1, 1, true), new ItemPedido("B", 100, 1, 1, 1, true)),
                "PR", false, null);

            assertEquals(1_700, calculadora.calcular(comDoisFrageis, new Cliente(false, false, 1), 100));
        }
    }

    @Nested
    @DisplayName("combinacoes entre as etapas")
    class Combinacoes {

        @Test
        @DisplayName("VIP com entrega expressa e item fragil")
        void deveEncadearOsTresAjustes() {
            // 1200 de base, metade por ser VIP, mais 1500 do expresso e 500 do fragil.
            assertEquals(2_600, frete("PR", 2_000, 100, true, true, true));
        }

        @Test
        @DisplayName("a fragilidade continua sendo cobrada sobre frete gratuito")
        void deveCobrarFragilidadeSobreFreteGratuito() {
            // Base 1800 zerada pela gratuidade; resta apenas o adicional de fragilidade.
            assertEquals(500, frete("PR", 3_001, 30_001, false, false, true));
        }

        @Test
        @DisplayName("a metade do VIP aplicada sobre frete gratuito continua zero")
        void deveManterZeroParaVipComFreteGratuito() {
            assertEquals(0, frete("PR", 2_000, LIQUIDO_NA_GRATUIDADE, false, true, false));
        }
    }

    @Nested
    @DisplayName("entrada recusada")
    class EntradaRecusada {

        @ParameterizedTest(name = "valor liquido {0} e recusado")
        @ValueSource(longs = { -1, -30_000 })
        void deveRecusarValorLiquidoNegativo(long liquido) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> frete("PR", 2_000, liquido, false, false, false));

            assertEquals("Valor líquido negativo", erro.getMessage());
        }
    }
}
