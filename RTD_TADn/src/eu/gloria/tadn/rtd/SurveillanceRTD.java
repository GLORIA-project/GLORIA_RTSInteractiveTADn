package eu.gloria.tadn.rtd;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.http.HttpServletResponse;

import eu.gloria.rt.entity.device.ActivityState;
import eu.gloria.rt.entity.device.AlarmState;
import eu.gloria.rt.entity.device.CameraAcquisitionMode;
import eu.gloria.rt.entity.device.CameraDigitizingMode;
import eu.gloria.rt.entity.device.CommunicationState;
import eu.gloria.rt.entity.device.Device;
import eu.gloria.rt.entity.device.DeviceGeneral;
import eu.gloria.rt.entity.device.DeviceType;
import eu.gloria.rt.entity.device.Image;
import eu.gloria.rt.entity.device.MeasureUnit;
import eu.gloria.rt.entity.environment.config.device.DeviceProperty;
import eu.gloria.rt.exception.RTException;
import eu.gloria.rt.exception.UnsupportedOpException;
import eu.gloria.rtd.RTDSurveillanceCameraInterface;

public class SurveillanceRTD extends DeviceRTD implements
		RTDSurveillanceCameraInterface {


	public static void main(String[] args){	

		SurveillanceRTD rtd = new SurveillanceRTD ("scam_dome");
		
		try {
			rtd.scamGetImageURL();
		} catch (RTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	public SurveillanceRTD(String deviceId) {
		super(deviceId);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean scamHasBrightness() throws RTException {
		
		return false;
	}

	@Override
	public boolean scamHasContrast() throws RTException {
		
		return false;
	}

	@Override
	public CameraAcquisitionMode scamAcquisitionMode() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public float scamGetFPS() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public CameraDigitizingMode scamGetDigitizingMode() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean scamIsPTSupported() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean scamIsZoomSupported() throws RTException {
		
		return false;
	}

	@Override
	public int scamGetPanMin() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public int scamGetPanMax() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public int scamGetTiltMin() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public int scamGetTiltMax() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public int scamGetZoomMax() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public double scamGetExposureTime() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public void scamSetExposureTime(double value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public boolean scamIsImageReady() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public String scamGetVideoStreamingURL() throws RTException {

		DeviceProperty url = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "VIDEO_URL");
		
		if (url != null)
			return url.getDefaultValue();
		else		
			throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public void scamSetBrightness(long value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public long scamGetBrightness() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public void scamSetContrast(long value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public long scamGetContrast() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public int scamGetPanRotation() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public void scamSetPanRotation(int value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public int scamGetTiltRotation() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public void scamSetTiltRotation(int value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public int scamGetZoom() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public void scamSetZoom(int value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void scamAbortExposure() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void scamStartExposure(boolean light) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void scamStopExposure() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public Image scamGetImage() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}
	
	@Override
	public String scamGetImageURL() throws RTException {
		
		DeviceProperty url = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "IMAGE_URL");
		
		if (url != null)
			return url.getDefaultValue();
		else
			throw new UnsupportedOpException ("Operation not supported");
		
		
	}
	
	@Override
	public Device devGetDevice(boolean allProperties) throws RTException {
		
DeviceGeneral dev = new DeviceGeneral();
		
		URL url;
		try {
			url = new URL(this.scamGetImageURL());
			URLConnection urlConnection = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection)urlConnection;
			if (httpUrlConnection.getResponseCode() == HttpServletResponse.SC_OK){
				dev.setCommunicationState(CommunicationState.READY);
				dev.setAlarmState(AlarmState.NONE);
				dev.setActivityState(ActivityState.READY);		
			}else{
				dev.setCommunicationState(CommunicationState.BUSY);
				dev.setAlarmState(AlarmState.MALFUNCTION);
				dev.setActivityState(ActivityState.ERROR);
			}
				
		} catch (MalformedURLException e) {
			dev.setCommunicationState(CommunicationState.BUSY);
			dev.setAlarmState(AlarmState.MALFUNCTION);
			dev.setActivityState(ActivityState.ERROR);
		} catch (IOException e) {
			dev.setCommunicationState(CommunicationState.BUSY);
			dev.setAlarmState(AlarmState.MALFUNCTION);
			dev.setActivityState(ActivityState.ERROR);
		} catch (Exception e){
			dev.setCommunicationState(CommunicationState.BUSY);
			dev.setAlarmState(AlarmState.MALFUNCTION);
			dev.setActivityState(ActivityState.ERROR);
		}
		
		
		//Other additional information
		eu.gloria.rt.entity.environment.config.device.Device devConfig = DeviceRTD.configDeviceManager.getDevice(this.getDeviceId());
		dev.setDescription(devConfig.getDescription());
		dev.setMeasureUnit(MeasureUnit.NONE);	
		dev.setShortName(devConfig.getShortName());
		dev.setType(DeviceType.SURVEILLANCE_CAMERA);
		dev.setVersion(devConfig.getVersion());
		
		return dev;
	}

}
