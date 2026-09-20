/**
 * Geradores de dados usados pelos cenarios de senha.
 *
 * Construir as senhas pelo tamanho desejado evita contar caracteres a olho nu e
 * deixa explicito qual valor-limite cada cenario exercita.
 */

/** Monta uma senha valida em formato com exatamente `tamanho` caracteres. */
export function senhaComTamanho(tamanho: number): string {
  const obrigatorios = 'Aa1';
  return obrigatorios + 'x'.repeat(Math.max(0, tamanho - obrigatorios.length));
}

export const TAMANHO_MINIMO = 8;
export const TAMANHO_MAXIMO = 20;
