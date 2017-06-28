public class MyEffector implements AudioReceiver, AudioTransmitter {
	int tick = 0;
	
	double distortionPower = 0; // 0 = silence, 1 = no change, +INF = max power
	double sidechainPower = 1.3; // 0 = min power, 1 = limited max power, +INF = extended max power
	// TODO: double recoveryTime = 1; // In beats
	
	private double sidechain(double sample) {
		return sample * Math.max(0, ((tick / ((double) Main.sampleRate * 60 / Main.bpm)) % 1 - 1) * sidechainPower + 1);
	}
	
	private double distort(double sample) {
		if (distortionPower == 0) return sample;
		else if (distortionPower < 1) return Math.max(-1 / distortionPower, Math.min(1 / distortionPower, sample));
		else return Math.max(-1, Math.min(1, sample * distortionPower));
	}
	
	public double[] calculateSample(int channel) {
		if (channel > 0) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		double[] sample = Main.transmitAudio(this, 0);
		sample[0] = sidechain(distort(sample[0]));
		sample[1] = sidechain(distort(sample[1]));
		tick++;
		return sample;
	}
}
