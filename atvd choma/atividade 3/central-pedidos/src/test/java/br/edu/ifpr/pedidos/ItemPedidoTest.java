package br.edu.ifpr.pedidos;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do registro {@link ItemPedido}.
 *
 * O construtor compacto encadeia cinco validacoes, quase todas com dois operandos
 * ligados por "ou". Como o operador e de curto-circuito, cada operando precisa ser
 * a causa isolada da recusa em algum teste: por isso os limites inferior e superior
 * de preco, quantidade e peso aparecem separados.
 */
@DisplayName("ItemPedido")
class ItemPedidoTest {

    private static final long PRECO_MAXIMO = 1_000_000;
    private static final int QUANTIDADE_MAXIMA = 100;
    private static final int PESO_MAXIMO = 100_000;

    @Nested
    @DisplayName("criacao valida")
    class CriacaoValida {

        @Test
        @DisplayName("aceita o item nos menores valores permitidos")
        void deveAceitarOsMenoresValores() {
            ItemPedido item = new ItemPedido("A", 1, 0, 0, 1, false);

            assertAll(
                () -> assertEquals("A", item.sku()),
                () -> assertEquals(0, item.totalCentavos()),
                () -> assertTrue(item.disponivel()),
                () -> assertFalse(item.fragil())
            );
        }

        @Test
        @DisplayName("aceita o item nos maiores valores permitidos")
        void deveAceitarOsMaioresValores() {
            ItemPedido item =
                new ItemPedido("B", PRECO_MAXIMO, QUANTIDADE_MAXIMA, QUANTIDADE_MAXIMA, PESO_MAXIMO, true);

            assertAll(
                () -> assertEquals(100_000_000L, item.totalCentavos()),
                () -> assertTrue(item.disponivel()),
                () -> assertTrue(item.fragil())
            );
        }
    }

    @Nested
    @DisplayName("validacao do SKU")
    class ValidacaoDoSku {

        @ParameterizedTest(name = "SKU {0} e recusado")
        @NullAndEmptySource
        @ValueSource(strings = { " ", "   ", "\t" })
        void deveRecusarSkuAusenteOuEmBranco(String sku) {
            assertThrows(IllegalArgumentException.class,
                () -> new ItemPedido(sku, 1, 1, 1, 1, false));
        }
    }

    @Nested
    @DisplayName("validacao dos numeros")
    class ValidacaoDosNumeros {

        @ParameterizedTest(name = "preco {0} e recusado")
        @ValueSource(longs = { 0, -1, PRECO_MAXIMO + 1 })
        void deveRecusarPrecoForaDaFaixa(long preco) {
            assertThrows(IllegalArgumentException.class,
                () -> new ItemPedido("A", preco, 1, 1, 1, false));
        }

        @ParameterizedTest(name = "quantidade {0} e recusada")
        @ValueSource(ints = { -1, QUANTIDADE_MAXIMA + 1 })
        void deveRecusarQuantidadeForaDaFaixa(int quantidade) {
            assertThrows(IllegalArgumentException.class,
                () -> new ItemPedido("A", 1, quantidade, 1, 1, false));
        }

        @Test
        @DisplayName("recusa estoque negativo")
        void deveRecusarEstoqueNegativo() {
            assertThrows(IllegalArgumentException.class,
                () -> new ItemPedido("A", 1, 1, -1, 1, false));
        }

        @ParameterizedTest(name = "peso {0} e recusado")
        @ValueSource(ints = { 0, -1, PESO_MAXIMO + 1 })
        void deveRecusarPesoForaDaFaixa(int peso) {
            assertThrows(IllegalArgumentException.class,
                () -> new ItemPedido("A", 1, 1, 1, peso, false));
        }
    }

    @Nested
    @DisplayName("regras de negocio do item")
    class RegrasDoItem {

        @ParameterizedTest(name = "{0} centavos x {1} unidades = {2}")
        @CsvSource({
            "100,     0,   0",
            "100,     1,   100",
            "100,     3,   300",
            "1000000, 100, 100000000"
        })
        void deveMultiplicarPrecoPelaQuantidade(long preco, int quantidade, long esperado) {
            assertEquals(esperado, new ItemPedido("A", preco, quantidade, 100, 1, false).totalCentavos());
        }

        @ParameterizedTest(name = "quantidade {0} com estoque {1}: disponivel={2}")
        @CsvSource({
            "1, 2, true",    // sobra estoque
            "2, 2, true",    // estoque exatamente suficiente
            "3, 2, false",   // falta uma unidade
            "0, 0, true"     // item inativo nao consome estoque
        })
        void deveCompararQuantidadeComEstoque(int quantidade, int estoque, boolean esperado) {
            assertEquals(esperado, new ItemPedido("A", 100, quantidade, estoque, 1, false).disponivel());
        }
    }
}
