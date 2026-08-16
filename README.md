<div align="center">

# AlkaVips

### Sistema completo de VIPs

Keys, upgrades, mercado P2P e benefícios pra rede Alka*.

![Java](https://img.shields.io/badge/Java-21-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Version](https://img.shields.io/badge/Version-1.0.29-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

</div>

---

## 📋 Sobre o Projeto

O **AlkaVips** é o sistema de VIPs da rede Alka*: ativação por key, troca
entre VIPs, mercado entre jogadores, sistema de afiliados, carteira com
histórico e uma árvore de benefícios opcional. Tudo integrado nativamente
com a AlkaEconomy e o LuckPerms.

## ✨ Funcionalidades Principais

- 🔑 **Ativação por key** — cada key ativa um VIP com duração configurável.
- 🔄 **Troca entre VIPs** — jogadores com mais de um VIP ativo podem
  alternar qual está em uso.
- 🛒 **Marketplace de keys** — venda e compra de keys entre jogadores, com
  preço definido em qualquer moeda da AlkaEconomy.
- 🤝 **Mercado P2P de assinaturas** — jogadores também podem negociar VIPs
  já ativos entre si.
- 💼 **Carteira VIP** — histórico de compras, gastos e conquistas.
- 🎉 **Sistema de afiliados** — indique jogadores e ganhe recompensas.
- 🌳 **Árvore de benefícios** — progressão opcional de perks por VIP.
- ❄️ **Congelamento de tempo** — pausa a contagem do VIP quando o jogador
  quiser.
- ⚡ **Perks práticos** — fly, voltar pro local da morte, saciar fome,
  reparar item na mão, bigorna e bancada portáteis, tudo liberado por VIP.
- 🎁 **Bônus de ativação** — recompensa configurável ao ativar um VIP pela
  primeira vez.
- 💎 **Moeda própria (Prisma)** — crédito específico de VIP, com comandos
  de consulta e gestão administrativa.

## 🎮 Comandos

| Comando | Descrição | Permissão |
| --- | --- | --- |
| `/vip` | Abre o menu principal de VIPs | — |
| `/vips` | Vê seus VIPs ativos | — |
| `/tempovip` | Vê o tempo restante do seu VIP | — |
| `/usarkey <código>` | Ativa uma key de VIP | — |
| `/trocarvip` | Troca entre VIPs ativos | — |
| `/transferirvip <jogador>` | Transfere seu VIP ativo pra outro jogador | — |
| `/vendervip <código> <moeda> <preço>` | Coloca uma key à venda no marketplace | — |
| `/comprarvip <código>` | Compra uma key do marketplace | — |
| `/vendasvip` | Abre a loja de chaves (marketplace) | — |
| `/mercadovip` | Abre o mercado P2P de VIPs ativos | — |
| `/carteiravip` | Abre a carteira VIP (histórico e conquistas) | — |
| `/indicarvip <jogador>` | Indica um jogador pro sistema de afiliados | — |
| `/perksvip` | Abre a árvore de benefícios do VIP | — |
| `/creditovip` | Vê seu saldo de Prisma | — |
| `/congelarvip` | Congela/descongela o tempo do seu VIP | `alkavips.freeze` |
| `/darvip \| removervip \| setvip` | Gerencia o VIP de um jogador | `alkavips.admin.*` |
| `/gerarkey \| criarkey \| darkey` | Gera e entrega keys | `alkavips.admin.*` |
| `/alkavips reload` | Recarrega as configurações | `alkavips.admin.reload` |

## 🔗 Integrações

Construído sobre o **AlkaCore** e a **AlkaEconomy**. Suporte opcional a
**LuckPerms**, **PlaceholderAPI**, **AdvancedEnchantments**, **BattlePass**,
**Citizens**, **ItemsAdder**, **mcMMO**, **MCPets**, **MythicMobs**, **TAB**
e **AlkaItems** (recompensas de item por ativação).

## 🔧 Tecnologias Utilizadas

- **Java 21** · **Paper API 1.21.8**
- **AlkaCore** (banco de dados e GUI compartilhados)
- **MiniMessage** para todas as mensagens

## ⚙️ Instalação

1. Baixe a versão mais recente do plugin.
2. Coloque o `.jar` na pasta `plugins/` do servidor.
3. Certifique-se de ter o **AlkaCore** e a **AlkaEconomy** instalados
   (dependências obrigatórias).
4. Reinicie o servidor e configure `plugins/AlkaVips/vips.yml` com os tiers
   de VIP da sua rede.

## 🔐 Permissões

- `alkavips.freeze` — congelar/descongelar o próprio VIP (padrão: `true`)
- `alkavips.admin.give` \| `remove` \| `set` \| `removetime` — gestão de VIP
  de jogadores (padrão: `op`)
- `alkavips.admin.genkey` \| `createkey` \| `delkey` \| `editkey` \|
  `seekeys` \| `givekey` \| `bonus` — gestão de keys (padrão: `op`)
- `alkavips.admin.credit` — gerenciar Prisma de outros jogadores (padrão: `op`)
- `alkavips.admin.info` \| `reload` — administração geral (padrão: `op`)

## 📝 Licença

> ⚠️ **Projeto proprietário da AlkaStudio.**
>
> Código fonte destinado exclusivamente ao uso interno da rede `Alka*`.
> Reprodução, distribuição ou uso não autorizado não são permitidos.

## 🎯 Créditos

- **Desenvolvido por**: MestreDEV — AlkaStudio
- **Parte do ecossistema**: `Alka*`

---

<div align="center">

**Desenvolvido com ❤️ pela AlkaStudio**

[![AlkaStudio](https://img.shields.io/badge/AlkaStudio-JLob0-blue)](https://github.com/JLob0)

</div>
