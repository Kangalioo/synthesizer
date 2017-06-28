public class Harmonizer implements ControlMessageReceiver, ControlMessageTransmitter {
	private int undertones = 0;
	private Chord chord = null;
	
	public void receiveControlMessage(ControlMessage message, int channel) {
		if (channel > 1) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		
		if (channel == 0 && message instanceof NoteEvent) {
			NoteEvent msg = (NoteEvent) message;
			
			int note = msg.note;
			Main.receiveControlMessage(this, msg, 0);
			if (note != -1) {
				for (int i = 0; i < undertones; i++) {
					note = getHarmonicNote(note);
					Main.receiveControlMessage(this, new NoteEvent(note, msg.velocity), 0);
				}
			}
		} else if (channel == 1 && message instanceof Chord) {
			Chord msg = (Chord) message;
			
			chord = msg;
		}
	}
	
	private int getHarmonicNote(int note) {
		if (chord == null) return -1;
		int pitchChange = note / 12 * 12;
		int[] harmonics = chord.getScale().clone();
		for (int i = 0; i < harmonics.length; i++) harmonics[i] += chord.key + pitchChange;
		
		while (true) {
			for (int i = harmonics.length - 1; i >= 0; i--) {
				if (note - harmonics[i] >= 3) return harmonics[i];
				harmonics[i] -= 12;
			}
		}
	}
}
