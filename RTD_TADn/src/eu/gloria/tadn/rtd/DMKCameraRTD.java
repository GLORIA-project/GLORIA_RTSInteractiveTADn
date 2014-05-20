package eu.gloria.tadn.rtd;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eso.fits.FitsData;
import org.eso.fits.FitsHDUnit;
import org.eso.fits.FitsHeader;
import org.eso.fits.FitsKeyword;

import ciclope.devices.camera.client.CameraOperation;
import ciclope.devices.camera.client.TISCamera;
import ciclope.devices.camera.client.TISCameraManager;
import ciclope.utils.remote.FileTicket;
import ciclope.utils.remote.LinkException;
import ciclope.utils.remote.ServerException;

import eu.gloria.rt.catalogue.Catalogue;
import eu.gloria.rt.catalogue.ObjCategory;
import eu.gloria.rt.catalogue.ObjInfo;
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
import eu.gloria.rt.entity.environment.config.device.DeviceProperty;
import eu.gloria.rt.exception.CommunicationException;
import eu.gloria.rt.exception.RTException;
import eu.gloria.rt.exception.UnsupportedOpException;
import eu.gloria.rt.tools.img.ConverterInterface;
import eu.gloria.rt.tools.img.ConverterNetpbm;
import eu.gloria.rt.unit.Radec;
import eu.gloria.rtc.DeviceDiscoverer;
import eu.gloria.rtc.op.OpManager;
import eu.gloria.rtd.RTDCameraInterface;
import eu.gloria.rtd.RTDFilterWheelInterface;
import eu.gloria.rtd.RTDMountInterface;
import eu.gloria.rti_db.tools.RTIDBProxyConnection;
import eu.gloria.tools.configuration.Config;
import eu.gloria.tools.conversion.DegreeFormat;
import eu.gloria.tools.log.LogUtil;
import eu.gloria.tools.time.DateTools;
import eu.gloria.tools.time.RunTimeCounter;
import eu.gloria.tools.time.TimeOut;
import eu.gloria.tools.uuid.UUIDGenerator;

/**
 * TAD - CAMERA - RTD
 * 
 * 
 * Errors:
 * - TISCamera
 *   - getFileTime(). If the file does not exist --> return null.
 * 
 * NOTES:
 *  - The TISCamera operations (in this implementation) are (ONLY) oneshot operations.  
 *  - It generates images in JPG format. 
 *  
 *  CHANGE REQUEST:
 *  
 *  [20130524]
 *  	fserena: comment all cancelAll (NOT into the method camStopContinueMode)
 * 
 * @author mclopez 
 *
 */
public class DMKCameraRTD extends DeviceRTD implements RTDCameraInterface {
	
	
	private static String lock = "SYNCH_PURPOSE";
	
	private ContextOneShot contextOneShot;
	private ContextContinue contextContinue;

	private int continueQuality = -1;

	private Timer timerOneShot = null;
	private Timer timerContinue = null;
	
	private RTIDBProxyConnection dbProxy;

	/**
	 * Internal Service.
	 */
	private TISCamera camImpl = null;

	//QUITAR
	public TISCamera getCamImpl() {
		return camImpl;
	}

	private TISCameraManager camMan = null;	


	/*private synchronized boolean checkExposingOneShot() throws LinkException, ServerException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{

			if (this.contextOneShot.isExposing()){
				
				if (this.contextOneShot.getTimeOut().timeOut()){
					this.resetRTD();
					return false;
				}
				
				if (camImpl.getCurrentOperation() == null){

					try {
						camMan.getFileTime(camMan.getDefaultImagesPath(), (this.contextOneShot.getUuid()+".jpg"));
						this.contextOneShot.setExposing(false);

						transferFile (this.contextOneShot.getUuid());

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}				

					

					if (!this.contextContinue.isExposing()){
						expstopTimer.cancel();
						timerRunning = false;
					}

				}	
			}	

			return this.contextOneShot.isExposing();

		}finally{
			rtc.stop();
			rtc.writeLog("checkExposingOneShot");
		}

	}*/
	
	private synchronized boolean checkExposingOneShot() throws LinkException, ServerException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD.[ONESHOT]", this.getDeviceId());
		rtc.start();

		try{
			
			LogUtil.info(this, "CameraRTD.[ONESHOT] checkExposingOneShot(). StackTrace.......debug purpose");
			try{
				throw new Exception("Debug purpose");
			}catch(Exception ex){
				ex.printStackTrace();
			}
			
			Date init = this.contextOneShot.getTimeOut().getTimeoutIni();
			Date end = this.contextOneShot.getTimeOut().getTimeoutDeadline();
			
			LogUtil.info(this, "CameraRTD.[ONESHOT] checkExposingOneShot(). BEGIN. DateIni=" + init + ", DateDeadline=" + end);

			if (this.contextOneShot.isExposing()){
				
				LogUtil.info(this, "CameraRTD.[ONESHOT] checkExposingOneShot(). isExposing....");
				
				if (this.contextOneShot.getTimeOut().timeOut()){
					
					LogUtil.info(this, "CameraRTD.[ONESHOT][ONESHOTPROCESS-END] checkExposingOneShot(). TIMEOUT!!!. DateIni=" + init + ", DateDeadline=" + end);
					
					//-----------------------------------
					//CHANGE REQUEST[20130524]: commented cancelALL
					//this.resetRTD();
					this.resetRTDForOneShotTimeOut();
					//-----------------------------------
					
					return false;
				}
				
				if (camImpl.getCurrentOperation() == null){

					try {
						LogUtil.info(this, "CameraRTD.[ONESHOT][ONESHOTPROCESS-END] checkExposingOneShot(). There is NOT an operation running on the image server...looking for the image....");
						LogUtil.info(this, "CameraRTD.[ONESHOT] checkExposingOneShot(). getting current operation...");
						camMan.getFileTime(camMan.getDefaultImagesPath(), (this.contextOneShot.getUuid()+".fits")); 
						
						//Stops the timer.
						LogUtil.info(this, "CameraRTD.checkExposingOneShot(). [ONESHOT] Cancelling timerOneShot.");
						timerOneShot.cancel();
						timerOneShot = null;
						
						//Sets the flag to false
						this.contextOneShot.setExposing(false);
						
						//Transfer the file
						//transferFile (this.contextOneShot.getUuid(), "checkExposingOneShot", true, true);
						transferFitsFile(this.contextOneShot.getUuid(), "checkExposingOneShot");

					} catch (Exception e) {
						// TODO Auto-generated catch block
						LogUtil.info(this, "CameraRTD.[ONESHOT] checkExposingOneShot(). Error getting current operation: " + e.getMessage());
						e.printStackTrace();
					}				

				} else{
					LogUtil.info(this, "CameraRTD.[ONESHOT] checkExposingOneShot(). There is an operation running on the image server");
				}
			}	

			return this.contextOneShot.isExposing();

		}finally{
			
			rtc.stop();
			rtc.writeLog("checkExposingOneShot");
			
			LogUtil.info(this, "CameraRTD. [ONESHOT] checkExposingOneShot(). END.");
			
		}

	}

	/*private synchronized void checkExposingContinue() {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{

			if (this.contextContinue.isExposing()){
				try {
					
//					if (this.contextContinue.getTimeOut().timeOut()){
//						resetRTD();
//						return;
//					}

					Date date = camMan.getFileTime(camMan.getDefaultImagesPath(), this.contextContinue.getUuid()+".jpg");

					
					if (this.contextContinue.getFileDate() != date){						
						this.contextContinue.setFileDate(date);
						transferFile (this.contextContinue.getUuid());
						this.contextContinue.getTimeOut().reset();
					}else{
						this.contextContinue.getTimeOut().timeOut();
					}


				} catch (Exception e) {


				}

			}	
		}finally{
			rtc.stop();
			rtc.writeLog("checkExposingContinue");
		}

	}*/
	
	private synchronized void checkExposingContinue() {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{

			if (this.contextContinue.isExposing()){
				try {
					
					Date date = camMan.getFileTime(camMan.getDefaultImagesPath(), this.contextContinue.getUuid()+".jpg");
					
					if (this.contextContinue.getFileDate() != date){ //the image changed!!! (continue mode)
						
						this.contextContinue.setFileDate(date);
						transferFile (this.contextContinue.getUuid(), "checkExposingContinue", false, false);
						this.contextContinue.getTimeOut().reset();
						
					}else{
						//If timeout, nothing happens......It's in continue mode...
						this.contextContinue.getTimeOut().timeOut();
					}


				} catch (Exception e) {
				}

			}	
		}finally{
			rtc.stop();
			rtc.writeLog("checkExposingContinue");
		}

	}


	private String generateUUID () throws Exception{

		return UUIDGenerator.singleton.getUUID().getValue();

	}

	public static void main(String[] args) {

		try {
//			DMKCameraRTD camera = new DMKCameraRTD ("DBx 41AU02.AS");	
			CameraRTD camera = new CameraRTD ("DMx 41AU02.AS");
			DeviceCamera cam = (DeviceCamera) camera.devGetDevice(true);
			System.out.println(cam.getActivityState());
			
//			camera.camSetExposureTime(0.2);
//			String UID = camera.camStartExposure(true);	
//			Thread.sleep(100000000);

//			while (camera.camImageReady() != true){
//				DeviceCamera dev = (DeviceCamera) camera.devGetDevice(true);
//				System.out.println("AlarmState >>" + dev.getAlarmState());
//				System.out.println("ActivityContinueState >>" + dev.getActivityContinueState());
//				System.out.println("ActivityState >>" + dev.getActivityState());
//				System.out.println("ImageReady>>" + camera.camImageReady());
//				System.out.println("Operation>>: ");
//				System.out.println((camera. getCamImpl()).getCurrentOperation());
//				System.out.println("Brightness>>: ");
//				System.out.println(camera.camGetBrightness());
//				System.out.println("Gain>>:");
//				System.out.println(+ camera.camGetGain());				
//				System.out.println("ETime>>:");
//				System.out.println(+ camera.camGetExposureTime());
//			}
//			System.out.println(">>" + camera.camImageReady());
			//System.out.println(">>" + camera.camGetImageURL("hola"));
			//System.out.println(">>" + camera.camGetImageURL(UID));

			//		camera.camStopContinueMode();
			//			
			//			
			//String uuid = camera.camStartContinueMode();
			//System.out.println(">>" + camera.camGetImageURL(uuid));
			//String UID = camera.camStartExposure(true);
			//			while (true){
			//				System.out.println(">>" + camera.camGetImageURL(uuid));
			//			}

			//Thread.sleep(10000);
			//			
			//			
			//System.out.println(">>" + camera.camGetImageURL(uuid));



		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Constructor
	 * @param deviceId Identifier
	 */
	public DMKCameraRTD (String deviceId) throws Exception{

		super(deviceId);
		
		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();
		
		//eu.gloria.rt.entity.environment.config.device.Device devConfig = DeviceRTD.configDeviceManager.getDevice(this.getDeviceId());
		DeviceProperty ipProp = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "IP");
		DeviceProperty portProp = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PORT");
		DeviceProperty defaultPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "STORAGE_PATH");
		DeviceProperty tisCameraTmpFolder = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "TISCAMERA_TMP_FOLDER");
		DeviceProperty oneShotTimeOutSec = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "ONE_SHOT_TIMEOUT_MSEC");

		String[] names ={"ip", "port"};
		String[] values = {ipProp.getDefaultValue(), portProp.getDefaultValue()};
		LogUtil.info(this, "Camera service configuration=" + LogUtil.getLog(names, values));

		try{

			String ip = ipProp.getDefaultValue();
			int port = Integer.parseInt(portProp.getDefaultValue());
			camImpl = new TISCamera (this.getDeviceId(), ip, port, tisCameraTmpFolder.getDefaultValue());
			camMan = new TISCameraManager (ip,port, tisCameraTmpFolder.getDefaultValue());		


			//INIT - camera
			//-----------------------------------
			//CHANGE REQUEST[20130524]: commented cancelALL
			//camImpl.cancelAll();
			//-----------------------------------
			camMan.setDefaultImagesPath(defaultPath.getDefaultValue());	
			
		}catch(LinkException le){
			//Exception not thrown to get the device in error state
			//throw new CommunicationException(le.getMessage());
		}
		
		long oneShotTimeOut = 10000; //10000 mseconds by default
		try{
			oneShotTimeOut = Long.parseLong(oneShotTimeOutSec.getDefaultValue());
		}catch(Exception ex){
			oneShotTimeOut = 10000;
		}
		
		
		String proxyHost = Config.getProperty("rt_config","proxyHost");
		String proxyPort = Config.getProperty("rt_config","proxyPort");
		String proxyAppName = Config.getProperty("rt_config","proxyAppName");
		String proxyUser = Config.getProperty("rt_config","proxyUser");
		String proxyPw = Config.getProperty("rt_config","proxyPw");
		boolean proxyHttps = Config.getPropertyBoolean("rt_config","proxyHttps",false);
		String proxyCertRep = Config.getProperty("rt_config","proxyCertRep");
		
		dbProxy = new RTIDBProxyConnection(proxyHost, proxyPort, proxyAppName, proxyUser, proxyPw, proxyHttps, proxyCertRep);
		
		
		this.contextContinue = new ContextContinue(10000);
		this.contextOneShot = new ContextOneShot(oneShotTimeOut);
		
		rtc.stop();
		rtc.writeLog("CameraRTD");

	}


	@Override
	public synchronized String camGetFocuser() throws RTException {

		eu.gloria.rt.entity.environment.config.device.DeviceProperty mapping = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "ASSOCIATED_CAMERA");
		return mapping.getDefaultValue();
	}

	@Override
	public synchronized List<String> camGetFilters() throws RTException {

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
	public synchronized CameraType camGetCameraType() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized int camGetXsize() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized int camGetYSize() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized boolean camCanAbortExposure() throws RTException {

		return false;
	}

	@Override
	public synchronized boolean camCanAsymetricBin() throws RTException {

		return false;
	}

	@Override
	public synchronized boolean camCanGetCoolerPower() throws RTException {

		return false;
	}

	@Override
	public synchronized boolean camCanSetCooler() throws RTException {

		return false;
	}

	@Override
	public synchronized boolean camCanSetCCDTemperature() throws RTException {

		return false;
	}

	@Override
	public synchronized boolean camCanControlTemperature() throws RTException {

		return false;
	}

	@Override
	public synchronized boolean camCanStopExposure() throws RTException {

		return false;
	}

	@Override
	public synchronized float camGetCoolerPower() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized double camGetElectronsPerADU() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized double camGetFullWellCapacity() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized boolean camHasShutter() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized boolean camHasBrightness() throws RTException {

		return true;
	}

	@Override
	public synchronized boolean camHasConstrast() throws RTException {

		return false;
	}

	@Override
	public synchronized boolean camHasGain() throws RTException {

		return true;
	}

	@Override
	public synchronized boolean camHasGamma() throws RTException {

		return false;
	}

	@Override
	public synchronized boolean camHasSubframe() throws RTException {

		return false;
	}

	@Override
	public synchronized boolean camHasExposureTime() throws RTException {

		return true;
	}

	@Override
	public synchronized double camHeatSinkTemperature() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized boolean camIsPulseGuiding() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized String camGetLastError() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized double camGetLastExposureDuration() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized Date camGetLastExposureStart() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized long camGetMaxAdu() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized int camGetMaxBinX() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized int camGetMaxBinY() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized int camGetPixelSizeX() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized int camGetPixelSizeY() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized CameraAcquisitionMode camGetAcquisitionMode() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized float camGetFPS() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized CameraDigitizingMode camGetDigitilizingMode() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized int camGetBinX() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized void camSetBinX(int value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized int camGetBinY() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized void camSetBinY(int value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized boolean camIsCoolerOn() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized void camSetCoolerOn(boolean value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized int camGetROINumX() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized void camSetROINumX(int value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized void camSetROINumY(int value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized int camGetROINumY() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized void camSetROIStartX(int ROIStartX) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized int camGetROIStartX() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public synchronized void camSetROIStartY(int value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized int camGetROIStartY() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized void camSetBrightness(long value) throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{
			camImpl.setPropertyValue("BRIGHTNESS", value);
		}catch(LinkException le){
			throw new CommunicationException(le.getMessage());
		}catch(ServerException se){
			throw new RTException(se.getMessage());
		}
		

		rtc.stop();
		rtc.writeLog("camSetBrightness: " + value);

	}

	@Override
	public synchronized long camGetBrightness() throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try {
			return (long) camImpl.getPropertyValue("BRIGHTNESS");
		} catch (Exception e) {
			throw new RTException("The property does not exist.");
		}finally{
			rtc.stop();
			rtc.writeLog("camGetBrightness");
		}
	}

	@Override
	public synchronized void camSetContrast(long value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized long camGetContrast() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized void camSetGain(long value) throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{
			camImpl.setPropertyValue("GAIN", value);
		}catch(LinkException le){
			throw new CommunicationException(le.getMessage());
		}catch(ServerException se){
			throw new RTException(se.getMessage());
		}
		
		rtc.stop();
		rtc.writeLog("camSetGain");

	}

	@Override
	public synchronized long camGetGain() throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try {
			return (long) camImpl.getPropertyValue("GAIN");
		} catch (Exception e) {
			throw new RTException("The property does not exist.");
		}finally{
			rtc.stop();
			rtc.writeLog("camGetGain");
		}

	}

	@Override
	public synchronized void camSetGamma(long value) throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{
			camImpl.setPropertyValue("GAMMA", value);
		}catch(LinkException le){
			throw new CommunicationException(le.getMessage());
		}catch(ServerException se){
			throw new RTException(se.getMessage());
		}

		rtc.stop();
		rtc.writeLog("camSetGamma");

	}

	@Override
	public synchronized long camGetGamma() throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try {
			return (long) camImpl.getPropertyValue("GAMMA");
		} catch (Exception e) {
			throw new RTException("The property does not exist.");
		}finally{
			rtc.stop();
			rtc.writeLog("camGetGamma");
		}

	}

	@Override
	public synchronized void camSetExposureTime(double value) throws RTException { 

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD.[ONESHOT-CONTINUE]", this.getDeviceId());
		rtc.start();

		try{
			camImpl.setPropertyValue("EXPOSURE", value);
			LogUtil.info(this, "RTDCamera.[ONESHOT-CONTINUE].camSetExposureTime. value=" + value);
		}catch(LinkException le){
			LogUtil.severe(this, "RTDCamera.[ONESHOT-CONTINUE].camSetExposureTime.Error:" + le.getMessage());
			throw new CommunicationException(le.getMessage());
		}catch(ServerException se){
			LogUtil.severe(this, "RTDCamera.[ONESHOT-CONTINUE].camSetExposureTime.Error:" + se.getMessage());
			throw new RTException(se.getMessage());
		}

		rtc.stop();
		rtc.writeLog("camSetExposureTime");

	}

	@Override
	public synchronized Double camGetExposureTime() throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try {
			return camImpl.getPropertyValue("EXPOSURE");
		} catch (Exception e) {
			throw new RTException("The property does not exist.");
		}finally{
			rtc.stop();
			rtc.writeLog("camGetExposureTime");
		}

	}

	@Override
	public synchronized void camSetCCDTemperature(float value) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized float camGetCCDTemperature() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized float camGetCCDCurrentTemperature() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");		
	}

	@Override
	public synchronized void camAbortExposure() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");		

	}

	@Override
	public synchronized void camPulseGuide(int direction, long duration) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");		

	}

	/**
	 * 
	 */
	@Override
	public synchronized String camStartExposure(boolean light) throws RTException {
		
		LogUtil.info(this, "camStartExposure().[ONESHOT] BEGIN.");
		
		//throw new UnsupportedOpException ("Operation not supported");
		
		long exposureTime =  camGetExposureTime().longValue();

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD.[ONESHOT]", this.getDeviceId());
		rtc.start();
		
		checkImagePath ();

		try{

			if (contextOneShot.isExposing())
				throw new RTException ("The previous OneShot exposure hasn't stoped yet.");
			
			//this.contextOneShot.setUuid(generateUUID());
			
			this.contextOneShot.setUuid(dbProxy.getProxy().uuidCreate(UuidType.FILE)); 
			
			this.contextOneShot.setTransfer(TransferStatus.NOT_STARTED);

			camImpl.takeImage(this.contextOneShot.getUuid());	
			this.contextOneShot.setExposing(true);
			this.contextOneShot.setExposureTime(exposureTime);
			this.contextOneShot.getTimeOut().reset();

			timerOneShot = new Timer(true);
			timerOneShot.schedule(new TimerTaskOneShotRecovering(), 0, 1000);
			
			LogUtil.info(this, "CameraRTD.[ONESHOT][ONESHOTPROCESS-BEGIN]. camStartExposure().");

			return this.contextOneShot.getUuid();

		}catch(RTException ex){
			throw ex;
		}catch(Exception ex){
			throw new RTException ("Error. " + ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("camStartExposure");
			LogUtil.info(this, "camStartExposure(). END.");
		}

	}

	@Override
	public synchronized void camStopExposure() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}


	@Override
	public synchronized boolean camImageReady() throws RTException {				

		
//		if ((imageContext.getUuid() != null) && (imageContext.isTransfered())){
//			return true;
//		}else{
//			return false;
//		}
		
		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{

			if (contextOneShot.isExposing() /* checkExposingOneShot()*/){ //Operation in execution
				
				if (this.contextContinue.getUuid() == null)
					return false;

				if ((contextOneShot.getUuid() != null) && (contextOneShot.getTransfer()==TransferStatus.FINISHED)){
					return true;
				}else{
					return false;
				}
//				
//				try {
//					
//					camMan.getFileTime(camMan.getDefaultImagesPath(), this.contextContinue.getUuid()+".jpg");
//					
//				}catch(LinkException le){
//					throw new CommunicationException(le.getMessage());
//				} catch (Exception e) {
//					if (e.getMessage().equals("ERROR - The file does not exist."))
//						return false;
//				}
//				
//				return true;
				
			}else{
				
				if (this.contextOneShot.getUuid() != null){

					return true;

				}else{

					if (this.contextContinue.getUuid() == null)
						return false;

					try {
						
						camMan.getFileTime(camMan.getDefaultImagesPath(), this.contextContinue.getUuid()+".jpg");
						
					}catch(LinkException le){
						throw new CommunicationException(le.getMessage());
					} catch (Exception e) {
						if (e.getMessage().equals("ERROR- The file does not exist."))
							return false;
					}
					return true;
				}
				
			}	
		/*}catch(LinkException le){
			throw new CommunicationException(le.getMessage());
		}catch(ServerException se){
			throw new RTException(se.getMessage());*/
		}catch(Exception ex){
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("camImageReady");
		}
	}

	@Override
	public synchronized ImageContentType camGetImageDataType() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized int camGetBitDepth() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized void camSetBitDepth(int bits) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public synchronized int camGetContinueModeQuality() throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{

			if (continueQuality == -1)
				return camGetOneShotModeQuality();
			else
				return continueQuality;		

		}finally{
			rtc.stop();
			rtc.writeLog("camGetContinueModeQuality");
		}

	}

	@Override
	public synchronized void camSetContinueModeQuality(int value) throws RTException {

		continueQuality = value;

	}

	@Override
	public synchronized int camGetOneShotModeQuality() throws RTException {

		//throw new UnsupportedOpException ("Operation not supported");
		
		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{

			return camImpl.getImageQuality();
			
		}catch(LinkException le){
			le.printStackTrace();
			throw new CommunicationException(le.getMessage());
		}catch(ServerException se){
			se.printStackTrace();
			throw new RTException(se.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("camGetOneShotModeQuality");
		}
	}

	@Override
	public synchronized void camSetOneShotModeQuality(int value) throws RTException {
		
		throw new UnsupportedOpException ("Operation not supported");

//		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
//		rtc.start();
//
//		try{
//			camImpl.setImageQuality(value);
//		}catch(LinkException le){
//			throw new CommunicationException(le.getMessage());
//		}catch(ServerException se){
//			throw new RTException(se.getMessage());
//		}
//
//		rtc.stop();
//		rtc.writeLog("camSetOneShotModeQuality");

	}

	@Override
	public synchronized String camGetContinueModeImagePath() throws RTException {


			DeviceProperty publicPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PUBLIC_PATH");

			return publicPath.getDefaultValue();		

	}

	@Override
	public synchronized String camGetOneShotModeImagePath() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
		
//			DeviceProperty publicPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PUBLIC_SERVLET_PATH");
//
//			return publicPath.getDefaultValue();

	}

	@Override
	public synchronized boolean camGetAutoGain() throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try {
			return camImpl.getPropertyAuto("GAIN");
		} catch (Exception e) {
			throw new RTException("The property does not exist.");
		}finally{
			rtc.stop();
			rtc.writeLog("camGetAutoGain");
		}

	}

	@Override
	public synchronized void camSetAutoGain(boolean value) throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try {
			camImpl.setPropertyAuto("GAIN", value);
		} catch (Exception e) {
			throw new RTException("The property does not exist.");
		}finally{
			rtc.stop();
			rtc.writeLog("camSetAutoGain");
		}

	}

	@Override
	public synchronized boolean camGetAutoExposureTime() throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try {
			return camImpl.getPropertyAuto("EXPOSURE");
		} catch (Exception e) {
			throw new RTException("The property does not exist.");
		}finally{
			rtc.stop();
			rtc.writeLog("camGetAutoExposureTime");
		}
	}

	@Override
	public synchronized void camSetAutoExposureTime(boolean value) throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try {
			camImpl.setPropertyAuto("EXPOSURE", value);
		} catch (Exception e) {
			throw new RTException("The property does not exist.");
		}	finally{
			rtc.stop();
			rtc.writeLog("camSetAutoExposureTime");
		}
	}

	private void checkImagePath () throws RTException{

		try {
			camMan.getDefaultImagesPath();
		} catch (LinkException e) {
			throw new CommunicationException(e.getMessage());
		} catch (ServerException e) {
			DeviceProperty defaultPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "STORAGE_PATH");
			try {
				camMan.setDefaultImagesPath(defaultPath.getDefaultValue());
			} catch (LinkException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	/**
	 * The continue exposing starts.
	 * 
	 * Procedure:
	 * 
	 * -If  continue_exposing in progress -> ERROR 
	 * -If currentOp exist (oneshot exposure inprogress) --> ERROR
	 * -Else start continue_exposing.
	 * 
	 * @throws RTException in error case.
	 */
	@Override
	public synchronized String camStartContinueMode() throws RTException { 
		
		LogUtil.info(this, "camStartContinueMode. BEGIN ");

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		checkImagePath();
		
		try{
			
			LogUtil.info(this, "camStartContinueMode. CP-1 ");
			
			/*if (this.contextContinue.isExposing())
				throw new RTException ("Continue mode is already running");*/
			
			if (this.contextContinue.isExposing()){
				this.camStopContinueMode();
			}

			LogUtil.info(this, "camStartContinueMode. CP-2 ");
			
			if (this.contextOneShot.isExposing()){
				throw new RTException ("The previous OneShot exposure hasn't stoped yet.");
			}
			LogUtil.info(this, "camStartContinueMode. CP-3 ");

			this.contextContinue.setUuid(generateUUID());
			
			LogUtil.info(this, "camStartContinueMode. CP-4 ");

			camImpl.takeContinuousImages(this.contextContinue.getUuid(), 1000, camGetContinueModeQuality());
			
			LogUtil.info(this, "camStartContinueMode. CP-5 ");
			
			this.contextContinue.setExposing(true);
			this.contextContinue.setFileDate(null);
			this.contextContinue.getTimeOut().reset();
			
			LogUtil.info(this, "camStartContinueMode. CP-6 ");

			timerContinue = new Timer(true);
			timerContinue.schedule(new TimerTaskContinueRecovering(), 0, 1000);
			
			LogUtil.info(this, "camStartContinueMode. CP-7 ");
			
			//Delete obsolete continue files....
			deleteObsoleteContinueModeFiles();
			
			LogUtil.info(this, "camStartContinueMode. CP-8 ");

			return this.contextContinue.getUuid();		
			
		}catch(LinkException le){
			le.printStackTrace();
			LogUtil.severe(this, "camStartContinueMode. Error-LinkException::" + le.getMessage());
			throw new CommunicationException(le.getMessage());
		}catch(RTException ex){
			ex.printStackTrace();
			LogUtil.severe(this, "camStartContinueMode. Error-RTException::" + ex.getMessage());
			throw ex;
		}catch(Exception ex){
			ex.printStackTrace();
			LogUtil.severe(this, "camStartContinueMode. Error-Exception::" + ex.getMessage());
			throw new RTException ("Error. " + ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("camStartCotinueMode");
			LogUtil.info(this, "camStartContinueMode. END ");
		}

	}

	/**
	 * The continue exposing stops.
	 * 
	 * Procedure:
	 * 
	 * -If  continue_exposing is not in progress -> ERROR 
	 * -Else cancellAll.
	 * 
	 * @throws RTException in error case.
	 */
	/*@Override
	public synchronized void camStopContinueMode() throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		if (!this.contextContinue.isExposing())
			throw new RTException ("This mode isn't running yet");


		try{
			if (checkExposingOneShot())
				throw new RTException ("The previous exposure hasn't stoped yet.");		
		}catch(LinkException le){
			throw new CommunicationException(le.getMessage());
		}catch(ServerException se){
			throw new RTException(se.getMessage());
		}


		try{
			
			camImpl.cancelAll();		//it isn't considered as operation
			this.contextContinue.setTransfer(TransferStatus.NOT_STARTED);
			
		}catch(LinkException le){
			throw new CommunicationException(le.getMessage());
		}catch(ServerException se){
			throw new RTException(se.getMessage());
		}

		transferFile (this.contextContinue.getUuid());

		if (!this.contextOneShot.isExposing()){
			expstopTimer.cancel();
			timerRunning = false;
		}

		this.contextContinue.setExposing(false);

		rtc.stop();
		rtc.writeLog("camStopContinueMode");

	}*/
	
	@Override
	public synchronized void camStopContinueMode() throws RTException {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		if (!this.contextContinue.isExposing())
			throw new RTException ("This mode isn't running yet");

		if (this.contextOneShot.isExposing()){
			throw new RTException ("The previous One shot exposure hasn't stoped yet.");	
		}
		
		//Disable Timer
		timerContinue.cancel();
		timerContinue = null;

		//Cancel all camera operations...
		try{
			
			camImpl.cancelAll();		//it isn't considered as operation
			this.contextContinue.setTransfer(TransferStatus.NOT_STARTED);
			
		}catch(LinkException le){
			throw new CommunicationException(le.getMessage());
		}catch(ServerException se){
			throw new RTException(se.getMessage());
		}
		
		this.contextContinue.setExposing(false);

		rtc.stop();
		rtc.writeLog("camStopContinueMode");

	}

	/**
	 * Recover the last image (oneshotUID or continueUID, if it exists).
	 * @return 
	 */
	/*@Override
	public synchronized String camGetImageURL(String uid) throws RTException {				

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{

			List<String> uuidList = new ArrayList<String> ();		

			if (uid == null)
				throw new RTException ("No UUID provided");

			if (this.contextContinue.getUuid() != null)
				uuidList.add(this.contextContinue.getUuid());

			if (this.contextOneShot.getUuid() != null)
				uuidList.add(this.contextOneShot.getUuid());

			if (uuidList.contains(uid)){
				
				if (uid.equals(contextContinue.getUuid())){
					if (contextContinue.getTransfer() == TransferStatus.FINISHED){
						DeviceProperty publicPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PUBLIC_PATH");

						return publicPath.getDefaultValue()+ uid + ".jpg";
					}else{
						if (contextOneShot.getTransfer() == TransferStatus.FAILED){
							throw new RTException(contextOneShot.getTransfer().toString());
						}else{
							throw new RTException("NOT_AVAILABLE");
						}
					}
				}else if (uid.equals(contextOneShot.getUuid())){
					if (contextOneShot.getTransfer() == TransferStatus.FINISHED){
						DeviceProperty publicPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PUBLIC_PATH");

						return publicPath.getDefaultValue()+ uid + ".jpg";
					}else{
						if (contextOneShot.getTransfer() == TransferStatus.FAILED){
							throw new RTException(contextOneShot.getTransfer().toString());
						}else{
							throw new RTException("NOT_AVAILABLE");
						}
					}
				}						
				throw new RTException ("Invalid UID");
			}else{
				throw new RTException ("Invalid UID");
			}

		}finally{
			rtc.stop();
			rtc.writeLog("camGetImageUrl");
		}

	}*/
	
	/**
	 * Recover the last image (oneshotUID or continueUID, if it exists).
	 * @return URL
	 */
	@Override
	public synchronized String camGetImageURL(String uid, ImageFormat format) throws RTException {				

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{
			
			//check the Image Format
			if (format != ImageFormat.JPG && format != ImageFormat.FITS ){
				throw new UnsupportedOpException ("Unsupported Format:" + format);
			}

			List<String> uuidList = new ArrayList<String> ();		

			if (uid == null)
				throw new RTException ("No UUID provided");
			
			DeviceProperty publicServletPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PUBLIC_SERVLET_PATH");

			if (this.contextContinue.getUuid() != null && this.contextContinue.getUuid().equals(uid)){ //Requesting current continue image
				
				if (contextContinue.getTransfer() == TransferStatus.FINISHED){
					DeviceProperty publicPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PUBLIC_PATH");

					if (format == ImageFormat.JPG){
						return publicPath.getDefaultValue()+ uid + ".jpg";
					}else{
						throw new UnsupportedOpException ("Continue Mode cannot be provided in this format:" + format);
					}
				}else{
					if (contextOneShot.getTransfer() == TransferStatus.FAILED){
						throw new RTException(contextOneShot.getTransfer().toString());
					}else{
						throw new RTException("NOT_AVAILABLE");
					}
				}
			} else if (this.contextOneShot.getUuid() != null  && this.contextOneShot.getUuid().equals(uid)){ //Requesting current oneshot image
				
				if (contextOneShot.getTransfer() == TransferStatus.FINISHED){
					//publicPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PUBLIC_PATH");

					
					if (format == ImageFormat.JPG){
						return publicServletPath.getDefaultValue()+ uid + "&format=JPG";
					}else{
						return publicServletPath.getDefaultValue()+ uid + "&format=FITS";
					}
					
				}else{
					if (contextOneShot.getTransfer() == TransferStatus.FAILED){
						throw new RTException(contextOneShot.getTransfer().toString());
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

			

		}finally{
			rtc.stop();
			rtc.writeLog("camGetImageUrl");
		}

	}
	
	@Override
	public List<Double> camGetObjectExposureTime(String filter, String object)
			throws RTException {
		
		double planetMin;
		double planetMax;
		double mMin;
		double mMax;
		double ngcMin;
		double ngcMax;
		
		List<Double> result = null;
		
		double longitude = Config.getPropertyDouble("rt_config", "rts_longitude");
		double latitude = Config.getPropertyDouble("rt_config", "rts_latitude");		

		Catalogue catalogue = new Catalogue(longitude, latitude, 0);
		ObjInfo objInfo = catalogue.getObject(object);
		if (objInfo == null){
			
			LogUtil.info(this, "Camera. Catalogue:: Object NOT found:" + object);
			throw new RTException("Not Object Found");
			
		}else{
			
			LogUtil.info(this, "Mount. Catalogue:: Object found:" + object);				
			
			String general = null;
			double generalMin = 0;
			double generalMax = 0;
			try{
				general = DeviceRTD.configDeviceManager.getProperty(getDeviceId(), "GENERAL_EXP").getDefaultValue();
				String[] dataGeneral = general.split(":");		
				generalMin = Double.valueOf(dataGeneral[0]);
				generalMax = Double.valueOf(dataGeneral[1]);
			} catch (Exception e){
				
			}
			
					
			if (objInfo.getCategory() == ObjCategory.MajorPlanetAndMoon){
				String planet = null;
				eu.gloria.rt.entity.environment.config.device.DeviceProperty planetProperty = DeviceRTD.configDeviceManager.getProperty(getDeviceId(), "PLANET_EXP");

				if (planetProperty == null){
					if (general != null){
						result = new ArrayList <Double> ();
						
						result.add(generalMin);
						result.add(generalMax);
					}else
						return result;
				}
				
				planet = planetProperty.getDefaultValue();
				String[] dataPlanet = planet.split(":");		
				planetMin = Double.valueOf(dataPlanet[0]);
				planetMax = Double.valueOf(dataPlanet[1]);
				
				result = new ArrayList <Double> ();
				
				result.add(planetMin);
				result.add(planetMax);
				
			}else if (objInfo.getCategory() == ObjCategory.OutsideSSystemObj){
				String[] data = object.split(" ");
				
				int index = 0;
				for (String a : data){
					if (a.isEmpty())
						index++;
					else
						break;
				}
				
				if (data[index].toUpperCase().startsWith("M")){	//Messier
					String messier = null ;
					eu.gloria.rt.entity.environment.config.device.DeviceProperty messierProperty = DeviceRTD.configDeviceManager.getProperty(getDeviceId(), "MESSIER_EXP");
					if (messierProperty == null){
						if (general != null){
							result = new ArrayList <Double> ();
							
							result.add(generalMin);
							result.add(generalMax);
						}
						return result;
					}
					
					messier = messierProperty.getDefaultValue();
					String[] dataMessier = messier.split(":");		
					mMin = Double.valueOf(dataMessier[0]);
					mMax = Double.valueOf(dataMessier[1]);

					result = new ArrayList <Double> ();

					result.add(mMin);
					result.add(mMax);
					
					
				}else if (data[index].toUpperCase().startsWith("NGC")){	//NGC
					String ngc = null;
					eu.gloria.rt.entity.environment.config.device.DeviceProperty ngcProperty = DeviceRTD.configDeviceManager.getProperty(getDeviceId(), "NGC_EXP");
					if (ngcProperty == null){	
						
						if (general != null){
							result = new ArrayList <Double> ();
							
							result.add(generalMin);
							result.add(generalMax);
						}
						return result;
					}
					
					ngc = ngcProperty.getDefaultValue();
					String[] dataNgc = ngc.split(":");		
					ngcMin = Double.valueOf(dataNgc[0]);
					ngcMax = Double.valueOf(dataNgc[1]);

					result = new ArrayList <Double> ();

					result.add(ngcMin);
					result.add(ngcMax);
					
				}else if (general != null){
					result = new ArrayList <Double> ();
					
					result.add(generalMin);
					result.add(generalMax);
				}
				
			}
			
			
		}
		
		return result;
	}
	
	@Override
	public synchronized Device devGetDevice(boolean allProperties) throws RTException  {

		RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
		rtc.start();

		try{		
			
			boolean continueImage = false;
			boolean error = false;
			CameraOperation currentOp = null;
			CameraOperation lastOp = null;

			try{

				currentOp = camImpl.getCurrentOperation();
				lastOp = camImpl.getLastOperation();

			}catch(Exception ex){
				LogUtil.severe(this, "Error recovering the camera operation data. " + ex.getMessage());
				error = true;
			}

			DeviceCamera dev = new DeviceCamera();

			//	Resolve the activity state
			if (error){ //Error

				dev.setCommunicationState(CommunicationState.BUSY);
				dev.setAlarmState(AlarmState.MALFUNCTION);
//				dev.setActivityState(ActivityStateCamera.ERROR);
				dev.setActivityState(ActivityStateCamera.OFF);
				dev.setActivityContinueState(ActivityContinueStateCamera.ERROR);
				dev.setHasImage(false);
				
				resetRTD();

			}else { //No error.

				dev.setCommunicationState(CommunicationState.READY);
				dev.setAlarmState(AlarmState.NONE);

				//ContinueActivity state
				if (this.contextContinue.isExposing())
					dev.setActivityContinueState(ActivityContinueStateCamera.EXPOSING);
				else
					dev.setActivityContinueState(ActivityContinueStateCamera.READY);

				
				dev.setActivityState(ActivityStateCamera.OFF);
				
//				if (contextOneShot.isExposing() /*checkExposingOneShot()*/)
//					dev.setActivityState(ActivityStateCamera.EXPOSING);
//				else
//					dev.setActivityState(ActivityStateCamera.READY);

				//TODO it is necessary to detect the optype (video or image).
				//has image
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

			/*}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;*/
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("getDevice");
		}

	}
	
	private void deleteObsoleteContinueModeFiles() throws Exception{
		
		DeviceProperty privatePath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PRIVATE_PATH");
		
		String folderS = privatePath.getDefaultValue();
		if (!folderS.equals("/home/tadn/public_jee/RTI/db_imgs/")){
			LogUtil.severe(this, "deleteObsoleteContinueModeFiles():: CANNOT DELETE OBSOLETE CONTINUE MODE FILES");
			return;
		}
		
		File dir = new File(folderS);
		
		Date yesterday = DateTools.increment(new Date(), Calendar.DATE, -1);
		for(File file: dir.listFiles()) {
			if (dir.isFile()){
				Date d = new Date(file.lastModified());
				if (d.compareTo(yesterday) <= 0){
					LogUtil.info(this, "deleteObsoleteContinueModeFiles: to delete:" + file.getName());
					if (!file.delete()){
						throw new Exception("Impossible to delete an obsoleteContinueModeFile: " + file.getName());
					}
				}else{
					LogUtil.info(this, "deleteObsoleteContinueModeFiles: to maintain:" + file.getName());
				}
			}
		}
		
	}

	private void transferFile (String UUID, String logMessageHead, boolean generateFits, boolean oneShotMode){
		
		LogUtil.info(this, "CameraRTD.transferFile().BEGIN");
		
		synchronized (lock) {
			
			if (UUID.equals(contextContinue.getUuid()))
				contextContinue.setTransfer(TransferStatus.STARTED);
			else if (UUID.equals(contextOneShot.getUuid()))
				contextOneShot.setTransfer(TransferStatus.STARTED);
			
			RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
			rtc.start();

			//DeviceProperty privatePath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PRIVATE_PATH");
//			DeviceProperty privatePath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PRIVATE_PATH_ONESHOT_MODE");
			DeviceProperty privatePath = null;
			File finalJpgFile = null;

			privatePath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PRIVATE_PATH");
			//privatePath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PRIVATE_PATH_CONTINUE_MODE");
			finalJpgFile = new File(privatePath.getDefaultValue()+ UUID + ".jpg");
			
			
//			String tmpUUID = java.util.UUID.randomUUID().toString();
			File tmpJpgFile = new File(privatePath.getDefaultValue()+ "temp.jpg");
			if (tmpJpgFile.exists() && tmpJpgFile.isFile()) {
				tmpJpgFile.delete();
			}
			

			try {
				String path = camMan.getDefaultImagesPath();
				
				LogUtil.info(this, "CameraRTD.transferFile(). UUID=" + UUID);
				LogUtil.info(this, "CameraRTD.transferFile(). camMan.getDefaultImagesPath=" + camMan.getDefaultImagesPath());

				FileTicket fileticket = camMan.getFile(path, UUID + ".jpg");
				LogUtil.info(this, "CameraRTD.transferFile(). fileTicket=" + fileticket);
				File file = fileticket.getFile();
				
				LogUtil.info(this, "CameraRTD.transferFile(). fileTicket.getFile()=" + file.toString());

				int i= 1;
				while (true){
					if (i>30){
						LogUtil.info(this, "CameraRTD.transferFile(). Time Exceeded.");
						if (UUID.equals(contextContinue.getUuid()))
							contextContinue.setTransfer(TransferStatus.FAILED);
						else if (UUID.equals(contextOneShot.getUuid()))
							contextOneShot.setTransfer(TransferStatus.FAILED); 
						throw new RTException ("Time exceeded");
					}if (file.length() > 0)
						break;
					Thread.sleep(1000);
					LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Waiting for...");
					i++;
				}

				
				try{

					//COPY to temporal JPG file.
					InputStream in = new FileInputStream(file);
					OutputStream out = new FileOutputStream(finalJpgFile);
					OutputStream out2 = new FileOutputStream(tmpJpgFile);
					byte[] buf = new byte[1024];

					int len;
					while ((len = in.read(buf)) > 0){
						out.write(buf, 0, len);
						out2.write(buf, 0, len);
					}

					in.close();
					out.close();
					out2.close();

					//RENAME temporal JPG file --> final JPG file
					//if (finalJpgFile.exists()) finalJpgFile.delete();
					//tmpJpgFile.renameTo(finalJpgFile);

				}finally{
					//if (tmpJpgFile.exists()) tmpJpgFile.delete();
				}				
				
				LogUtil.info(this, "CameraRTD.transferFile(). generating FITS");
				

				//FITS Generation
				File fitsFile = null;
				try{
					
/*					
					if (generateFits){
//						if (UUID.equals(contextContinue.getUuid())){
							fitsFile = new File(privatePath.getDefaultValue()+ UUID + ".fits");
//						}else if (UUID.equals(contextOneShot.getUuid())){
//							fitsFile = new File(file.getAbsolutePath()+ UUID + ".fits");
//						}
						
							LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Generating file=[" + fitsFile.toString() + "]");
							ImagePlus imp = IJ.openImage(tmpJpgFile.toString());
							LogUtil.info(this, "CameraRTD.transferFile(). IJ.openImage: "+imp);
							ImageConverter converter = new ImageConverter(imp);
							LogUtil.info(this, "CameraRTD.transferFile(). ImageConverter: "+converter);
							converter.convertToGray32();
							LogUtil.info(this, "CameraRTD.transferFile(). convertToGray32"+converter+"  "+imp);
							IJ.saveAs(imp,"fits", fitsFile.toString());
							LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Generated file=[" + fitsFile.toString() + "]. Exists=" + fitsFile.exists());
					
							LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Changing fits head=[" + fitsFile.toString() + "]");
							try{
								fitsHeadModification(fitsFile.toString(), UUID + ".fits");
							}catch(Exception ex){
								ex.printStackTrace();
								LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Error changing fits head=[" + fitsFile.toString() + "]");
								throw ex;
							}
					
							//tmpJpgFile.delete();
					
							LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Changed fits head=[" + fitsFile.toString() + "]. Exists="  + fitsFile.exists());

					}*/
					
					if (generateFits){
						ConverterInterface converter = null;
						
						converter = new ConverterNetpbm();
						LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ".Converter:Netpbm");
						//converter = new ConverterImageMagic();
						//LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ".Converter:ImageMagick");
					
						fitsFile = new File(privatePath.getDefaultValue()+ UUID + ".fits");
						LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Generating file=[" + fitsFile.toString() + "]");
						converter.jpegtofits(finalJpgFile, fitsFile, null);
						LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Generated file=[" + fitsFile.toString() + "]");
					
						LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Changing fits head=[" + fitsFile.toString() + "]");
						try{
							fitsHeadModification(fitsFile.toString(), UUID + ".fits");
						}catch(Exception ex){
							ex.printStackTrace();
							LogUtil.severe(this, "CameraRTD.transferFile()." + logMessageHead + ". Error changing fits head=[" + fitsFile.toString() + "]");
						}
						
						//tmpJpgFile.delete();
					
						LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Changed fits head=[" + fitsFile.toString() + "]");
					
					}
					
				}catch(Exception ex){
					
					generateFits = false; //to avoid to upload it.
					
					LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Error generaing FITS" + ex.getMessage());
					
				}finally{
					
					if (tmpJpgFile != null && tmpJpgFile.exists()) tmpJpgFile.delete();
				}
				
				//Continue Mode:  Servlet doesn't use
				if (oneShotMode){					
					fileToRepository(finalJpgFile.getCanonicalPath());					
				}
				if (generateFits){
//					fileToRepository(fitsFile.getAbsolutePath());
					fileToRepository(fitsFile.toString());
				}

				if (UUID.equals(contextContinue.getUuid()))
					contextContinue.setTransfer(TransferStatus.FINISHED);
				else if (UUID.equals(contextOneShot.getUuid()))
					contextOneShot.setTransfer(TransferStatus.FINISHED);
				
				//System.out.println("Imagen transferida");
				LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Transfered File=[" + finalJpgFile.toString() + "]");

				
			} catch (Exception e) {
				if (UUID.equals(contextContinue.getUuid()))
					contextContinue.setTransfer(TransferStatus.FAILED);
				else if (UUID.equals(contextOneShot.getUuid()))
					contextOneShot.setTransfer(TransferStatus.FAILED);
				e.printStackTrace();
			}finally{
				rtc.stop();
				rtc.writeLog("transferFile");
			}
			
		}
		
		LogUtil.info(this, "CameraRTD.transferFile().END");

		
	}
	
	private void transferFitsFile(String UUID, String logMessageHead){
		
		synchronized (lock) {
			
			if (UUID.equals(contextContinue.getUuid()))
				contextContinue.setTransfer(TransferStatus.STARTED);
			else if (UUID.equals(contextOneShot.getUuid()))
				contextOneShot.setTransfer(TransferStatus.STARTED);
			
			RunTimeCounter rtc = new RunTimeCounter("CameraRTD", this.getDeviceId());
			rtc.start();

			//DeviceProperty privatePath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PRIVATE_PATH");
//			DeviceProperty privatePath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PRIVATE_PATH_ONESHOT_MODE");
			DeviceProperty privatePath = null;

			privatePath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PRIVATE_PATH");
			File finalFitsFile = new File(privatePath.getDefaultValue()+ UUID + ".fits");
			File jpgFile = new File(privatePath.getDefaultValue()+ UUID + ".jpg");

			try {
				String path = camMan.getDefaultImagesPath();
				
				//Download fits...

				FileTicket fileticket = camMan.getFile(path, UUID + ".fits");
				File file = fileticket.getFile();

				int i= 1;
				while (true){
					if (i>30){
						if (UUID.equals(contextContinue.getUuid()))
							contextContinue.setTransfer(TransferStatus.FAILED);
						else if (UUID.equals(contextOneShot.getUuid()))
							contextOneShot.setTransfer(TransferStatus.FAILED);
						throw new RTException ("Time exceeded");
					}if (file.length() > 0)
						break;
					Thread.sleep(1000);
					LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Waiting for...");
					i++;
				}

				
				try{

					InputStream in = new FileInputStream(file);
					OutputStream out = new FileOutputStream(finalFitsFile);
					byte[] buf = new byte[1024];

					int len;
					while ((len = in.read(buf)) > 0){
						out.write(buf, 0, len);
					}

					in.close();
					out.close();

					//RENAME temporal JPG file --> final JPG file
					//if (finalJpgFile.exists()) finalJpgFile.delete();
					//tmpJpgFile.renameTo(finalJpgFile);

				}finally{
					//if (tmpJpgFile.exists()) tmpJpgFile.delete();
				}		
				
				//Download jpg
				fileticket = camMan.getFile(path, UUID + ".jpg");
				file = fileticket.getFile();
				try{

					InputStream in = new FileInputStream(file);
					OutputStream out = new FileOutputStream(jpgFile);
					byte[] buf = new byte[1024];

					int len;
					while ((len = in.read(buf)) > 0){
						out.write(buf, 0, len);
					}

					in.close();
					out.close();

					//RENAME temporal JPG file --> final JPG file
					//if (finalJpgFile.exists()) finalJpgFile.delete();
					//tmpJpgFile.renameTo(finalJpgFile);

				}finally{
					//if (tmpJpgFile.exists()) tmpJpgFile.delete();
				}		
				
				
				
				//JPG generation
				/*LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ".Converter:Netpbm");
				ConverterInterface converter = new ConverterNetpbm();
				
				
				File pnmFile = new File(privatePath.getDefaultValue()+ UUID + ".pnm");
				
				LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Generating file=[" + pnmFile.toString() + "]");
				converter.fitstopnm(finalFitsFile, pnmFile);
				LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Generated file=[" + pnmFile.toString() + "]");
				
				LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Generating file=[" + jpgFile.toString() + "]");
				converter.pnmtojpeg(pnmFile, jpgFile);
				LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Generated file=[" + jpgFile.toString() + "]");*/
				
				LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Changing fits head=[" + finalFitsFile.toString() + "]");
				try{
					fitsHeadModification(finalFitsFile.toString(), UUID + ".fits"); 
				}catch(Exception ex){
					ex.printStackTrace();
					LogUtil.severe(this, "CameraRTD.transferFile()." + logMessageHead + ". Error changing fits head=[" + finalFitsFile.toString() + "]");
				}
				
				//pnmFile.delete();
			
				LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Changed fits head=[" + finalFitsFile.toString() + "]");
				
				//upload
				fileToRepository(jpgFile.getCanonicalPath());			
				fileToRepository(finalFitsFile.toString());

				if (UUID.equals(contextContinue.getUuid()))
					contextContinue.setTransfer(TransferStatus.FINISHED);
				else if (UUID.equals(contextOneShot.getUuid()))
					contextOneShot.setTransfer(TransferStatus.FINISHED);
				
				//System.out.println("Imagen transferida");
				LogUtil.info(this, "CameraRTD.transferFile()." + logMessageHead + ". Transfered File=[" + finalFitsFile.toString() + "]");

				
			} catch (Exception e) {
				if (UUID.equals(contextContinue.getUuid()))
					contextContinue.setTransfer(TransferStatus.FAILED);
				else if (UUID.equals(contextOneShot.getUuid()))
					contextOneShot.setTransfer(TransferStatus.FAILED);
				e.printStackTrace();
			}finally{
				rtc.stop();
				rtc.writeLog("transferFile");
			}
			
		}

		
	}
	
	
	private void fitsHeadModification(String fitsFilefullPath, String fitsFileName) throws Exception{
		
		//Exposure time
		double exposureTime = 0;
		try{
			exposureTime = camImpl.getPropertyValue("EXPOSURE");
		}catch(LinkException le){
			throw new CommunicationException(le.getMessage());
		}catch(ServerException se){
			throw new RTException(se.getMessage());
		}
		
		//FilterWheel
		String filterName = "OPEN";
		DeviceProperty filterWheelIdProperty = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "FILTER_WHEEL_ID");
		if (filterWheelIdProperty != null){
			String filterWheelId = filterWheelIdProperty.getDefaultValue();
			RTDFilterWheelInterface filterWheel = (RTDFilterWheelInterface) DeviceDiscoverer.getRTD(filterWheelId);
			if (filterWheel != null){
				filterName = filterWheel.fwGetFilterKind();
				LogUtil.info(this, "CameraRTD.fitsHeadModification(). Filter returned by the FilterWheel:" + filterName);
			}
		}
		
		
		//Mount radec position
		DeviceProperty mountIdProperty = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "MOUNT_ID");
		String mountId = mountIdProperty.getDefaultValue();
		
		RTDMountInterface mount = (RTDMountInterface) DeviceDiscoverer.getRTD(mountId);
		String ra = String.valueOf(mount.mntGetPosAxis1());
		String dec = String.valueOf(mount.mntGetPosAxis2());
		LogUtil.info(this, "CameraRTD.fitsHeadModification(). Pointed Object. Ra=" + ra + ", Dec=" + dec);
		String pointedObject = mount.mntGetPointedObject();
		LogUtil.info(this, "CameraRTD.fitsHeadModification(). Pointed Object Name:" + pointedObject);
		
		/*String ra = null;
		String dec = null;
		
		
		//TO REMOVE....radec from sun
		double longitude = Config.getPropertyDouble("rt_config", "rts_longitude");
		double latitude = Config.getPropertyDouble("rt_config", "rts_latitude");
		Catalogue catalogue = new Catalogue(longitude, latitude, 0);
		ObjInfo objInfo = catalogue.getObject("sun");
		if (objInfo == null){
			LogUtil.info(this, "CameraRTD.fitsHeadModification:: Object NOT found:" + "sun");
			throw new RTException("Not Object Found");
		}else{
			Radec pos = objInfo.getPosition();
			dec = String.valueOf(pos.getDecDecimal());
			ra = String.valueOf(pos.getRaDecimal());
		}*/
		
		//DateTime 
		Date now = new Date();
		Date gmtNow = DateTools.getGMT(now);
		String date = DateTools.getDate(gmtNow, "yyyy-MM-dd");
		String time = DateTools.getDate(gmtNow, "HH:mm:ss");
		String dateTime = date + "T" + time;
		
		//Get the header in order to transform it.
		DataInputStream dis = new DataInputStream( new BufferedInputStream( new FileInputStream(fitsFilefullPath) ) );
		FitsHeader headerfile = new FitsHeader(dis);
		//System.out.println(headerfile.toString());			
		
		// Access to the different elements that are part of the header.
		FitsKeyword parameter;
		
		// Transformation of the first keyword.
		parameter = headerfile.getKeyword(0);
		headerfile.removeKeywordAt(0); //removes the api fits generator message (by)
		parameter.setName("SIMPLE");
		//parameter.setValue("T");
		parameter.setValue(true);
		parameter.setComment("FITS Standard");
		headerfile.insertKeywordAt(parameter, 0);
		
		//Add more keywords:
		eu.gloria.rtc.op.OpInfo opInfo = OpManager.getOpManager().getOp();
		String user = opInfo.getUser();
		if (user== null || user.isEmpty()) user= "GLORIA";
		
		headerfile.addKeyword(new FitsKeyword("NAME", fitsFileName, "Name file"));
		headerfile.addKeyword(new FitsKeyword("TELESCOP", "TADn", "Telescope name"));
		headerfile.addKeyword(new FitsKeyword("OBSERVER", user, "User name"));
		headerfile.addKeyword(new FitsKeyword("OBJECT", pointedObject, "Object name given by the user"));
		headerfile.addKeyword(new FitsKeyword("FILTER", /*filterName*/ "no filter", "Type of filter used"));
		headerfile.addKeyword(new FitsKeyword("EXPTIME", exposureTime, "Exposure time in seconds"));
		headerfile.addKeyword(new FitsKeyword("DAT-OBS", dateTime, "UT start of exposure"));
		//headerfile.addKeyword(new FitsKeyword("JD", "2456353.50833333", "exposure JD"));
		headerfile.addKeyword(new FitsKeyword("LAT", Config.getProperty("rt_config", "rts_latitude"), "Latitude"));
		headerfile.addKeyword(new FitsKeyword("LNG", Config.getProperty("rt_config", "rts_longitude"), "Longitude"));
		headerfile.addKeyword(new FitsKeyword("RA", ra, "RA coordenate"));
		headerfile.addKeyword(new FitsKeyword("DEC", dec, "DEC coordenate"));
		headerfile.addKeyword(new FitsKeyword("IMAGETYP", "SKY", "Type of Image"));
		
		headerfile.addKeyword(new FitsKeyword("EQUINOX ", "J2000", ""));
		headerfile.addKeyword(new FitsKeyword("DETECTOR ", "Sony DBK", ""));
					
		// Obtain the rest of the image data.
		FitsData v = new FitsData(headerfile,dis,true);
		dis.close();
		FitsHDUnit p = new FitsHDUnit(headerfile,v);
		DataOutputStream dos = new DataOutputStream( new BufferedOutputStream( new FileOutputStream(fitsFilefullPath) ) );
		
		// Write a new file with the header modified and the same image data.
		p.writeFile(dos);
		dos.close();
		
	}
	
	private synchronized void resetRTD(){
		
		boolean errorCancelling = false;
		
		//Cancel all operations.
		//-----------------------------------
		//CHANGE REQUEST[20130524]: commented cancelALL
		//try{
		//	camImpl.cancelAll();
		//	
		//	DeviceProperty defaultPath = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "STORAGE_PATH");
		//	camMan.setDefaultImagesPath(defaultPath.getDefaultValue());
		//	
		//}catch(Exception ex){
		//	LogUtil.severe(this, "CameraRTD.resetRTD().Cancelling operations. Error:" + ex.getMessage());
		//	errorCancelling = true;
		//}
		//-----------------------------------
		
		//if (!errorCancelling){
			
			try{
				
				timerOneShot.cancel();
				timerOneShot = null;
				
			}catch(Exception ex){
				LogUtil.severe(this, "CameraRTD.resetRTD(). Stop the thread - timerOneShot. Error:" + ex.getMessage());
			};
			
			//try{
			//	
			//	timerContinue.cancel();
			//	timerContinue = null;
			//	
			//}catch(Exception ex){
			//	LogUtil.severe(this, "CameraRTD.resetRTD().Stop the thread. Error:" + ex.getMessage());
			//};

			
			//this.contextContinue.reset();
			this.contextOneShot.reset();
			
		//}
		
	}
	
	private synchronized void resetRTDForOneShotTimeOut(){
		
		try{
				
			timerOneShot.cancel();
			timerOneShot = null;
				
		}catch(Exception ex){
			LogUtil.severe(this, "CameraRTD.resetRTDForOneShotTimeOut(). Stop the thread - timerOneShot. Error:" + ex.getMessage());
		};
			
		this.contextOneShot.reset();
			
	}
	
	@Override
	public Image camGetImage(ImageFormat format) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public List<ImageFormat> camGetOneShotModeImageFormats() throws RTException {
		
		List<ImageFormat> result = new ArrayList<ImageFormat>();
		result.add(ImageFormat.JPG);
		///result.add(ImageFormat.FITS);
		return result;
	}
	
	@Override
	public List<ImageFormat> camGetContinueModeImageFormats() throws RTException {
		
		List<ImageFormat> result = new ArrayList<ImageFormat>();
		result.add(ImageFormat.JPG);
		return result;
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
						file.setUuid(contextOneShot.getUuid());
						
						if (fileFormat == FileFormat.JPG){
							dbProxy.getProxy().fileCreate(idOp, file);

							LogUtil.info(this, "CameraRTD.fileToRepository(" + idOp + "). CREATED GLORIA file UUID=" + file.getUuid());
						}

					}catch(Exception ex){
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
			contextOneShot.setTransfer(TransferStatus.FAILED);
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

	public class Task extends TimerTask { 
		@Override
		public void run() {

			RunTimeCounter rtc = new RunTimeCounter("CameraRTD.[ONESHOT-CONTINUE]", getDeviceId());
			rtc.start();

			try{

				LogUtil.info(this, "CameraRTD.[ONESHOT-CONTINUE] TimerTask.run [ONESHOT] BEGIN");
				
				try{
					checkExposingOneShot();
				}catch(Exception ex){};
				
				LogUtil.info(this, "CameraRTD.[ONESHOT-CONTINUE] TimerTask.run  [ONESHOT] END");
				
				LogUtil.info(this, "CameraRTD.[ONESHOT-CONTINUE] TimerTask.run  [CONTINUE] BEGIN");
				
				checkExposingContinue();
				
				LogUtil.info(this, "CameraRTD.[ONESHOT-CONTINUE] TimerTask.run  [CONTINUE] BEGIN");

			}finally{
				rtc.stop();
				rtc.writeLog("TimerTask.run");
			}

		}		
	}
	
	public class TimerTaskOneShotRecovering extends TimerTask {
		@Override
		public void run() {

			RunTimeCounter rtc = new RunTimeCounter("CameraRTD.[ONESHOT]", getDeviceId());
			rtc.start();

			try{

				try{
					checkExposingOneShot();
				}catch(Exception ex){
					LogUtil.info(this, "CameraRTD.[ONESHOT] TimerTaskOneShotRecovering.run(). Error:" + ex.getMessage());
				};
				
				//checkExposingContinue();

			}finally{
				rtc.stop();
				rtc.writeLog("TimerTaskOneShotRecovering.run");
			}

		}		
	}
	
	public class TimerTaskContinueRecovering extends TimerTask {
		@Override
		public void run() {

			RunTimeCounter rtc = new RunTimeCounter("CameraRTD", getDeviceId());
			rtc.start();

			try{

				LogUtil.info(this, "CameraRTD. TimerTaskContinueRecovering.run BEGIN");
				
				checkExposingContinue();

			}finally{
				rtc.stop();
				rtc.writeLog("TimerTaskContinueRecovering.run");
			}

		}		
	}
	
	public enum TransferStatus{
		
		NOT_STARTED,
		STARTED,
		FINISHED,
		FAILED
	}
	
	public class ContextOneShot{
		
		private long exposureTime;
		private TimeOut timeOut;
		private boolean exposing;
		private String uuid;
		private TransferStatus transfer;
		private long timeoutLong;
		
		public ContextOneShot(long timeout){
			this.timeoutLong = timeout;
			this.exposureTime = 0;
			this.timeOut = new TimeOut(timeout);
			this.exposing = false;
			this.uuid = null;
			this.transfer = TransferStatus.NOT_STARTED;
		}
		
		public void reset(){
			this.exposureTime = 0;
			this.timeOut = new TimeOut(timeoutLong);
			this.exposing = false;
			this.uuid = null;
		}

		public long getExposureTime() {
			return exposureTime;
		}

		public void setExposureTime(long exposureTime) {
			this.exposureTime = exposureTime;
		}

		public TimeOut getTimeOut() {
			return timeOut;
		}

		public void setTimeOut(TimeOut timeOut) {
			this.timeOut = timeOut;
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

		public TransferStatus getTransfer() {
			return transfer;
		}

		public void setTransfer(TransferStatus transfer) {
			this.transfer = transfer;
		}
		
		
	}
	
	public class ContextContinue{
		
		private long timeoutLong;
		private Date fileDate;
		private boolean exposing;
		private String uuid;
		private TimeOut timeOut;
		private TransferStatus transfer;
		
		public ContextContinue(long timeout){
			this.timeoutLong = timeout;
			this.fileDate = null;
			this.exposing = false;
			this.uuid = null;
			timeOut = new TimeOut(timeoutLong);
			this.transfer = TransferStatus.NOT_STARTED;
		}
		
		public void reset(){
			this.fileDate = null;
			this.exposing = false;
			this.uuid = null;
			timeOut = new TimeOut(timeoutLong);
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

	
}
