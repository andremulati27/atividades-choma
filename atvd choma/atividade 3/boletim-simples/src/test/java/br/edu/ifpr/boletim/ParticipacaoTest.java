package br.edu.ifpr.boletim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Testes da classe {@link Participacao}.
 *
 * O metodo calcularPontos tem duas decisoes independentes e uma unica saida.
 * Como cada beneficio soma um valor diferente, a tabela abaixo percorre as quatro
 * combinacoes possiveis: alem de cobrir os dois ramos de cada if, ela distingue
 * qual parcela foi somada, o que a cobertura sozinha nao revelaria.
 */
@DisplayName("Participacao")
class ParticipacaoTest {

    private final Participacao participacao = new Participacao();

    @ParameterizedTest(name = "entregou={0}, participou={1} deve valer {2} ponto(s)")
    @CsvSource({
        "false, false, 0",   // nenhum beneficio: os dois ifs sao falsos
        "true,  false, 2",   // somente a atividade entregue
        "false, true,  1",   // somente a presenca na aula
        "true,  true,  3"    // os dois beneficios somados
    })
    void deveSomarOsPontosDeCadaBeneficio(boolean entregou, boolean participou, int esperado) {
        // Preparar: o objeto nao guarda estado, entao a instancia do campo basta.
        // Executar:
        int pontos = participacao.calcularPontos(entregou, participou);

        // Verificar:
        assertEquals(esperado, pontos);
    }

    @Test
    @DisplayName("a atividade entregue vale o dobro da presenca em aula")
    void deveValorizarMaisAAtividadeEntregue() {
        int somenteAtividade = participacao.calcularPontos(true, false);
        int somenteAula = participacao.calcularPontos(false, true);

        assertEquals(somenteAula * 2, somenteAtividade);
    }
}
