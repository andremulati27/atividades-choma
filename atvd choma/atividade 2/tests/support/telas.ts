import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Objetos de tela (Page Objects) das duas interfaces desta entrega.
 *
 * Os seletores e as ações ficam concentrados aqui; os arquivos de teste descrevem
 * apenas o cenário e o resultado esperado. Se um rótulo da interface mudar, o
 * ajuste acontece em um único lugar.
 */

/** Base comum: as duas telas respondem com "status" (aceito) ou "alert" (recusado). */
abstract class TelaComResposta {
  protected constructor(protected readonly pagina: Page) {}

  get respostaAceita(): Locator {
    return this.pagina.getByRole('status');
  }

  get respostaRecusada(): Locator {
    return this.pagina.getByRole('alert');
  }

  /** Confere a mensagem de aceite e garante que nenhum alerta ficou na tela. */
  async deveAceitarCom(mensagem: string): Promise<void> {
    await expect(this.respostaAceita).toHaveText(mensagem);
    await expect(this.respostaRecusada).toHaveCount(0);
  }

  /** Confere a mensagem de recusa e garante que nenhum aceite ficou na tela. */
  async deveRecusarCom(mensagem: string): Promise<void> {
    await expect(this.respostaRecusada).toHaveText(mensagem);
    await expect(this.respostaAceita).toHaveCount(0);
  }
}

export class TelaDeFrete extends TelaComResposta {
  readonly cep: Locator;
  readonly valor: Locator;
  readonly calcular: Locator;

  constructor(pagina: Page) {
    super(pagina);
    this.cep = pagina.getByLabel('CEP', { exact: true });
    this.valor = pagina.getByLabel('Valor do pedido');
    this.calcular = pagina.getByRole('button', { name: 'Calcular frete' });
  }

  async abrir(): Promise<void> {
    await this.pagina.goto('/frete');
  }

  async calcularFrete(cep: string, valor: string): Promise<void> {
    await this.cep.fill(cep);
    await this.valor.fill(valor);
    await this.calcular.click();
  }
}

export class TelaDeSenha extends TelaComResposta {
  readonly nova: Locator;
  readonly confirmacao: Locator;
  readonly cadastrar: Locator;

  constructor(pagina: Page) {
    super(pagina);
    this.nova = pagina.getByLabel('Nova senha');
    this.confirmacao = pagina.getByLabel('Confirmar senha');
    this.cadastrar = pagina.getByRole('button', { name: 'Cadastrar senha' });
  }

  async abrir(): Promise<void> {
    await this.pagina.goto('/senha');
  }

  async cadastrarSenha(senha: string, confirmacao: string): Promise<void> {
    await this.nova.fill(senha);
    await this.confirmacao.fill(confirmacao);
    await this.cadastrar.click();
  }

  /** Após um cadastro aceito o formulário é limpo; após recusa, permanece preenchido. */
  async camposDevemEstarLimpos(): Promise<void> {
    await expect(this.nova).toHaveValue('');
    await expect(this.confirmacao).toHaveValue('');
  }
}
