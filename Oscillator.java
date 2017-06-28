import java.util.ArrayList;
import javax.sound.midi.ShortMessage;

/*
Channel 0: Keyboard
Channel 1: Synthesizer knobs
*/
public class Oscillator implements AudioTransmitter, ControlMessageReceiver {
	static class Operator {
		double pitchMul;
		int waveform;
		int[] modulators = new int[0]; // Indexes of operators
		double[] modulationPowers = new double[0];
		boolean isCalculated = false;
		
		public Operator(int waveform) {
			this(waveform, 1);
		}
		
		public Operator(int waveform, double pitchMul) {
			this.waveform = waveform;
			this.pitchMul = pitchMul;
		}
		
		public void setModulators(int... modulators) {
			this.modulators = modulators;
		}
		
		public void setModulationPowers(double... modulationPowers) {
			if (modulationPowers.length != modulators.length) {
				throw new IllegalArgumentException("Modulation power amount does not match modulator amount!");
			}
			this.modulationPowers = modulationPowers;
		}
	}
	
	static class Note {
		public boolean remove = false, wasReleased = false;
		private int adsrPhase = 0; // 0..3 = attack, decay, sustain, release
		public int note;
		public double velocity;
		public double freq;
		private double adsrVol = 0;
		private double[] unisonPhases = new double[unisonVoices];
		
		private double[] phases = new double[operators.length];
		
		public Note(int note) {
			this(note, 0.8);
		}
		
		public Note(int note, double velocity) {
			this.note = note;
			calculateFreq();
			this.velocity = velocity;
			randomizeUnisonPhases();
		}
		
		private void randomizeUnisonPhases() {
			for (int i = 0; i < unisonVoices; i++) unisonPhases[i] = unisonInitialPhaseRandomization * Math.random();
		}
		
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof Note)) return false;
			Note otherNote = (Note) obj;
			return otherNote.note == this.note; // If the other note shares the "note"
		}
		
		private void calculateFreq() {
			freq = Main.convertToFrequency(note);
		}
	
		private double calculateOperator(int operatorIndex, double f, int phaseIndex) {
			// Calculate modulator frequency
			Operator op = operators[operatorIndex];
			double modulation = 0;
			for (int i = 0; i < op.modulators.length; i++) {
				double amp = calculateOperator(op.modulators[i], f, phaseIndex);
				modulation += amp * op.modulationPowers[i];
			}
			
			double res = generateWaveform(op.waveform, phases[operatorIndex]);
			if (!op.isCalculated) {
				phases[operatorIndex] += Oscillator.calculatePhaseChange(f * op.pitchMul + modulation * f); // f + modulator because of linear modulation
				phases[operatorIndex] %= 1;
				op.isCalculated = true;
			}
			
			return res;
		}
		
		public double[] calculateSample() { // Phase goes from 0 to 1 (exclusive)
			double[] res = new double[]{0, 0};
			double f = freq * freqMultiplier;
			
			// Unison
			for (int i = 0; i < unisonVoices; i++) {
				// This evenly and exponentially distributes the freqMul's of each
				// unison voice over a logarithmic range from 1/unisonPitch to 1*unisonPitch
				// example: unisonVoices = 5, unisonPitch = 4  -->  freqMul = {0.25, 0.5, 1, 2, 4}
				double freqMul = unisonVoices == 1 ? 1 : Math.pow(unisonPitch, i / ((double) unisonVoices - 1) * 2 - 1);
				
				double wave = 0;
				for (int j = 0; j < outputVolumes.length; j++) {
					if (outputVolumes[j] > 0) wave += calculateOperator(j, f * freqMul, i) * outputVolumes[j];
				}
				
				if (i * 2 + 1 == unisonVoices) {
					res[0] += wave;
					res[1] += wave;
				} else if (i < unisonVoices / 2) {
					res[0] += wave * 2;
				} else if (i >= unisonVoices / 2) {
					res[1] += wave * 2;
				}
			}
			res[0] /= unisonVoices;
			res[1] /= unisonVoices;
			
			resetOperatorStates();
			
			calculateADSR();
			
			// Apply ADSR
			res[0] *= adsrVol;
			res[1] *= adsrVol;
			
			// Apply velocity volume change
			res[0] *= velocity;
			res[1] *= velocity;
			
			return res;
		}
		
		private void calculateADSR() {
			if (adsrPhase == 0) {
				adsrVol += peak / attackTime;
				if (adsrVol >= peak || attackTime == 0) {
					adsrVol = peak;
					adsrPhase = 1;
				}
			} else if (adsrPhase == 1) {
				if (decayTime == 0) adsrPhase = 2;
				adsrVol += (sustainLevel - peak) / decayTime;
				boolean decayFinished = (sustainLevel - peak) > 0 ? adsrVol >= sustainLevel : adsrVol <= sustainLevel;
				if (decayFinished || decayTime == 0) {
					adsrVol = sustainLevel;
					adsrPhase = sustain ? 2 : 3; // If there is sustain, enter sustain phase, otherwise enter release phase
				}
			} else if (adsrPhase == 3) {
				adsrVol -= sustainLevel / releaseTime;
				if (adsrVol <= 0) {
					adsrVol = 0;
					remove = true;
				}
			}
		}
		
		private double generateWaveform(int shape, double phase) {
			if (shape == SINE) {
				return Math.sin(phase * Math.PI * 2);
			} else if (shape == TRIANGLE) {
				if (phase >= 0 && phase < 0.25) return phase * 4;
				else if (phase < 0.75) return (phase - 0.25) * -4 + 1;
				else return (phase - 0.75) * 4 - 1;
			} else if (shape == SAW) {
				return phase * 2 - 1;
			} else if (shape == SQUARE) {
				return phase < 0.5 ? -1 : 1;
			} else if (shape == NOISE) {
				return Math.random() * 2 - 1;
			}
			return 0;
		}
		
		private void release() {
			adsrPhase = 3;
		}
	}
	
	/*
	BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR
	BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR  BEGIN OF OSCILLATOR
	*/
	
	public static final int
			SINE = 0,
			TRIANGLE = 1,
			SAW = 2,
			SQUARE = 3,
			NOISE = 4,
			AMOUNT_OF_WAVEFORMS = 5;
	
	// BEGIN: SYNTHESIZER "KNOB VALUES"
	
	// ADSR related things
	public static int // In samples
			attackTime = 0,
			decayTime = 0,
			releaseTime = 500;
	public static double peak = 1, sustainLevel = 1;
	public static boolean sustain = true;
	
	// Other
	public static double freqMultiplier = 1;
	
	// FM matrix
	private static Operator[] operators = {
		new Operator(SINE),
		new Operator(SINE),
		new Operator(SINE)
	};
	private static double[][] modulationPowers = {
		{1},
		{0},
		{}
	};
	private static double[] outputVolumes = {1, 0, 0};
	
	// TODO: Make unison work
	// Unison related stuff
	public static int unisonVoices = 1;
	public static double unisonPitch = 1.013;
	public static double unisonInitialPhaseRandomization = 0.1;
	public static double unisonStereo = 1; // 0 = mono, 1 = stereo, 0.x = centralized stereo
	
	// END: SYNTHESIZER "KNOB VALUES"
	
	private ArrayList<Note> notes = new ArrayList<>();
	
	private int tick = 0;
	
	public Oscillator() {
		linkOperators();
	}
	
	private void linkOperators() {
		operators[0].setModulators(1);
		operators[1].setModulators(2);
		for (int i = 0; i < operators.length; i++) {
			operators[i].setModulationPowers(modulationPowers[i]);
		}
	}
	
	public double[] calculateSample(int channel) {
		if (channel > 0) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		double[] res = new double[]{0, 0};
		for (int i = 0; i < notes.size(); i++) {
			Note note;
			note = notes.get(i);
			
			if (note == null || note.remove) {
				notes.remove(note);
				i--;
				continue;
			}
			
			double[] noteSample = note.calculateSample();
			res[0] += noteSample[0];
			res[1] += noteSample[1];
			
		}
		tick += 1;
		return res;
	}
	
	private static void resetOperatorStates() {
		for (Operator op : operators) op.isCalculated = false;
	}
	
	private void stopAllNotes() {
		for (Note note : notes) note.remove = true;
	}
	
	public String nameOfWaveform(int waveform) {
		if (waveform == 0) return "sine";
		else if (waveform == 1) return "triangle";
		else if (waveform == 2) return "saw";
		else if (waveform == 3) return "square";
		else if (waveform == 4) return "noise";
		else {
			System.err.println("Waveform " + waveform + " does not exist.");
			return null;
		}
	}
	
	// "knob": 0..15 are real knobs, 16..23 are buttons
	private void changeKnob(int knob, double value) { // THIS IS THE LAUNCHCONTROL SYNTHESIZER CONTROL CODE
		if (knob == 0) {
			stopAllNotes();
			unisonVoices = (int) (1 + 9 * value); // 1..9 possible voice amounts
			System.out.println("Unison voices: " + unisonVoices);
		} else if (knob == 1) {
			unisonPitch = 1 + 0.05 * value;
			System.out.println("Unison pitch: " + unisonPitch);
		} else if (knob == 2) {
			unisonInitialPhaseRandomization = value * 0.4;
			System.out.println("Unison initial phase randomization: " + unisonInitialPhaseRandomization);
		} else if (knob == 3) {
			freqMultiplier = Math.pow(2, value);
			System.out.println("Freq multiplier: " + freqMultiplier);
		} else if (knob >= 4 && knob <= 5) {
			knob -= 4;
			double modulationPower = value * 10;
			modulationPowers[knob][0] = modulationPower;
			System.out.println("FM impact for OP " + knob + ": " + modulationPower);
		} else if (knob >= 16 && knob <= 23) {
			knob -= 16;
			if (knob >= operators.length) {
				System.out.println("Operator " + knob + " does not exist.");
				return;
			}
			operators[knob].waveform = Math.floorMod(operators[knob].waveform + (int) value, AMOUNT_OF_WAVEFORMS);
			System.out.println("Operator " + knob + " waveform: " + nameOfWaveform(operators[knob].waveform));
		}
	}
	
	private static double calculatePhaseChange(double freq) {
		return freq / Main.sampleRate % 1;
	}
	
	public void keyPressed(int note, double velocity) {
		notes.add(new Note(note, velocity));
	}
	
	public void keyReleased(int note) {
		int index = -1;
		Note refNote = new Note(note);
		for (int i = 0; i < notes.size(); i++) {
			if (notes.get(i).equals(refNote) && notes.get(i).wasReleased == false) {
				index = i;
				break;
			}
		}
		if (index != -1) {
			Note noteObj = notes.get(index);
			noteObj.wasReleased = true;
			noteObj.release();
		}
	}
	
	public void receiveControlMessage(ControlMessage message, int channel) {
		if (channel > 1) throw new IllegalArgumentException("The " + channel + ". channel which you tried to access does not exist.");
		if (channel == 0 && message instanceof NoteEvent) {
			NoteEvent msg = (NoteEvent) message;
			
			if (msg.velocity == 0) keyReleased(msg.note);
			else keyPressed(msg.note, msg.velocity);
		} else if (channel == 1 && message instanceof KnobChange) {
			KnobChange msg = (KnobChange) message;
			
			changeKnob(msg.knob, msg.value);
		}
	}
}
