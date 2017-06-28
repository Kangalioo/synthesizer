import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;
import javax.sound.midi.Receiver;

public class ExternalMidiTransmitter implements MidiReceiver {
	private MidiDevice midiDevice;
	private Receiver midiReceiver = null;
	
	public ExternalMidiTransmitter(MidiDevice midiDevice) {
		this.midiDevice = midiDevice;
	}
	
	public void open() throws MidiUnavailableException {
		if (midiDevice == null) {
			System.err.println("Warning: Midi device is null");
			return;
		}
		if (!midiDevice.isOpen()) midiDevice.open();
		midiReceiver = midiDevice.getReceiver();
	}
	
	public void close() {
		midiDevice.close();
	}
	
	public void receiveMidi(MidiMessage msg, int channel) {
		if (midiReceiver != null) midiReceiver.send(msg, -1);
	}
}
