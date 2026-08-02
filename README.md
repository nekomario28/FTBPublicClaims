# FTB Public Claims

Minecraft 1.21.1／NeoForge向けのFTB Chunksアドオンです。サーバー全体で1つの公共claim領域を提供し、通常の個人／パーティーclaimとは別の所有先と容量で管理します。

## 仕組み

`Global Public Claims`というFTB TeamsのServer Teamを、公共claimの保存先・所有者IDとして使用します。このチームへの加入状況はアクセス境界ではありません。

**ワールド参加者全員が、公共領域の対等な利用者です。**

- 誰でも公共チャンクをclaim／unclaim可能
- 誰でも公共領域内のブロック編集、コンテナ・Entity操作が可能
- FTB Chunksマップ上部の`対象: 個人`／`対象: 公共`ボタンでclaim先を切替
- 公共モードでは左ドラッグでclaim、右ドラッグでunclaim
- 公共claimの強制ロードは不可
- 既存の個人／パーティーclaimを上書きしない
- 公共容量は個人／パーティー容量から独立

初期設定では、公共領域内のPvPは有効、爆発による地形破壊とmob griefingは無効です。fake playerによる編集も初期状態では許可しません。

クライアントUIと独自Payloadを含むため、MODをサーバーとクライアントの両方へ導入してください。

## コマンド

次のコマンドは一般プレイヤーも実行できます。

```text
/publicclaim select personal
/publicclaim select public
/publicclaim status
/publicclaim settings pvp <true|false>
/publicclaim settings explosions <true|false>
/publicclaim settings mob_griefing <true|false>
```

`settings`は公共領域全体の設定を変更します。公共領域は全参加者の共有物であるため、特定の所有者・管理者は設けていません。

## 設定

`config/ftbpublicclaims-common.toml`の`publicClaims`で調整できます。

```toml
[publicClaims]
enabled = true
maxChunksPerProject = 64
maxClaimDistance = 16
requireAdjacency = true
```

- `enabled`: サーバー共通の公共領域を有効化
- `maxChunksPerProject`: 公共領域の基本チャンク上限。旧開発設定との互換性のためキー名を維持
- `maxClaimDistance`: マップ操作を許可するプレイヤーからの最大チャンク距離
- `requireAdjacency`: 各ディメンションで、2チャンク目以降をそのディメンション内の既存公共claimへ上下左右で隣接させる

Overworld・Nether・Endはそれぞれ最初の公共claimを独立して作成できます。追加の公共容量は全ディメンション共通の基本上限へ加算されます。1つのPayloadで処理する変更数は最大64チャンクです。

## BuyClaimChunks Continuedとの併用

BuyClaimChunks Continuedの`/buyclaim`は、プレイヤー個人の追加claim容量だけを増やします。

FTB Public Claimsは別のServer Teamへ公共容量と公共claimを保存するため、次は相互に影響しません。

- `/buyclaim`で購入した個人容量
- 公共領域の追加容量
- 個人／パーティーのclaim数
- 公共領域のclaim数

CIではBuyClaimChunks Continued 1.1.1を同時ロードし、実際のダイヤ支払いと容量分離を検証しています。

## 対応バージョン

- Minecraft 1.21.1
- NeoForge 21.1.242（21.1系）
- Java 21
- FTB Chunks 2101.1.20
- FTB Teams 2101.1.9
- FTB Library 2101.1.30
- Architectury API 13.0.8

FTB Teams／FTB Chunksのバージョン依存処理は`FTBServerTeamBridge`とクライアント統合部へ隔離しています。CIでは使用するFTB Chunks JARのクライアントABIも検査します。

## 検証

GitHub Actionsでは次を自動検証します。

- Java 21でのclean buildと配布JAR監査
- BuyClaimChunks Continuedとの同時ロードと実購入
- 異なる参加者による公共claim／unclaim
- 全参加者のブロック編集・コンテナ操作権限
- Xvfb上の実NeoForgeクライアントと実マウス入力
- Personal／Public切替、左ドラッグclaim、右ドラッグunclaim
- RCONによるサーバー側claim数の`0 → 1 → 0`確認
- 通常停止後の別JVM再起動によるチームUUID、設定、容量、claimの復元

## 開発セーブの移行境界

以前の未公開開発版には、複数の公共プロジェクトを作成するモデルがありました。現在の設計はサーバー全体で1つの公共領域です。

旧開発セーブを開いた場合は、保存順で最初のプロジェクト／Server Teamを公共領域として維持します。複数の旧チームとclaimを自動統合すると所有競合や意図しない上書きを起こし得るため、自動マージは行いません。新規ワールドと単一公共領域のセーブ形式は自動試験の対象です。
