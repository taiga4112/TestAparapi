import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

/**
 * aparapiを用いた単純な加算プログラム (動作確認用)
 * @author Taiga Mitsuyuki
 *
 */
public class SimpleAddTestAparapi {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// GPUと授受する変数を定義(final変数のみ授受可能)
		final float a[] = new float[] {1.0f, 2.0f, 3.0f, 4.0f};
		final float b[] = new float[] {1.0f, 2.0f, 3.0f, 4.0f};
		final float result[] = new float[a.length];
		
		//GPGPU用の無名クラスを生成
		Kernel kernel = new Kernel() {
			@Override
			public void run() {
				int i = this.getGlobalId();
				result[i] = a[i] + b[i];
			}
		};
		
		//配列の要素数を指定してGPGPU実行
		Range range = Range.create(result.length);
		kernel.execute(range);
		
		//実行結果の出力
		for( float el : result ){ System.out.println( el ); }
		System.out.println( kernel.getExecutionMode().name() );
		
		//GPGPU用の無名クラスの開放
		kernel.dispose();
	}

}
