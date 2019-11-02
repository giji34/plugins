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

ビルド
===

- 以下のコマンドでビルド
  ```
  ./gradlew assemble
  ```
- assemble 結果は `build/libs/` に生成される
