import { test } from '@playwright/test';
import { TelaDeFrete } from './support/telas';

/**
 * Cálculo de frete.
 *
 * Três regras convivem na mesma tela:
 *   1. CEP iniciado por 8 paga R$ 15,00; os demais pagam R$ 25,00;
 *   2. pedidos de R$ 200,00 em diante têm frete grátis, independentemente do CEP;
 *   3. CEP e valor fora do formato produzem a mesma recusa, sem cálculo.
 *
 * Os cenários abaixo tratam cada regra em um bloco, sempre com o valor-limite e
 * seus vizinhos imediatos.
 */

type CenarioDeTarifa = {
  cep: string;
  valor: string;
  mensagem: string;
  observacao: string;
};

const MENSAGEM_DE_RECUSA = 'Dados inválidos';

test.describe('Calculadora de frete', () => {
  test.describe('tarifa conforme a região do CEP', () => {
    const cenarios: CenarioDeTarifa[] = [
      { cep: '80000000', valor: '199,99', mensagem: 'Frete: R$ 15,00', observacao: 'região 8, logo abaixo do frete grátis' },
      { cep: '01000000', valor: '199.99', mensagem: 'Frete: R$ 25,00', observacao: 'demais regiões, logo abaixo do frete grátis' },
      { cep: '80000000', valor: '0,01', mensagem: 'Frete: R$ 15,00', observacao: 'região 8, menor valor aceito' },
      { cep: '01000000', valor: '0.01', mensagem: 'Frete: R$ 25,00', observacao: 'demais regiões, menor valor aceito' },
      { cep: '89999999', valor: '50', mensagem: 'Frete: R$ 15,00', observacao: 'maior CEP da região 8' },
      { cep: '79999999', valor: '50', mensagem: 'Frete: R$ 25,00', observacao: 'CEP imediatamente anterior à região 8' },
    ];

    for (const { cep, valor, mensagem, observacao } of cenarios) {
      test(`cobra "${mensagem}" para CEP ${cep} e valor ${valor} (${observacao})`, async ({ page }) => {
        const tela = new TelaDeFrete(page);
        await tela.abrir();

        await tela.calcularFrete(cep, valor);

        await tela.deveAceitarCom(mensagem);
      });
    }
  });

  test.describe('frete grátis a partir de R$ 200,00', () => {
    const cenarios: CenarioDeTarifa[] = [
      { cep: '80000000', valor: '200', mensagem: 'Frete grátis', observacao: 'valor-limite, região 8' },
      { cep: '01000000', valor: '200,00', mensagem: 'Frete grátis', observacao: 'valor-limite com centavos, demais regiões' },
      { cep: '80000000', valor: '200.01', mensagem: 'Frete grátis', observacao: 'um centavo acima do limite, com ponto' },
      { cep: '01000000', valor: '200,01', mensagem: 'Frete grátis', observacao: 'um centavo acima do limite, com vírgula' },
      { cep: '01000000', valor: '999999', mensagem: 'Frete grátis', observacao: 'valor muito acima do limite' },
    ];

    for (const { cep, valor, mensagem, observacao } of cenarios) {
      test(`isenta o pedido de CEP ${cep} e valor ${valor} (${observacao})`, async ({ page }) => {
        const tela = new TelaDeFrete(page);
        await tela.abrir();

        await tela.calcularFrete(cep, valor);

        await tela.deveAceitarCom(mensagem);
      });
    }
  });

  test.describe('CEP fora do formato de oito dígitos', () => {
    const cepsRecusados = [
      { cep: '', observacao: 'campo em branco' },
      { cep: '8000000', observacao: 'sete dígitos' },
      { cep: '800000000', observacao: 'nove dígitos' },
      { cep: 'abcdefgh', observacao: 'oito letras' },
      { cep: '80000-000', observacao: 'hífen de máscara' },
      { cep: '8000 000', observacao: 'espaço no meio' },
    ];

    for (const { cep, observacao } of cepsRecusados) {
      test(`recusa o CEP "${cep}" (${observacao})`, async ({ page }) => {
        const tela = new TelaDeFrete(page);
        await tela.abrir();

        await tela.calcularFrete(cep, '100');

        await tela.deveRecusarCom(MENSAGEM_DE_RECUSA);
      });
    }
  });

  test.describe('valor do pedido fora do formato monetário', () => {
    const valoresRecusados = [
      { valor: '', observacao: 'campo em branco' },
      { valor: ' ', observacao: 'apenas um espaço' },
      { valor: '0', observacao: 'zero não é pedido válido' },
      { valor: '-0.01', observacao: 'valor negativo' },
      { valor: 'abc', observacao: 'texto no lugar do número' },
      { valor: '100.001', observacao: 'três casas decimais' },
      { valor: '1e2', observacao: 'notação científica' },
      { valor: '1.000,00', observacao: 'separador de milhar' },
      { valor: 'Infinity', observacao: 'palavra reservada do JavaScript' },
    ];

    for (const { valor, observacao } of valoresRecusados) {
      test(`recusa o valor "${valor}" (${observacao})`, async ({ page }) => {
        const tela = new TelaDeFrete(page);
        await tela.abrir();

        await tela.calcularFrete('80000000', valor);

        await tela.deveRecusarCom(MENSAGEM_DE_RECUSA);
      });
    }
  });

  test.describe('combinações entre os dois campos', () => {
    test('descarta espaços em volta do CEP e do valor antes de calcular', async ({ page }) => {
      const tela = new TelaDeFrete(page);
      await tela.abrir();

      await tela.calcularFrete(' 80000000 ', ' 100,50 ');

      await tela.deveAceitarCom('Frete: R$ 15,00');
    });

    test('recusa quando o CEP está incompleto, mesmo com pedido de frete grátis', async ({ page }) => {
      const tela = new TelaDeFrete(page);
      await tela.abrir();

      await tela.calcularFrete('8000000', '200');

      await tela.deveRecusarCom(MENSAGEM_DE_RECUSA);
    });

    test('recusa quando os dois campos estão fora do formato', async ({ page }) => {
      const tela = new TelaDeFrete(page);
      await tela.abrir();

      await tela.calcularFrete('cep', 'valor');

      await tela.deveRecusarCom(MENSAGEM_DE_RECUSA);
    });
  });

  test('permite corrigir os dados e recalcular sem recarregar a tela', async ({ page }) => {
    const tela = new TelaDeFrete(page);
    await tela.abrir();

    await test.step('envio em branco é recusado', async () => {
      await tela.calcular.click();
      await tela.deveRecusarCom(MENSAGEM_DE_RECUSA);
    });

    await test.step('dados corrigidos passam a ser cobrados', async () => {
      await tela.calcularFrete('80000000', '100');
      await tela.deveAceitarCom('Frete: R$ 15,00');
    });

    await test.step('elevar o pedido até o limite concede a isenção', async () => {
      await tela.valor.fill('200');
      await tela.calcular.click();
      await tela.deveAceitarCom('Frete grátis');
    });

    await test.step('voltar abaixo do limite restaura a cobrança', async () => {
      await tela.valor.fill('199,99');
      await tela.calcular.click();
      await tela.deveAceitarCom('Frete: R$ 15,00');
    });
  });
});
