import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.InvalidMidiDataException;
import java.util.ArrayList;

public class ExternalMidiReceiver implements MidiTransmitter {
	private MidiDevice midiDevice;
	
	public ExternalMidiReceiver(MidiDevice midiDevice) throws MidiUnavailableException {
		this.midiDevice = midiDevice;
	}
	
	public void open() throws MidiUnavailableException {
		if (midiDevice == null) {
			System.err.println("Warning: Midi device is null");
			return;
		}
		if (!midiDevice.isOpen()) midiDevice.open();
		Receiver receiver = new Receiver() {
			public void send(MidiMessage message, long timestamp) {
				sendMidi(message);
			}
			
			public void close() {}
		};
		midiDevice.getTransmitter().setReceiver(receiver);
	}
	
	private void sendMidi(MidiMessage msg) {
		Main.receiveMidi(this, msg, 0);
	}
}
