public class NoteEvent implements ControlMessage {
	public int note;
	public double velocity;
	
	public NoteEvent(int note, double velocity) {
		this.note = note;
		this.velocity = velocity;
	}
}
