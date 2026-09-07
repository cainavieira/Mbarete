# ADR-0032 Repositório público no GitHub

- **Data:** 07-09-2026
- **Situação:** aceito
- **Supera:** ADR-0030

## Contexto

O ADR-0030 escolheu repositório privado, e o argumento que decidiu foi a cota de
2.000 minutos de Actions contra os 400 do GitLab. Ele recusou público por
possibilidade de cobrança futura e por irreversibilidade.

Três coisas foram revistas depois, duas por consulta à documentação oficial.

**Público não é aberto.** Repositório público sem arquivo de licença é "todos os
direitos reservados", e os termos do GitHub concedem a outros usuários ver e
bifurcar, não usar, modificar ou distribuir. O argumento da cobrança futura, que
era o principal do ADR-0030, não se sustenta.

**Proteção de branch existe no plano gratuito para repositório público.** A
documentação distingue público de privado exatamente nesse ponto: "Protected
branches are available in public repositories with GitHub Free". Era o único
item em que o plano pago ainda ganharia, e em público o gratuito já o tem.

**E apareceu uma razão que o ADR-0030 não considerou:** a especificação deste
projeto é peça de portfólio, legível por recrutador e por líder técnico sem
envio de arquivo. Repositório privado nunca entrega isso, e nenhum plano pago
compra.

## Decisão

Repositório público no GitHub, no plano gratuito, com três condições.

1. **Sem arquivo de licença.** Fica "todos os direitos reservados", preservando
   a opção de cobrar. Abrir de verdade depois é acrescentar um arquivo, e é
   decisão separada.
2. **Os commits usam o endereço `noreply` do GitHub**, não o e-mail pessoal. Os
   onze commits existentes foram reescritos antes do primeiro push, que é a
   única janela em que reescrever histórico não afeta ninguém.
3. **Os requisitos não funcionais foram lidos** com atenção ao que descreve a
   rede doméstica do AT-02, antes de publicar.

A razão que sustenta a decisão é a visibilidade da especificação. A ausência de
cota de Actions em repositório público é consequência bem-vinda, não fundamento:
o plano Pro elevaria a cota privada a 3.000 minutos, que também bastaria, e a
decisão continuaria a mesma. Um ADR cuja razão central evapora com uma
assinatura é um ADR frágil.

## Consequências

Actions gratuito em runners hospedados padrão, com texto literal na
documentação. Runners maiores continuam cobrados mesmo em público, e não são
usados aqui.

Proteção de branch disponível, ainda que num repositório com uma pessoa
empurrando commit ela seja mais registro de intenção que controle.

A irreversibilidade continua real: tornar privado depois não desfaz clone nem
bifurcação existentes. É aceito, e o que reduz o risco é a ausência de licença
somada à ausência de qualquer dado de usuário no repositório.

Artefato de integração contínua ganha regra própria no `fluxo-de-trabalho.md`,
porque a documentação afirma isenção de cota para pacote público e não afirma o
mesmo para artefato.

## Alternativa recusada: privado no GitHub, plano gratuito

Era a decisão do ADR-0030. Perde a visibilidade, e mantém uma cota de minutos
que o público dispensa, sem proteger nada que a ausência de licença já não
proteja.

## Alternativa recusada: privado no GitHub, plano Pro

Resolveria a cota (3.000 minutos) e a proteção de branch em privado, que são as
duas faltas do gratuito privado. Recusado porque não compra visibilidade, que é
a razão da decisão, e porque tem custo recorrente. Observação registrada: o
plano Pro deixou de aparecer em `github.com/pricing`, e a última cifra oficial
publicada é de abril de 2020, US$4 por mês. O plano continua comprável, mas o
preço atual não está publicado.

## Alternativa recusada: GitLab

O ADR-0030 já a havia recusado por 400 minutos contra 2.000. Com repositório
público a diferença deixa de ser cinco vezes, e os dois pontos em que o GitLab
ganhava, armazenamento maior e proteção de branch no gratuito, desaparecem:
público no GitHub Free tem os dois.
