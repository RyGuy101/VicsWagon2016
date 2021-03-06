package ioio.examples.hello;

/**************************************************************************
 * Happy version 140517A...ultrasonics working
 **************************************************************************/

import java.util.concurrent.TimeoutException;

import ioio.examples.watchdog.TimeoutObserver;
import ioio.examples.watchdog.Watchdog;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PulseInput.PulseMode;
import ioio.lib.api.exception.ConnectionLostException;

import android.os.SystemClock;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * An UltraSonicSensors instance is used to access three ultrasonic sensors (leftInput, frontInput, and rightInput) and read the measurements from these sensors....modified by Vic...ultrasonics works using Ytai's suggestions.
 *
 * @author Erik Colban
 */
public class UltraSonicSensor {
    private IOIO ioio;
    private TextView log;
    private ScrollView scroller;
    private double frontDistance;
    private double frontLeftDistance;
    private double backLeftDistance;
    private double rightDistance;
    private DigitalOutput frontStrobe;
    private DigitalOutput rearStrobe;
    private DigitalOutput leftStrobe;
    private DigitalOutput rightStrobe;
    private static final int FRONT_STROBE_ULTRASONIC_OUTPUT_PIN = 16;
    private static final int LEFT_STROBE_ULTRASONIC_OUTPUT_PIN = 17;
    private static final int RIGHT_STROBE_ULTRASONIC_OUTPUT_PIN = 15;
    private static final int REAR_STROBE_ULTRASONIC_OUTPUT_PIN = 14;
    private static final int FRONT_ULTRASONIC_INPUT_PIN = 12;
    private static final int REAR_ULTRASONIC_INPUT_PIN = 10;// input to ioio
    private static final int RIGHT_ULTRASONIC_INPUT_PIN = 11;
    private static final int LEFT_ULTRASONIC_INPUT_PIN = 13;
    private PulseInput leftInput;
    private PulseInput frontInput;
    private PulseInput rightInput;
    private PulseInput rearInput;
    private float μSECS_PER_SEC = 1000000; // Gives ultrasonics reqadings in
    // microseconds
    private static final double MM_PER_MICROSEC = 0.1717;
    static int timeout = 11;
    public static final double MAX_DISTANCE_MM = timeout * 1000 * MM_PER_MICROSEC;
    boolean getDurationTimedOut;
    int storageSpace = 4;
    public double[] frontDistances = new double[storageSpace];
    public double[] frontLeftDistances = new double[storageSpace];
    public double[] backLeftDistances = new double[storageSpace];
    public double[] rightDistances = new double[storageSpace];

    /**
     * Constructor of a UltraSonicSensors instance.
     *
     * @param ioio the IOIO instance used to communicate with the sensor
     * @throws ConnectionLostException
     */
    public UltraSonicSensor(IOIO ioio) throws ConnectionLostException {
        this.ioio = ioio;
        this.leftStrobe = ioio.openDigitalOutput(LEFT_STROBE_ULTRASONIC_OUTPUT_PIN);
        this.rightStrobe = ioio.openDigitalOutput(RIGHT_STROBE_ULTRASONIC_OUTPUT_PIN);
        this.rearStrobe = ioio.openDigitalOutput(REAR_STROBE_ULTRASONIC_OUTPUT_PIN);
        this.frontStrobe = ioio.openDigitalOutput(FRONT_STROBE_ULTRASONIC_OUTPUT_PIN);
    }

    /**
     * Makes a reading of the ultrasonic sensors and stores the results locally. To access these readings, use {@link #getBackLeftDistance()}, {@link #getFrontDistance()}, and {@link #getRightDistance()}.
     *
     * @throws ConnectionLostException
     * @throws InterruptedException
     */
    public void readAverage(int numReads) throws ConnectionLostException, InterruptedException {// In beta
        int[] leftReads = new int[numReads];
        int[] frontReads = new int[numReads];
        int[] rightReads = new int[numReads];
        int[] rearReads = new int[numReads];
        for (int i = 0; i < numReads; i++) {
            leftReads[i] = read(leftStrobe, LEFT_ULTRASONIC_INPUT_PIN);
            frontReads[i] = read(frontStrobe, FRONT_ULTRASONIC_INPUT_PIN);
            rightReads[i] = read(rightStrobe, RIGHT_ULTRASONIC_INPUT_PIN);
            rearReads[i] = read(rearStrobe, REAR_ULTRASONIC_INPUT_PIN);
            if (i + 1 < numReads) {
                SystemClock.sleep(100);//might need to be longer
            }
        }
        backLeftDistance = 0;
        frontDistance = 0;
        rightDistance = 0;
        frontLeftDistance = 0;
        for (int i = 0; i < numReads; i++) {
            backLeftDistance += leftReads[i];
            frontDistance += frontReads[i];
            rightDistance += rightReads[i];
            frontLeftDistance += rearReads[i];
        }
        backLeftDistance /= (double) numReads;
        frontDistance /= (double) numReads;
        rightDistance /= (double) numReads;
        frontLeftDistance /= (double) numReads;
    }

    public void readAll() throws ConnectionLostException, InterruptedException {
        backLeftDistance = read(leftStrobe, LEFT_ULTRASONIC_INPUT_PIN) * MM_PER_MICROSEC;
        frontDistance = read(frontStrobe, FRONT_ULTRASONIC_INPUT_PIN) * MM_PER_MICROSEC;
        rightDistance = read(rightStrobe, RIGHT_ULTRASONIC_INPUT_PIN) * MM_PER_MICROSEC;
        frontLeftDistance = read(rearStrobe, REAR_ULTRASONIC_INPUT_PIN) * MM_PER_MICROSEC;
        updateArrays();
    }

    public void readExcludeRight() throws ConnectionLostException, InterruptedException {
        backLeftDistance = read(leftStrobe, LEFT_ULTRASONIC_INPUT_PIN) * MM_PER_MICROSEC;
        frontDistance = read(frontStrobe, FRONT_ULTRASONIC_INPUT_PIN) * MM_PER_MICROSEC;
        frontLeftDistance = read(rearStrobe, REAR_ULTRASONIC_INPUT_PIN) * MM_PER_MICROSEC;
        updateArrays();
    }

    public void readFront() throws ConnectionLostException, InterruptedException {
        frontDistance = read(frontStrobe, FRONT_ULTRASONIC_INPUT_PIN) * MM_PER_MICROSEC;
        updateArrays();
    }

    private void updateArrays() {
        for (int i = storageSpace - 1; i >= 1; i--) {
            frontDistances[i] = frontDistances[i - 1];
            frontLeftDistances[i] = frontLeftDistances[i - 1];
            backLeftDistances[i] = backLeftDistances[i - 1];
            rightDistances[i] = rightDistances[i - 1];
        }
        frontDistances[0] = frontDistance;
        frontLeftDistances[0] = frontLeftDistance;
        backLeftDistances[0] = backLeftDistance;
        rightDistances[0] = rightDistance;
    }

    public void startThread() {//TODO for iARoC 2016: Use this thread. Get rid of anomalies by using last 3 reads. If middle one is different by at least x amount from both the other 2, don't save any values. If the middle one is not different, it is not an anomaly and so that value is saved. The most recent reading is not recorded as the distance because it is uncertain if it is an anomaly.
        for (int i = 0; i < storageSpace; i++) {
            try {
                Thread.sleep(10);
//                readAll();
                readFront();
            } catch (ConnectionLostException e) {
                e.printStackTrace();
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(10);
//                        readAll();
                        readFront();
                    } catch (ConnectionLostException e) {
                        e.printStackTrace();
                        break;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }

    private int read(DigitalOutput strobe, int inputPin) throws ConnectionLostException, InterruptedException // Order of following statements is very important...do not change
    {
        int distance = 0;
        ioio.beginBatch();
        strobe.write(true);
        final PulseInput input = ioio.openPulseInput(inputPin, PulseMode.POSITIVE);
        ioio.endBatch();
        SystemClock.sleep(40);
        strobe.write(false);
        getDurationTimedOut = false;
        Watchdog wd = new Watchdog(timeout + 10);
        wd.addTimeoutObserver(new TimeoutObserver() {
            @Override
            public void timeoutOccured(Watchdog w) {
                getDurationTimedOut = true;
                synchronized (input) {
                    input.notify();
                    input.close();
                }
            }
        });
        //		long start = System.currentTimeMillis();
        wd.start();
        try {
            distance += (int) (input.getDuration() * μSECS_PER_SEC);
        } catch (InterruptedException e) {
        }
        //		MainActivity.activity.log("Took " + (System.currentTimeMillis() - start) + " millis");
        wd.stop();
        if (getDurationTimedOut) {
            //			throw new InterruptedException("getDuration timed out!");
            distance = (int) (timeout * 1000);
        } else {
            input.close();
        }
        if (distance == 0) {
            return read(strobe, inputPin);
        } else {
            return distance;
        }
    }

    public synchronized double getBackLeftDistance() {
        return backLeftDistance;
    }

    public synchronized double getFrontDistance() {
        return frontDistance;
    }

    public synchronized double getRightDistance() {
        return rightDistance;
    }

    public synchronized double getFrontLeftDistance() {
        return frontLeftDistance;
    }

    public void closeConnection() {
        leftInput.close();
        frontInput.close();
        rightInput.close();
        rearInput.close();
        leftStrobe.close();
        frontStrobe.close();
        rightStrobe.close();
        rearStrobe.close();
    }
}
