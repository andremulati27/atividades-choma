package br.edu.ifpr.pedidos;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do registro {@link Cliente}.
 *
 * O construtor compacto tem uma unica decisao: recusar historico negativo. Os
 * testes percorrem os dois ramos dessa decisao e conferem que os tres campos
 * chegam intactos aos acessores.
 */
@DisplayName("Cliente")
class ClienteTest {

    @ParameterizedTest(name = "historico {0} e aceito")
    @ValueSource(ints = { 0, 1, 500 })
    @DisplayName("aceita historico de compras a partir de zero")
    void deveAceitarHistoricoNaoNegativo(int compras) {
        assertEquals(compras, new Cliente(false, false, compras).comprasAnteriores());
    }

    @Test
    @DisplayName("recusa historico negativo, mesmo em -1")
    void deveRecusarHistoricoNegativo() {
        IllegalArgumentException erro =
            assertThrows(IllegalArgumentException.class, () -> new Cliente(false, false, -1));

        assertEquals("Histórico inválido", erro.getMessage());
    }

    @Test
    @DisplayName("preserva os tres campos informados na criacao")
    void devePreservarOsCampos() {
        Cliente cliente = new Cliente(true, true, 7);

        assertAll(
            () -> assertTrue(cliente.vip()),
            () -> assertTrue(cliente.bloqueado()),
            () -> assertEquals(7, cliente.comprasAnteriores())
        );
    }
}
