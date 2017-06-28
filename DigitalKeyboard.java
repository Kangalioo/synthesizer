import javax.sound.midi.ShortMessage;
import javax.sound.midi.MidiMessage;

public class DigitalKeyboard implements MidiReceiver, ControlMessageTransmitter {
	public void receiveMidi(MidiMessage message, int channel) {
		if (channel > 0) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		ShortMessage msg;
		if (message instanceof ShortMessage) {
			msg = (ShortMessage) message;
		} else {
			return;
		}
		
		if (msg.getStatus() == ShortMessage.NOTE_OFF || msg.getStatus() == ShortMessage.NOTE_ON && msg.getData2() == 0) {
			Main.receiveControlMessage(this, new NoteEvent(msg.getData1(), 0), 0);
		} else if (msg.getStatus() == ShortMessage.NOTE_ON) {
			Main.receiveControlMessage(this, new NoteEvent(msg.getData1(), Main.convertVelocity(msg.getData2())), 0);
		}
	}
}
