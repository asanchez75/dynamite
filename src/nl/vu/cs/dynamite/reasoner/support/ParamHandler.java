package nl.vu.cs.dynamite.reasoner.support;

/**
 * Singleton class containing all the configuration parameters for the
 * execution.
 */
public class ParamHandler {
	private static final ParamHandler instance = new ParamHandler();
	private boolean usingCount;
	private int lastStep;
	private String copyDir;

	public static ParamHandler get() {
		return instance;
	}

	private ParamHandler() {
		usingCount = false;
		lastStep = 0;
	}

	public boolean isUsingCount() {
		return usingCount;
	}

	public void setUsingCount(boolean usingCount) {
		this.usingCount = usingCount;
	}

	public int getLastStep() {
		return lastStep;
	}

	public void setLastStep(int lastStep) {
		this.lastStep = lastStep;
	}

	public String getCopyDir() {
		return copyDir;
	}

	public void setCopyDir(String dir) {
		copyDir = dir;
	}

}
