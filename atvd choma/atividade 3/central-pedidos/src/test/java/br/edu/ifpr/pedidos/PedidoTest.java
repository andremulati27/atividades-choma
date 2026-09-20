package br.edu.ifpr.pedidos;

import static br.edu.ifpr.pedidos.ApoioDePedidos.item;
import static br.edu.ifpr.pedidos.ApoioDePedidos.itemFragil;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do registro {@link Pedido}.
 *
 * Quatro dos cinco metodos percorrem a lista de itens em laco. Para exercitar a
 * estrutura de cada laco os cenarios usam listas com zero, uma e varias linhas, e
 * colocam o item problematico ora na primeira, ora na ultima posicao, o que
 * importa para o break de estoqueSuficiente.
 */
@DisplayName("Pedido")
class PedidoTest {

    private static final String UF_VALIDA = "PR";
    private static final int LIMITE_DE_LINHAS = 100;

    @Nested
    @DisplayName("validacao na criacao")
    class ValidacaoNaCriacao {

        @Test
        @DisplayName("recusa lista de itens nula")
        void deveRecusarListaNula() {
            assertThrows(IllegalArgumentException.class,
                () -> new Pedido(null, UF_VALIDA, false, null));
        }

        @Test
        @DisplayName("aceita o limite de 100 linhas e recusa a linha 101")
        void deveLimitarAQuantidadeDeLinhas() {
            List<ItemPedido> noLimite = Collections.nCopies(LIMITE_DE_LINHAS, item(100));
            List<ItemPedido> acimaDoLimite = Collections.nCopies(LIMITE_DE_LINHAS + 1, item(100));

            assertAll(
                () -> assertEquals(LIMITE_DE_LINHAS,
                    new Pedido(noLimite, UF_VALIDA, false, null).itens().size()),
                () -> assertThrows(IllegalArgumentException.class,
                    () -> new Pedido(acimaDoLimite, UF_VALIDA, false, null))
            );
        }

        @Test
        @DisplayName("recusa item nulo dentro da lista")
        void deveRecusarItemNuloNaLista() {
            List<ItemPedido> comNulo = Arrays.asList(item(100), null);

            assertThrows(NullPointerException.class,
                () -> new Pedido(comNulo, UF_VALIDA, false, null));
        }

        @ParameterizedTest(name = "UF {0} e recusada")
        @NullSource
        @ValueSource(strings = { "", "pr", "P", "PRR", "1R", "P1", "P R" })
        void deveRecusarUfForaDoPadrao(String uf) {
            assertThrows(IllegalArgumentException.class,
                () -> new Pedido(List.of(), uf, false, null));
        }

        @ParameterizedTest(name = "UF {0} e aceita")
        @ValueSource(strings = { "PR", "SP", "RJ", "ZZ", "AA" })
        void deveAceitarQualquerParDeLetrasMaiusculas(String uf) {
            assertEquals(uf, new Pedido(List.of(), uf, false, null).uf());
        }

        @Test
        @DisplayName("guarda uma copia imutavel da lista recebida")
        void deveGuardarCopiaImutavel() {
            List<ItemPedido> original = new ArrayList<>(List.of(item(100)));
            Pedido pedido = new Pedido(original, UF_VALIDA, false, null);

            original.clear();

            assertAll(
                () -> assertEquals(1, pedido.itens().size(), "a lista original nao afeta o pedido"),
                () -> assertThrows(UnsupportedOperationException.class, () -> pedido.itens().clear())
            );
        }
    }

    @Nested
    @DisplayName("pedido sem itens: laco com zero repeticoes")
    class PedidoSemItens {

        private final Pedido vazio = new Pedido(List.of(), UF_VALIDA, false, null);

        @Test
        @DisplayName("responde com os valores neutros de cada metodo")
        void deveResponderComOsValoresNeutros() {
            assertAll(
                () -> assertEquals(0, vazio.subtotalCentavos()),
                () -> assertEquals(0, vazio.pesoGramas()),
                () -> assertFalse(vazio.temFragil()),
                () -> assertTrue(vazio.estoqueSuficiente())
            );
        }
    }

    @Nested
    @DisplayName("subtotal e peso")
    class SubtotalEPeso {

        /** Uma linha inativa seguida de duas linhas ativas com quantidades diferentes. */
        private Pedido pedidoMisto() {
            return new Pedido(
                List.of(item(500, 0, 0), item(100, 1, 5), item(100, 2, 5)),
                UF_VALIDA, false, null);
        }

        @Test
        @DisplayName("ignora no subtotal as linhas com quantidade zero")
        void deveIgnorarLinhasInativasNoSubtotal() {
            assertEquals(300, pedidoMisto().subtotalCentavos());
        }

        @Test
        @DisplayName("soma o peso de cada unidade pedida")
        void deveSomarOPesoDeTodasAsUnidades() {
            // Tres unidades de 1000 g: a linha inativa contribui com zero.
            assertEquals(3_000, pedidoMisto().pesoGramas());
        }

        @Test
        @DisplayName("suporta o maior pedido possivel sem estouro de tipo")
        void deveSuportarOMaiorPedidoPossivel() {
            ItemPedido maximo = new ItemPedido("MAX", 1_000_000, 100, 100, 100_000, false);
            Pedido pedido = new Pedido(Collections.nCopies(LIMITE_DE_LINHAS, maximo), UF_VALIDA, false, null);

            assertAll(
                () -> assertEquals(10_000_000_000L, pedido.subtotalCentavos()),
                () -> assertEquals(1_000_000_000, pedido.pesoGramas())
            );
        }
    }

    @Nested
    @DisplayName("presenca de item fragil")
    class PresencaDeItemFragil {

        @Test
        @DisplayName("acusa fragilidade quando ha item fragil ativo")
        void deveAcusarItemFragilAtivo() {
            assertTrue(new Pedido(List.of(item(100), itemFragil()), UF_VALIDA, false, null).temFragil());
        }

        @Test
        @DisplayName("nao acusa fragilidade quando o item fragil esta inativo")
        void deveIgnorarItemFragilInativo() {
            ItemPedido fragilInativo = new ItemPedido("SKU-FRAGIL", 100, 0, 0, 1_000, true);

            assertFalse(new Pedido(List.of(fragilInativo, item(100)), UF_VALIDA, false, null).temFragil());
        }

        @Test
        @DisplayName("nao acusa fragilidade quando nenhum item e fragil")
        void deveIgnorarPedidoSemItemFragil() {
            assertFalse(new Pedido(List.of(item(100), item(200)), UF_VALIDA, false, null).temFragil());
        }
    }

    @Nested
    @DisplayName("suficiencia de estoque")
    class SuficienciaDeEstoque {

        @Test
        @DisplayName("confirma o estoque quando todas as linhas cabem")
        void deveConfirmarEstoqueSuficiente() {
            assertTrue(new Pedido(List.of(item(100, 1, 1), item(100, 2, 5)), UF_VALIDA, false, null)
                .estoqueSuficiente());
        }

        @Test
        @DisplayName("interrompe a varredura na primeira linha sem estoque")
        void deveInterromperNaPrimeiraLinha() {
            assertFalse(new Pedido(List.of(item(100, 2, 1), item(100, 1, 5)), UF_VALIDA, false, null)
                .estoqueSuficiente());
        }

        @Test
        @DisplayName("tambem acusa falta de estoque na ultima linha")
        void deveAcusarFaltaNaUltimaLinha() {
            assertFalse(new Pedido(List.of(item(100, 1, 5), item(100, 2, 1)), UF_VALIDA, false, null)
                .estoqueSuficiente());
        }
    }

    @Test
    @DisplayName("preserva os campos de entrega informados")
    void devePreservarOsCamposDeEntrega() {
        Pedido pedido = new Pedido(List.of(item(100)), "SP", true, "EXTRA10");

        assertAll(
            () -> assertEquals("SP", pedido.uf()),
            () -> assertTrue(pedido.expresso()),
            () -> assertEquals("EXTRA10", pedido.cupom())
        );
    }
}
