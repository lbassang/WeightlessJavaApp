

public class Start {
	private static final String BS_IP = "160.98.31.186";
	private static final int BS_PORT =  7979;
	public static final int SAMPLE_TIMEOUT = 600000;

	// time to run sample. 10 minute
	
	public static void main(String[] args) {
		ConnectionSample connectionSample = new ConnectionSample(BS_IP, BS_PORT,SAMPLE_TIMEOUT);
		Thread t = new Thread(connectionSample);
		t.start();
		
		try {
			t.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
		
	}

}
