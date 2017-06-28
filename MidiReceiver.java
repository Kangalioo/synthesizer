import javax.sound.midi.MidiMessage;

public interface MidiReceiver extends Module {
	public void receiveMidi(MidiMessage msg, int channel);
}
