public class Limiter implements AudioReceiver, AudioTransmitter {
	public static int releaseTime = Main.sampleRate; // Time in samples for full recovery (currentVol: 0 -> 1)
	
	private double currentVol = 1;
	
	public double[] calculateSample(int channel) {
		if (channel > 0) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		
		double[] sample = Main.transmitAudio(this, 0);
		sample[0] *= currentVol;
		sample[1] *= currentVol;
		double max = Math.max(Math.abs(sample[0]), Math.abs(sample[1]));
		if (max > 1) {
			double multiplier = 1 / max;
			currentVol *= multiplier;
			sample[0] *= multiplier;
			sample[1] *= multiplier;
			//if (currentVol <= 0.3) System.err.println("Limiter is dominating! (" + currentVol + ")");
		}
		
		
		currentVol += 1.0 / releaseTime;
		if (currentVol > 1) currentVol = 1;
		
		return sample;
	}
}
