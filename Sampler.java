import java.io.File;
import javax.sound.midi.ShortMessage;
import java.io.IOException;
import java.util.Arrays;

public class Sampler implements ControlMessageReceiver, AudioTransmitter {
	private int[] keyMappings;
	private float[][][] sounds; // sounds[soundIndex][sampleIndex][stereoChannel]
	private int[] pos;
	private boolean restart = false;
	
	public Sampler(String[] paths, int[] keyMappings) {
		if (paths.length != keyMappings.length) {
			throw new IllegalArgumentException("Length of 'paths' and 'keyMappings' do not match! (" + paths.length + " and " + keyMappings.length + ")");
		}
		pos = new int[paths.length];
		for (int i = 0; i < pos.length; i++) pos[i] = -1;
		this.keyMappings = keyMappings;
		readFiles(paths);
	}
	
	private void readFiles(String... paths) {
		sounds = new float[paths.length][][];
		for (int i = 0; i < paths.length; i++) {
			sounds[i] = Main.readWav(new File(paths[i]));
		}
	}
	
	public double[] calculateSample(int channel) {
		if (channel > 0) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		double[] res = new double[]{0, 0};
		
		for (int i = 0; i < pos.length; i++) {
			if (sounds[i] == null) continue;
			if (pos[i] == sounds[i].length) pos[i] = -1;
			if (pos[i] == -1) continue;
			res[0] += sounds[i][pos[i]][0];
			res[1] += sounds[i][pos[i]][1];
			pos[i]++;
		}
		return res;
	}
	
	public void receiveControlMessage(ControlMessage message, int channel) {
		if (channel > 0) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		
		if (message instanceof NoteEvent) {
			NoteEvent msg = (NoteEvent) message;
			if (msg.velocity > 0) {
				int noteIndex = -1;
				for (int i = 0; i < keyMappings.length; i++) {
					if (keyMappings[i] == msg.note) {
						noteIndex = i;
						break;
					}
				}
				
				if (noteIndex != -1) pos[noteIndex] = 0;
			}
		}
	}
}
