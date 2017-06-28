public interface Module {
	// The tick is only coming after "calculateSample" if the ExternalMidiReceiver to the speakers is registered as the first module (which would be a very good thing to do)
	default public void tick() {};
	default public void close() {}; // Release resources
}
