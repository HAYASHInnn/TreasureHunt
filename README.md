# Treasure Hunt - Minecraft Plugin

## 使用技術一覧
<img src="https://img.shields.io/badge/-Java-007396.svg?logo=java&style=for-the-badge"> <img alt="Static Badge" src="https://img.shields.io/badge/mysql-brightgreen?style=for-the-badge&logo=mysql&logoColor=white&logoSize=auto&color=4479A1"> <img alt="Static Badge" src="https://img.shields.io/badge/MyBatis-brightgreen?style=for-the-badge&color=D74C4C"> <img alt="Static Badge" src="https://img.shields.io/badge/spigotmc-v1.20.4-brightgreen?style=for-the-badge&logo=spigotmc&logoColor=white&logoSize=auto&labelColor=ED8106&color=333333">




## 概要

**Treasure Hunt**は、制限時間内にフィールドにランダムに配置された「飾り壺」を壊し、りんごや金のりんごを見つけてスコアを獲得するミニゲームです。りんごを早く見つけるほど高得点となり、特定の条件下でボーナススコアが加算されます。

## 特徴

- **飾り壺を壊してスコアを獲得**
  
   飾り壺にはランダムなアイテム（りんご（2個）・金のりんご（1個））が入っており、見つけたタイミングでスコアが変動します。

- **スコアのランキング表示**

   プレイヤーは現在のスコアランキングをコマンドで確認できます。

- **ボスバーでのタイマー表示**

   残り時間がボスバーで表示され、プレイヤーが視覚的に時間を確認できます。

- **スコアボードでのトータルスコア表示**

   プレイヤーの合計スコアがサイドバーに表示され、ゲームの進行状況を確認できます。


## ゲームのルール

**◾️ ゲーム開始**
ゲームはコマンドを使用して開始します。

**◾️ 飾り壺を見つけて壊す**
ランダムに出現する飾り壺を壊してアイテムを発見すると、以下のスコアが加算されます。

   - 発見タイミングによって加算スコアが変わります。
     
     - 残り時間が40秒以上：+100 点
     - 残り時間が20秒以上：+50 点
     - 残り時間が19秒以下：+10 点
  - **金のりんごの場合**　上記のスコアに**ボーナススコア +50 点**が加算されます。

**◾️ スコアランキング**
ゲーム終了後、上位5位までのランキングが確認できます。


## コマンド

| コマンド                 | 説明                                                |
|--------------------------|-----------------------------------------------------|
| `/findGoldenApple`       | ゲームを開始する                                    |
| `/findGoldenApple list`  | 現在のスコアランキングを表示                        |


## 導入方法

1. **プラグインのインストール**
   - `FindGoldenAppleCommand.java` を含む `.jar` ファイルを作成し、Minecraftサーバーの `plugins` フォルダーに配置します。
   
2. **サーバーの起動**
   - サーバーを再起動してプラグインが正常に読み込まれるか確認します。



## 設定

**◾️ POT_AMOUNT**

出現する飾り壺の数（デフォルト：15個）

**◾️ GAME_TIME**

ゲームの制限時間（デフォルト：60秒）

**◾️ BONUS_SCORE**

金のりんごを見つけたときのボーナススコア（デフォルト：50点）

※設定は`FindGoldenAppleCommand`クラス内で調整可能です



## 注意事項

- **サーバーバージョン**: 本プラグインはBukkitまたはSpigot APIの特定バージョン向けに開発されています。互換性のあるサーバーバージョンを使用してください。
- **プラグインの依存関係**: 一部のBukkit APIの機能が必要です。プラグインが正しく動作するため、必要なAPIがサポートされていることを確認してください。

## 対応バージョン

Minecraft：1.20.4

Spigot：1.20.4

