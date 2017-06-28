public interface ControlMessageReceiver extends Module {
	public void receiveControlMessage(ControlMessage msg, int channel);
}
