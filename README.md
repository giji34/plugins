giji34-custom-server-plugin
===

- A custom server mod for giji34

コマンド
===

- `tpl`
  - op じゃないプレイヤーも使える `tp` コマンド
  - 書式
    ```
    /tpl <X> <Y> <Z>
    ```

- `tpb`
  - サーバー側で設定済みの地点に、名前を指定して移動する
  - サーバー側には `plugins/giji34/buildings.tsv` に地点名と座標を設定する
  - 書式
    ```
    /tpb <地点名>
    ```

- `gm`
  - ゲームモードをクリエイティブ/スペクテイターに切り替える
  - 実行するたびに交互に切り替わる
  - 書式
    ```
    /gm
    ```

- `gfill`
  - 木の斧で選択した範囲を、指定したブロックで埋める
  - 木の斧の使い方は worldedit と同じ
  - 書式
    ```
    /gfill <ブロック名>
    ```
    ブロック名には `dirt` など Namespased ID の `minecraft:` を除いたものを設定する

- `greplace`
  - 木の斧で選択した範囲にあるブロックのうち、引数 1 で設定したブロックを引数 2 で設定したブロックに置き換える
  - 木の斧の使い方は worldedit と同じ
  - 書式
    ```
    /greplace <探すブロック名> <置くブロック名>
    ```
    ブロック名には `dirt` など Namespased ID の `minecraft:` を除いたものを設定する

- `gundo`
  - `gfill`, `greplace` コマンドの操作を一回に限りもとに戻します

ビルド
===

- 以下のコマンドでビルド
  ```
  ./gradlew assemble
  ```
- assemble 結果は `build/libs/` に生成される
