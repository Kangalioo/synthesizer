import java.util.ArrayList;
import javax.sound.midi.ShortMessage;

// TODO: Add mixing functionality

public class Mixer implements AudioReceiver, AudioTransmitter {
	public double[] calculateSample(int channel) {
		if (channel > 0) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		double[] res = new double[]{0, 0};
		for (int n : Main.getConnectedChannels(this)) {
			double[] sample = Main.transmitAudio(this, n);
			res[0] += sample[0];
			res[1] += sample[1];
		}
		
		return res;
	}
}
