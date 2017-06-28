import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.InvalidMidiDataException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Future;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

// The main class is responsible for the connections between the modules, it also provides some utility
// methods (loading wav file, converting frequencies, ...) and utility variables (BPM, sampleRate, ...)
public class Main {
	private static Future<?> mainLoopFuture;
	private static ArrayList<Module> modules = new ArrayList<>();
	public static double bpm = 110; // BPM cannot be changed while running at the moment
	public static int sampleRate = 44100;
	public static double ticksPerBeat = sampleRate * 60 / bpm;
	public static int frequencyA4 = 440; // Frequency for note A4
	
	private static int tick = 0;
	
	private static HashMap<AudioReceiver, ArrayList<Connection>> audioConnections = new HashMap<>();
	private static HashMap<ControlMessageTransmitter, ArrayList<Connection>> controlConnections = new HashMap<>();
	private static HashMap<MidiTransmitter, ArrayList<Connection>> midiConnections = new HashMap<>();
	private static class Connection {
		Module m; // The other module
		int c1, c2; // The channels (c1 = transmitter channel, c2 = receiver channel)
		
		public Connection(Module m, int c1, int c2) {
			this.m = m;
			this.c1 = c1;
			this.c2 = c2;
		}
	}
		
	public static void main(String[] args) {
		init();
		System.console().readLine();
		close();
		System.exit(0);
	}
	
	private static void init() {
		// Sampler drums
		String[] drumSamples = new String[]{
			"/home/kangalioo/fl studio/samples/hands up kicks/VTDS1 HandsUp Kick 001.wav",
			"/home/kangalioo/fl studio/samples/hi hat samples/hihat_012a.wav",
			"/home/kangalioo/fl studio/samples/hi hat samples/hihat_012b.wav",
			"/home/kangalioo/fl studio/samples/my own/trance clap.wav",
			"/home/kangalioo/fl studio/samples/native/Legacy/Drums/Dance/DNC_Clap_5.wav"
		};
		int[] drumMappings = new int[]{
			60, 61, 62, 63, 64
		};
		
		// Drum piano roll notes
		PianoRoll.Melody drumPattern = new PianoRoll.Melody(new double[]{
			60, 1, 0,
			60, 0, 0,
			
			//61, 1, 64,
			//61, 0, 64,
			61, 1, 48,
			61, 0, 48,
			61, 1, 72,
			61, 0, 72,
			
			63, 1, 96,
			63, 0, 96,
			60, 1, 96,
			60, 0, 96,
			
			//61, 1, 160,
			//61, 0, 160,
			61, 1, 48 + 96,
			61, 0, 48 + 96,
			61, 1, 72 + 96,
			61, 0, 72 + 96,
			
			-1, -1, 192
		});
		
		// Create speaker module and 
		ExternalAudioTransmitter speakers = new ExternalAudioTransmitter();
		
		// Create all other modules
		ExternalMidiReceiver keyboardMidiIn = null;
		ExternalMidiReceiver launchControlMidiIn = null;
		ExternalMidiTransmitter launchControlMidiOut = null;
		try {
			keyboardMidiIn = new ExternalMidiReceiver(getMidiDevice("Keyboard", false));
			launchControlMidiIn = new ExternalMidiReceiver(getMidiDevice("Control", false));
			launchControlMidiOut = new ExternalMidiTransmitter(getMidiDevice("Control", true));
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
		DigitalKeyboard keyboard = new DigitalKeyboard();
		LaunchControl launchControl = new LaunchControl();
		PianoRoll drumRoll = new PianoRoll(drumPattern);
		Sampler sampler = new Sampler(drumSamples, drumMappings);
		Harmonizer harmonizer = new Harmonizer();
		Oscillator synthesizer = new Oscillator();
		Delay delay = new Delay();
		MyEffector effector = new MyEffector();
		Limiter synthLimiter = new Limiter();
		Mixer mixer = new Mixer();
		Limiter limiter = new Limiter();
		
		// Link midi modules via converters to piano receivers
		connectMidi(keyboardMidiIn, keyboard, 0, 0);
		connectMidi(launchControlMidiIn, launchControl, 0, 0);
		connectMidi(launchControl, launchControlMidiOut, 0, 0);
		connectControl(keyboard, harmonizer, 0, 0);
		connectControl(drumRoll, sampler, 0, 0);
		connectControl(launchControl, synthesizer, 0, 1);
		
		// Setup synthesizer module chain
		connectControl(harmonizer, synthesizer, 0, 0);
		connectAudio(synthesizer, delay, 0, 0);
		connectAudio(delay, effector, 0, 0);
		
		// Setup final audio management
		connectAudio(effector, mixer, 0, 0);
		connectAudio(sampler, mixer, 0, 1);
		connectAudio(mixer, limiter, 0, 0);
		connectAudio(limiter, speakers, 0, 0);
		
		// Start piano roll and open midi devices
		drumRoll.receiveControlMessage(new PlaybackControl(PlaybackControl.LOOP), 0);
		try {
			keyboardMidiIn.open();
			launchControlMidiIn.open();
			launchControlMidiOut.open();
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
		
		// TODO: Make Reverb module
		// TODO: Make AudioRecorder and MidiRecorder module
		// TODO: Implement importing from midi in piano roll
		
		initMainLoop();
		System.out.println("Main loop initialized, you can start playing now!");
	}
	
	private static void initMainLoop() {
		int nsDelay = 1000000000 / sampleRate;
		ScheduledExecutorService mainLoop = Executors.newSingleThreadScheduledExecutor();
		mainLoopFuture = mainLoop.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					tick();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}, nsDelay, nsDelay, TimeUnit.NANOSECONDS);
	}
	
	private static void tick() {
		// Trigger ticks for each module
		for (Module module : modules) {
			module.tick();
		}
		
		tick++;
	}
	
	// Link audio from m1 on channel c1 to m2 on channel c2
	public static void connectAudio(AudioTransmitter m1, AudioReceiver m2, int c1, int c2) {
		initModules(m1, m2);
		
		audioConnections.putIfAbsent(m2, new ArrayList<Connection>());
		ArrayList<Connection> list = audioConnections.get(m2);
		list.add(new Connection(m1, c1, c2));
	}
	
	// Link control messages from m1 on channel c1 to m2 on channel c2
	public static void connectControl(ControlMessageTransmitter m1, ControlMessageReceiver m2, int c1, int c2) {
		initModules(m1, m2);
		
		controlConnections.putIfAbsent(m1, new ArrayList<Connection>());
		ArrayList<Connection> list = controlConnections.get(m1);
		list.add(new Connection(m2, c1, c2));
	}
	
	// Link midi from m1 on channel c1 to m2 on channel c2
	public static void connectMidi(MidiTransmitter m1, MidiReceiver m2, int c1, int c2) {
		initModules(m1, m2);
		
		midiConnections.putIfAbsent(m1, new ArrayList<Connection>());
		ArrayList<Connection> list = midiConnections.get(m1);
		list.add(new Connection(m2, c1, c2));
	}
	
	private static void initModules(Module... mods) {
		for (Module m : mods) {
			if (modules.indexOf(m) == -1) modules.add(m);
		}
	}
	
	public static double[] transmitAudio(AudioReceiver receiver, int channel) {
		double[] res = {0, 0};
		
		for (Connection connection : audioConnections.get(receiver)) {
			if (connection.c2 == channel) {
				double[] sample = ((AudioTransmitter) connection.m).calculateSample(connection.c1);
				res[0] += sample[0];
				res[1] += sample[1];
			}
		}
		return res;
	}
	
	public static void receiveControlMessage(ControlMessageTransmitter transmitter, ControlMessage msg, int channel) {
		for (Connection connection : controlConnections.get(transmitter)) {
			if (connection.c1 == channel) {
				((ControlMessageReceiver) connection.m).receiveControlMessage(msg, connection.c2);
			}
		}
	}
	
	public static void receiveMidi(MidiTransmitter transmitter, MidiMessage msg, int channel) {
		for (Connection connection : midiConnections.get(transmitter)) {
			if (connection.c1 == channel) {
				((MidiReceiver) connection.m).receiveMidi(msg, connection.c2);
			}
		}
	}
	
	public static int[] getConnectedChannels(AudioReceiver module) {
		ArrayList<Integer> channels = new ArrayList<>();
		ArrayList<Connection> connectionList = audioConnections.get(module);
		
		for (Connection connection : connectionList) {
			if (!channels.contains(connection.c2)) channels.add(connection.c2);
		}
		
		int[] arr = new int[channels.size()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = channels.get(i);
		}
		
		return arr;
	}
	
	private static void close() {
		for (Module module : modules) {
			module.close();
		}
	}
	
	// Here come the utility methods
	
	// "outputDevice": false = search for input device, true = search for output device
	public static MidiDevice getMidiDevice(String name, boolean outputDevice) {
		try {
			MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
			
			for (MidiDevice.Info info : infos) {
				MidiDevice device = MidiSystem.getMidiDevice(info);
				if (!outputDevice && device.getMaxTransmitters() == 0
						|| outputDevice && device.getMaxReceivers() == 0) {
					continue;
				}
				
				// 'info.getName().split(" ")[0].equals(name)' compares the first word of the device to the first word of the midi device to the given name
				if (info.getName().split(" ")[0].equals(name)) { // "info.getName().equals(name)" for Windows
					return device;
				}
			}
			
			throw new MidiUnavailableException("No correct midi device connected");
		} catch (MidiUnavailableException e) {
			//e.printStackTrace();
		}
		
		return null;
	}
	
	public static double convertToFrequency(double note) {
		note -= 69; // Make note relative to concert pitch A4 (69)
		double frequency = frequencyA4 * Math.pow(2, note / 12.0);
		return frequency;
	}
	
	public static float[][] readWav(File file) {
		float[][] samples = null;
		try {
			AudioInputStream stream = AudioSystem.getAudioInputStream(file);
			
			samples = new float[(int) stream.getFrameLength()][];
			
			byte[] byteArray = new byte[4];
			int i = 0;
			while (i < samples.length) {
				float[] currentSample = new float[2];
				stream.read(byteArray);
				currentSample[0] = bytesToFloat(byteArray);
				// TODO: Fix this, so that the line below can be uncommented again
				//stream.read(byteArray);
				currentSample[1] = bytesToFloat(byteArray);
				samples[i] = currentSample;
				i++;
			}
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return samples;
	}
	
	// Little endian 32 bit float
	public static float bytesToFloat(byte[] byteArray) {
		long n = (byteArray[0] & 0xFF) | ((byteArray[1] & 0xFF) << 8) | ((byteArray[2] & 0xFF) << 16) | ((byteArray[3] & 0xFF) << 24);
		return (float) n / Integer.MAX_VALUE;
	}
	
	public static byte[] convertToByte(double v, int dataBytes) {
		if (v != (v = Math.min(1, Math.max(-1, v)))) System.err.println("Distortion :(");
		
		short converted = (short) (v * ((1 << (dataBytes * 8 - 1)) - 1));
		byte[] bytes = new byte[dataBytes];
		
		for (int i = dataBytes - 1; i >= 0; i--) {
			bytes[dataBytes - 1 - i] = (byte) ((converted & (0xFF << (8 * i))) >> (8 * i));
		}
		//bytes[0] = (byte) ((converted & 0xFF00) >> 8);
		//bytes[1] = (byte) (converted & 0x00FF);
		
		return bytes;
	}
	
	// e.g. "Status: <status>, Data 1: <data1>, Data 2: <data2>"
	public static String shortMessageToString(ShortMessage msg) {
		return "Status: " + msg.getStatus() + ", Data 1: " + msg.getData1() + ", Data 2: " + msg.getData2();
	}
	
	// e.g. "<status>: <data1>, <data2>"
	public static String shortMessageToCompactString(ShortMessage msg) {
		return msg.getStatus() + ": " + msg.getData1() + ", " + msg.getData2();
	}
	
	public static double convertVelocity(int velocity) {
		return (velocity + 1) / 128.0;
	}
	
	public static boolean noteInRange(int note) {
		return (note >= 0) && (note <= 127);
	}
}
