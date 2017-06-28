import java.util.ArrayList;

// A point is a 1/96 of a quarter note (PointsPerQuarter = PPQ)
public class PianoRoll implements ControlMessageTransmitter, ControlMessageReceiver {
	public static class Melody {
		int[] notes;
		double[] velocities;
		int[] pos;
		
		public Melody(double...  values) {
			if (values.length % 3 != 0) throw new IllegalArgumentException("Amount of values not divisible by 3");
			notes = new int[values.length / 3];
			velocities = new double[values.length / 3];
			pos = new int[values.length / 3];
			for (int i = 0; i * 3 < values.length; i++) {
				notes[i] = (int) values[i * 3 + 0];
				velocities[i] = values[i * 3 + 1];
				pos[i] = (int) values[i * 3 + 2];
				
				if ((i + 1) * 3 < values.length && 
						(!Main.noteInRange(notes[i]) // Invalid note
						|| velocities[i] < 0 || velocities[i] > 1 // Invalid velocity
						|| pos[i] < (i == 0 ? 0 : pos[i - 1]))) { // Pos is smaller than previous
					throw new IllegalArgumentException("Invalid arguments");
				}
			}
		}
	}
	
	private static class Note {
		// If velocity is 0, it's a 'note off' event
		public int note, pos;
		public double velocity;
		
		public Note(int note, double velocity, int pos) {
			this.note = note;
			this.velocity = velocity;
			this.pos = pos;
		}
	}
	
	public static int ppq = 96;
	public static double ticksPerPoint = Main.ticksPerBeat / ppq;
	
	private double tick = 0;
	private double point = 0; // Current point
	private boolean stopPlayback = false;
	private boolean loop = false;
	
	private ArrayList<Note> notes = new ArrayList<>();
	private int nextPos = 0; // Index in 'notes'
	
	// 'notes' MUST end with [<anything>, -1, <length of melody in points>]
	public PianoRoll(Melody melody) {
		for (int i = 0; i < melody.notes.length; i++) {
			notes.add(new Note(melody.notes[i], melody.velocities[i], melody.pos[i]));
		}
	}
	
	public void sendMidiMessages() {
		Note note;
		while ((note = notes.get(nextPos)).pos == point) {
			if (note.velocity == 0) { // Note off event
				Main.receiveControlMessage(this, new NoteEvent(note.note, 0), 0);
			} else if (note.velocity == -1) { // Loop end event
				if (!loop) {
					stopPlayback = true;
				}
				nextPos = 0;
				point = 0;
				break;
			} else { // Note on event
				Main.receiveControlMessage(this, new NoteEvent(note.note, note.velocity), 0);
			}
			nextPos++;
		}
	}
	
	public void receiveControlMessage(ControlMessage message, int channel) {
		if (channel > 0) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		
		if (message instanceof PlaybackControl) {
			int event = ((PlaybackControl) message).event;
			if (event == PlaybackControl.STOP) {
				nextPos = 0;
				point = 0;
				tick = 0;
				stopPlayback = true;
			} else if (event == PlaybackControl.PAUSE) {
				stopPlayback = true;
			} else if (event == PlaybackControl.PLAY) {
				stopPlayback = false;
				loop = false;
			} else if (event == PlaybackControl.LOOP) {
				stopPlayback = false;
				loop = true;
			}
		}
	}
	
	public void tick() {
		if (!stopPlayback) {
			if (nextPos == 0) sendMidiMessages();
			if (tick >= ticksPerPoint) {
				point++;
				tick -= ticksPerPoint;
				sendMidiMessages();
			}
			tick++;
			
		}
	}
}
