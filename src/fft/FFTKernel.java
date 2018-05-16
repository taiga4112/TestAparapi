package fft;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

import com.amd.aparapi.Kernel;

/**
 * aparapiを用いたFFTのKernel
 * @author Taiga Mitsuyuki
 *
 */
public class FFTKernel extends Kernel{
	// データサイズ
	private int n = 0;
	// 入力データの実数部
	private float[] real;
	// 入力データの虚数部
	private float[] imag;
	// ビット反転表
	private int[] bitrev;
	// バタフライ演算の指示書(i)
	private int[] buttI;
	// バタフライ演算の指示書(ik)
	private int[] buttIK;
	// バタフライ演算の指示書(sin)
	private float[] buttSin;
	// バタフライ演算の指示書(cos)
	private float[] buttCos;
	// バタフライ演算の段数
	private int buttStep;
	// バタフライ演算の一段あたりの計算の組数
	private int buttCal;
	// FFT(1) / 逆FFT(-1) フラグ
	private float sign = 1.0f;
	
	public FFTKernel(int size) {
		this.n = size;
		this.real = new float[n];
		this.imag = new float[n];
		createzBitrev();//ビット反転表
		createButterfly();
	}
	

	private void createzBitrev() {
		this.bitrev = new int[n];
		int i = 0;
		int j = 0;
		this.bitrev[i] = 0;
		while(++i<n) {
			int k = n/2;
			while(k<=j) {
				j -= k;
				k /= 2;
			}
			j += k;
			this.bitrev[i] = j;
		}
	}
	
	private void createButterfly() {
		double step = 2.0 * Math.PI / n;
		List<Integer> buttIArray = new LinkedList<>();
		List<Integer> buttIKArray = new LinkedList<>();
		List<Float> buttSinArray = new LinkedList<>();
		List<Float> buttCosArray = new LinkedList<>();
		this.buttStep = 0;
		for (int k = 1; k < n; k *= 2) {
			int h = 0;
			int d = n / (k * 2);
			buttCal = 0; // バタフライ計算の段数を数える
			for (int j = 0; j < k; j++) {
				float s = (float) Math.sin(step * h);
				float c = (float) Math.cos(step * h);
				for (int i = j; i < n; i+= k * 2) {
					int ik = i + k;
					buttIArray.add(i);
					buttIKArray.add(ik);
					buttSinArray.add(s);
					buttCosArray.add(c);
				}
				h += d;
				buttCal += 1; // 一段につき何組の計算があるかを数える
			}
			buttStep += 1;
		}
		buttI = ArrayUtils.toPrimitive(buttIArray.toArray(new Integer[0]));
		buttIK = ArrayUtils.toPrimitive(buttIKArray.toArray(new Integer[0]));
		buttSin = ArrayUtils.toPrimitive(buttSinArray.toArray(new Float[0]));
		buttCos = ArrayUtils.toPrimitive(buttCosArray.toArray(new Float[0]));
	}

	@Override
	public void run() {
		int p = getPassId() * buttCal + getGlobalId();
		
		int i = buttI[p];
		int ik = buttIK[p];
		float s = sign * buttSin[p];
		float c = buttCos[p];
		
		float dx = s * imag[ik] + c * real[ik];
		float dy = c * imag[ik] - s * real[ik];
		
		real[ik] = real[i] - dx;
		real[i] += dx;
		
		imag[ik] = imag[i] -dy;
		imag[i] += dy;
	}
	
	public void fft(float[] x, float[] y) {
		fftsub(x,y,1.0f);
	}
	
	public void ifft(float[] x, float[] y) {
		fftsub(x,y,-1.0f);
		
		for (int cnt = 0; cnt < n; cnt++) {
			real[cnt] /= n;
			imag[cnt] /= n;
		}
	}
	
	private void fftsub(float[] x, float[] y, float sign) {
		if (n != x.length || n != y.length) {
			throw new IllegalArgumentException("Illegal data size");
		}
		for (int cnt = 0; cnt < n; cnt++) {
			this.real[cnt] = x[cnt];
			this.imag[cnt] = y[cnt];
		}
		this.sign = sign;
		
		// ビット反転
		for (int i = 0; i < n; i++) {
			int j = bitrev[i];
			if (i < j) {
				float t;
				t = real[i];
				real[i] = real[j];
				real[j] = t;
				
				t = imag[i];
				imag[i] = imag[j];
				imag[j] = t;
			}
		}
		
		// バタフライ演算
		// buttCal 組の演算を buttStep 段繰り返す
		// このようにループにしないと、GPGPU はステップごとに順に計算せずに
		// 一気に全段計算してしまい、まともな結果が得られない
		this.execute(buttCal, buttStep);
	}
	
	public float[] getReal() {
		return real;
	}
	
	public float[] getImag() {
		return imag;
	}
	
	public float[] getSin() {
		float[] sin = new float[n/2];
		
		sin[0] = imag[0] / n; // this must be zero.
		for (int cnt = 1; cnt < sin.length; cnt++) {
			sin[cnt] = -2.0f * imag[cnt] / n;
		}
		
		return sin;
	}
	
	public float[] getCos() {
		float[] cos = new float[n/2];
		
		cos[0] = real[0] / n;
		for (int cnt = 1; cnt < cos.length; cnt++) {
			cos[cnt] = 2.0f * real[cnt] / n;
		}
		
		return cos;
	}
}
