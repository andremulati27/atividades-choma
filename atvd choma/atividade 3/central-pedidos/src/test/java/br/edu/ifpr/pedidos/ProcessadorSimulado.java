package br.edu.ifpr.pedidos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Dubl&ecirc; de teste do processador de pagamento.
 *
 * O contrato de {@link ProcessadorPagamento} tem tres desfechos possiveis: aprovar,
 * recusar em definitivo e falhar. Esta classe permite combinar um roteiro com um
 * desfecho por chamada, de modo que um teste possa dizer, por exemplo, "falhe duas
 * vezes e aprove na terceira" sem escrever logica de controle dentro do teste.
 *
 * Alem do resultado, o dubl&ecirc; registra quantas vezes foi chamado e com quais
 * valores, o que permite verificar que o servico nao cobrou o cliente duas vezes.
 *
 * Esgotado o roteiro, a ultima instrucao passa a se repetir indefinidamente.
 */
final class ProcessadorSimulado implements ProcessadorPagamento {

    private final List<Supplier<Boolean>> roteiro;
    private final List<Long> cobrancas = new ArrayList<>();

    private ProcessadorSimulado(List<Supplier<Boolean>> roteiro) {
        this.roteiro = roteiro;
    }

    @SafeVarargs
    static ProcessadorSimulado comRoteiro(Supplier<Boolean>... passos) {
        if (passos.length == 0) {
            throw new IllegalArgumentException("Informe ao menos um desfecho");
        }
        return new ProcessadorSimulado(List.of(passos));
    }

    /** Atalho para o caso mais comum: o mesmo desfecho em todas as chamadas. */
    static ProcessadorSimulado sempre(Supplier<Boolean> desfecho) {
        return comRoteiro(desfecho);
    }

    // ---------------------------------------------------------------- desfechos

    static Supplier<Boolean> aprova() {
        return () -> true;
    }

    static Supplier<Boolean> recusa() {
        return () -> false;
    }

    /** Indisponibilidade temporaria: o contrato permite nova tentativa. */
    static Supplier<Boolean> indisponivel() {
        return () -> {
            throw new IllegalStateException("Processador temporariamente fora do ar");
        };
    }

    /** Falha que nao é prevista no contrato e deve subir para quem chamou. */
    static Supplier<Boolean> falhaGrave(RuntimeException erro) {
        return () -> {
            throw erro;
        };
    }

    // ------------------------------------------------------------------ contrato

    @Override
    public boolean autorizar(long totalCentavos) {
        cobrancas.add(totalCentavos);
        int passo = Math.min(cobrancas.size() - 1, roteiro.size() - 1);
        return roteiro.get(passo).get();
    }

    // --------------------------------------------------------------- verificacao

    int chamadas() {
        return cobrancas.size();
    }

    List<Long> cobrancas() {
        return List.copyOf(cobrancas);
    }
}
