## はじめに
本リポジトリはJava学習中の「まか」が作成したMinecraftのプラグイン『TreasureHunt（宝探しミニゲーム）』に関するものです。


## コンセプト
- スクール課題のお題で「宝探し」と言われたときに頭に思い浮かんだ、某勇者が民家で壺を割ってアイテムを入手するゲーム、ド◯クエをモチーフにしております。
- これまで学習してきたことを活かし、ミニゲーム開発という成果物を作ることで、アウトプットや学習のモチベーションアップに繋げたいと思い作成しました。

## 使用技術一覧
<img alt="Static Badge" src="https://img.shields.io/badge/Java-v17-brightgreen?style=for-the-badge&labelColor=007396&color=4F4F4F"> <img alt="Static Badge" src="https://img.shields.io/badge/mysql-v8.0.37-brightgreen?style=for-the-badge&logo=mysql&logoColor=white&logoSize=auto&labelColor=%234479A1&color=4F4F4F"> <img alt="Static Badge" src="https://img.shields.io/badge/MyBatis-v3.5.16-brightgreen?style=for-the-badge&logoColor=white&logoSize=auto&labelColor=D74C4C&color=4F4F4F"> <img alt="Static Badge" src="https://img.shields.io/badge/spigotmc-v1.20.4-brightgreen?style=for-the-badge&logo=spigotmc&logoColor=white&logoSize=auto&labelColor=ED8106&color=333333"> <img alt="Static Badge" src="https://img.shields.io/badge/Minecraft-v1.20.4-brightgreen?style=for-the-badge&labelColor=55A630&color=593E1A"> <img alt="Static Badge" src="https://img.shields.io/badge/Docker-latest-brightgreen?style=for-the-badge&logo=docker&logoColor=white&labelColor=2496ED&color=4F4F4F">


## 概要

**TreasureHunt**は、制限時間内にフィールドにランダムに配置された「飾り壺」を壊し、りんごや金のりんごを見つけてスコアを獲得するミニゲームです。りんごを早く見つけるほど高得点となり、特定の条件下でボーナススコアが加算されます。

## プレイ動画

[![プレイ動画](https://img.youtube.com/vi/epIGIR9BIM8/0.jpg)](https://youtu.be/epIGIR9BIM8)

> [!NOTE]
> ### 特徴
> - 飾り壺にはランダムにアイテム（りんご・金のりんご）が入っており、見つけたときの残り時間でスコアが変動します。
> - プレイヤーは現在のスコアランキングをコマンドで確認できます。
> - 残り時間がボスバーで表示され、プレイヤーが視覚的に時間を確認できます。
> - プレイヤーの合計スコアがサイドバーに表示され、ゲームの進行状況を確認できます。

## ゲームのルール

**1. ゲーム開始**

ゲームはコマンドを使用して開始します。

**2. 飾り壺を見つけて壊す**

ランダムな場所に出現した飾り壺を壊してアイテム（りんご or 金のりんご）を発見すると、以下のスコアが加算されます。

   - ゲームの残り時間によって加算スコアが変動します。
     
     - 残り時間が40秒以上：+100 点
     - 残り時間が20秒以上：+50 点
     - 残り時間が19秒以下：+10 点
  - **金のりんごの場合**　上記のスコアに**ボーナススコア +50 点**が加算されます。

**3. スコアランキング**

ゲーム終了後、コマンドを入力することでTop5までのランキングが確認できます。


## コマンド

| コマンド | 説明 |
|-|-|
| `/findGoldenApple`       | ゲームを開始する |
| `/findGoldenApple list`  | 現在のスコアランキングを表示 |


## 導入方法

**1. 環境の準備**
   - Docker Desktopをインストール
   - プロジェクトをクローン

**2. データベースの準備**
   ```bash
   # プロジェクトのルートディレクトリで実行
   docker-compose up -d
   ```
**3. プラグインのインストール**

`gradle shadowJar` ファイルを作成し、Minecraftサーバーの `plugins` フォルダーに配置します。

**4. サーバーの起動**

サーバーを起動してプラグインが正常に読み込まれるか確認します。

> [!IMPORTANT]
> **Docker環境でのMySQL設定**
> 
> Docker Composeを使用することで、以下の設定が自動的に行われます。
> - データベース `treasure_hunt_db` の作成
> - `player_score` テーブルの作成
> - 必要な権限の設定
>
> :bulb: デフォルトの接続情報：
> ```
> ホスト: localhost
> ポート: 3307
> データベース: treasure_hunt_db
> ユーザー: root
> パスワード: rootroot

> [!NOTE]
> これらの接続情報は自動的に設定されるため、ローカルのMySQLの設定に関係なく、この設定で動作します。


## データベース構成

テーブル: `player_score`

| カラム名 | 型 | 説明 |
|-|-|-|
| id | INT | 主キー、自動採番 |
| player_name | VARCHAR(100) | プレイヤー名 |
| score | INT | スコア |
| registered_at | DATETIME | 登録日時 |


## ゲーム設定

| 定数 | 説明 | デフォルト値 |
|-|-|-|
| `POT_AMOUNT`   | 出現する飾り壺の数 | 15個 |  
| `APPLE_AMOUNT` | りんごの数 | 2個 |   
| `GAME_TIME`    | ゲームの制限時間 | 60秒 |
| `BONUS_SCORE`  | 金のりんごのボーナススコア | 50点 | 

:bulb:  設定は`FindGoldenAppleCommand`クラス内で調整可能です。


## 注意事項
- サーバーバージョン
   - 本プラグインはBukkitまたはSpigot APIの特定バージョン向けに開発されています。互換性のあるサーバーバージョンを使用してください。
- プラグインの依存関係
   - 一部のBukkit APIの機能が必要です。プラグインが正しく動作するため、必要なAPIがサポートされていることを確認してください。

## 今後 実装予定の機能
- 環境変数ファイルを作成する
- コマンド実行時に設定値を変更できるようにする

## おわりに
- 感想・コメント等ございましたら、Xアカウントまでご連絡いただけますと幸いです。

<img src="https://github.com/user-attachments/assets/5df37342-c70e-4e0d-b9cd-21e390c9069c" style="width: 15px; height: auto; margin-top: 30px;" alt="Example SVG"> <sup>：https://x.com/makaJava368748</sup>
