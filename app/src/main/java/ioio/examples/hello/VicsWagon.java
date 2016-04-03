package ioio.examples.hello;

/******************************************************************************************
 * Happy version 150228B...IntelliJ version
 * Added Duane's code for wave drive ... commented out
 ********************************************************************************************/

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.Sequencer;
import ioio.lib.api.Sequencer.ChannelConfig;
import ioio.lib.api.Sequencer.ChannelConfigBinary;
import ioio.lib.api.Sequencer.ChannelConfigFmSpeed;
import ioio.lib.api.Sequencer.ChannelConfigSteps;
import ioio.lib.api.Sequencer.ChannelCueBinary;
import ioio.lib.api.Sequencer.ChannelCueFmSpeed;
import ioio.lib.api.Sequencer.Clock;
import ioio.lib.api.exception.ConnectionLostException;

import java.util.ArrayList;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class VicsWagon {
    private IOIO ioio_;
    private static final int FRONT_STROBE_ULTRASONIC_OUTPUT_PIN = 16;
    private static final int LEFT_STROBE_ULTRASONIC_OUTPUT_PIN = 17;
    private static final int RIGHT_STROBE_ULTRASONIC_OUTPUT_PIN = 15;
    private static final int FRONT_ULTRASONIC_INPUT_PIN = 12;
    private static final int REAR_ULTRASONIC_INPUT_PIN = 10;// input to ioio
    private static final int RIGHT_ULTRASONIC_INPUT_PIN = 11;
    private static final int LEFT_ULTRASONIC_INPUT_PIN = 13;
    private static final int MOTOR_ENABLE_PIN = 3;// Low turns both motors
    private static final int MOTOR_RIGHT_DIRECTION_PIN = 20;// High => cw
    private static final int MOTOR_LEFT_DIRECTION_PIN = 21;
    private static final int MOTOR_CONTROLLER_CONTROL_PIN = 6;// For both motors
    private static final int REAR_STROBE_ULTRASONIC_OUTPUT_PIN = 14;// ioio out
    private static final int MOTOR_HALF_FULL_STEP_PIN = 7;// For both motors
    private static final int MOTOR_RESET = 22;// For both motors
    private static final int MOTOR_CLOCK_LEFT_PIN = 27;
    private static final int MOTOR_CLOCK_RIGHT_PIN = 28;
    private ToggleButton button;
    public UltraSonicSensor sonar;
    private TextView mText;
    private ScrollView mScroller;
    private TextToSpeech mTts;
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorMagneticField;
    private float[] valuesAccelerometer;
    private float[] valuesMagneticField;
    private float[] matrixR;
    private float[] matrixI;
    private float[] matrixValues;
    private double azimuth;
    private double pitch;
    private double roll;
    private DigitalOutput led;// The on-board LED
    private DigitalOutput motorEnable; // Both motors
    private DigitalOutput rightMotorClock; // Step right motor
    private DigitalOutput leftMotorClock; // Step left motor
    private DigitalOutput motorControllerReset;
    private DigitalOutput rightMotorDirection;
    private DigitalOutput leftMotorDirection;
    private DigitalOutput motorControllerControl;// Decay mode high => slow
    private DigitalOutput halfFull;// High => half step
    private Sequencer sequencer;
    final ChannelConfigSteps stepperStepConfig = new ChannelConfigSteps(new DigitalOutput.Spec(MOTOR_CLOCK_RIGHT_PIN));
    final ChannelConfigBinary stepperRightDirConfig = new Sequencer.ChannelConfigBinary(false, false, new DigitalOutput.Spec(MOTOR_RIGHT_DIRECTION_PIN));
    final ChannelConfigBinary stepperLeftDirConfig = new Sequencer.ChannelConfigBinary(false, false, new DigitalOutput.Spec(MOTOR_LEFT_DIRECTION_PIN));
    private Sequencer.ChannelCueBinary stepperDirCue = new ChannelCueBinary();
    final ChannelConfigFmSpeed stepperRightFMspeedConfig = new ChannelConfigFmSpeed(Clock.CLK_62K5, 2, new DigitalOutput.Spec(MOTOR_CLOCK_RIGHT_PIN));
    final ChannelConfigFmSpeed stepperLeftFMspeedConfig = new ChannelConfigFmSpeed(Clock.CLK_62K5, 2, new DigitalOutput.Spec(MOTOR_CLOCK_LEFT_PIN));
    final ChannelConfig[] channelConfigList = new ChannelConfig[]{stepperRightFMspeedConfig, stepperLeftFMspeedConfig};// stepperFMspeedConfig
    private Sequencer.ChannelCueFmSpeed stepperRightFMspeedCue = new ChannelCueFmSpeed();
    private Sequencer.ChannelCueFmSpeed stepperLeftFMspeedCue = new ChannelCueFmSpeed();
    private Sequencer.ChannelCue[] cueList = new Sequencer.ChannelCue[]{stepperRightFMspeedCue, stepperLeftFMspeedCue};// stepperStepCue//stepperFMspeedCue
    private int MAX_FM_SPEED_PERIOD = 60000;
    private int MIN_FM_SPEED_PERIOD = 600;
    private static final double STEPS_PER_MM = 0.63;
    private static final double STEPS_PER_DEGREE = 1.31;//1.36 good for 90 degrees as of 4/2/16. 1.31 for 180
    private int timePerPush = 6250;// measured in 16 microseconds
    private double accelerationStraight = 0.0000001024;// measured in steps/((16
    // microseconds)^2)
    private double accelerationTurn = 0.0000000256;
    private double accelerationSlowDown = 0.0000002048;
    private final int maxVelocity = 750;//measured in millimeters per second
    private int minPeriod = (int) (62500 / (maxVelocity * STEPS_PER_MM));// time between steps, measured in 16 microseconds
    private double currentVelocity = accelerationStraight * timePerPush;
    private int numReads = 5;

    public static final int EAST = 0;
    public static final int NORTH = 90;
    public static final int WEST = 180;
    public static final int SOUTH = 270;

    public static final double ROBOT_WIDTH = 2;
    public static final double ROBOT_LENGTH = 3;
    private static final int CELL_SIZE = 700;
    private static final int IR_SENSOR_INPUT_PIN = 4;

    private boolean FORWARD_RIGHT = true;
    private boolean FORWARD_LEFT = !FORWARD_RIGHT;
    private boolean BACKWARD_RIGHT = FORWARD_LEFT;
    private boolean BACKWARD_LEFT = FORWARD_RIGHT;

    public MainActivity main;
    int logPriority = 3;

    private boolean seesIr;

    public void setMain(MainActivity main) {
        this.main = main;
    }

    public VicsWagon(IOIO ioio_) {
        this.ioio_ = ioio_;
    }

    boolean doneMapping = false;
    boolean doSolution = false;
    ArrayList<GridSquare> gridSquares = new ArrayList<GridSquare>();
    ArrayList<GridSquare> gridSquares2 = new ArrayList<GridSquare>();
    private int counter = -1;
    int openSpaceSideDistance = 75;//600;
    int openSpaceFrontDistance = 325;//500;
    double desiredWallDistance = 227.25;
    double tooCloseToTurnDistance = 177.25;
    double tooCloseWallDistance = 113.625;
    double tooFarWallDistance = 340.875;
    int startX = 2;
    int startY = 0;
    int startDirection = NORTH;
    boolean aligned = false;
    double optimalDifference = 0;
    boolean firstBumpAlign = false;

    public void urbanLeft() throws Exception {
        if (!doneMapping) {
            if (counter == -1) {
                gridSquares.add(new GridSquare(startX, startY, startDirection));
                counter++;
                //			} else {
                //				for (int i = counter + 1; i < gridSquares.size(); i++) {
                //					gridSquares.remove(i);
                //				}
            }
        }
        while (!doneMapping) {
            mapMazeLeft();
        }
        aligned = false;
        gridSquares2.clear();
        counter = -1;
        gridSquares2.add(new GridSquare(startX, startY, startDirection));
        counter++;
        log1("--PRESS BUTTON TO START SOLUTION RUN--");
        while (!doSolution) {
            SystemClock.sleep(100);
        }
        while (doSolution) {
            solveMaze();
        }
    }

    public void urbanRight() throws Exception {
        if (!doneMapping) {
            if (counter == -1) {
                gridSquares.add(new GridSquare(startX, startY, startDirection));
                counter++;
                //			} else {
                //				for (int i = counter + 1; i < gridSquares.size(); i++) {
                //					gridSquares.remove(i);
                //				}
            }
        }
        while (!doneMapping) {
            mapMazeRight();
        }
        aligned = false;
        gridSquares2.clear();
        counter = -1;
        gridSquares2.add(new GridSquare(startX, startY, startDirection));
        counter++;
        log1("--PRESS BUTTON TO START SOLUTION RUN--");
        while (!doSolution) {
            SystemClock.sleep(100);
        }
        while (doSolution) {
            solveMaze();
        }
    }

    private double IRPulseDuration;
    private PulseInput frontIRSensorInput;
    private long spinTime = 0;
    int goldSteps = (int) ((50 * STEPS_PER_MM) / 2.0 + 0.5) * 2;

    public void goldRush() throws ConnectionLostException, InterruptedException {
        if (System.currentTimeMillis() - spinTime >= 10000) {
            spinTime = System.currentTimeMillis();
            spinScan();
        }
        if (seesIr) {
            goForward(sonar.getFrontDistance() + 200);
        } else {
            if (sonar.getFrontDistance() < openSpaceFrontDistance) {
                goBackward(50);
                spinLeft(45);
                currentVelocity = accelerationStraight * timePerPush;
            } else if (sonar.getRightDistance() < openSpaceSideDistance && (sonar.getBackLeftDistance() >= openSpaceSideDistance || sonar.getFrontLeftDistance() >= openSpaceSideDistance)) {
                spinLeft(10);
                currentVelocity = accelerationStraight * timePerPush;
            } else if ((sonar.getBackLeftDistance() < openSpaceSideDistance || sonar.getFrontLeftDistance() < openSpaceSideDistance) && sonar.getRightDistance() >= openSpaceSideDistance) {
                spinRight(10);
                currentVelocity = accelerationStraight * timePerPush;
            } else if (sonar.getRightDistance() < openSpaceSideDistance && (sonar.getBackLeftDistance() < openSpaceSideDistance || sonar.getFrontLeftDistance() < openSpaceSideDistance)) {
                goBackward(270);
                spinLeft(45);
                currentVelocity = accelerationStraight * timePerPush;
            } else {
                setDirection(FORWARD_LEFT, FORWARD_RIGHT);
                sequencer.start();
                accelerateUp(goldSteps, accelerationStraight, 1, 1, minPeriod);
                waitToFinish();
                sequencer.pause();
            }
        }
    }

    private void spinScan() throws ConnectionLostException, InterruptedException {
        setDirection(FORWARD_LEFT, BACKWARD_RIGHT);
        int steps = (int) ((5 * STEPS_PER_DEGREE) / 2.0 + 0.5) * 2;
        currentVelocity = accelerationTurn * timePerPush;
        for (int i = 0; i < 72; i++) {
            if (seesIr) {
                break;
            }
            sequencer.start();
            accelerateUp(steps, accelerationTurn, 1, 1, minPeriod);
            waitToFinish();
            sequencer.pause();
        }
        currentVelocity = accelerationStraight * timePerPush;
    }

    public void dragRace() throws ConnectionLostException, InterruptedException {
        goStraightFast();//TODO fix
        goBackward(270);
        spinRight(180);
        currentVelocity = timePerPush * accelerationStraight;
        goStraightFast();
        sequencer.close();
    }

    public void test() throws ConnectionLostException, InterruptedException {
        spinRight(90);
//        if (seesIr) {
//            log1("See IR");
//        }
        SystemClock.sleep(250);
    }

    public void testUltrasonic() throws ConnectionLostException, InterruptedException {
        //		readUltrasonic();
        log1("Front: " + sonar.getFrontDistance());
        log1("Right: " + sonar.getRightDistance());
        log1("Back Left: " + (float) sonar.getBackLeftDistance());
        log1("Front Left: " + (float) sonar.getFrontLeftDistance());
        //		float difference = (float) (sonar.getFrontLeftDistance() - sonar.getBackLeftDistance());
        //		log1("Difference = " + difference);
        //		if (difference > 0) {
        //			log1("TILTED RIGHT");
        //
        //		} else if (difference < 0) {
        //			log1("TILTED LEFT");
        //		} else {
        //			log1("PERFECTLY STRAIGHT");
        //		}
        log1("");
        SystemClock.sleep(500);
    }

    public void showcase1() {
        while (true) {
            goForward(200);
            goBackward(200);
            goForward(200);
            goBackward(200);
            spinLeft(30);
            spinRight(60);
            spinLeft(60);
            spinRight(60);
            spinLeft(30);
        }
    }

    public void showcase2() throws ConnectionLostException, InterruptedException {
        setDirection(FORWARD_LEFT, FORWARD_RIGHT);
        double span = 25;//mm
        int steps = (int) ((span * STEPS_PER_MM) / 2.0 + 0.5) * 2;
//            int minPeriod = Math.max((int) (11875 / (double) steps), this.minPeriod);//The number 11875 ensures that the duration is never shorter than 190 (gives time for sensor reading)
//            sonar.readFront();
        startSequencer();
        double stopDistance = 200;
        for (int i = 0; i < 10; i++) {
            accelerateUp(steps, accelerationStraight, 1, 1, minPeriod);
            log1(String.valueOf(sonar.getFrontDistance()));
            waitToFinish();
            if (sonar.frontDistances[0] <= stopDistance && sonar.frontDistances[1] <= stopDistance) {
                log1("WALL");
                break;
            }
        }
        if (accelerateDownTo0(accelerationSlowDown)) {
            waitToFinish();
        }
        sequencer.pause();
        SystemClock.sleep(500);
        spinRight(180);
    }

    private void goStraightFast() throws ConnectionLostException, InterruptedException {
        setDirection(FORWARD_LEFT, FORWARD_RIGHT);
        double span = 300;
        int steps = (int) ((span * STEPS_PER_MM) / 2.0 + 0.5) * 2;
        int minPeriod = Math.max((int) (11875 / (double) steps), this.minPeriod);
        log1("minimum period = " + minPeriod);
        sonar.readExcludeRight();
        double difference;
        startSequencer();
        double slowDownDistance = 1400;
        double prevFrontDistance = slowDownDistance;
        while (sonar.getFrontDistance() >= slowDownDistance || prevFrontDistance >= slowDownDistance) {
            prevFrontDistance = sonar.getFrontDistance();
            difference = sonar.getFrontLeftDistance() - sonar.getBackLeftDistance();
            log1("difference = " + difference);
            if (difference > 0) {
                accelerateUp(steps, accelerationStraight, 1.04, 1, minPeriod);//TODO Experiment with 1.05 and 1
            } else if (difference < 0) {
                accelerateUp(steps, accelerationStraight, 1, 1.04, minPeriod);//TODO vice versa for this ones
            } else {
                accelerateUp(steps, accelerationStraight, 1, 1, minPeriod);
            }
            waitToFinishAndReadUltraExcludeRight();
        }
        log1("Slow down!");
        accelerateDownTo0(accelerationSlowDown);
        waitToFinish();
        sequencer.pause();
    }

    private void startSequencer() throws ConnectionLostException, InterruptedException {
        sequencer.start();
        sequencer.waitEventType(Sequencer.Event.Type.STALLED);
    }

    private void readUltrasonic() throws ConnectionLostException, InterruptedException {
        //		try {
        sonar.readAll();
        //		} catch (InterruptedException e) {
        //			readUltrasonic();
        //		}
    }

    private void alignWith2LeftSensors() throws ConnectionLostException, InterruptedException {
        readUltrasonic();
        float difference = (float) (sonar.getFrontLeftDistance() - sonar.getBackLeftDistance());
        if (difference > 0) {
            while (difference > 0) {
                spinLeftAndReadUltra(5);
                difference = (float) (sonar.getFrontLeftDistance() - sonar.getBackLeftDistance());
            }
        } else if (difference < 0) {
            while (difference < 0) {
                spinRightAndReadUltra(5);
                difference = (float) (sonar.getFrontLeftDistance() - sonar.getBackLeftDistance());
            }

        }
    }

    private void log1(String msg) {
        if (logPriority >= 1)
            MainActivity.activity.log(msg);
    }

    private void log2(String msg) {
        if (logPriority >= 2)
            MainActivity.activity.log(msg);
    }

    private void log3(String msg) {
        if (logPriority >= 3)
            MainActivity.activity.log(msg);
    }

    private void victoryDance() throws Exception {
        log1("--VICTORY!!!--");
        spinRight(1800);
    }

    private void solveMaze() throws Exception {
        boolean foundMatch = false;
        for (int i = gridSquares.size() - 1; i >= 0; i--) {
            // log1("Have been in the forloop " + i + "time(s).");
            // log1("the current grid is: " + gridSquares2.get(counter).getX() + ", " + gridSquares2.get(counter).getY());
            // log1("it's comparing it to the grid of last run: " + gridSquares.get(i).getX() + ", " + gridSquares.get(i).getY());
            if (gridSquares2.get(counter).getX() == gridSquares.get(i).getX() && gridSquares2.get(counter).getY() == gridSquares.get(i).getY()) {
                foundMatch = true;
                goDirection(gridSquares.get(i).getDirection());
                log2("At coordinates: " + gridSquares.get(counter).getX() + ", " + gridSquares.get(counter).getY());
                if (gridSquares2.get(counter).getDirection() == NORTH) {
                    log2("The Direction is: North");
                } else if (gridSquares2.get(counter).getDirection() == SOUTH) {
                    log2("The Direction is: South");
                } else if (gridSquares2.get(counter).getDirection() == EAST) {
                    log2("The Direction is: East");
                } else if (gridSquares2.get(counter).getDirection() == WEST) {
                    log2("The Direction is: West");
                }
                currentVelocity = accelerationStraight * timePerPush;
                followWallOneCell();
                if (seesIr) {//(i == gridSquares.size() - 1) {
                    readUltrasonic();
                    forwardAlign();
                    victoryDance();
                    doSolution = false;
                    //					((TextView) MainActivity.activity.findViewById(R.id.toggleSolutionButton)).setText("Start Solution Run");
                    log2("--PRESS BUTTON TO REDO SOLUTION RUN--");
                }
                updateGrid2();
                break;
            }
        }
        if (!foundMatch) {
            log2("--NO MATCH FOR CURRENT COORDINATES FOUND. CONTINUIING BY MAPPING--");
            MainActivity.activity.clickSolution = false;
            MainActivity.activity.clickMap = true;
            doSolution = false;
            doneMapping = false;
            counter--;
            if (gridSquares2.get(counter).getDirection() == NORTH) {
                gridSquares.add(new GridSquare(gridSquares2.get(counter).getX(), gridSquares2.get(counter).getY() + 1, gridSquares2.get(counter).getDirection()));
            } else if (gridSquares2.get(counter).getDirection() == EAST) {
                gridSquares.add(new GridSquare(gridSquares2.get(counter).getX() + 1, gridSquares2.get(counter).getY(), gridSquares2.get(counter).getDirection()));
            } else if (gridSquares2.get(counter).getDirection() == SOUTH) {
                gridSquares.add(new GridSquare(gridSquares2.get(counter).getX(), gridSquares2.get(counter).getY() - 1, gridSquares2.get(counter).getDirection()));
            } else if (gridSquares2.get(counter).getDirection() == WEST) {
                gridSquares.add(new GridSquare(gridSquares2.get(counter).getX() - 1, gridSquares2.get(counter).getY(), gridSquares2.get(counter).getDirection()));
            }
            counter = gridSquares.size() - 1;
            //			((TextView) MainActivity.activity.findViewById(R.id.toggleSolutionButton)).setText("Start Solution Run");
            //			MainActivity.activity.findViewById(R.id.toggleSolutionButton).setVisibility(View.GONE);
            //			MainActivity.activity.findViewById(R.id.finishMapButton).setVisibility(View.VISIBLE);
        }

    }

    private void updateGrid2() {
        if (gridSquares2.get(counter).getDirection() == NORTH) {
            gridSquares2.add(new GridSquare(gridSquares2.get(counter).getX(), gridSquares2.get(counter).getY() + 1, gridSquares2.get(counter).getDirection()));
        } else if (gridSquares2.get(counter).getDirection() == EAST) {
            gridSquares2.add(new GridSquare(gridSquares2.get(counter).getX() + 1, gridSquares2.get(counter).getY(), gridSquares2.get(counter).getDirection()));
        } else if (gridSquares2.get(counter).getDirection() == SOUTH) {
            gridSquares2.add(new GridSquare(gridSquares2.get(counter).getX(), gridSquares2.get(counter).getY() - 1, gridSquares2.get(counter).getDirection()));
        } else if (gridSquares2.get(counter).getDirection() == WEST) {
            gridSquares2.add(new GridSquare(gridSquares2.get(counter).getX() - 1, gridSquares2.get(counter).getY(), gridSquares2.get(counter).getDirection()));
        }
        counter++;
    }

    private void goDirection(int direction) throws Exception {
        boolean frontAligned = false;
        if (direction != gridSquares2.get(counter).getDirection()) {
            if (!aligned) {
                frontAligned = checkForBumpAlignment();
            } else {
                adjustFrontDistance();
            }
            if (direction == (gridSquares2.get(counter).getDirection() + 90) % 360) {

                if (sonar.getRightDistance() < tooCloseToTurnDistance) {
                    forwardAlignRight();
                }
                solutionSpinLeft90();
            } else if (direction == (gridSquares2.get(counter).getDirection() + 180) % 360) {
                solutionSpinRight90();
                solutionSpinRight90();
                logSolutionFailed();
            } else if (direction == (gridSquares2.get(counter).getDirection() + 270) % 360) {
                if (sonar.getBackLeftDistance() < tooCloseToTurnDistance) {
                    forwardAlignLeft();
                }
                solutionSpinRight90();
            }
            currentVelocity = accelerationStraight * timePerPush;
        }
    }

    private void logSolutionFailed() {
        for (int i = 0; i < 10; i++) {
            log2("Solution Failed!!!");
        }
    }

    private void forwardAlignRight() throws ConnectionLostException, InterruptedException, Exception {
        spinRight(90);
        readUltrasonic();
        forwardAlign();
        spinLeft(90);
    }

    private void forwardAlignLeft() throws ConnectionLostException, InterruptedException, Exception {
        spinLeft(90);
        readUltrasonic();
        forwardAlign();
        spinRight(90);
    }

    private void mapMazeLeft() throws Exception {
        // sonar.getRightDistance() = sonar.getRightDistance();
        if (!aligned || seesIr || !firstBumpAlign) {
            checkForBumpAlignment();
        } else {
            adjustFrontDistance();
        }
        log1("left distance: " + sonar.getBackLeftDistance());
        log1("front distance: " + sonar.getFrontDistance());
        if (sonar.getBackLeftDistance() >= openSpaceSideDistance || sonar.getFrontLeftDistance() >= openSpaceSideDistance) {
            log1("Found space to the left");
            if (sonar.getRightDistance() < tooCloseToTurnDistance) {
                forwardAlignRight();
            }
            mapSpinLeft90();
            //			fixSelf();
            log2("The current grid is: " + gridSquares.get(counter).getX() + ", " + gridSquares.get(counter).getY());
            if (gridSquares.get(counter).getDirection() == NORTH) {
                log2("The Direction is: North");
            } else if (gridSquares.get(counter).getDirection() == SOUTH) {
                log2("The Direction is: South");
            } else if (gridSquares.get(counter).getDirection() == EAST) {
                log2("The Direction is: East");
            } else if (gridSquares.get(counter).getDirection() == WEST) {
                log2("The Direction is: West");
            }
            //			readUltrasonic();
            //			checkForBackwardAlignment();
            goOneCell();
            // goForward(-300, 50);
            // spinRight(300, 360);
        } else if (sonar.getFrontDistance() >= openSpaceFrontDistance) {
            log1("No space to the left. Going forward");
            log2("The current grid is: " + gridSquares.get(counter).getX() + ", " + gridSquares.get(counter).getY());
            if (gridSquares.get(counter).getDirection() == NORTH) {
                log2("The Direction is: North");
            } else if (gridSquares.get(counter).getDirection() == SOUTH) {
                log2("The Direction is: South");
            } else if (gridSquares.get(counter).getDirection() == EAST) {
                log2("The Direction is: East");
            } else if (gridSquares.get(counter).getDirection() == WEST) {
                log2("The Direction is: West");
            }
            goOneCell();
        } else {
            if (sonar.getRightDistance() < openSpaceSideDistance) {
                log1("In dead end.");
                if (sonar.getBackLeftDistance() < tooCloseToTurnDistance) {
                    mapSpinLeft90();
                    readUltrasonic();
                    forwardAlign();
                    mapSpinLeft90();
                } else if (sonar.getRightDistance() < tooCloseToTurnDistance) {
                    mapSpinRight90();
                    readUltrasonic();
                    forwardAlign();
                    mapSpinRight90();
                } else if (sonar.getRightDistance() > sonar.getBackLeftDistance()) {
                    mapSpinLeft90();
                    mapSpinLeft90();
                } else if (sonar.getBackLeftDistance() > sonar.getRightDistance()) {
                    mapSpinRight90();
                    mapSpinRight90();
                }
            } else {
                log1("No space to the left or in front. Turning right.");
                if (sonar.getBackLeftDistance() < tooCloseToTurnDistance) {
                    forwardAlignLeft();
                }
                mapSpinRight90();
            }
        }
    }

    private void mapMazeRight() throws Exception {
        // sonar.getRightDistance() = sonar.getRightDistance();
        if (!aligned || seesIr || !firstBumpAlign) {
            checkForBumpAlignment();
        } else {
            adjustFrontDistance();
        }
        log1("left distance: " + sonar.getBackLeftDistance());
        log1("front distance: " + sonar.getFrontDistance());
        log2("right distance = " + sonar.getRightDistance());
        if (sonar.getRightDistance() >= openSpaceSideDistance) {
            log1("Found space to the right");
            if (sonar.getBackLeftDistance() < tooCloseToTurnDistance) {
                forwardAlignLeft();
            }
            mapSpinRight90();
            //			fixSelf();
            log2("The current grid is: " + gridSquares.get(counter).getX() + ", " + gridSquares.get(counter).getY());
            if (gridSquares.get(counter).getDirection() == NORTH) {
                log2("The Direction is: North");
            } else if (gridSquares.get(counter).getDirection() == SOUTH) {
                log2("The Direction is: South");
            } else if (gridSquares.get(counter).getDirection() == EAST) {
                log2("The Direction is: East");
            } else if (gridSquares.get(counter).getDirection() == WEST) {
                log2("The Direction is: West");
            }
            //			readUltrasonic();
            //			checkForBackwardAlignment();
            goOneCell();
            // goForward(-300, 50);
            // spinRight(300, 360);
        } else if (sonar.getFrontDistance() >= openSpaceFrontDistance) {
            log1("No space to the left. Going forward");
            log2("The current grid is: " + gridSquares.get(counter).getX() + ", " + gridSquares.get(counter).getY());
            if (gridSquares.get(counter).getDirection() == NORTH) {
                log2("The Direction is: North");
            } else if (gridSquares.get(counter).getDirection() == SOUTH) {
                log2("The Direction is: South");
            } else if (gridSquares.get(counter).getDirection() == EAST) {
                log2("The Direction is: East");
            } else if (gridSquares.get(counter).getDirection() == WEST) {
                log2("The Direction is: West");
            }
            goOneCell();
        } else {
            if (sonar.getBackLeftDistance() < openSpaceSideDistance && sonar.getFrontLeftDistance() < openSpaceSideDistance) {
                log1("In dead end.");
                if (sonar.getBackLeftDistance() < tooCloseToTurnDistance) {
                    mapSpinLeft90();
                    readUltrasonic();
                    forwardAlign();
                    mapSpinLeft90();
                } else if (sonar.getRightDistance() < tooCloseToTurnDistance) {
                    mapSpinRight90();
                    readUltrasonic();
                    forwardAlign();
                    mapSpinRight90();
                } else if (sonar.getRightDistance() > sonar.getBackLeftDistance()) {
                    mapSpinLeft90();
                    mapSpinLeft90();
                } else if (sonar.getBackLeftDistance() > sonar.getRightDistance()) {
                    mapSpinRight90();
                    mapSpinRight90();
                }
            } else {
                log1("No space to the left or in front. Turning right.");
                if (sonar.getRightDistance() < tooCloseToTurnDistance) {
                    forwardAlignRight();
                }
                mapSpinLeft90();
            }
        }
    }

    private void adjustFrontDistance() throws ConnectionLostException, InterruptedException {
        readUltrasonic();
        double firstFrontDistance = sonar.getFrontDistance();
        SystemClock.sleep(100);
        readUltrasonic();
        if (sonar.getFrontDistance() < openSpaceFrontDistance && firstFrontDistance < openSpaceFrontDistance) {
            if (sonar.getFrontDistance() < 250) {
                goBackward(257 - sonar.getFrontDistance());
            } else if (sonar.getFrontDistance() > 264) {
                goForward(sonar.getFrontDistance() - 257);
            }
        }
        readUltrasonic();
    }

    private boolean checkForBumpAlignment() throws Exception {
        readUltrasonic();
        double firstFrontDistance = sonar.getFrontDistance();
        SystemClock.sleep(100);
        readUltrasonic();
        log1(String.valueOf(sonar.getFrontDistance()));
        if (sonar.getFrontDistance() < openSpaceFrontDistance && firstFrontDistance < openSpaceFrontDistance || seesIr) {
            log1("wall in front. Aligning...");
            forwardAlign();
            return true;
        } /*
         * else if (rearSensor < wallDistance) { backwardAlign(); readUltrasonic(); }
		 */
        return false;
    }

    private void forwardAlign() throws Exception {
        goForwardSlow(sonar.getFrontDistance() + 20);
        goBackward(250);
        readUltrasonic();
        if (sonar.getFrontLeftDistance() < openSpaceSideDistance && sonar.getBackLeftDistance() < openSpaceSideDistance) {
            optimalDifference = sonar.getFrontLeftDistance() - sonar.getBackLeftDistance();
            firstBumpAlign = true;
        }
    }

    private void goOneCell() throws Exception {
        followWallOneCell();
        currentVelocity = timePerPush * accelerationStraight;
        if (!doneMapping) {
            if (gridSquares.get(counter).getDirection() == NORTH) {
                gridSquares.add(new GridSquare(gridSquares.get(counter).getX(), gridSquares.get(counter).getY() + 1, gridSquares.get(counter).getDirection()));
            } else if (gridSquares.get(counter).getDirection() == EAST) {
                gridSquares.add(new GridSquare(gridSquares.get(counter).getX() + 1, gridSquares.get(counter).getY(), gridSquares.get(counter).getDirection()));
            } else if (gridSquares.get(counter).getDirection() == SOUTH) {
                gridSquares.add(new GridSquare(gridSquares.get(counter).getX(), gridSquares.get(counter).getY() - 1, gridSquares.get(counter).getDirection()));
            } else if (gridSquares.get(counter).getDirection() == WEST) {
                gridSquares.add(new GridSquare(gridSquares.get(counter).getX() - 1, gridSquares.get(counter).getY(), gridSquares.get(counter).getDirection()));
            }
            counter++;
            //			MainActivity.activity.savePrefs("x" + counter, gridSquares.get(counter).getX());
            //			MainActivity.activity.savePrefs("y" + counter, gridSquares.get(counter).getY());
            //			MainActivity.activity.savePrefs("d" + counter, gridSquares.get(counter).getDirection());
            log1("New GridSquare added");
        } else {
            log1("Finish Mapping Run pressed. Excluding the next GridSquare from map.");
        }
    }

    private void followWallOneCell() throws Exception {//TODO Make this method awesome
        setDirection(FORWARD_LEFT, FORWARD_RIGHT);
        int preferredSpan = 50;
        int numOfLoops = CELL_SIZE / (int) preferredSpan;
        double span = CELL_SIZE / (double) (numOfLoops);
        log1("span = " + span);
        int totalSteps = (int) ((CELL_SIZE * STEPS_PER_MM) / 2.0 + 0.5) * 2;
        int steps = (int) ((span * STEPS_PER_MM) / 2.0 + 0.5) * 2;
        int minPeriod = (int) (15625 / (double) steps);
        double prevRightDistance1 = openSpaceSideDistance + 1;
        double prevRightDistance2 = openSpaceSideDistance + 1;
        double prevRightDistance3 = openSpaceSideDistance + 1;
        double prevRightDistance4 = openSpaceSideDistance + 1;
        readUltrasonic();
        double tilt;
        double ratio;
        double distanceFromCenter;
        boolean sawRight;
        boolean correctedDistanceLeft = false;
        boolean correctedDistanceRight = false;
        startSequencer();
        for (int i = 0; i < numOfLoops; i++) {
            aligned = false;
            sawRight = false;
            if (i == numOfLoops - 1) {
                steps = totalSteps - (steps * (numOfLoops - 1));
            }
            log1("front left = " + sonar.getFrontLeftDistance());
            log1("back left = " + sonar.getBackLeftDistance());
            log1("right = " + sonar.getRightDistance());
            log1("");
            if (sonar.getBackLeftDistance() < openSpaceSideDistance && sonar.getFrontLeftDistance() < openSpaceSideDistance) {
                tilt = sonar.getFrontLeftDistance() - sonar.getBackLeftDistance();
                ratio = Math.min(sonar.getFrontLeftDistance() / sonar.getBackLeftDistance(), sonar.getBackLeftDistance() / sonar.getFrontLeftDistance());
                distanceFromCenter = sonar.getBackLeftDistance() - desiredWallDistance;
                aligned = true;
            } else if (sonar.getRightDistance() < openSpaceSideDistance) {
                sawRight = true;
                distanceFromCenter = -sonar.getRightDistance() + desiredWallDistance;
                //								if (sonar.getRightDistance() < openSpaceDistance && prevRightDistance4 < openSpaceDistance) {
                //									tilt = sonar.getRightDistance() - prevRightDistance2;
                //									ratio = Math.min(sonar.getRightDistance() / prevRightDistance4, prevRightDistance4 / sonar.getRightDistance());
                //								} else {
                tilt = 0;
                ratio = 0;
                //
            } else {
                tilt = 0;
                ratio = 0;
                distanceFromCenter = 0;
            }
            log1("ratio = " + ratio);
            log1("distanceFromCenter = " + distanceFromCenter);
            log2("difference = " + String.valueOf(tilt));
            log2("optimal difference = " + optimalDifference);

            if (tilt > optimalDifference + 1 /* && distanceFromCenter > tooCloseWallDistance - desiredWallDistance */ && (ratio > 0.86 || correctedDistanceRight)) {
                accelerateUp(steps, accelerationStraight, 0 + ((1 - ratio) + 1) * 1, 0 + ratio * 1, minPeriod);
                log3("ratio = " + String.valueOf(ratio));
                correctedDistanceLeft = false;
                correctedDistanceRight = false;
            } else if (tilt < optimalDifference - 1 /* && distanceFromCenter < tooFarWallDistance - desiredWallDistance */ && (ratio > 0.86 || correctedDistanceLeft)) {
                accelerateUp(steps, accelerationStraight, 0 + ratio * 1, 0 + ((1 - ratio) + 1) * 1, minPeriod);
                log3("ratio = " + String.valueOf(ratio));
                correctedDistanceLeft = false;
                correctedDistanceRight = false;
            } else if (distanceFromCenter + desiredWallDistance > tooFarWallDistance && !correctedDistanceLeft) {
                //				ratio = tooFarWallDistance / (distanceFromCenter + desiredWallDistance);
                accelerateUp(steps, accelerationStraight, 1.25, 0.75, minPeriod);
                correctedDistanceLeft = true;
                correctedDistanceRight = false;
            } else if (distanceFromCenter + desiredWallDistance < tooCloseWallDistance && !sawRight && !correctedDistanceRight) {
                //				ratio = (distanceFromCenter + desiredWallDistance) / tooCloseWallDistance;
                accelerateUp(steps, accelerationStraight, 0.75, 1.25, minPeriod);
                correctedDistanceRight = true;
                correctedDistanceLeft = false;
            } else {
                accelerateUp(steps, accelerationStraight, 1, 1, minPeriod);
                log3("not correcting");
                correctedDistanceLeft = false;
                correctedDistanceRight = false;
            }
            prevRightDistance4 = prevRightDistance3;
            prevRightDistance3 = prevRightDistance2;
            prevRightDistance2 = prevRightDistance1;
            prevRightDistance1 = sonar.getRightDistance();
            waitToFinishAndReadUltra();
        }
        sequencer.pause();
    }

    private void runRobotTest() {
        int sinePeriod = 0;
        try {
            stepperRightFMspeedCue.period = 400;
            stepperLeftFMspeedCue.period = 400;
            openSequencer();
            startSequencer();
            while (sequencer.available() > 0) // fill cue
            {
                {
                    /* Untested */
                    // for (int i = 0; i < 314; i++) {
                    // sinePeriod = (int) ((MAX_FM_SPEED_PERIOD
                    // * (1 + Math.cos(1 / 100)) + MIN_FM_SPEED_PERIOD));
                    // stepperRightFMspeedCue.period = sinePeriod;
                    // stepperLeftFMspeedCue.period = sinePeriod;
                    // sequencer.push(cueList, 600);
                    // }
                    sequencer.push(cueList, 1000);
                }
            }

        } catch (Exception e) {
        }
    }

    private void goBackward(double millimeters) {
        try {
            setDirection(BACKWARD_LEFT, BACKWARD_RIGHT);
            int steps = (int) ((millimeters * STEPS_PER_MM) / 2.0 + 0.5) * 2;
            go(steps, accelerationStraight);
        } catch (Exception e) {
        }
    }

    private void goForward(double millimeters) {
        try {
            setDirection(FORWARD_LEFT, FORWARD_RIGHT);
            int steps = (int) ((millimeters * STEPS_PER_MM) / 2.0 + 0.5) * 2;
            go(steps, accelerationStraight);
        } catch (Exception e) {
        }
    }

    private void goForwardAndReadUltra(double millimeters) {
        try {
            setDirection(FORWARD_LEFT, FORWARD_RIGHT);
            int steps = (int) ((millimeters * STEPS_PER_MM) / 2.0 + 0.5) * 2;
        } catch (Exception e) {
        }
    }

    private void goForwardSlow(double millimeters) {
        try {
            setDirection(FORWARD_LEFT, FORWARD_RIGHT);
            int steps = (int) ((millimeters * STEPS_PER_MM) / 2.0 + 0.5) * 2;
            go(steps, accelerationTurn);
        } catch (Exception e) {
        }
    }

    private void go(final int steps, double acceleration) throws ConnectionLostException, InterruptedException {
        accelerateUpAndDown(steps, acceleration);
        waitToFinish();
        sequencer.pause();
    }

    private void accelerateUpAndDown(final int steps, double acceleration) throws ConnectionLostException, InterruptedException {
        double velocity = acceleration * timePerPush;
        int period = 0;
        int currentSteps = 0;
        int lastDuration = timePerPush;
        startSequencer();
        while (currentSteps < steps / 2.0) {
            period = Math.max((int) (1 / velocity), minPeriod);
            stepperRightFMspeedCue.period = period;
            stepperLeftFMspeedCue.period = period;
            if (currentSteps + timePerPush / period > steps / 2.0) {
                lastDuration = (int) ((steps / 2.0 - currentSteps) * period);
                pushCue(lastDuration);
                currentSteps += lastDuration / period;
                velocity += acceleration * lastDuration;
                break;
            } else {
                pushCue(timePerPush);
                currentSteps += timePerPush / period;
                velocity += acceleration * timePerPush;
            }
        }
        velocity -= acceleration * lastDuration;
        pushCue(lastDuration);
        currentSteps += (int) (lastDuration / period);
        velocity -= acceleration * timePerPush;
        while (currentSteps < steps) {
            period = Math.max((int) (1 / velocity), minPeriod);
            stepperRightFMspeedCue.period = period;
            stepperLeftFMspeedCue.period = period;
            if (currentSteps + timePerPush / period > steps) {
                lastDuration = (steps - currentSteps) * period;
                pushCue(lastDuration);
                currentSteps += lastDuration / period;
                break;
            } else {
                pushCue(timePerPush);
                currentSteps += timePerPush / period;
            }
            velocity -= acceleration * timePerPush;
        }
    }

    private void goAndReadUltra(final int steps, double acceleration) throws ConnectionLostException, InterruptedException {
        accelerateUpAndDown(steps, acceleration);
        waitToFinishAndReadUltra();
        sequencer.pause();
    }

    //@param wheelOffset - 0 for none, < 0 for left wheel, > 0 for right wheel. -1 means don't turn left wheel. 1 means don't turn right wheel. Anything else
    private void accelerateUp(final int steps, double acceleration, double leftWheelOffset, double rightWheelOffset, int minPeriod) throws ConnectionLostException, InterruptedException {
        double velocity = currentVelocity;
        int period = 0;
        int currentSteps = 0;
        int lastDuration = timePerPush;
        while (currentSteps < steps) {
            period = Math.max((int) (1 / velocity), minPeriod);
            stepperRightFMspeedCue.period = (int) (period * rightWheelOffset);
            stepperLeftFMspeedCue.period = (int) (period * leftWheelOffset);
            if (currentSteps + timePerPush / period > steps) {
                lastDuration = (int) ((steps - currentSteps) * period);
                pushCue(lastDuration);
                currentSteps += lastDuration / period;
                velocity += acceleration * lastDuration;
                break;
            } else {
                pushCue(timePerPush);
                currentSteps += timePerPush / period;
                velocity += acceleration * timePerPush;
            }
            //			amountOfOffset -= 0.5;
            //			if (amountOfOffset < 0) {
            //				amountOfOffset = 0;
            //			}
        }
        currentVelocity = Math.min(velocity, 1 / (double) minPeriod);
    }

    private boolean accelerateDownTo0(double acceleration) throws ConnectionLostException, InterruptedException {
        double velocity = currentVelocity;
        int period;
        velocity -= acceleration * timePerPush;
        boolean slowedDown = false;
        while (velocity >= timePerPush * acceleration) {
            period = Math.max((int) (1 / velocity), minPeriod);
            stepperRightFMspeedCue.period = period;
            stepperLeftFMspeedCue.period = period;
            pushCue(timePerPush);
            velocity -= acceleration * timePerPush;
            slowedDown = true;
        }
//        pushCue(timePerPush * 9);
        currentVelocity = accelerationStraight * timePerPush;
        return slowedDown;
    }

    // public void followWallLeft(int distance) throws Exception {
    // int span = 200;
    // distance = (int) (distance / (double) MICROS_PER_MILI);
    //
    // readUltrasonic();
    // int prevDistance = sonar.getLeftDistance();
    // goForward(span);
    // while (true) {
    // readUltrasonic();
    // log1(String.valueOf(sonar.getLeftDistance()));
    // if (sonar.getLeftDistance() > prevDistance && sonar.getLeftDistance() >
    // distance) {
    // prevDistance = sonar.getLeftDistance();
    // double angle = Math.acos(Math.abs(sonar.getLeftDistance() - prevDistance)
    // / span);
    // spinLeft(angle);
    // goForward(span);
    // } else if (sonar.getLeftDistance() < prevDistance &&
    // sonar.getLeftDistance() < distance) {
    // prevDistance = sonar.getLeftDistance();
    // double angle = Math.acos(Math.abs(sonar.getLeftDistance() - prevDistance)
    // / span);
    // spinRight(angle);
    // goForward(span);
    // } else {
    // prevDistance = sonar.getLeftDistance();
    // goForward(span);
    // }
    // }
    // }

    private void followWallLeft(int wallDistance, int millimeters) throws Exception {
        int preferredSpan = 200;
        int numOfLoops = millimeters / (int) preferredSpan;
        double span = millimeters / (double) (numOfLoops);
        readUltrasonic();
        double prevDistance = sonar.getBackLeftDistance();
        goForward(span);
        for (int i = 0; i < numOfLoops - 1; i++) {
            readUltrasonic();
            log1("left distance :" + sonar.getBackLeftDistance());
            if (sonar.getBackLeftDistance() > prevDistance && sonar.getBackLeftDistance() > wallDistance) {
                double angle = Math.toDegrees(Math.asin(Math.abs(sonar.getBackLeftDistance() - prevDistance) / span));
                log1("angle is " + angle);
                angle = 7;//temporary
                if (angle > 4) {
                    spinLeft((int) angle);
                }
                prevDistance = sonar.getBackLeftDistance();
                goForward(span);
            } else if (sonar.getBackLeftDistance() < prevDistance && sonar.getBackLeftDistance() < wallDistance) {
                double angle = Math.toDegrees(Math.asin(Math.abs(sonar.getBackLeftDistance() - prevDistance) / span));
                log1("angle is " + angle);
                angle = 7;//temporary
                if (angle > 4) {
                    spinRight((int) angle);
                }
                prevDistance = sonar.getBackLeftDistance();
                goForward(span);
            } else {
                prevDistance = sonar.getBackLeftDistance();
                goForward(span);
            }
        }
    }

    private void followWallLeftImproved() {

    }

    // public void goForwardAndCheckForWall(double speed, int duration, int
    // distanceFromWall) {
    // try {
    // stepperRightFMspeedCue.period = (int) (1000 / speed);
    // stepperLeftFMspeedCue.period = (int) (1000 / speed);
    // forwardCue(duration);
    // sequencer.start();
    // waitToFinishOrForWall(distanceFromWall);
    // sequencer.stop();
    // } catch (Exception e) {
    // }
    // }

    // private void forwardCue(int duration) throws ConnectionLostException,
    // InterruptedException {
    // setDirection(FORWARD_LEFT, FORWARD_RIGHT);
    // pushCue(duration);
    // }

    private void pushCue(int duration) throws ConnectionLostException, InterruptedException {
        sequencer.push(cueList, duration);
    }

    private void spinLeft(double angle) {
        try {
            setDirection(BACKWARD_LEFT, FORWARD_RIGHT);
            int steps = (int) ((angle * STEPS_PER_DEGREE) / 2.0 + 0.5) * 2;
            go(steps, accelerationTurn);
        } catch (Exception e) {
        }
    }

    private void spinLeftAndReadUltra(double angle) {
        try {
            setDirection(BACKWARD_LEFT, FORWARD_RIGHT);
            int steps = (int) ((angle * STEPS_PER_DEGREE) / 2.0 + 0.5) * 2;
            goAndReadUltra(steps, accelerationTurn);
        } catch (Exception e) {
        }
    }

    private void mapSpinLeft90() {
        gridSquares.get(counter).setDirection((gridSquares.get(counter).getDirection() + 90) % 360);
        spinLeft(90);
    }

    private void solutionSpinLeft90() {
        gridSquares2.get(counter).setDirection((gridSquares2.get(counter).getDirection() + 90) % 360);
        spinLeft(90);
    }

    private void oldSpinLeft(int period, double angle) {
        try {
            stepperRightFMspeedCue.period = period;
            stepperLeftFMspeedCue.period = period;
            setDirection(BACKWARD_LEFT, FORWARD_RIGHT);
            spinRightCue((int) (angle * STEPS_PER_DEGREE * period));
            startSequencer();
            waitToFinish();
            sequencer.pause();
        } catch (Exception e) {
        }
    }

    private void spinLeftCue(int duration) throws ConnectionLostException, InterruptedException {
        setDirection(BACKWARD_LEFT, FORWARD_RIGHT);
        pushCue(duration);
    }

    private void spinRight(double angle) {
        try {
            setDirection(FORWARD_LEFT, BACKWARD_RIGHT);
            int steps = (int) ((angle * STEPS_PER_DEGREE) / 2.0 + 0.5) * 2;
            go(steps, accelerationTurn);
        } catch (Exception e) {
        }
    }

    private void spinRightAndReadUltra(double angle) {
        try {
            setDirection(FORWARD_LEFT, BACKWARD_RIGHT);
            int steps = (int) ((angle * STEPS_PER_DEGREE) / 2.0 + 0.5) * 2;
            goAndReadUltra(steps, accelerationTurn);
        } catch (Exception e) {
        }
    }

    private void mapSpinRight90() {
        gridSquares.get(counter).setDirection((gridSquares.get(counter).getDirection() + 270) % 360);
        spinRight(90);
    }

    private void solutionSpinRight90() {
        gridSquares2.get(counter).setDirection((gridSquares2.get(counter).getDirection() + 270) % 360);
        spinRight(90);
    }

    private void oldSpinRight(int period, double angle) {
        try {
            stepperRightFMspeedCue.period = period;
            stepperLeftFMspeedCue.period = period;
            setDirection(FORWARD_LEFT, BACKWARD_RIGHT);
            spinRightCue((int) (angle * STEPS_PER_DEGREE * period));
            startSequencer();
            waitToFinish();
            sequencer.pause();
        } catch (Exception e) {
        }
    }

    // private void spinRightForever(double speed) {
    // try {
    // stepperRightFMspeedCue.period = (int) (1000 / speed);
    // stepperLeftFMspeedCue.period = (int) (1000 / speed);
    // setDirection(FORWARD_LEFT, BACKWARD_RIGHT);
    // sequencer.manualStart(cueList);
    // } catch (Exception e) {
    // }
    // }

    private void spinRightCue(int duration) throws ConnectionLostException, InterruptedException {
        setDirection(FORWARD_LEFT, BACKWARD_RIGHT);
        pushCue(duration);
    }

    private void turnLeft(double degrees) throws ConnectionLostException, InterruptedException {
        turnSegment(degrees, true);
    }

    private void turnRight(double degrees) throws ConnectionLostException, InterruptedException {
        turnSegment(degrees, false);
    }

    private void turnSegment(double degrees, boolean left) throws ConnectionLostException, InterruptedException {
        double r = ROBOT_WIDTH;
        double l = ROBOT_LENGTH;
        double sinθ = Math.sin(Math.toRadians(degrees));
        double cosθ = Math.cos(Math.toRadians(degrees));
        double tanθ = Math.tan(Math.toRadians(degrees));
        double m = tanθ;
        double b = -(r / 2.0) * m * sinθ - (r / 2.0) + (l / 2.0) * m - (r / 2.0) * cosθ;
        double x = (-2 * b * m - Math.sqrt(4 * b * b * m * m - 4 * (m * m + 1) * (b * b - r * r))) / (2 * (m * m + 1));
        double y = m * x + b;
        double firstTurnAngle = Math.toDegrees(Math.atan(x / y));
        double secondTurnAngle = degrees + firstTurnAngle;
        log1("First turn angle: " + firstTurnAngle);
        log1("Second turn angle: " + secondTurnAngle);
        double m2 = m;
        double b2 = -(r / 2.0) * m2 * sinθ - (r / 2.0) * cosθ;
        double r2 = Math.sqrt(r * r + l * l) / 2.0;
        double x2 = (-2 * b2 * m2 + Math.sqrt(4 * b2 * b2 * m2 * m2 - 4 * (m2 * m2 + 1) * (b2 * b2 - r2 * r2))) / (2 * (m2 * m2 + 1));
        double y2 = m2 * x2 + b2;
        double sinα = Math.sin(Math.toRadians(firstTurnAngle));
        double cosα = Math.cos(Math.toRadians(firstTurnAngle));
        double m3 = -1 / m;
        double b3 = m3 * -((r / 2.0) * tanθ + (l / 2.0) - r * sinα - (r * cosα) * tanθ);
        log1(String.valueOf(b3));// forward distance should
        // be half of robot
        // length when b3 is 0.
        double y3 = (b2 * m3 - b3 * m2) / (m3 - m2);
        double x3 = (y3 - b2) / m2;
        double forwardDistance = Math.sqrt((y3 - y2) * (y3 - y2) + (x3 - x2) * (x3 - x2));
        log1("Forward distance: " + forwardDistance);
        setDirection(BACKWARD_LEFT, BACKWARD_RIGHT);
        startSequencer();
        int period = 800;
        if (left) {
            stepperRightFMspeedCue.period = period;
            stepperLeftFMspeedCue.period = 0;
        } else {
            stepperRightFMspeedCue.period = 0;
            stepperLeftFMspeedCue.period = period;
        }
        int steps = (int) (STEPS_PER_DEGREE * 2 * firstTurnAngle);
        int duration = steps * period;
        pushCue(duration);
        if (left) {
            stepperRightFMspeedCue.period = 0;
            stepperLeftFMspeedCue.period = period;
        } else {
            stepperRightFMspeedCue.period = period;
            stepperLeftFMspeedCue.period = 0;
        }
        int steps2 = (int) (STEPS_PER_DEGREE * 2 * secondTurnAngle);
        int duration2 = steps2 * period;
        pushCue(duration2);
        goForward((int) (forwardDistance * STEPS_PER_MM));
        waitToFinish();
        sequencer.pause();
    }

    public void calculateTurn(double degrees) {
        double r = ROBOT_WIDTH;
        double l = ROBOT_LENGTH;
        double sinθ = Math.sin(Math.toRadians(degrees));
        double cosθ = Math.cos(Math.toRadians(degrees));
        double tanθ = Math.tan(Math.toRadians(degrees));
        double m = tanθ;
        double b = -(r / 2.0) * m * sinθ - (r / 2.0) + (l / 2.0) * m - (r / 2.0) * cosθ;
        double x = (-2 * b * m - Math.sqrt(4 * b * b * m * m - 4 * (m * m + 1) * (b * b - r * r))) / (2 * (m * m + 1));
        double y = m * x + b;
        double firstTurnAngle = Math.toDegrees(Math.atan(x / y));
        double secondTurnAngle = degrees + firstTurnAngle;
        log1("First turn angle: " + firstTurnAngle);
        log1("Second turn angle: " + secondTurnAngle);
        double m2 = m;
        double b2 = -(r / 2.0) * m2 * sinθ - (r / 2.0) * cosθ;
        double r2 = Math.sqrt(r * r + l * l) / 2.0;
        double x2 = (-2 * b2 * m2 + Math.sqrt(4 * b2 * b2 * m2 * m2 - 4 * (m2 * m2 + 1) * (b2 * b2 - r2 * r2))) / (2 * (m2 * m2 + 1));
        double y2 = m2 * x2 + b2;
        double sinα = Math.sin(Math.toRadians(firstTurnAngle));
        double cosα = Math.cos(Math.toRadians(firstTurnAngle));
        double m3 = -1 / m;
        double b3 = m3 * (-(r / 2.0) * tanθ - (l / 2.0) + r * sinα + (r * cosα) * tanθ);
        log1(String.valueOf(b3));// forward distance should
        // be half of robot
        // length when b3 is 0.
        double y3 = (b2 * m3 - b3 * m2) / (m3 - m2);
        double x3 = (y3 - b2) / m2;
        double forwardDistance = Math.sqrt((y3 - y2) * (y3 - y2) + (x3 - x2) * (x3 - x2));
        log1("Forward distance: " + forwardDistance);
    }

    private void testTurn(double degrees, int period) throws ConnectionLostException, InterruptedException {
        setDirection(BACKWARD_LEFT, BACKWARD_RIGHT);
        stepperRightFMspeedCue.period = 0;
        stepperLeftFMspeedCue.period = period;
        int steps = (int) (STEPS_PER_DEGREE * 2 * degrees);
        int duration = steps * period;
        pushCue(duration);
        waitToFinish();
        sequencer.pause();
    }

    private void waitToFinish() throws ConnectionLostException, InterruptedException {
        sequencer.waitEventType(Sequencer.Event.Type.CUE_STARTED);
        sequencer.waitEventType(Sequencer.Event.Type.STALLED);
    }

    private boolean waitToFinishAndCheckIR() throws ConnectionLostException, InterruptedException {
        while (sequencer.getLastEvent().type.equals(Sequencer.Event.Type.STOPPED)) {
        }
        while (sequencer.getLastEvent().type.equals(Sequencer.Event.Type.STALLED)) {
            SystemClock.sleep(1);
        }
        while (!sequencer.getLastEvent().type.equals(Sequencer.Event.Type.STALLED)) {
            if (seesIr) {
                sequencer.stop();
                sequencer.close();
                openSequencer();
                return true;
            }
            SystemClock.sleep(10);
        }
        sequencer.pause();
        return false;
    }

    private void waitToFinishAndReadUltra() throws ConnectionLostException, InterruptedException {
        sequencer.waitEventType(Sequencer.Event.Type.CUE_STARTED);
        readUltrasonic();
        sequencer.waitEventType(Sequencer.Event.Type.STALLED);
    }

    private void waitToFinishAndReadUltraExcludeRight() throws ConnectionLostException, InterruptedException {
        sequencer.waitEventType(Sequencer.Event.Type.CUE_STARTED);
        sonar.readExcludeRight();
        sequencer.waitEventType(Sequencer.Event.Type.STALLED);
    }

    private void waitToFinishAndReadFrontUltra() throws ConnectionLostException, InterruptedException {
        sequencer.waitEventType(Sequencer.Event.Type.CUE_STARTED);
        sonar.readFront();
        sequencer.waitEventType(Sequencer.Event.Type.STALLED);
    }

    private void waitUntilFrontWall(int distanceFromWall) throws ConnectionLostException, InterruptedException {
        readUltrasonic();
        while (sonar.getFrontDistance() > distanceFromWall) {
            log1(String.valueOf(sonar.getFrontDistance()));
            readUltrasonic();
        }
    }

    private boolean wallInFront(int distanceFromWall) throws ConnectionLostException, InterruptedException {
        readUltrasonic();
        return sonar.getFrontDistance() <= distanceFromWall;
    }

    private void waitToFinishOrForWall(int distanceFromWall) throws ConnectionLostException, InterruptedException {
        while (sequencer.getLastEvent().type.equals(Sequencer.Event.Type.STALLED)) {
        }
        SystemClock.sleep(100);
        readUltrasonic();
        while (sonar.getFrontDistance() > distanceFromWall && !sequencer.getLastEvent().type.equals(Sequencer.Event.Type.STALLED)) {
            SystemClock.sleep(100);
            log1(String.valueOf(sonar.getFrontDistance()));
            readUltrasonic();
        }
    }

    private void setDirection(boolean leftDirection, boolean rightDirection) throws ConnectionLostException {
        leftMotorDirection.write(leftDirection);
        rightMotorDirection.write(rightDirection);
    }

    private void goMM(int mm) throws ConnectionLostException { // Go Millimeters

        double StepsPerMM = 1.000;
        double stepsPerPush = 16.0;
        int steps = (int) Math.abs((double) (mm) * StepsPerMM);
        // MaxFreq based on what you motor can handle without slipping...
        double maxFreq = 1000.0; // steps per second ran at 3000 some glitches
        // 2500 was good
        double minFreq = 16.0;
        double maxDelFreq = 4.0; // steps per second ran at 5.0 some glitches 4
        // was good
        int numPushes = (int) (steps / stepsPerPush);
        if (mm >= 0) {
            rightMotorDirection.write(FORWARD_RIGHT);
            leftMotorDirection.write(FORWARD_LEFT);
        } else {
            rightMotorDirection.write(BACKWARD_RIGHT);
            leftMotorDirection.write(BACKWARD_LEFT);
        }

        double freq;
        double maxfrac;
        double delfreq;
        double duration;
        int period;
        int i;
        // log1("In goSteps");
        freq = minFreq;
        try {
            startSequencer();
            for (i = 0; i < numPushes; i++) {
                maxfrac = (maxFreq - freq) / maxFreq;
                delfreq = (maxfrac * maxfrac) * maxDelFreq;
                if (i < numPushes / 2) {
                    freq += delfreq;
                } else if (i > numPushes / 2) {
                    freq -= delfreq;
                }

                // log1("i = " + i + " freq = " + freq + " numPushes " +
                // numPushes);

                // translate freq steps/sec into period (usec) period = 1/freq
                // 1/2000 = 0.0005 but translate to usec divide by 0.000001
                // gives 500
                // period = 1000000/freq;
                if (freq <= minFreq)
                    freq = minFreq;
                period = (int) (50000.0 / freq);
                if (period < 1)
                    period = 1; // limits for the period
                if (period > 65535)
                    period = 65535;
                stepperRightFMspeedCue.period = period; // period is in micro
                // seconds
                stepperLeftFMspeedCue.period = period;
                duration = (20 * period / 60.0 * stepsPerPush);
                // log1("i = " + i + " freq = " + freq +
                // " period " + period + " duration " + duration);
                // second parameter in the push(cue, duration)
                // 62500 cue duration 62500 * 16us = 1s
                // using period gives 16 steps per push
                sequencer.push(cueList, (int) duration);
                if (wallInFront(1000)) {
                    sequencer.stop();
                    sequencer.close();
                    openSequencer();
                    break;
                }
            }
            waitToFinishOrForWall(1000);
            sequencer.stop();
            sequencer.close();
            openSequencer();
        } catch (Exception e) {
            log1("EXCEPTION" + e.getMessage());
        }
        log1("Finished going forward");
    }

    private void turn(int deg) throws ConnectionLostException {
        // zero radius turn of deg degrees
        if (deg >= 0) {
            rightMotorDirection.write(FORWARD_RIGHT);
            leftMotorDirection.write(BACKWARD_LEFT);
        } else {
            rightMotorDirection.write(BACKWARD_RIGHT);
            leftMotorDirection.write(FORWARD_LEFT);
        }
        int period;
        double duration;
        int StepsPerPush = 16;
        int steps = (int) (2.75 * (double) deg);
        period = (int) (50000.0 / 30.0);
        int numPushes = steps / StepsPerPush;
        try {
            startSequencer();
            for (int i = 0; i < numPushes; i++) {
                stepperRightFMspeedCue.period = period; // period is in 16 micro
                // seconds
                stepperLeftFMspeedCue.period = period;
                duration = (int) ((double) 20 * period / 60.0 * (double) StepsPerPush);
                log1("Turn " + period + " duration " + duration);
                sequencer.push(cueList, (int) duration);
            }
            waitToFinish();
            sequencer.pause();
        } catch (InterruptedException e) {
            log1("EXCEPTION" + e.getMessage());
        }
    }

    public void configureVicsWagonStandard() {
        try {
            halfFull = ioio_.openDigitalOutput(MOTOR_HALF_FULL_STEP_PIN, false);// Full
            rightMotorDirection = ioio_.openDigitalOutput(MOTOR_RIGHT_DIRECTION_PIN, true);// forward
            leftMotorDirection = ioio_.openDigitalOutput(MOTOR_LEFT_DIRECTION_PIN, false);
            motorControllerControl = ioio_.openDigitalOutput(MOTOR_CONTROLLER_CONTROL_PIN, true);// slow
            motorEnable = ioio_.openDigitalOutput(MOTOR_ENABLE_PIN, true);// enable - make true if using motors, false otherwise
            motorControllerReset = ioio_.openDigitalOutput(MOTOR_RESET, true);
            motorControllerReset.write(false);
            motorControllerReset.write(true);
            setUpMotorControllerChipForWaveDrive();
            frontIRSensorInput = ioio_.openPulseInput(new DigitalInput.Spec(IR_SENSOR_INPUT_PIN), PulseInput.ClockRate.RATE_62KHz, PulseInput.PulseMode.NEGATIVE, false);
            setupIRThread();
            openSequencer();
            sonar.startThread();
            spinTime = System.currentTimeMillis();
        } catch (Exception e) {
        }
    }

    private void setupIRThread() {
        Thread irThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    SystemClock.sleep(100);
                    try {
                        IRPulseDuration = frontIRSensorInput.getDuration();
                        seesIr = IRPulseDuration > 0.00075;
                    } catch (Exception e) {
                        e.printStackTrace();
                        log1("FRONT_IR_SENSOR: exception : " + e);
                        break;
                    }
                }
            }
        });
        irThread.start();
    }

    private void openSequencer() throws ConnectionLostException, InterruptedException {
        sequencer = ioio_.openSequencer(channelConfigList);
        sequencer.waitEventType(Sequencer.Event.Type.STOPPED);
    }

    /*********************************************************************************
     * Wave drive mode (full step one phase on) A LOW level on the pin HALF/FULL input selects the full step mode. When the low level is applied when the state machine is at an EVEN numbered state the wave drive mode is selected. To enter the wave drive mode the state machine must be in an EVEN numbered state. The most direct method to select the wave drive mode is to first apply a RESET, then while keeping the HALF/FULL input high apply one pulse to the clock input then take the HALF/FULL input low. This sequence first forces the state machine to state 1. The clock pulse, with the HALF/FULL input high advances the state machine from state 1 to either state 2 or 8 depending on the CW/CCW input. Starting from this point, after each clock pulse (rising edge) will advance the state machine following the sequence 2, 4, 6, 8, etc. if CW/CCW is high (clockwise movement) or 8, 6, 4, 2, etc. if CW/CCW is low (counterclockwise movement).
     **********************************************************************************/
    public void setUpMotorControllerChipForWaveDrive() {
        try {
            leftMotorClock = ioio_.openDigitalOutput(MOTOR_CLOCK_LEFT_PIN, true);
            rightMotorClock = ioio_.openDigitalOutput(MOTOR_CLOCK_RIGHT_PIN, true);
            motorControllerReset.write(false);
            motorControllerReset.write(true);
            halfFull.write(true);// Half
            rightMotorClock.write(false);
            rightMotorClock.write(true);
            leftMotorClock.write(false);
            leftMotorClock.write(true);
            halfFull.write(false);// Full
            leftMotorClock.close();
            rightMotorClock.close();

        } catch (ConnectionLostException e) {
        }
    }

    /***********************************************************************************************************
     * A HIGH logic level on the HALF/FULL input selects Half Step Mode. At Start-Up or after a RESET the Phase Sequencer is at state 1. After each clock pulse the state changes following the sequence 1,2,3,4,5,6,7,8,â€¦ if CW/ CCW is high (Clockwise movement) or 1,8,7,6,5,4,3,2,â€¦ if CW/CCW is low (Counterclockwise movement).
     *************************************************************************************************************/
    public void setUpMotrollerChipForHalfStepDrive() {

    }

    /***********************************************************************************************************
     * A LOW level on the HALF/FULL input selects the Full Step mode. When the low level is applied when the state machine is at an ODD numbered state the Normal Drive Mode is selected. The Normal Drive Mode can easily be selected by holding the HALF/FULL input low and applying a RESET. AT start -up or after a RESET the State Machine is in state1. While the HALF/FULL input is kept low, state changes following the sequence 1,3,5,7,â€¦ if CW/CCW is high (Clockwise movement) or 1,7,5,3,â€¦ if CW/CCW is low (Counterclockwise movement).
     *************************************************************************************************************/
    public void setUpMotrollerChipForFullStepDrive() {

    }

    // private void duanesCode()
    // {
    // @Override
    // protected void setup() throws ConnectionLostException {
    // sonar = new UltraSonicSensor(ioio_);
    // led = ioio_.openDigitalOutput(0, true);
    //
    // // motor setup
    // rightMotorDirection = ioio_.openDigitalOutput(
    // MOTOR_RIGHT_DIRECTION_PIN, true);
    // leftMotorDirection = ioio_.openDigitalOutput(
    // MOTOR_LEFT_DIRECTION_PIN, false);
    // motorControllerReset = ioio_.openDigitalOutput(MOTOR_RESET, true);
    // motorEnable = ioio_.openDigitalOutput(MOTOR_ENABLE_PIN, true);// enable
    // motorControllerControl = ioio_.openDigitalOutput(
    // MOTOR_CONTROLLER_CONTROL_PIN, true);// true = slow fast
    // halfFull = ioio_.openDigitalOutput(MOTOR_HALF_FULL_STEP_PIN, false);//
    // high
    // // is
    // // halfstep
    // // configure to wave drive
    // leftMotorClock = ioio_
    // .openDigitalOutput(MOTOR_CLOCK_LEFT_PIN, true);
    // rightMotorClock = ioio_.openDigitalOutput(MOTOR_CLOCK_RIGHT_PIN,
    // true);
    // motorControllerReset.write(false);
    // motorControllerReset.write(true);
    // halfFull.write(true);// Half
    // rightMotorClock.write(false);
    // rightMotorClock.write(true);
    // leftMotorClock.write(false);
    // leftMotorClock.write(true);
    // halfFull.write(false);// Full
    // leftMotorClock.close();
    // rightMotorClock.close();
    //
    // try {
    // sequencer = ioio_.openSequencer(channelConfigList);
    // sequencer.waitEventType(Sequencer.Event.Type.STOPPED);
    //
    // // while (sequencer.available() > 0)
    // // {
    // // addCueToCueList();
    // // }
    // sequencer.start();
    //
    // } catch (Exception e) {
    // }
    // log1("sequencer started .");
    // }
    //
    // @Override
    // public void loop() throws ConnectionLostException {
    // if (button.isChecked()) {
    // led.write(false);
    // try {
    // rightMotorDirection.write(true);
    // leftMotorDirection.write(false);
    // // addCueToCueList();
    // goSteps(2000);
    // Thread.sleep(4000);
    // rightMotorDirection.write(false);
    // leftMotorDirection.write(true);
    // // addCueToCueList();
    // goSteps(2000);
    // Thread.sleep(4000);
    //
    // } catch (Exception e) {
    // }
    // } else {
    // led.write(true);
    // }
    // }
    // }
    //
    // private void goSteps(int steps) {
    // int StepsPerPush = 10;
    // double maxFreq = 3000.0; // steps per second
    // double maxDelFreq = 5.0; // steps per second
    // int numPushes = steps / StepsPerPush;
    // double freq = 0.0;
    // int period;
    // int i;
    // log1("In goSteps");
    // for (i = 0; i < numPushes; i++) {
    // if (i < numPushes / 2) {
    // freq += ((maxFreq - freq) / (maxFreq)) * ((maxFreq - freq) / (maxFreq)) *
    // maxDelFreq;
    // } else if (i > numPushes / 2) {
    // freq -= ((maxFreq - freq) / (maxFreq)) * ((maxFreq - freq) / (maxFreq)) *
    // maxDelFreq;
    // }
    //
    // //log1("i = " + i + " freq = " + freq + " numPushes " + numPushes);
    //
    // try {
    // // translate freq steps/sec into period (usec) period = 1/freq
    // // 1/2000 = 0.0005 but translate to usec divide by 0.000001
    // // gives 500
    // // period = 1000000/freq;
    // if(freq <= 0.0) freq = 1.0;
    // period = (int) (1000000.0 / freq);
    // if(period < 0) period = 0;
    // if(period > 65535) period = 65535;
    // stepperRightFMspeedCue.period = period; // period is in micro seconds
    // stepperLeftFMspeedCue.period = period;
    // sequencer.push(cueList, 625);
    // } catch (Exception e) {
    // log1("EXCEPTION" + e.getMessage());
    // }
    // }
    // }
    //
    // private void addCueToCueList() {
    // int minperiod = 5000;
    // int maxperiod = 50000;
    // int frac;
    //
    // try {
    //
    // /*
    // * for(frac = 0; frac <= 10; frac ++) {
    // * stepperRightFMspeedCue.period = (minperiod * frac + maxperiod *
    // * (10 - frac)) / 10; stepperLeftFMspeedCue.period = (minperiod *
    // * frac + maxperiod * (10 - frac)) / 10; sequencer.push(cueList,
    // * 6250); // 62500 cue duration 62500 * 16us = 1s }
    // *
    // * stepperRightFMspeedCue.period = minperiod;
    // * stepperLeftFMspeedCue.period = minperiod; sequencer.push(cueList,
    // * 62500); // 62500 cue duration 62500 * 16us = 1s
    // *
    // * for(frac = 10; frac >= 0; frac --) {
    // * stepperRightFMspeedCue.period = (minperiod * frac + maxperiod *
    // * (10-frac)) / 10; stepperLeftFMspeedCue.period = (minperiod * frac
    // * + maxperiod * (10-frac)) / 10; sequencer.push(cueList, 6250); //
    // * 62500 cue duration 62500 * 16us = 1s }
    // */
    // int sineperiod;
    //
    // for (int i = 0; i < 20; i++) {
    // sineperiod = (int) ((double) maxperiod
    // * ((Math.cos((double) i / 40.0 * 6.28) + 1.0) * 0.5) + (double)
    // minperiod);
    // stepperRightFMspeedCue.period = sineperiod;
    // stepperLeftFMspeedCue.period = sineperiod;
    // sequencer.push(cueList, 6250); // 62500 cue duration 62500 *
    // // 16us = 1s
    // log1("sin period = " + sineperiod);
    // }
    //
    // for (int i = 19; i >= 0; i--) {
    // sineperiod = (int) ((double) maxperiod
    // * ((Math.cos((double) i / 40.0 * 6.28) + 1.0) * 0.5) + (double)
    // minperiod);
    // stepperRightFMspeedCue.period = sineperiod;
    // stepperLeftFMspeedCue.period = sineperiod;
    // sequencer.push(cueList, 6250); // 62500 cue duration 62500 *
    // // 16us = 1s
    // log1("sin period = " + sineperiod);
    // }
    // // period of 5000 give 200 steps in 1 sec
    //
    // // log1("AddCueToQueue " + i++);
    // } catch (Exception e) {
    // }
    // }
    // }

}