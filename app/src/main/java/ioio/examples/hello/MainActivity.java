package ioio.examples.hello;

/******************************************************************************************
 * Happy version 150228B...IntelliJ version
 * Added comments for Full and Half Step modes
 * minor tweaks
 ********************************************************************************************/

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class MainActivity extends IOIOActivity {
    private static final String PREFERENCES = "gridSquares";
    private IOIO ioio;
    public UltraSonicSensor sonar;
    private TextView mText;
    private ScrollView mScroller;
    private TextToSpeech mTts;
    private SensorManager sensorManager;
    private DigitalOutput led;// The on-board LED
    private Accelerometer accelerometer;
    private AccelerometerTest accelerometerTest;
    private VicsWagon vw;
    private boolean powerOn = false;
    private double defaultSpeed = 2.5;
    boolean clickMap = true;
    boolean clickSolution = false;
    //	private SharedPreferences sp;

    private Runnable urbanLeft = new Runnable() {
        @Override
        public void run() {
            try {
                vw.urbanLeft();
            } catch (Exception e) {
            }
        }
    };
    private Runnable urbanRight = new Runnable() {
        @Override
        public void run() {
            try {
                vw.urbanRight();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private Runnable gold = new Runnable() {
        @Override
        public void run() {
            try {
                vw.goldRush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private Runnable drag = new Runnable() {
        @Override
        public void run() {
            try {
                vw.dragRace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private Runnable test = new Runnable() {
        @Override
        public void run() {
            try {
                vw.test();
                SystemClock.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private Runnable testUltra = new Runnable() {
        @Override
        public void run() {
            try {
                vw.testUltrasonic();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private Runnable showcase1 = new Runnable() {
        @Override
        public void run() {
            try {
                vw.showcase1();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private Runnable showcase2 = new Runnable() {
        @Override
        public void run() {
            try {
                vw.showcase2();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    public Runnable challenge = urbanLeft;

    public static MainActivity activity;

    //	public static Button endMapButton;
    //	public static Button toggleSolutionButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //		getActionBar().setIcon(R.drawable.rylexactionbaricon);
        setTitle(" ");
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mText = (TextView) findViewById(R.id.logText);
        mScroller = (ScrollView) findViewById(R.id.scroller);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        activity = this;
        //		sp = getSharedPreferences(PREFERENCES, 0);
        // accelerometerTest = new AccelerometerTest(sensorManager);
        // accelerometerTest.configureAccelerometer();
    }

    //	void savePrefs(String key, int value) {
    //		Editor edit = sp.edit();
    //		edit.putInt(key, value);
    //		edit.commit();
    //	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.power:
                if (!powerOn) {
                    item.setIcon(R.drawable.power_on);
                    powerOn = true;
                } else {
                    item.setIcon(R.drawable.power_off);
                    powerOn = false;
                    //				findViewById(R.id.toggleSolutionButton).setVisibility(View.GONE);
                }
                break;
            case R.id.urbanLeft:
                if (!powerOn) {
                    challenge = urbanLeft;
                    ((TextView) findViewById(R.id.title)).setText(getString(R.string.urban_label));
                    findViewById(R.id.finishMapButton).setVisibility(View.VISIBLE);
                    findViewById(R.id.toggleSolutionButton).setVisibility(View.VISIBLE);
                }
                break;
            case R.id.urbanRight:
                if (!powerOn) {
                    challenge = urbanRight;
                    ((TextView) findViewById(R.id.title)).setText(getString(R.string.urban_label_right));
                    findViewById(R.id.finishMapButton).setVisibility(View.VISIBLE);
                    findViewById(R.id.toggleSolutionButton).setVisibility(View.VISIBLE);
                }
                break;
            case R.id.goldRush:
                if (!powerOn) {
                    challenge = gold;
                    ((TextView) findViewById(R.id.title)).setText(getString(R.string.gold_label));
                    findViewById(R.id.finishMapButton).setVisibility(View.GONE);
                    findViewById(R.id.toggleSolutionButton).setVisibility(View.GONE);
                }
                break;
            case R.id.dragRace:
                if (!powerOn) {
                    challenge = drag;
                    ((TextView) findViewById(R.id.title)).setText(getString(R.string.drag_label));
                    findViewById(R.id.finishMapButton).setVisibility(View.GONE);
                    findViewById(R.id.toggleSolutionButton).setVisibility(View.GONE);
                }
                break;
            case R.id.test:
                if (!powerOn) {
                    challenge = test;
                    ((TextView) findViewById(R.id.title)).setText(getString(R.string.test_label));
                    findViewById(R.id.finishMapButton).setVisibility(View.GONE);
                    findViewById(R.id.toggleSolutionButton).setVisibility(View.GONE);
                }
                break;
            case R.id.testUltrasonic:
                if (!powerOn) {
                    challenge = testUltra;
                    ((TextView) findViewById(R.id.title)).setText(getString(R.string.test_ultra_label));
                    findViewById(R.id.finishMapButton).setVisibility(View.GONE);
                    findViewById(R.id.toggleSolutionButton).setVisibility(View.GONE);
                }
                break;
            case R.id.showcase1:
                if (!powerOn) {
                    challenge = showcase1;
                    ((TextView) findViewById(R.id.title)).setText(getString(R.string.showcase_1_label));
                    findViewById(R.id.finishMapButton).setVisibility(View.GONE);
                    findViewById(R.id.toggleSolutionButton).setVisibility(View.GONE);
                }
                break;
            case R.id.showcase2:
                if (!powerOn) {
                    challenge = showcase2;
                    ((TextView) findViewById(R.id.title)).setText(getString(R.string.showcase_2_label));
                    findViewById(R.id.finishMapButton).setVisibility(View.GONE);
                    findViewById(R.id.toggleSolutionButton).setVisibility(View.GONE);
                }
                break;
        }
        return true;
    }

    public void endMap(View v) {
        //		endMapButton = (Button) v;
        if (powerOn && clickMap) {
            vw.doneMapping = true;

            PopupMenu popup = new PopupMenu(MainActivity.this, v);

            //Inflating the Popup using xml file
            popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
            popup.getMenu().getItem(0).setTitle("No Spaces Back - " + vw.gridSquares.get(vw.gridSquares.size() - 1).getX() + ", " + vw.gridSquares.get(vw.gridSquares.size() - 1).getY());
            popup.getMenu().getItem(1).setTitle("One Space Back - " + vw.gridSquares.get(vw.gridSquares.size() - 2).getX() + ", " + vw.gridSquares.get(vw.gridSquares.size() - 2).getY());
            popup.getMenu().getItem(2).setTitle("Two Spaces Back - " + vw.gridSquares.get(vw.gridSquares.size() - 3).getX() + ", " + vw.gridSquares.get(vw.gridSquares.size() - 3).getY());
            popup.getMenu().getItem(3).setTitle("Three Spaces Back - " + vw.gridSquares.get(vw.gridSquares.size() - 4).getX() + ", " + vw.gridSquares.get(vw.gridSquares.size() - 4).getY());

            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    //					v.setVisibility(View.GONE);
                    //					findViewById(R.id.toggleSolutionButton).setVisibility(View.VISIBLE);
                    if (item.getItemId() == R.id.one) {
                        vw.gridSquares.remove(vw.gridSquares.size() - 1);
                    } else if (item.getItemId() == R.id.two) {
                        for (int i = 0; i < 2; i++) {
                            vw.gridSquares.remove(vw.gridSquares.size() - 1);
                        }
                    } else if (item.getItemId() == R.id.three) {
                        for (int i = 0; i < 3; i++) {
                            vw.gridSquares.remove(vw.gridSquares.size() - 1);
                        }
                    }
                    log("Ending Mapping Run...");
                    return true;
                }
            });

            popup.show();//showing popup menu
            clickMap = false;
            clickSolution = true;
        }
        //		activity = this;
    }

    public void startSolution(View v) {
        if (clickSolution) {
            //		toggleSolutionButton = (Button) v;
            if (vw.doSolution) {
                log("STOPPING SOLUTION...");
                vw.doSolution = false;
                //			((TextView) v).setText("Start Solution Run");
            } else {
                log("Starting Solution Run...");
                //			((TextView) v).setText("STOP Solution Run");
                vw.doSolution = true;
            }
            //		activity = this;

        }
    }

    class Looper extends BaseIOIOLooper {
        @Override
        protected void setup() throws ConnectionLostException {
            MainActivity.this.ioio = ioio_;
            accelerometer = new Accelerometer(sensorManager, ioio_);
            vw = new VicsWagon(ioio_);
            sonar = new UltraSonicSensor(ioio_);
            vw.sonar = sonar;
            led = ioio_.openDigitalOutput(0, true);
            vw.configureVicsWagonStandard();
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            if (powerOn) {
                led.write(false);
                try {
                    challenge.run();
                } catch (Exception e) {
                    log(e.getClass().getName() + ", " + e.getMessage());
                }
            } else {
                led.write(true);
            }
        }
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }

    public void log(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                mText.append(msg);
                mText.append("\n");
                mScroller.smoothScrollTo(0, mText.getBottom());
            }
        });
    }

    public void onSensorChanged(SensorEvent event) {
        accelerometer.onSensorChanged(event);
    }

    //	public void changeButtons() {
    //		((Button) findViewById(R.id.toggleSolutionButton)).setText("Start Solution Run");
    //		findViewById(R.id.toggleSolutionButton).setVisibility(View.GONE);
    //		findViewById(R.id.finishMapButton).setVisibility(View.VISIBLE);
    //	}
}
