import java.util.ArrayList;
import javax.sound.midi.ShortMessage;

// This just adds an arbitrary amount of input devices together and transmits the result.
public class AudioMerger implements AudioReceiver, AudioTransmitter {
	private ArrayList<AudioTransmitter> inputs = new ArrayList<>();
	private ArrayList<Integer> inputChannels = new ArrayList<>();
	
	public void setAudioInput(AudioTransmitter input, int channel, int inputChannel) {
		if (channel > inputs.size()) {
			throw new IllegalArgumentException("The " + channel + ". channel which you tried to access is not yet accessable. Try the " + inputs.size() + ". channel instead.");
		}
		if (channel == inputs.size()) {
			inputs.add(input);
			inputChannels.add(inputChannel);
		} else {
			inputs.set(channel, input);
			inputChannels.set(channel, inputChannel);
		}
	}
	
	public double[] calculateSample(int channel) {
		if (channel > 0) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		double[] res = new double[]{0, 0};
		for (int i = 0; i < inputs.size(); i++) {
			double[] sample = inputs.get(i).calculateSample(inputChannels.get(i));
			res[0] += sample[0];
			res[1] += sample[1];
		}
		
		return res;
	}
}
