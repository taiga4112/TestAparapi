# TestAparapi
JavaからGPGPU(OpenCL)を呼び出すライブラリのテスト
- float[] や double[] を操作する Java バイトコードが、実行時に OpenCL にコンパイルされて実行される

# 手順
1. Aparapiのダウンロード
- 最新版は[GitHub](https://github.com/aparapi)にて公開されているが、コンパイルが面倒なので[Google Code](https://code.google.com/archive/p/aparapi/downloads)からコンパイル済みライブラリをダウンロードして利用する

2. Eclipseにて新規プロジェクトを作成

3. プロジェクトにjarファイルを追加
- プロジェクトフォルダの直下にlibフォルダを作成し、その中にaparapi.jarファイルを入れる
- jarファイルを右クリックして、[Build Path] -> [Add to Build Path]

4. プロジェクトへネイティブライブラリ(Windows:dll, MacOX:dylib, Linux:so)を組み込む
- libフォルダにネイティブライブラリファイルを入れる
- その後、[このページ](http://sgrit.hatenablog.com/entry/2014/04/27/043015)を参考に追加

# テスト的に作成したコード
- 加算プログラム src/SimpleAddTestAparapi.java
- テンプレートマッチング with JavaFX src/SimpleTempleteMatchingTestAparapi.java
- 高速フーリエ変換(FFT) src/fft
