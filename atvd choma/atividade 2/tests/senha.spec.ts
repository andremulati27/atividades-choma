import { test } from '@playwright/test';
import { TelaDeSenha } from './support/telas';
import { senhaComTamanho, TAMANHO_MAXIMO, TAMANHO_MINIMO } from './support/dados';

/**
 * Cadastro de senha.
 *
 * A tela aplica duas verificações em sequência: primeiro o formato (tamanho,
 * composição e ausência de espaços) e, só depois, a igualdade com a confirmação.
 * Essa ordem é importante e está coberta por um cenário próprio no fim do arquivo.
 *
 * As senhas de tamanho-limite são geradas por `senhaComTamanho`, evitando contar
 * caracteres manualmente.
 */

const FORA_DO_PADRAO = 'Senha fora do padrão';
const NAO_COINCIDEM = 'As senhas não coincidem';
const CADASTRADA = 'Senha cadastrada';

test.describe('Criação de senha', () => {
  test.describe('tamanho da senha', () => {
    const tamanhos = [
      { tamanho: TAMANHO_MINIMO - 1, aceita: false, observacao: 'um caractere abaixo do mínimo' },
      { tamanho: TAMANHO_MINIMO, aceita: true, observacao: 'tamanho mínimo' },
      { tamanho: TAMANHO_MINIMO + 1, aceita: true, observacao: 'vizinho interno do mínimo' },
      { tamanho: TAMANHO_MAXIMO - 1, aceita: true, observacao: 'vizinho interno do máximo' },
      { tamanho: TAMANHO_MAXIMO, aceita: true, observacao: 'tamanho máximo' },
      { tamanho: TAMANHO_MAXIMO + 1, aceita: false, observacao: 'um caractere acima do máximo' },
    ];

    for (const { tamanho, aceita, observacao } of tamanhos) {
      test(`${aceita ? 'aceita' : 'recusa'} senha de ${tamanho} caracteres (${observacao})`, async ({ page }) => {
        const tela = new TelaDeSenha(page);
        const senha = senhaComTamanho(tamanho);
        await tela.abrir();

        await tela.cadastrarSenha(senha, senha);

        if (aceita) {
          await tela.deveAceitarCom(CADASTRADA);
        } else {
          await tela.deveRecusarCom(FORA_DO_PADRAO);
        }
      });
    }
  });

  test.describe('composição obrigatória', () => {
    const composicoes = [
      { senha: 'Aa1!xxxx', aceita: true, observacao: 'caractere especial é permitido' },
      { senha: 'Aa1ãéxxx', aceita: true, observacao: 'acentos são permitidos' },
      { senha: 'aa1xxxxx', aceita: false, observacao: 'sem letra maiúscula' },
      { senha: 'AA1XXXXX', aceita: false, observacao: 'sem letra minúscula' },
      { senha: 'Aaxxxxxx', aceita: false, observacao: 'sem algarismo' },
      { senha: '12345678', aceita: false, observacao: 'apenas algarismos' },
      { senha: '', aceita: false, observacao: 'campo em branco' },
    ];

    for (const { senha, aceita, observacao } of composicoes) {
      test(`${aceita ? 'aceita' : 'recusa'} "${senha}" (${observacao})`, async ({ page }) => {
        const tela = new TelaDeSenha(page);
        await tela.abrir();

        await tela.cadastrarSenha(senha, senha);

        if (aceita) {
          await tela.deveAceitarCom(CADASTRADA);
        } else {
          await tela.deveRecusarCom(FORA_DO_PADRAO);
        }
      });
    }
  });

  test.describe('espaços em branco não são permitidos', () => {
    const comEspaco = [
      { senha: 'Aa1 xxxx', observacao: 'espaço no meio' },
      { senha: ' Aa1xxxxx', observacao: 'espaço no início' },
      { senha: 'Aa1xxxxx ', observacao: 'espaço no fim' },
      { senha: 'Aa1\txxxx', observacao: 'tabulação' },
    ];

    for (const { senha, observacao } of comEspaco) {
      test(`recusa senha com ${observacao}`, async ({ page }) => {
        const tela = new TelaDeSenha(page);
        await tela.abrir();

        await tela.cadastrarSenha(senha, senha);

        await tela.deveRecusarCom(FORA_DO_PADRAO);
      });
    }
  });

  test.describe('conferência da confirmação', () => {
    const confirmacoes = [
      { senha: 'Aa1xxxxx', confirmacao: 'Aa1xxxxy', observacao: 'último caractere diferente' },
      { senha: 'Aa1xxxxx', confirmacao: '', observacao: 'confirmação em branco' },
      { senha: 'Aa1xxxxx', confirmacao: 'aa1xxxxx', observacao: 'diferença de maiúsculas e minúsculas' },
      { senha: 'Aa1xxxxx', confirmacao: 'Aa1xxxxx ', observacao: 'espaço extra ao fim da confirmação' },
    ];

    for (const { senha, confirmacao, observacao } of confirmacoes) {
      test(`recusa quando há ${observacao}`, async ({ page }) => {
        const tela = new TelaDeSenha(page);
        await tela.abrir();

        await tela.cadastrarSenha(senha, confirmacao);

        await tela.deveRecusarCom(NAO_COINCIDEM);
      });
    }
  });

  test('limpa os dois campos depois de um cadastro aceito', async ({ page }) => {
    const tela = new TelaDeSenha(page);
    const senha = senhaComTamanho(TAMANHO_MINIMO);
    await tela.abrir();

    await tela.cadastrarSenha(senha, senha);

    await tela.deveAceitarCom(CADASTRADA);
    await tela.camposDevemEstarLimpos();
  });

  test('verifica o formato antes da confirmação e aceita a senha após as correções', async ({ page }) => {
    const tela = new TelaDeSenha(page);
    await tela.abrir();

    await test.step('formato inválido tem prioridade sobre a divergência', async () => {
      await tela.cadastrarSenha('curta', 'diferente');
      await tela.deveRecusarCom(FORA_DO_PADRAO);
    });

    await test.step('com o formato corrigido, a divergência aparece', async () => {
      await tela.nova.fill('Aa1xxxxx');
      await tela.cadastrar.click();
      await tela.deveRecusarCom(NAO_COINCIDEM);
    });

    await test.step('confirmação igual conclui o cadastro', async () => {
      await tela.confirmacao.fill('Aa1xxxxx');
      await tela.cadastrar.click();
      await tela.deveAceitarCom(CADASTRADA);
      await tela.camposDevemEstarLimpos();
    });
  });
});
