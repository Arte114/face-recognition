import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.*;
import java.util.Vector;
import java.util.Enumeration;
import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;
import javax.media.datasink.*;
import javax.media.control.MonitorControl;
import javax.media.control.*;

/**
 * Sample program to read data from an output DataSource of a Processor.
 */
public class DataSourceChecker extends Frame 
                implements ControllerListener, DataSinkListener{

    Processor p;
    Object waitSync = new Object();
    boolean stateTransitionOK = true;
    static boolean monitorOn = false;
    public DataSourceChecker()
    {
        addWindowListener(
                            new WindowAdapter()
                            {
                                public void windowClosing(WindowEvent we)
                                {
                                        System.exit(0);
                                }
                            }
                         );
    }
    public void actionPerformed(ActionEvent ae)
    {
        System.exit(0);
    }
    /**
     * Given a DataSource, create a processor and hook up the output
     * DataSource from the processor to a customed DataSink.
     */
    public boolean open(DataSource ds) {

	System.err.println("create processor for: " + ds.getContentType());

	if (monitorOn) {
	    // If monitoring is on, we'd like to enable synchronization
	    // by enabling the use of the RawSyncBufferMux.  The default
	    // is RawBufferMux which does not perform sychronization.
	    enableSyncMux();
	}

	try {
	    p = Manager.createProcessor(ds);
	} catch (Exception e) {
	    System.err.println("Failed to create a processor from the given DataSource: " + e);
	    return false;
	}

	p.addControllerListener(this);

	// Put the Processor into configured state.
	p.configure();
	if (!waitForState(p.Configured)) {
	    System.err.println("Failed to configure the processor.");
	    return false;
	}

	// Get the raw output from the processor.
	p.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));
// Obtain the track controls.
	TrackControl tc[] = p.getTrackControls();
	if (tc == null) {
	    System.err.println("Failed to obtain track controls from the processor.");
	    return false;
	}
	// Search for the track control for the video track.
	TrackControl videoTrack = null;
	for (int i = 0; i < tc.length; i++) {
	    if (tc[i].getFormat() instanceof VideoFormat) {
		videoTrack = tc[i];
		break;
	    }
	}
	if (videoTrack == null) {
	    System.err.println("The input media does not contain a video track.");
	    return false;
	}

	// Program the videotrack
	float frameRate = ((VideoFormat)videoTrack.getFormat()).getFrameRate();
	Dimension size = ((VideoFormat)videoTrack.getFormat()).getSize();
	
	int w = (size.width % 8 == 0 ? size.width :
				(int)(size.width / 8) * 8);
	int h = (size.height % 8 == 0 ? size.height :
				(int)(size.height / 8) * 8);
	VideoFormat jpegFormat = new VideoFormat(VideoFormat.JPEG,
						 new Dimension(w, h),
						 Format.NOT_SPECIFIED,
						 Format.byteArray,
						 frameRate);
	videoTrack.setFormat(jpegFormat);
	System.err.println("Video transmitted as:");
	System.err.println("  " + jpegFormat);
	

	// Instantiate and set the frame access codec to the data flow path.

	try {
	    Codec codec[] = {  new MotionDetectionEffect()};
	    videoTrack.setCodecChain(codec);
	} catch (UnsupportedPlugInException e) {
	    System.err.println("The processor does not support effects.");
	}

	// Realize the processor.
	
	p.prefetch();
	if (!waitForState(p.Prefetched)) {
	    System.err.println("Failed to realize the processor.");
	    return false;
	}	
	p.realize();
	if (!waitForState(p.Realized)) {
	    System.err.println("Failed to realize the processor.");
	    return false;
	}

	// Get the output DataSource from the processor and
	// hook it up to the DataSourceHandler.
	DataSource ods = p.getDataOutput();
	DataSourceHandler handler = new DataSourceHandler();

	try {
	    handler.setSource(ods);
	} catch (IncompatibleSourceException e) {
	    System.err.println("Cannot handle the output DataSource from the processor: " + ods);
	    return false;
	}

	handler.addDataSinkListener(this);
	handler.start();

	// Prefetch the processor.
	p.prefetch();
	if (!waitForState(p.Prefetched)) {
	    System.err.println("Failed to prefetch the processor.");
	    return false;
	}

	if (monitorOn) {

	    // If monitor is on, try to obtain any monitor controls 
	    // from the processor and display them.

	    setLayout(new BorderLayout());

	    Control controls[] = p.getControls();
	    Panel monitorPanel = null;
	    Component monitorComp = null;

	    for (int i = 0; i < controls.length; i++) {
		if (controls[i] instanceof MonitorControl) {
		    MonitorControl mc = (MonitorControl)controls[i];
		    monitorComp = mc.getControlComponent();
		    if (monitorPanel == null) {
			monitorPanel = new Panel();
			monitorPanel.setLayout(new GridLayout(0, 1));
		    }
		    if (monitorComp != null)
			monitorPanel.add(monitorComp);
		    mc.setEnabled(true);
		}
	    }

	    if (monitorPanel != null)
	        add("Center", monitorPanel);

	    // Display the processor's control component.
	    Component cc;
	    if ((cc = p.getControlPanelComponent()) != null) {
		add("South", cc);
	    }
	}

	// Start the processor.
	p.start();
	if (monitorOn)
		setVisible(true);

	return true;
    }

    public void addNotify() {
	super.addNotify();
	pack();
    }


    /**
     * Change the plugin list to disable the default RawBufferMux 
     * thus allowing the RawSyncBufferMux to be used.
     * This is a handy trick.  You wouldn't know this, would you? :)
     */
    void enableSyncMux() {
	Vector muxes = PlugInManager.getPlugInList(null, null, 
					PlugInManager.MULTIPLEXER);
	for (int i = 0; i < muxes.size(); i++) {
	    String cname = (String)muxes.elementAt(i);
	    if (cname.equals("com.sun.media.multiplexer.RawBufferMux")) {
		muxes.removeElementAt(i);
		break;
	    }
	}
	PlugInManager.setPlugInList(muxes, PlugInManager.MULTIPLEXER);
    }


    /**
     * Block until the processor has transitioned to the given state.
     * Return false if the transition failed.
     */
    boolean waitForState(int state) {
	synchronized (waitSync) {
	    try {
		while (p.getState() < state && stateTransitionOK)
		    waitSync.wait();
	    } catch (Exception e) {}
	}
	return stateTransitionOK;
    }


    /**
     * Controller Listener.
     */
    public void controllerUpdate(ControllerEvent evt) {

	if (evt instanceof ConfigureCompleteEvent ||
	    evt instanceof RealizeCompleteEvent ||
	    evt instanceof PrefetchCompleteEvent) {
	    synchronized (waitSync) {
		stateTransitionOK = true;
		waitSync.notifyAll();
	    }
	} else if (evt instanceof ResourceUnavailableEvent) {
	    synchronized (waitSync) {
		stateTransitionOK = false;
		waitSync.notifyAll();
	    }
	} else if (evt instanceof EndOfMediaEvent) {
	    p.close();
	} else if (evt instanceof SizeChangeEvent) {
	}
    }


    /**
     * DataSink Listener
     */
    public void dataSinkUpdate(DataSinkEvent evt) {

	if (evt instanceof EndOfStreamEvent) {
	    System.err.println("All done!");
	    evt.getSourceDataSink().close();
	    System.exit(0);
	}
    }


    /***************************************************
     * Inner class
     *
     ***************************************************/

    /**
     * This DataSourceHandler class reads from a DataSource and display
     * information of each frame of data received.
     */
    class DataSourceHandler implements DataSink, BufferTransferHandler {
	DataSource source;
	PullBufferStream pullStrms[] = null;
	PushBufferStream pushStrms[] = null;

	// Data sink listeners.
	private Vector listeners = new Vector(1);

	// Stored all the streams that are not yet finished (i.e. EOM
	// has not been received.
	SourceStream unfinishedStrms[] = null;

	// Loop threads to pull data from a PullBufferDataSource.
	// There is one thread per each PullSourceStream.
	Loop loops[] = null;

	Buffer readBuffer;

	/**
	 * Sets the media source this <code>MediaHandler</code>
	 * should use to obtain content.
	 */
	public void setSource(DataSource source) throws IncompatibleSourceException {

	    // Different types of DataSources need to handled differently.
	    if (source instanceof PushBufferDataSource) {

		pushStrms = ((PushBufferDataSource)source).getStreams();
		unfinishedStrms = new SourceStream[pushStrms.length];

		// Set the transfer handler to receive pushed data from
		// the push DataSource.
		for (int i = 0; i < pushStrms.length; i++) {
		    pushStrms[i].setTransferHandler(this);
		    unfinishedStrms[i] = pushStrms[i];
		}


	    } else if (source instanceof PullBufferDataSource) {

		pullStrms = ((PullBufferDataSource)source).getStreams();
		unfinishedStrms = new SourceStream[pullStrms.length];

		// For pull data sources, we'll start a thread per
		// stream to pull data from the source.
		loops = new Loop[pullStrms.length];
		for (int i = 0; i < pullStrms.length; i++) {
		    loops[i] = new Loop(this, pullStrms[i]);
		    unfinishedStrms[i] = pullStrms[i];
		}

	    } else {

		// This handler only handles push or pull buffer datasource.
		throw new IncompatibleSourceException();

	    }

	    this.source = source;
	    readBuffer = new Buffer();
	}


	/**
	 * For completeness, DataSink's require this method.
	 * But we don't need it.
	 */
	public void setOutputLocator(MediaLocator ml) {
	}


	public MediaLocator getOutputLocator() {
	    return null;
	}


	public String getContentType() {
	    return source.getContentType();
	}


	/**
	 * Our DataSink does not need to be opened.
	 */
	public void open() {
	}


	public void start() {
	    try {
		source.start();
	    } catch (IOException e) {
		System.err.println(e);
	    }

	    // Start the processing loop if we are dealing with a
	    // PullBufferDataSource.
	    if (loops != null) {
		for (int i = 0; i < loops.length; i++)
		    loops[i].restart();
	    }
	}


	public void stop() {
	    try {
		source.stop();
	    } catch (IOException e) {
		System.err.println(e);
	    }

	    // Start the processing loop if we are dealing with a
	    // PullBufferDataSource.
	    if (loops != null) {
		for (int i = 0; i < loops.length; i++)
		    loops[i].pause();
	    }
	}


	public void close() {
	    stop();
	    if (loops != null) {
		for (int i = 0; i < loops.length; i++)
		    loops[i].kill();
	    }
	}

	
	public void addDataSinkListener(DataSinkListener dsl) {
	    if (dsl != null)
		if (!listeners.contains(dsl))
		    listeners.addElement(dsl);
	}


	public void removeDataSinkListener(DataSinkListener dsl) {
	    if (dsl != null)
		listeners.removeElement(dsl);
	}


	protected void sendEvent(DataSinkEvent event) {
	    if (!listeners.isEmpty()) {
		synchronized (listeners) {
		    Enumeration list = listeners.elements();
		    while (list.hasMoreElements()) {
			DataSinkListener listener = 
				(DataSinkListener)list.nextElement();
			listener.dataSinkUpdate(event);
		    }
		}
	    }
	}


	/**
	 * This will get called when there's data pushed from the
	 * PushBufferDataSource.
	 */
	public void transferData(PushBufferStream stream) {

	    try {
		stream.read(readBuffer);
	    } catch (IOException e) {
		System.err.println(e);
		sendEvent(new DataSinkErrorEvent(this, e.getMessage()));
		return;
	    }

	    printDataInfo(readBuffer);

	    // Check to see if we are done with all the streams.
	    if (readBuffer.isEOM() && checkDone(stream)) {
		sendEvent(new EndOfStreamEvent(this));
	    }
	}


	/**
	 * This is called from the Loop thread to pull data from
	 * the PullBufferStream.
	 */
	public boolean readPullData(PullBufferStream stream) {
	    try {
		stream.read(readBuffer);
	    } catch (IOException e) {
		System.err.println(e);
		return true;
	    }

	    printDataInfo(readBuffer);

	    if (readBuffer.isEOM()) {
	        // Check to see if we are done with all the streams.
		if (checkDone(stream)) {
		    System.err.println("All done!");
		    close();
		}
		return true;
	    }
	    return false;
	}


	/**
	 * Check to see if all the streams are processed.
	 */
	public boolean checkDone(SourceStream strm) {
	    boolean done = true;

	    for (int i = 0; i < unfinishedStrms.length; i++) {
		if (strm == unfinishedStrms[i])
		    unfinishedStrms[i] = null;
		else if (unfinishedStrms[i] != null) {
		    // There's at least one stream that's not done.
		    done = false;
		}
	    }
	    return done;
	}


	void printDataInfo(Buffer buffer) {
	    //System.err.println("Read from stream: " + stream);
	    if (buffer.getFormat() instanceof AudioFormat)
		System.err.println("Read audio data:");
	    else
		System.err.println("Read video data:");
	    System.err.println("  Time stamp: " + buffer.getTimeStamp());
	    System.err.println("  Sequence #: " + buffer.getSequenceNumber());
	    System.err.println("  Data length: " + buffer.getLength());

	    if (buffer.isEOM())
		System.err.println("  Got EOM!");

	    // Write the data in a file.
	    try {
                String imageFile = "C:\\checks\\image"+buffer.getTimeStamp()+".jpg";
	    	RandomAccessFile raFile;
	    	raFile = new RandomAccessFile(imageFile,"rw");

	    	byte data[] = null;
	    	data = (byte[])buffer.getData();
	    	raFile.write(data, 0 , buffer.getLength());
	    	raFile.close();
	    } catch (Exception e) {
		    e.printStackTrace();
	    }
	}

	public Object [] getControls() {
	    return new Object[0];
	}

	public Object getControl(String name) {
	    return null;
	}
    }


    /**
     * A thread class to implement a processing loop.
     * This loop reads data from a PullBufferDataSource.
     */
    class Loop extends Thread {

	DataSourceHandler handler;
	PullBufferStream stream;
	boolean paused = true;
	boolean killed = false;

	public Loop(DataSourceHandler handler, PullBufferStream stream) {
	    this.handler = handler;
	    this.stream = stream;
	    start();
	}

	public synchronized void restart() {
	    paused = false;
	    notify();
	}

	/**
	 * This is the correct way to pause a thread; unlike suspend.
	 */
	public synchronized void pause() {
	    paused = true;
	}

	/**
	 * This is the correct way to kill a thread; unlike stop.
	 */
	public synchronized void kill() {
	    killed = true;
	    notify();
	}
	/**
	 * This is the processing loop to pull data from a 
	 * PullBufferDataSource.
	 */
	public void run() {
	    while (!killed) {
		try {
		    while (paused && !killed) {
			wait();
		    }
		} catch (InterruptedException e) {}

		if (!killed) {
		    boolean done = handler.readPullData(stream);
		    if (done)
			pause();
		}
	    }
	}
    }


    /**
     * Main program
     */
    public static void main(String [] args) {

	if (args.length == 0) {
	    prUsage();
	    System.exit(0);
 	}

	// Parse the arguments.
	int i = 0;
	String inputURL = null;
	while (i < args.length) {

	    if (args[i].equals("-monitor")) {
		monitorOn = true;
	    } else {
		inputURL = args[i];
	    }
	    i++;
	}

	if (inputURL == null) {
	    System.err.println("No input url is specified");
	    prUsage();
	}

	MediaLocator ml;

	if ((ml = new MediaLocator(inputURL)) == null) {
	    System.err.println("Cannot build media locator from: " + inputURL);
	    prUsage();
	    System.exit(0);
	}

	DataSource ds = null;

	// Create a DataSource given the media locator.
	try {
	    ds = Manager.createDataSource(ml);
	} catch (Exception e) {
	    System.err.println("Cannot create DataSource from: " + ml);
	    System.exit(0);
	}

	DataSourceChecker dsr = new DataSourceChecker();
	if (!dsr.open(ds))
	    System.exit(0);
    }

    static void prUsage() {
	System.err.println("Usage: java DataSourceChecker [-monitor] <url>");
    }
}

