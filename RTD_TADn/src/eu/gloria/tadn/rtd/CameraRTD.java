package eu.gloria.tadn.rtd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.persistence.EntityManager;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.io.IOUtils;

import eu.gloria.rt.db.scheduler.ObservingPlanManager;
import eu.gloria.rt.db.util.DBUtil;
import eu.gloria.rt.entity.db.FileContentType;
import eu.gloria.rt.entity.db.FileFormat;
import eu.gloria.rt.entity.db.FileType;
import eu.gloria.rt.entity.db.ObservingPlanOwner;
import eu.gloria.rt.entity.db.ObservingPlanType;
import eu.gloria.rt.entity.device.ActivityContinueStateCamera;
import eu.gloria.rt.entity.device.ActivityStateCamera;
import eu.gloria.rt.entity.device.AlarmState;
import eu.gloria.rt.entity.device.BlockState;
import eu.gloria.rt.entity.device.CameraAcquisitionMode;
import eu.gloria.rt.entity.device.CameraDigitizingMode;
import eu.gloria.rt.entity.device.CameraType;
import eu.gloria.rt.entity.device.CommunicationState;
import eu.gloria.rt.entity.device.Device;
import eu.gloria.rt.entity.device.DeviceCamera;
import eu.gloria.rt.entity.device.DeviceType;
import eu.gloria.rt.entity.device.Image;
import eu.gloria.rt.entity.device.ImageContentType;
import eu.gloria.rt.entity.device.ImageFormat;
import eu.gloria.rt.entity.device.MeasureUnit;
import eu.gloria.rt.entity.environment.config.device.DeviceProperty;
import eu.gloria.rt.exception.RTException;
import eu.gloria.rt.exception.UnsupportedOpException;
import eu.gloria.rtc.op.OpManager;
import eu.gloria.rtd.RTDCameraInterface;
import eu.gloria.rti_db.tools.RTIDBProxyConnection;
import eu.gloria.tools.configuration.Config;
import eu.gloria.tools.log.LogUtil;
import eu.gloria.tools.time.RunTimeCounter;
import eu.gloria.tools.time.TimeOut;
import eu.gloria.tools.uuid.UUIDGenerator;

public class CameraRTD extends DeviceRTD implements RTDCameraInterface{
	
	private ContextContinue contextContinue;
	private Timer timerContinue = null;
	
	
	DeviceProperty urlPath = null;
	DeviceProperty privatePath = null;
	
	public static void main(String[] args) {
		
		CameraRTD camera = new CameraRTD("ContinueCCD");
		
		try {
			DeviceCamera dev = (DeviceCamera) camera.devGetDevice(false);
			System.out.println(dev.getCommunicationState()+"\n");
//			camera.camStartContinueMode();
			
			Thread.sleep(30000);
		} catch (RTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	/**
	 * Constructor.
	 * @param deviceId Device Identifier.
	 */
	public CameraRTD(String deviceId){
		super(deviceId);
		
		urlPath = DeviceRTD.configDeviceManager.getProperty(getDeviceId(), "IMAGE_URL");
		privatePath = DeviceRTD.configDeviceManager.getProperty(getDeviceId(), "PRIVATE_PATH");
		
		this.contextContinue = new ContextContinue();		
		
	}

	@Override
	public String camGetFocuser() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public List<String> camGetFilters() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public CameraType camGetCameraType() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetXsize() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetYSize() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camCanAbortExposure() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camCanAsymetricBin() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camCanGetCoolerPower() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camCanSetCooler() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camCanSetCCDTemperature() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camCanControlTemperature() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camCanStopExposure() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public float camGetCoolerPower() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public double camGetElectronsPerADU() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public double camGetFullWellCapacity() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camHasShutter() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camHasBrightness() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camHasConstrast() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camHasGain() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camHasGamma() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camHasSubframe() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camHasExposureTime() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public double camHeatSinkTemperature() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camIsPulseGuiding() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public String camGetLastError() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public double camGetLastExposureDuration() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public Date camGetLastExposureStart() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public long camGetMaxAdu() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetMaxBinX() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetMaxBinY() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetPixelSizeX() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetPixelSizeY() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public CameraAcquisitionMode camGetAcquisitionMode() throws RTException {
		
		return CameraAcquisitionMode.CONTINUOUS;
		
	}

	@Override
	public float camGetFPS() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public CameraDigitizingMode camGetDigitilizingMode() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetBinX() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetBinX(int value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetBinY() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetBinY(int value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camIsCoolerOn() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetCoolerOn(boolean value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetROINumX() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetROINumX(int value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetROINumY(int value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetROINumY() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetROIStartX(int ROIStartX) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetROIStartX() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetROIStartY(int value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetROIStartY() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetBrightness(long value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public long camGetBrightness() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetContrast(long value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public long camGetContrast() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetGain(long value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public long camGetGain() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetGamma(long value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public long camGetGamma() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetExposureTime(double value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public Double camGetExposureTime() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetCCDTemperature(float value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public float camGetCCDTemperature() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public float camGetCCDCurrentTemperature() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camAbortExposure() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camPulseGuide(int direction, long duration) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public String camStartExposure(boolean light) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camStopExposure() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public Image camGetImage(ImageFormat format) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camImageReady() throws RTException {
		
		if (this.contextContinue.getUuid() == null)
			return false;
		
		if (contextContinue.getTransfer() == TransferStatus.FINISHED)
			return true;
		else
			return false;
		
	}

	@Override
	public ImageContentType camGetImageDataType() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetBitDepth() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetBitDepth(int bits) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetContinueModeQuality() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetContinueModeQuality(int value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public int camGetOneShotModeQuality() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetOneShotModeQuality(int value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public String camGetContinueModeImagePath() throws RTException {

		DeviceProperty publicPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PUBLIC_PATH");

		return publicPath.getDefaultValue();
		
	}

	@Override
	public String camGetOneShotModeImagePath() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camGetAutoGain() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetAutoGain(boolean value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public boolean camGetAutoExposureTime() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public void camSetAutoExposureTime(boolean value) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
		
	}

	@Override
	public String camStartContinueMode() throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			if (this.contextContinue.isExposing())
				throw new RTException ("Continue mode is already running");
			
			
			this.contextContinue.setUuid(UUIDGenerator.singleton.getUUID().getValue());
			

			this.contextContinue.setExposing(true);
			this.contextContinue.setFileDate(null);
			this.contextContinue.getTimeOut().reset();

			timerContinue = new Timer(true);
			timerContinue.schedule(new TimerTaskContinueRecovering(), 0, 1000);			
			

			return this.contextContinue.getUuid();					
		
		}catch(RTException ex){
			throw ex;
		}catch(Exception ex){
			throw new RTException ("Error. " + ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("camStartCotinueMode");
		}
		
	}

	@Override
	public void camStopContinueMode() throws RTException {

		if (!this.contextContinue.isExposing())
			throw new RTException ("This mode isn't running yet");
		
		//Disable Timer
		timerContinue.cancel();
		timerContinue = null;
		
		this.contextContinue.setTransfer(TransferStatus.NOT_STARTED);
		
		this.contextContinue.setExposing(false);

	}

	@Override
	public String camGetImageURL(String uid, ImageFormat format) throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{
			
			//check the Image Format
			if (format != ImageFormat.JPG && format != ImageFormat.FITS ){
				throw new UnsupportedOpException ("Unsupported Format:" + format);
			}

			
			if (uid == null)
				throw new RTException ("No UUID provided");
			
			
			if (this.contextContinue.getUuid() != null && this.contextContinue.getUuid().equals(uid)){ //Requesting current continue image
				
				if (contextContinue.getTransfer() == TransferStatus.FINISHED){
					DeviceProperty publicPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PUBLIC_PATH");

					if (format == ImageFormat.JPG){
						return publicPath.getDefaultValue()+ uid + ".jpg";
					}else{
						throw new UnsupportedOpException ("Continue Mode cannot be provided in this format:" + format);
					}
				}else{					
					throw new RTException("NOT_AVAILABLE");					
				}
			}else{
				throw new RTException("NOT_AVAILABLE");
			}
		}finally{
			rtc.stop();
			rtc.writeLog("camGetImageUrl");
		}
		
	}

	@Override
	public Device devGetDevice(boolean allProperties) throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();
		
		boolean error = false;
		try {
			
			URL url = new URL(urlPath.getDefaultValue());		
		
			URLConnection urlConnection = url.openConnection();
			
			HttpURLConnection httpUrlConnection = (HttpURLConnection)urlConnection;
			
			if (httpUrlConnection.getResponseCode() >= 400) 
				error= true;
			
			DeviceCamera dev = new DeviceCamera();
//			Resolve the activity state
			if (error){ //Error

				dev.setCommunicationState(CommunicationState.BUSY);
				dev.setAlarmState(AlarmState.MALFUNCTION);
				dev.setActivityState(ActivityStateCamera.OFF);
				dev.setActivityContinueState(ActivityContinueStateCamera.ERROR);
				dev.setHasImage(false);
				
			}else { //No error.
				
				dev.setCommunicationState(CommunicationState.READY);
				dev.setAlarmState(AlarmState.NONE);
				
				dev.setActivityState(ActivityStateCamera.OFF);

				//ContinueActivity state
				if (this.contextContinue.isExposing())
					dev.setActivityContinueState(ActivityContinueStateCamera.EXPOSING);
				else
					dev.setActivityContinueState(ActivityContinueStateCamera.READY);
				
				dev.setHasImage(this.camImageReady());				
				
			}
			
			dev.setBlockState(BlockState.UNBLOCK);
			dev.setActivityStateDesc("");

			//Other additional information
			eu.gloria.rt.entity.environment.config.device.Device devConfig = DeviceRTD.configDeviceManager.getDevice(this.getDeviceId());
			dev.setDescription(devConfig.getDescription());
			dev.setMeasureUnit(MeasureUnit.NONE);	
			dev.setShortName(devConfig.getShortName());
			dev.setType(DeviceType.CCD);
			dev.setVersion(devConfig.getVersion());

			return dev;
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}

	@Override
	public List<ImageFormat> camGetOneShotModeImageFormats() throws RTException {
		return null;
	}
	
	@Override
	public List<ImageFormat> camGetContinueModeImageFormats() throws RTException {

		List<ImageFormat> result = new ArrayList<ImageFormat>();
		result.add(ImageFormat.JPG);
		return result;
	}

	@Override
	public List<Double> camGetObjectExposureTime(String filter, String object)
			throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}
	
	public enum TransferStatus{
		
		NOT_STARTED,
		STARTED,
		FINISHED,
		FAILED
	}
	
	public class ContextContinue{
		
		private Date fileDate;
		private boolean exposing;
		private String uuid;
		private TimeOut timeOut;
		private TransferStatus transfer;
		
		public ContextContinue(){
			this.fileDate = null;
			this.exposing = false;
			this.uuid = null;
			timeOut = new TimeOut(10000);
			this.transfer = TransferStatus.NOT_STARTED;
		}
		
		public void reset(){
			this.fileDate = null;
			this.exposing = false;
			this.uuid = null;
			timeOut = new TimeOut(10000);
		}
		
		public Date getFileDate() {
			return fileDate;
		}
		public void setFileDate(Date fileDate) {
			this.fileDate = fileDate;
		}
		public boolean isExposing() {
			return exposing;
		}
		public void setExposing(boolean exposing) {
			this.exposing = exposing;
		}
		public String getUuid() {
			return uuid;
		}
		public void setUuid(String uuid) {
			this.uuid = uuid;
		}
		public TimeOut getTimeOut() {
			return timeOut;
		}
		public void setTimeOut(TimeOut timeOut) {
			this.timeOut = timeOut;
		}
		
		public TransferStatus getTransfer() {
			return transfer;
		}

		public void setTransfer(TransferStatus transfer) {
			this.transfer = transfer;
		}
		
	}
	
	
	public class TimerTaskContinueRecovering extends TimerTask {
		@Override
		public void run() {

			RunTimeCounter rtc = new RunTimeCounter("CameraRTD", getDeviceId());
			rtc.start();		
			

			try{
				
				if (contextContinue.isExposing()){

					LogUtil.info(this, "CameraRTD. TimerTaskContinueRecovering.run BEGIN");			

					contextContinue.setTransfer(TransferStatus.STARTED);

					URL url = new URL(urlPath.getDefaultValue());

					URLConnection urlConnection = url.openConnection();    

					byte[] response = IOUtils.toByteArray(urlConnection.getInputStream());

					File newfile = new File(privatePath.getDefaultValue()+ contextContinue.getUuid() + ".jpg");
					OutputStream out = new FileOutputStream(newfile);

					out.write(response);
					out.close();
					
					contextContinue.setTransfer(TransferStatus.FINISHED);
					
				}

			} catch (IOException e) {
				contextContinue.setTransfer(TransferStatus.FAILED);
				e.printStackTrace();
			}finally{
				rtc.stop();
				rtc.writeLog("TimerTaskContinueRecovering.run");
			}

		}		
	}

	
}
