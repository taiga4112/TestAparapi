import java.io.File;
 
import com.amd.aparapi.Kernel;
import com.amd.aparapi.Kernel.EXECUTION_MODE;
import com.amd.aparapi.Range;
 
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.stage.Stage;
 
/**
 * aparapiの動作確認プログラム2
 * 『単純なパターンマッチング(aparapi)』
 *  画像　： https://www.pakutaso.com/20120450102post-1371.html
 * 
 * @author karura
 *
 */
public class SimpleTempleteMatchingTestAparapi extends Application
{
    public static void main(String[] args) {
        launch(args);
    }
     
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        System.out.println( "start!" );
         
        // GPGPU用のクラスを生成して初期化
        PatternMatchKernel  pmKernel    = new PatternMatchKernel();
        String              file1       = new File("img/01.png").toURI().toString();
        String              file2       = new File("img/02.png").toURI().toString();
        pmKernel.init( file1 , file2 );
        pmKernel.setExecutionMode(EXECUTION_MODE.GPU);
 
        // GPGPU実行(同時に処理時間取得)
        long    startTime   = System.nanoTime();
        pmKernel.execute();
        long    endTime     = System.nanoTime();
        float   interval    = ( endTime - startTime )  / 1000000000.0f ;
        System.out.println( String.format( "time   :%f [sec]", interval ) );
         
        // 結果を出力
        float[] result  = pmKernel.getResult();
        for( int i=0 ; i<result.length - 1 ; i++ )
        {
            // 2乗誤差が0になるインデックスを特定
            if( result[i] == 0 )
            {
                // インデックスから画像のxy座標を計算し、出力
                int x   = pmKernel.getResultX( i );
                int y   = pmKernel.getResultY( i );
                System.out.println( String.format( "result :(%d,%d) gap = %f" , x , y , result[i] ) );
            } 
        }
         
        // 実行モードを出力
        System.out.println( "mode   :" + pmKernel.getExecutionMode().name() );
         
        // GPGPU用の無名クラスを解放
        pmKernel.dispose();
        System.out.println("release!!!");
    }
 
}
 
/**
 * GPGPU用のクラス
 * 
 * @author tomo
 *
 */
class PatternMatchKernel extends Kernel
{
    // 探索対象画像情報
    private float   target[];           // 探索画像のピクセル配列
    private int     targetHeight;       // 探索画像の高さ
    private int     targetWidth;        // 探索画像の幅
     
    // パターン画像情報
    private float   pattern[];          // パターン画像のピクセル配列
    private int     patternHeight;      // パターン画像の高さ
    private int     patternWidth;       // パターン画像の幅
     
    // 結果情報
    private float   result[];           // 探索画像とパターン画像の誤差配列
     
    /**
     * クラスの初期化（画像のピクセル情報等を取得）
     * 
     * @param targetFile 探索対象画像ファイル
     * @param patternFile パターン画像ファイル
     */
    public void init( String targetFile , String patternFile )
    {
        // 探索対象の画像ファイルを読込
        try
        {
            // 画像ファイルの取込
            Image       img     = new Image( targetFile );
             
            // 変数の初期化
            targetHeight    = (int) img.getHeight();
            targetWidth     = (int) img.getWidth();
            target          = new float[ targetHeight * targetWidth ];
             
            // ピクセル配列を取得
            PixelReader reader  = img.getPixelReader();
            for( int y = 0 ; y < targetHeight ; y++ )
                for( int x = 0 ; x < targetWidth ; x++  )
                {
                    int i       = ( y * targetWidth ) + x;
                    target[ i ] = reader.getArgb( x, y );
                }
             
        }catch( Exception e ){
            e.printStackTrace();
        }
         
         
        // パターン画像ファイルを読込
        try
        {
            // 画像ファイルの取込
            Image       img     = new Image( patternFile );
             
            // ピクセル格納用の配列を初期化
            pattern         = new float[ (int) (img.getHeight() * img.getWidth()) ];
            patternHeight   = (int) img.getHeight();
            patternWidth    = (int) img.getWidth();
             
            // ピクセル配列を取得
            PixelReader reader  = img.getPixelReader();
            for( int y = 0 ; y < patternHeight ; y++ )
                for( int x = 0 ; x < patternWidth ; x++  )
                {
                    int i           = y *  patternWidth + x ;
                    pattern[ i ]    = reader.getArgb( x, y );
                }
             
        }catch( Exception e ){
            e.printStackTrace();
        }
         
         
        // 結果配列を初期化
        result  = new float[ target.length ];
    }
     
    @Override
    public void run()
    {
        // 値を初期化
        float value = 0;
         
        // GlobalIDが示す
        // 探索範囲の左上ピクセル位置を取得
        int i       = getGlobalId();
        int x       = i % targetHeight;
        int y       = i / targetHeight;
         
        // 探索範囲とパターン画像の2乗誤差を計算
        for( int dy=0 ; dy<patternHeight ; dy++ )
        {
            for( int dx=0 ; dx<patternWidth ; dx++ )
            {
                // 範囲外の場合は適当な誤差を加算し、処理を飛ばす
                if( y + dy >= targetHeight ){ value += 100; continue; }
                if( x + dx >= targetWidth ){  value += 100; continue; }
                 
                // インデックス計算
                int targetIndex     = ( y + dy ) * targetWidth + ( x + dx ) ;
                int patternIndex    = dy * patternWidth + dx;
                 
                // 2乗誤差を計算
                // 実際はR・G・B・Aの各色成分ごとに誤差をとるほうが良いが、
                // 今回はパターン画像と完全一致する部分を得るため、計算を簡略化している
                float gap       = target[ targetIndex ] - pattern[ patternIndex ];
                gap *= gap;
                 
                // 誤差を加算
                value   += gap;
                 
            }
        }
         
        // 誤差の合計を結果配列に格納
        result[i]   = value;
    }
     
    /**
     * GPGPU実行
     */
    public void execute()
    {
        // 配列の要素数を指定して、GPGPU実行
        Range range = Range.create( result.length );
        execute( range );
    }
     
    /**
     * GPGPU計算結果を返す
     * @return
     */
    public float[] getResult()
    {
        return result;
    }
     
    /**
     * GPGPU計算結果のインデックスから
     * 探索画像上のx座標を計算する
     * @param index
     * @return
     */
    public int getResultX( int index )
    {
        return index % targetHeight;
    }
     
    /**
     * GPGPU計算結果のインデックスから
     * 探索画像上のy座標を計算する
     * @param index
     * @return
     */
    public int getResultY( int index )
    {
        return index / targetHeight;
    }
         
};