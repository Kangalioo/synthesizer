public class PlaybackControl implements ControlMessage {
	public static final int
		STOP = 0,
		PAUSE = 1,
		PLAY = 2,
		LOOP = 3;
	
	public int event;
	
	public PlaybackControl(int event) {
		this.event = event;
	}
}
