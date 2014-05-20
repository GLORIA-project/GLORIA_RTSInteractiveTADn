package eu.gloria.tadn.rtd;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.io.IOUtils;
import org.ciclope.client.sbig.CommunicationException;
import org.ciclope.client.sbig.SBIGConnector;

import eu.gloria.rt.db.scheduler.ObservingPlanManager;
import eu.gloria.rt.db.util.DBUtil;
import eu.gloria.rt.entity.db.FileContentType;
import eu.gloria.rt.entity.db.FileFormat;
import eu.gloria.rt.entity.db.FileType;
import eu.gloria.rt.entity.db.ObservingPlanOwner;
import eu.gloria.rt.entity.db.ObservingPlanType;
import eu.gloria.rt.entity.db.UuidType;
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
import eu.gloria.rt.exception.RTException;
import eu.gloria.rt.exception.UnsupportedOpException;
import eu.gloria.rtc.DeviceDiscoverer;
import eu.gloria.rtc.op.OpManager;
import eu.gloria.rtd.RTDCameraInterface;
import eu.gloria.rti_db.tools.RTIDBProxyConnection;
import eu.gloria.tools.configuration.Config;
import eu.gloria.tools.log.LogUtil;
import eu.gloria.tools.time.TimeOut;
import eu.gloria.tools.uuid.UUIDGenerator;

public class SBIGCameraRTD extends DeviceRTD implements RTDCameraInterface {

	private SBIGConnector sbig = null;
	
	private int binning = 1;
	
	private double exposureTime = -1;
	
	private RTIDBProxyConnection dbProxy;
	
	private ExposureContext imageContext = null;
	
	private String storagePath = null;
	private String publicPath = null;
	
	private Task task = null;
	private Timer exposureTimer = null;
	
	
	public static void main(String[] args) {
		
		SBIGCameraRTD camera = new SBIGCameraRTD("SBIG");	
//		camera.exposureTime = 1.0;
		try {
			DeviceCamera dev = (DeviceCamera) camera.devGetDevice(false);
			System.out.println(dev.getCommunicationState()+"\n");
//			String UUID = camera.camStartExposure(true);
//			Thread.sleep(30000);
		} catch (RTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
		
	}
	
	

	public SBIGCameraRTD(String deviceId) {
		super(deviceId);
		
		String ip = DeviceRTD.configDeviceManager.getProperty(getDeviceId(), "IP").getDefaultValue();
		int port = 	Integer.valueOf(DeviceRTD.configDeviceManager.getProperty(getDeviceId(), "PORT").getDefaultValue());
				
		sbig = SBIGConnector.getInstance(ip, port);
		
		String proxyHost = Config.getProperty("rt_config","proxyHost");
		String proxyPort = Config.getProperty("rt_config","proxyPort");
		String proxyAppName = Config.getProperty("rt_config","proxyAppName");
		String proxyUser = Config.getProperty("rt_config","proxyUser");
		String proxyPw = Config.getProperty("rt_config","proxyPw");
		boolean proxyHttps = Config.getPropertyBoolean("rt_config","proxyHttps",false);
		String proxyCertRep = Config.getProperty("rt_config","proxyCertRep");
		
		dbProxy = new RTIDBProxyConnection(proxyHost, proxyPort, proxyAppName, proxyUser, proxyPw, proxyHttps, proxyCertRep);
		
		imageContext = new ExposureContext();
		
		storagePath = DeviceRTD.configDeviceManager.getProperty(getDeviceId(), "STORAGE_PATH").getDefaultValue();
		publicPath = DeviceRTD.configDeviceManager.getProperty(getDeviceId(), "PUBLIC_PATH").getDefaultValue();
	}

	@Override
	public String camGetFocuser() throws RTException {
		
		//DeviceDiscoverer disc = new DeviceDiscoverer();		
		
		for (Device dev: DeviceDiscoverer.devGetDevices(true)){
			
			if (dev.getType() == DeviceType.FOCUS){
				if (DeviceRTD.configDeviceManager.getProperty(dev.getShortName(), "ASSOCIATED_CAMERA").getDefaultValue().equals(this.getDeviceId())){
					return dev.getShortName();
					
				}
			}
			
		}
		
		return null;		
		
	}

	@Override
	public List<String> camGetFilters() throws RTException {
		
		List<String> filters = new ArrayList<String>();		
		
		for (Device dev: DeviceDiscoverer.devGetDevices(true)){
			
			if (dev.getType() == DeviceType.FW){
				if (DeviceRTD.configDeviceManager.getProperty(dev.getShortName(), "ASSOCIATED_CAMERA").getDefaultValue().equals(this.getDeviceId())){
					filters.add(dev.getShortName());
				}
			}
			
		}		
		
		return filters;
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
		
		return false;
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
		
		return true;
	}

	@Override
	public boolean camCanControlTemperature() throws RTException {
		
		return true;
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
		
		return true;
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
		
		return 3;
	}

	@Override
	public int camGetMaxBinY() throws RTException {
		
		return 3;
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
		
		return CameraAcquisitionMode.ONE_SHOT;
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

		return binning;
	}

	@Override
	public void camSetBinX(int value) throws RTException {
		
		if (value > camGetMaxBinX())
			throw new RTException ("Incorrect value");
		
		binning = value;

	}

	@Override
	public int camGetBinY() throws RTException {

		return binning;
	}

	@Override
	public void camSetBinY(int value) throws RTException {
		
		if (value > camGetMaxBinY())
			throw new RTException ("Incorrect value");
		
		binning = value;

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

		exposureTime = value;
	}

	@Override
	public Double camGetExposureTime() throws RTException {
		
		return exposureTime;
	}

	@Override
	public void camSetCCDTemperature(float value) throws RTException {
		
		double resp = 0;
		try {
			resp = sbig.coolSBIG((int) celsiusToKelvin(value));
			
			if (resp == -1.0)
				throw new RTException ("SBIG.coolSBIG -1.0");
		} catch (CommunicationException e) {
			throw new RTException ("SBIG.coolSBIG " + e.getMessage());
		}

	}

	@Override
	public float camGetCCDTemperature() throws RTException {
		
		double resp = 0;
		try {
			resp = sbig.getTemperature();
			if (resp == -1.0)
				throw new RTException ("SBIG.getTemperature -1.0");
			
			return  kelvinToCelsius((float) resp);
			
			
		} catch (CommunicationException e) {
			throw new RTException ("SBIG.getTemperature " + e.getMessage());
		}
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

		LogUtil.info(this, "CameraRTD.camStartExposure::BEGIN!!!");
		
		if (exposureTime == -1){
			throw new RTException("No exposure time specified");
		}else if (imageContext.isExposing()){
			throw new RTException ("The previous exposure hasn't stoped yet.");
		}else{
			
			FilterWheelRTD filter = (FilterWheelRTD) DeviceDiscoverer.getRTD(camGetFilters().get(0));
			
//			FilterWheelRTD filter = (FilterWheelRTD) DeviceDiscoverer.getRTD("filter_wheel");
			
			
			try {
				imageContext.exposeStart(dbProxy.getProxy().uuidCreate(UuidType.FILE));	
				
//				imageContext.exposeStart(UUIDGenerator.singleton.getUUID().getValue());
			
				double resp = 0;
				int filterPos = filter.getPosition();
				
				LogUtil.info(this, "CameraRTD.camStartExposure::takePhotoSBIG!!!  exposure:"+exposureTime+" binning:"+binning+" filter:"+filterPos);
				
				resp = sbig.takePhotoSBIG((int)exposureTime, binning, filterPos, storagePath+imageContext.getUuid()+".fits");
			
				LogUtil.info(this, "CameraRTD.camStartExposure::takePhotoSBIG finished!!!");
				
				if (resp == -1.0){
					imageContext.exposeCancel();
					throw new RTException("SBIG.takePhotoSBIG -1.0");
				}
				
				exposureTimer = new Timer(true);
				exposureTimer.schedule(new Task(exposureTime), 0, 5000);
				
			} catch (CommunicationException e) {
				throw new RTException ("SBIG.takePhotoSBIG " + e.getMessage());
			}catch (Exception e1) {
				throw new RTException ("Error. " + e1.getMessage());
			}
		}
		
		LogUtil.info(this, "CameraRTD.camStartExposure::END!!!");
		return imageContext.getUuid();
		
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
		
		if ((imageContext.getUuid() != null) && (imageContext.isTransfered())){
			return true;
		}else{
			return false;
		}
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
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public String camGetOneShotModeImagePath() throws RTException {
		
		eu.gloria.rt.entity.environment.config.device.DeviceProperty publicPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PUBLIC_SERVLET_PATH");

		return publicPath.getDefaultValue();
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
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public void camStopContinueMode() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public String camGetImageURL(String uid, ImageFormat format)
			throws RTException {
		
		//check the Image Format
		if (format != ImageFormat.JPG && format != ImageFormat.FITS ){
			throw new UnsupportedOpException ("Unsupported Format:" + format);
		}				

		if (uid == null)
			throw new RTException ("No UUID provided");

		eu.gloria.rt.entity.environment.config.device.DeviceProperty publicServletPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PUBLIC_SERVLET_PATH");

		if (this.imageContext.getUuid() != null  && this.imageContext.getUuid().equals(uid)){ //Requesting current image

			if (imageContext.getTransfer() == TransferStatus.FINISHED){				
				if (format == ImageFormat.JPG){
					return publicServletPath.getDefaultValue()+ uid + "&format=JPG";
				}else{
					return publicServletPath.getDefaultValue()+ uid + "&format=FITS";
				}				
			}else{
				if (imageContext.getTransfer() == TransferStatus.FAILED){
					throw new RTException(imageContext.getTransfer().toString());
				}else{
					throw new RTException("NOT_AVAILABLE"); //<- 
				}
			}

		} else { //historical image			

			String path = null;

			if (format == ImageFormat.JPG){
				path = publicServletPath.getDefaultValue()+ uid + "&format=JPG";
			}else{
				path = publicServletPath.getDefaultValue()+ uid + "&format=FITS";
			}

			try {
				URL url = new URL(path+"&exist=1");
				URLConnection urlConnection = url.openConnection();	
				HttpURLConnection httpUrlConnection = (HttpURLConnection)urlConnection;
				if (httpUrlConnection.getResponseCode() == HttpServletResponse.SC_OK){

					return path;

				}else{
					throw new RTException("NOT_AVAILABLE");
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
				throw new RTException("NOT_AVAILABLE");
			} catch (IOException e) {
				e.printStackTrace();
				throw new RTException("NOT_AVAILABLE");
			}	

		}
	}

	@Override
	public List<ImageFormat> camGetOneShotModeImageFormats() throws RTException {

		List<ImageFormat> result = new ArrayList<ImageFormat>();
		result.add(ImageFormat.JPG);
		result.add(ImageFormat.FITS);
		return result;
	}

	@Override
	public List<ImageFormat> camGetContinueModeImageFormats()
			throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public List<Double> camGetObjectExposureTime(String filter, String object)
			throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}
	
	@Override
	public Device devGetDevice(boolean allProperties) throws RTException {
		
		boolean error = false;
		
		try {
			sbig.getTemperature();
		} catch (CommunicationException e) {
			error = true;
		}
		
		DeviceCamera dev = new DeviceCamera();

		//	Resolve the activity state
		if (error){ //Error

			dev.setCommunicationState(CommunicationState.BUSY);
			dev.setAlarmState(AlarmState.MALFUNCTION);
			dev.setActivityState(ActivityStateCamera.ERROR);
			dev.setActivityContinueState(ActivityContinueStateCamera.OFF);
			dev.setHasImage(false);
			
			resetRTD();

		}else { //No error.

			dev.setCommunicationState(CommunicationState.READY);
			dev.setAlarmState(AlarmState.NONE);			
			dev.setActivityContinueState(ActivityContinueStateCamera.OFF);
			
			if (imageContext.isExposing()) 
				dev.setActivityState(ActivityStateCamera.EXPOSING);
			else
				dev.setActivityState(ActivityStateCamera.READY);

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
	}
	
	private synchronized void resetRTD(){
		
		try{
			
			exposureTimer.cancel();
			exposureTimer = null;
			
		}catch(Exception ex){
			LogUtil.severe(this, "CameraRTD.resetRTD(). Stop the thread - timerOneShot. Error:" + ex.getMessage());
		};
		
		imageContext.reset();
	}
	
	/**
	 * Conversion from Celsius degrees to Kelvin degrees
	 * 
	 * @param celsius degrees
	 * @return Kelvin degrees
	 */
	private float celsiusToKelvin (float celsius){
		return celsius + 273;
	}
	
	/**
	 * Conversion from Kelvin degrees to Celsius degrees
	 * 
	 * @param kelvin degrees
	 * @return Celsius degrees
	 */
	private float kelvinToCelsius (float kelvin){
		return kelvin -273;
	}
	
public class ExposureContext{
		
		private boolean exposing;
		private boolean transfered;	
		private String uuid;
		private TransferStatus transfer;
		
		public ExposureContext(){
			reset();
			this.transfer = TransferStatus.NOT_STARTED;
		}
		
		public synchronized void exposeStart(String uuid){
			this.uuid = uuid;
			this.exposing = true;
			this.transfered = false;
			
			this.transfer = TransferStatus.NOT_STARTED;
		}
		
		public synchronized void exposeCancel(){
			reset();
		}
		
		public synchronized void exposeDone(){
			this.exposing = false;
			this.transfered = true;			
		}
		
		public synchronized void reset(){
			this.uuid = null;
			this.exposing = false;
			this.transfered = false;
		}
		
		public synchronized boolean isTransfered() {
			return transfered;
		}

		public synchronized void setTransfered(boolean transfered) {
			this.transfered = transfered;
		}
		
		public synchronized String getUuid() {
			return uuid;
		}

		public synchronized void setUuid(String uuid) {
			this.uuid = uuid;
		}
		
		public synchronized boolean isExposing() {
			return exposing;
		}

		public synchronized void setExposing(boolean value) {
			this.exposing = value;
		}

		public TransferStatus getTransfer() {
			return transfer;
		}

		public void setTransfer(TransferStatus transfer) {
			this.transfer = transfer;
		}
		
		
		
	}
	
	public enum TransferStatus{
		
		NOT_STARTED,
		STARTED,
		FINISHED,
		FAILED
	}

	

	public class Task extends TimerTask {
		
		private TimeOut timeOut;
		private boolean running;
		
		public Task(double exposureTime){
			
			int exposureTimeSegs = ((int) exposureTime) + 1;
			this.timeOut = new TimeOut((exposureTimeSegs * 1000) + 20000); //Exposure time + 20 segs.
			this.running = false;
			
		}
		
		@Override
		public void run() {
			
			if (running) return;
			running = true;
			
			try {
				
				LogUtil.info(this, "CameraRTD.Task.run()::BEGIN!!!");
		
				if (timeOut.timeOut()){
							
					imageContext.exposeCancel();
					exposureTimer.cancel(); 

					LogUtil.severe(this, "CameraRTD.Task.run():: 		Timeout!!!!");
				}else{
					checkExposing();
				}
					
					
			} catch (Exception e) {
				
				imageContext.exposeCancel();
				imageContext.setTransfer(TransferStatus.FAILED);
				exposureTimer.cancel(); 

				LogUtil.severe(this, "CameraRTD.Task.run():: 		ERROR...aborting image transfer process!!!. Error = " + e.getMessage());
				e.printStackTrace();
				
			}finally{
				running = false;
			}
			
			LogUtil.info(this, "CameraRTD.Task.run()::END!!!");
				
		}		
	}

	
	private synchronized void checkExposing() throws RTException{
		
		if (imageContext.isExposing()){			
			
			try {
				URL urlFits = new URL(publicPath+imageContext.getUuid()+".fits");

				LogUtil.info(this, "CameraRTD.checkExposing()::URL " + urlFits);
				URLConnection urlConnectionFits = urlFits.openConnection();	
				HttpURLConnection httpUrlConnectionFits = (HttpURLConnection)urlConnectionFits;
				if (httpUrlConnectionFits.getResponseCode() == HttpServletResponse.SC_OK){					
					
					LogUtil.info(this, "CameraRTD.checkExposing()::transfer .fits!!!");
					transferFile(imageContext.getUuid()+".fits");
				}else{
					LogUtil.info(this, "CameraRTD.checkExposing()::URL " + urlFits +" NO SC_OK " + httpUrlConnectionFits.getResponseCode());
					imageContext.exposeCancel();
					imageContext.setTransfer(TransferStatus.FAILED);
					exposureTimer.cancel(); 
					
					return;
				}
			
				//Sleep!!
//				LogUtil.info(this, "CameraRTD.checkExposing()::start sleep");
//				try {
//					Thread.sleep(10000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}							
				LogUtil.info(this, "CameraRTD.checkExposing()::finishe sleep");
				URL urlJpg = new URL(publicPath+imageContext.getUuid()+".fits.jpg");
				LogUtil.info(this, "CameraRTD.checkExposing()::URL " + urlJpg);
				
				int i=1;
				while (i<30){
					URLConnection urlConnectionJpg = urlJpg.openConnection();	
					HttpURLConnection httpUrlConnectionJpg = (HttpURLConnection)urlConnectionJpg;
					if (httpUrlConnectionJpg.getResponseCode() == HttpServletResponse.SC_OK){					
	
						exposureTimer.cancel();
						exposureTimer = null;
	
						LogUtil.info(this, "CameraRTD.checkExposing()::transfer .jpg!!!");
						transferFile(imageContext.getUuid()+".fits.jpg");			
	
						imageContext.setTransfer(TransferStatus.FINISHED);
	
						imageContext.exposeDone();
						
						break;
	
					}else{
						LogUtil.info(this, "CameraRTD.checkExposing()::URL " + urlJpg +" NO SC_OK " + httpUrlConnectionJpg.getResponseCode());
						
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						i++;						
					}
				}
				
				if (i==30){
					imageContext.exposeCancel();
					imageContext.setTransfer(TransferStatus.FAILED);
					exposureTimer.cancel(); 
				}
				
			} catch (MalformedURLException e) {
				throw new RTException(e.getMessage());
			} catch (IOException e) {
				throw new RTException(e.getMessage());
			} catch (RTException e) {
				throw new RTException(e.getMessage());
			}
			
		}
		
	}
	
	private void transferFile (String UUID) throws RTException{
		
		URL url;
		try {
			url = new URL(publicPath+UUID);			
						
			URLConnection urlConnection = url.openConnection();    
			
			byte[] fileContent = IOUtils.toByteArray(urlConnection.getInputStream());
			
			if (UUID.endsWith(".jpg")){
				UUID=UUID.replace(".fits", "");
			}
			
			eu.gloria.rt.entity.environment.config.device.DeviceProperty privatePath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PRIVATE_PATH");
			File newfile = new File(privatePath.getDefaultValue()+ UUID);
			OutputStream out = new FileOutputStream(newfile);
		
			out.write(fileContent);
			out.close();
			
			LogUtil.info(this, "CameraRTD.transferFile()::fileToRepository " + UUID);
			fileToRepository(privatePath.getDefaultValue()+ UUID);		
			
			newfile.delete();
			
		} catch (MalformedURLException e) {
			imageContext.setTransfer(TransferStatus.FAILED);
			throw new RTException(e.getMessage());
		} catch (IOException e) {
			imageContext.setTransfer(TransferStatus.FAILED);
			throw new RTException(e.getMessage());
		}catch (Exception e) {
			imageContext.setTransfer(TransferStatus.FAILED);
			throw new RTException("fileToRepository error. " + e.getMessage());
		}

		
	}
	
	private void fileToRepository (String path) throws RTException{
				
		
		String idOp = OpManager.getOpManager().getExtExecInfo().getUuidOp();
		
		EntityManager em = DBUtil.getEntityManager();
		ObservingPlanManager manager = new ObservingPlanManager();
		
//		ObservingPlan dbOp = null;
		
		try{
			
			DBUtil.beginTransaction(em);
			
//			dbOp = manager.get(em, idOp);
//			
//			if (dbOp != null){				
				
				//DBRepository->Create the Observing Plan
				eu.gloria.rt.entity.db.ObservingPlan repOP = new eu.gloria.rt.entity.db.ObservingPlan();
				repOP.setOwner(ObservingPlanOwner.USER);
				repOP.setType(ObservingPlanType.OBSERVATION);
				repOP.setUser(Config.getProperty("rt_config", "rts_name", "RTS_DEFAULT"));
				repOP.setUuid(idOp);
				
				try{
					String uuid = dbProxy.getProxy().opCreate(repOP);
					repOP = dbProxy.getProxy().opGet(uuid);
					
					LogUtil.info(this, "CameraRTD.fileToRepository(" + idOp + "). DBRepository OP created. UUID= " + uuid);
					
				}catch(Exception ex){					
					if (!ex.getMessage().contains("The Observing Plan already exists into the Repository database."))
						throw new RTException("Error registering the Observing Plan into the DBRepository.");
				}
				
				eu.gloria.rt.entity.db.File file = null;
				
				//Resolve the file format.
            	FileFormat fileFormat = FileFormat.FITS;
            	if (path.endsWith("jpg")){
            		fileFormat = FileFormat.JPG;
            	}
            	
				//DBRepository->Create the File information
				try{
						file = new eu.gloria.rt.entity.db.File();
						file.setContentType(FileContentType.OBSERVATION);
						file.setDate(getDate(new Date()));
						file.setType(FileType.IMAGE);
						file.setUuid(imageContext.getUuid());
						
						if (fileFormat == FileFormat.FITS)
							dbProxy.getProxy().fileCreate(idOp, file);

						LogUtil.info(this, "CameraRTD.fileToRepository(" + idOp + "). CREATED GLORIA file UUID=" + file.getUuid());

					}catch(Exception ex){
						imageContext.setTransfer(TransferStatus.FAILED);
						throw new Exception("Error registering a file into the DBRepository.");
					}
				
				
				
				//Creates the format
            	String urlSource = "file://" + path;
            	            	
            	try{
        			dbProxy.getProxy().fileAddFormat(file.getUuid(), fileFormat, urlSource);
        			
        			LogUtil.info(this, "CameraRTD.fileToRepository(" + idOp + "). UPLOADED file format. url=" + urlSource);
        		}catch(Exception ex){
					throw new Exception("Error adding a file format to a file into the DBRepository. urlSourcefile=" + urlSource);
				}
            	
//			}
			
		} catch (Exception ex) {			
			DBUtil.rollback(em);			
			throw new RTException(ex.getMessage());
			
		} finally {
			DBUtil.close(em);
		}
		
	}
	
	private XMLGregorianCalendar getDate(Date date) throws Exception{
    	GregorianCalendar c = new GregorianCalendar();
		c.setTime(date);
		XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
		return xmlCalendar;
    }
}
