# FTBPublicClaims

Minecraft Forge 1.20.1 / FTB Chunks向けの独立アドオンです。一般プレイヤーがOP権限なしで、通常の個人／パーティーclaimとは別の公共保護領域を作成・管理できます。

## 公共claim

公共プロジェクトごとにFTB TeamsのServer Teamを作り、そのチームへFTB Chunksのclaimを所属させます。通常の個人／パーティーclaimとは所有先が分かれます。

- 所有者と管理者: ブロック編集・設備操作が可能
- 一般プレイヤー: チェストやドアなどの操作が可能、設置・破壊は不可
- 爆発、mob grief、fake playerによるブロック編集、PvP: 無効
- FTB Chunksのマップ上部にclaim先切替ボタンを追加
- 公共モードでは左クリックでclaim、右クリックでunclaim
- 公共claimの強制ロードは不可

この機能はクライアントUIと独自通信を含むため、MODをサーバーとクライアントの両方へ導入する必要があります。

## コマンド

すべて一般プレイヤーが実行できます。

```text
/publicclaim create <name>
/publicclaim list
/publicclaim select personal
/publicclaim select <project>
/publicclaim manager <project> add <online-player>
/publicclaim manager <project> remove <online-player>
/publicclaim delete <project> confirm
```

プロジェクト名は3～24文字の英数字、`_`、`-`を使用できます。作成するとそのプロジェクトがclaim先として自動選択されます。

## 荒らし対策の初期値

`config/ftbpublicclaims-common.toml`の`publicClaims`で調整できます。

- 1プレイヤーにつき1プロジェクト
- 1プロジェクト64チャンク
- プレイヤーから16チャンク以内の操作のみ
- 2チャンク目以降は既存領域への上下左右の隣接が必要
- 1パケットで最大64変更
- 既存claimの上書きや変換は不可（未claimチャンクのみ）

## 対応バージョンと参照ソース

- Minecraft 1.20.1 / Forge 47.4.13
- [FTB Chunks v2001.3.6](https://github.com/FTBTeam/FTB-Chunks/tree/v2001.3.6)
- [FTB Teams v2001.3.1](https://github.com/FTBTeam/FTB-Teams/tree/v2001.3.1)
- [FTB Library v2001.2.12](https://github.com/FTBTeam/FTB-Library/tree/v2001.2.12)
- [Architectury API 9.1.12](https://github.com/architectury/architectury-api)

FTB Teams 2001.3.1はServer Team作成を公開APIに含めていないため、そのバージョン固有処理は`FTBServerTeamBridge`へ隔離しています。内部実装とprivateフィールドを利用するため、`mods.toml`では検証済みバージョンに固定しています。
