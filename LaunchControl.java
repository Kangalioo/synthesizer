import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.InvalidMidiDataException;

public class LaunchControl implements MidiReceiver, ControlMessageTransmitter, MidiTransmitter {
	private int template = 0; // user template index (I don't know what these templates are)
	
	private int currentSelection = -1;
	
	public void receiveMidi(MidiMessage message, int channel) {
		if (channel > 0) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		ShortMessage msg;
		if (message instanceof ShortMessage) {
			msg = (ShortMessage) message;
		} else {
			return;
		}
		
		int knob = msg.getData1();
		if (msg.getStatus() == 0xB0 && knob < 114) {
			if (knob >= 21 && knob <= 28) {
				knob = knob - 21;
			} else if (knob >= 41 && knob <= 48) {
				knob = knob - 33;
			}
			Main.receiveControlMessage(this, new KnobChange(knob, msg.getData2() / 128.0), 0);
		} else if (msg.getStatus() == 0xB0 && currentSelection != -1) {
			knob -= 114;
			
			if (msg.getData2() == 0) { // Release
				setPadColor(knob + 8, 0, 0);
				setPadColor(currentSelection, 0, 2);
			} else { // Press
				setPadColor(knob + 8, 3, 0);
				
				if (knob == 0 || knob == 3) {
					setPadColor(currentSelection, 0, 3);
					Main.receiveControlMessage(this, new KnobChange(currentSelection + 16, 1, true), 0);
				} else {
					setPadColor(currentSelection, 0, 1);
					Main.receiveControlMessage(this, new KnobChange(currentSelection + 16, -1, true), 0);
				}
			}
		} else if (msg.getStatus() == 0x90 || msg.getStatus() == 0x80) { // Button press or release
			if (msg.getData1() >= 9 && msg.getData1() <= 12) {
				knob -= 9;
			} else if (msg.getData1() >= 25 && msg.getData1() <= 28) {
				knob -= 21;
			}
			
			if (msg.getStatus() == 0x90) { // Press
				setPadColor(currentSelection, 0, 0);
				setPadColor(knob, 1, 2);
				currentSelection = knob;
			} else {
				if (knob == currentSelection) setPadColor(knob, 0, 2);
			}
		}
	}
	
	private void setPadColor(int pad, int red, int green) {
		int color = (16 * green + red) | 0b1100;
		try {
			byte[] bytes = new byte[]{(byte) 240, (byte) 0, (byte) 32, (byte) 41, (byte) 2, (byte) 10, (byte) 120, (byte) template, (byte) pad, (byte) color, (byte) 247};
			Main.receiveMidi(this, new SysexMessage(bytes, bytes.length), 0);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}
	
	private void reset() {
		try {
			Main.receiveMidi(this, new ShortMessage(176 + template, 0, 0), 0);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		reset();
	}
}
