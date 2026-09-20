package br.edu.ifpr.boletim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testes da classe {@link Boletim}.
 *
 * Cada metodo da classe sob teste ganhou um bloco aninhado proprio. Dentro do
 * bloco, os casos foram escolhidos a partir da estrutura do codigo: os dois
 * limites de cada decisao (4 e 7), os vizinhos imediatos desses limites e, no
 * laco de contagem, execucoes com zero, uma e varias repeticoes.
 */
@DisplayName("Boletim")
class BoletimTest {

    /** Margem para comparacao de numeros de ponto flutuante. */
    private static final double TOLERANCIA = 0.0001;

    private final Boletim boletim = new Boletim();

    @Nested
    @DisplayName("calcularMedia soma as duas notas e divide por dois")
    class CalculoDaMedia {

        @ParameterizedTest(name = "media de {0} e {1} deve ser {2}")
        @CsvSource({
            "0,    0,    0",      // piso da escala
            "10,   10,   10",     // teto da escala
            "5,    8,    6.5",    // notas diferentes
            "8,    5,    6.5",    // mesmas notas em ordem trocada
            "0,    10,   5",      // maior distancia possivel entre as notas
            "3.1,  4.2,  3.65",   // resultado com duas casas decimais
            "7,    6.9,  6.95"    // resultado logo abaixo da aprovacao
        })
        void deveCalcularAMediaAritmetica(double primeira, double segunda, double esperada) {
            // Executar: o metodo nao tem decisao, entao um unico caminho atende a todos os casos.
            double obtida = boletim.calcularMedia(primeira, segunda);

            // Verificar: comparacao com tolerancia, por se tratar de ponto flutuante.
            assertEquals(esperada, obtida, TOLERANCIA);
        }

        @Test
        @DisplayName("a ordem das notas nao altera o resultado")
        void deveIgnorarAOrdemDasNotas() {
            assertEquals(boletim.calcularMedia(4, 9), boletim.calcularMedia(9, 4), TOLERANCIA);
        }
    }

    @Nested
    @DisplayName("verificarSituacao classifica a media em tres faixas")
    class SituacaoDoAluno {

        @ParameterizedTest(name = "media {0} deve resultar em {1}")
        @CsvSource({
            "0,     REPROVADO",     // muito abaixo do primeiro limite
            "3.99,  REPROVADO",     // imediatamente abaixo de 4
            "4,     RECUPERACAO",   // limite inferior da recuperacao
            "4.01,  RECUPERACAO",   // vizinho interno do limite 4
            "6.99,  RECUPERACAO",   // imediatamente abaixo de 7
            "7,     APROVADO",      // limite inferior da aprovacao
            "7.01,  APROVADO",      // vizinho interno do limite 7
            "10,    APROVADO"       // teto da escala
        })
        void deveClassificarPelaFaixaDaMedia(double media, String esperado) {
            assertEquals(esperado, boletim.verificarSituacao(media));
        }
    }

    @Nested
    @DisplayName("contarAprovados percorre o vetor de medias")
    class ContagemDeAprovados {

        /** Cada linha descreve um numero de repeticoes do laco e a contagem esperada. */
        static Stream<Arguments> vetoresDeMedias() {
            return Stream.of(
                Arguments.of("vetor vazio: o laco nao executa", new double[] {}, 0),
                Arguments.of("uma media aprovada: ramo verdadeiro", new double[] { 7 }, 1),
                Arguments.of("uma media reprovada: ramo falso", new double[] { 6.99 }, 0),
                Arguments.of("varias medias misturadas", new double[] { 8, 5, 7, 0, 10 }, 3),
                Arguments.of("todas aprovadas", new double[] { 7, 8, 9, 10 }, 4),
                Arguments.of("nenhuma aprovada", new double[] { 1, 2, 3 }, 0)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("vetoresDeMedias")
        void deveContarSomenteAsMediasDeAprovacao(String descricao, double[] medias, int esperado) {
            assertEquals(esperado, boletim.contarAprovados(medias), descricao);
        }

        @Test
        @DisplayName("a nota de corte 7 entra na contagem")
        void deveIncluirAMediaExatamenteIgualASete() {
            assertEquals(2, boletim.contarAprovados(new double[] { 7, 6.999, 7 }));
        }
    }
}
