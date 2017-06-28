public class Delay implements AudioReceiver, AudioTransmitter {
	int cyclesPerBar = 6;
	float[][] delayedSound = new float[(int) (Main.ticksPerBeat * 4 / cyclesPerBar)][2];
	double initialPan = -0.5; // -1 = left, 0 = center, 1 = right
	double feedback = 0.5;
	
	public static boolean pingPong = true;
	
	int pos = 0;
	
	public double[] calculateSample(int channel) {
		if (channel > 0) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		double[] sample = Main.transmitAudio(this, 0);
		
		if (initialPan > 0) {
			sample[1] += sample[0] * initialPan;
			sample[0] *= 1 - initialPan;
		} else if (initialPan < 0) {
			sample[0] += sample[1] * -initialPan;
			sample[1] *= 1 + initialPan;
		}
		
		delayedSound[pos][0] *= feedback;
		delayedSound[pos][1] *= feedback;
		if (pingPong) { // This has to happen before adding current sample, otherwise current sample's pan would be inverted as well
			float temp = delayedSound[pos][0];
			delayedSound[pos][0] = delayedSound[pos][1];
			delayedSound[pos][1] = temp;
		}
		delayedSound[pos][0] += sample[0];
		delayedSound[pos][1] += sample[1];
		
		double[] res = new double[]{delayedSound[pos][0], delayedSound[pos][1]};
		pos++;
		if (pos == delayedSound.length) pos = 0;
		
		return res;
	}
}
