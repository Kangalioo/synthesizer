public class Chord implements ControlMessage {
	// Chords
	public static final int
		NO_CHORD = 0,
		MAJOR = 1,
		POWER = 2;
	
	// Chord scales
	private static final int[][] chordScales = {
		{0},
		{0, 4, 7},
		{0, 3, 7},
		{0, 7}
	};
	
	int key;
	int chord;
	
	public Chord(int key, int chord) {
		this.key = key;
		this.chord = chord;
	}
	
	public int[] getScale() {
		return chordScales[chord];
	}
}
