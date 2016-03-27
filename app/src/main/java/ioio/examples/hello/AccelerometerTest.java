package ioio.examples.hello;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import ioio.lib.api.IOIO;

public class AccelerometerTest implements SensorEventListener {
	IOIO ioio_;
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

	public AccelerometerTest(SensorManager sensorManager) {
		this.sensorManager = sensorManager;
	}

	public void configureAccelerometer() {
		sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this, sensorMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
		valuesAccelerometer = new float[3];
		valuesMagneticField = new float[3];
		matrixR = new float[9];
		matrixI = new float[9];
		matrixValues = new float[3];
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			for (int i = 0; i < 3; i++) {
				valuesAccelerometer[i] = event.values[i];
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			for (int i = 0; i < 3; i++) {
				valuesMagneticField[i] = event.values[i];
			}
			break;
		}

		boolean success = SensorManager.getRotationMatrix(matrixR, matrixI, valuesAccelerometer, valuesMagneticField);
		// log(success + "  success");
		double prevAzimuth = azimuth;
		if (success) {
			SensorManager.getOrientation(matrixR, matrixValues);
			synchronized (this) {
				azimuth = Math.toDegrees(matrixValues[0]);
				pitch = Math.toDegrees(matrixValues[1]);
				roll = Math.toDegrees(matrixValues[2]);
			}
		}
		if (prevAzimuth != azimuth) {
			MainActivity.activity.log(String.valueOf(azimuth));
		}
	}

	public synchronized double getAzimuth() {
		return azimuth;
	}

	public synchronized double getPitch() {
		return pitch;
	}

	public synchronized double getRoll() {
		return roll;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
}
