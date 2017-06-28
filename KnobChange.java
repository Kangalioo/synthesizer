public class KnobChange implements ControlMessage {
	public boolean relativeValue = false;
	public int knob;
	public double value;
	
	public KnobChange(int knob, double value) {
		this.knob = knob;
		this.value = value;
	}
	
	public KnobChange(int knob, double value, boolean relativeValue) {
		this(knob, value);
		this.relativeValue = relativeValue;
	}
}
