import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;

// TODO: Add support for other audio outputs (e.g. headphones, maybe wav files)
public class ExternalAudioTransmitter implements AudioReceiver {
	private SourceDataLine sourceDataLine;
		
	private int bufferSize = 100;
	private int dataBytes = 2; // Audio bit width (in bytes)
	private int bufferState = 0;
	private byte[] buffer = new byte[bufferSize * dataBytes * 2]; // multiply by 2 because of stereo
	
	public ExternalAudioTransmitter() {
		try {
			initSourceDataLine();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	public void tick() {
		// Retrieve sample and output it
		double[] amp = Main.transmitAudio(this, 0);
		
		byte[] left = Main.convertToByte(amp[0], dataBytes);
		byte[] right = Main.convertToByte(amp[1], dataBytes);
		for (byte b : left) buffer[bufferState++] = b;
		for (byte b : right) buffer[bufferState++] = b;
		
		if (bufferState == buffer.length) {
			sourceDataLine.write(buffer, 0, buffer.length);
			bufferState = 0;
			buffer = new byte[buffer.length];
		}
	}
	
	public void initSourceDataLine() throws LineUnavailableException {
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, Main.sampleRate, 8 * dataBytes, 2, dataBytes * 2, Main.sampleRate, true);
		sourceDataLine = AudioSystem.getSourceDataLine(format);
		sourceDataLine.open(format, bufferSize);
		sourceDataLine.start();
	}
}
