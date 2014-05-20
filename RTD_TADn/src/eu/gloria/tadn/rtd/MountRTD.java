package eu.gloria.tadn.rtd;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import org.ciclope.server.telescope.gemini2.ServicesTelescopeGemini2Impl;
import org.ciclope.server.telescope.gemini.ServiceTelescopeGeminiConstants;

import eu.gloria.rt.catalogue.Catalogue;

import eu.gloria.rt.catalogue.CatalogueTools;
import eu.gloria.rt.catalogue.ObjInfo;
import eu.gloria.rt.catalogue.Observer;
import eu.gloria.rt.entity.device.ActivityStateMount;
import eu.gloria.rt.entity.device.AlarmState;
import eu.gloria.rt.entity.device.AxisRateType;
import eu.gloria.rt.entity.device.BlockState;
import eu.gloria.rt.entity.device.CommunicationState;
import eu.gloria.rt.entity.device.Device;
import eu.gloria.rt.entity.device.DeviceMount;
import eu.gloria.rt.entity.device.DeviceType;
import eu.gloria.rt.entity.device.MeasureUnit;
import eu.gloria.rt.entity.device.MountPointingModel;
import eu.gloria.rt.entity.device.TrackingRateType;
import eu.gloria.rt.entity.environment.config.device.DeviceProperty;
import eu.gloria.rt.exception.RTException;
import eu.gloria.rt.exception.UnsupportedOpException;
import eu.gloria.rt.unit.Altaz;
import eu.gloria.rt.unit.Radec;
import eu.gloria.rtd.RTDMountInterface;
import eu.gloria.tools.configuration.Config;
import eu.gloria.tools.conversion.DegreeFormat;
import eu.gloria.tools.log.LogUtil;
import eu.gloria.tools.time.RunTimeCounter;
import eu.gloria.tools.time.TimeOut;

/**
 * TAD - MOUNT - RTD
 * @author jcabello
 * 
 * Pending:
 *   - Action item: Position (axis).
 *   - Action item: SlewObject (it does not exist).
 *   - Action item: park
 *   
 * Error:
 *   - Impossible to disable the tracking. trackingspeed=0 does not work.
 *
 */
public class MountRTD extends DeviceRTD implements RTDMountInterface {
	
	/**
	 * Internal Service.
	 */
	private ServicesTelescopeGemini2Impl service;
	
	private String ip;
	private String user;
	private String pw;
	private long mountStateTimeRecovering;
	private String mountStateXml;
	private long movementTimeout;
	private String pointedObject;
	private Movement movement;
	
	public static void main(String[] args) {
		
		try {
			
//			ServicesTelescopeGemini2Impl service = new ServicesTelescopeGemini2Impl("http://161.72.128.201/", "admin", "admtads");
//			
//			String kk = service.getStatusXML();
//			
//			Radec radec2 = new Radec("+80:01:02", DegreeFormat.HHMMSS, "+80:01:02" , DegreeFormat.DDMMSS);
//			double kk1 = radec2.getDecDecimal();
//			
//			double kk2 = radec2.getRaDecimal();
//			
			
			  
			
			MountRTD rtd = new MountRTD("Gemini");
			DeviceMount dev = (DeviceMount) rtd.devGetDevice(true);
			
			System.out.println(dev.getActivityState()+"\n");
			
			System.out.println(rtd.mntGetPosAxis1()+"\n");
			System.out.println(rtd.mntGetPosAxis2()+"\n");
			
//			rtd.mntSetSlewRate("SLEW");
//			
//			double pos1 = rtd.mntGetPosAxis1();
//			double pos2 = rtd.mntGetPosAxis2();
//			
//			boolean parked = rtd.mntIsParked();
//			rtd.mntPark();
			/*parked = rtd.mntIsParked();
			rtd.mntMoveEast();
			parked = rtd.mntIsParked();*/
			
			/*rtd.mntMoveEast();
			rtd.mntMoveNorth();
			rtd.mntMoveSouth();
			rtd.mntMoveWest();*/
			
			/*rtd.mntSlewObject("0");
			
			System.out.println(dev.getActivityState());
			
			rtd.mntSlewObject("3");

			rtd.mntStopSlew();*/
			
			/*rtd.mntSlewObject("0");
			
			System.out.println(dev.getActivityState());
			System.out.println(dev.getActivityState());*/
			
			//rtd.mntSlewObject("0");
			//rtd.mntSlewObject("3");

//			rtd.mntStopSlew();
			
			/*rtd.mntSetTrackingRate(2);
			rtd.mntSetTracking(true);
			boolean tracking = rtd.mntGetTracking();
			rtd.mntSetTracking(false);
			tracking = rtd.mntGetTracking();*/
			
			
		
			/*if (dev.getActivityState() == ActivityStateMount.MOVING){
				rtd.mntStopSlew();
			}
			
			dev = (DeviceMount) rtd.getDevice();
			
			if (dev.getActivityState() == ActivityStateMount.TRACKING){
				rtd.mntSetTracking(false);
			}*/
			
			/*dev = (DeviceMount) rtd.getDevice();
			
			rtd.mntSetTrackingRate(TRACKING_MODE_SIDEREAL);
			rtd.mntSetTracking(true);*/
			
			/*int count = 20;
			
			for (int x = 0; x < count; x++){
				rtd.mntMoveNorth();
			}
			
			/*for (int x = 0; x < count; x++){
				rtd.mntMoveSouth();
			}
			
			for (int x = 0; x < count; x++){
				rtd.mntMoveEast();
			}
			
			for (int x = 0; x < count; x++){
				rtd.mntMoveWest();
			}*/
			
			
			System.out.println("");
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Traking mode.
	 */
	private TrackingRateType trackingMode;
	
	public MountRTD(String deviceId) {
		
		super(deviceId);
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		ip = Config.getProperty("telescopes", "telescope.ip.1");
		user = Config.getProperty("telescopes", "telescope.username.1");
		pw = Config.getProperty("telescopes", "telescope.password.1");
		
		mountStateTimeRecovering = 0;
		mountStateXml = null;
		
		DeviceProperty movementTimeoutProp = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "MOVEMENT_TIMEOUT");
		try{
			this.movementTimeout = Long.parseLong(movementTimeoutProp.getDefaultValue());
		}catch(Exception ex){
			this.movementTimeout = 120000;
		}
		
		LogUtil.info(this,"MountRTD.Constructor().ServicesTelescopeGemini2Impl. ip="   + ip );
		LogUtil.info(this,"MountRTD.Constructor().ServicesTelescopeGemini2Impl. user=" + user );
		LogUtil.info(this,"MountRTD.Constructor().ServicesTelescopeGemini2Impl. pw="   + pw );
		
		this.service = new ServicesTelescopeGemini2Impl(ip, user, pw);
		
		
		rtc.stop();
		rtc.writeLog("MountRTD");

	}
	
	private void waitMovement() throws RTException, InterruptedException{ 
		
		LogUtil.info(this,"MountRTD.waitMovement(). initial wait: 2000" );
		Thread.sleep(2000); //sleep 2 seconds.
		TimeOut timeOut = new TimeOut(this.movementTimeout);
		while (true){
			
			DeviceMount device = (DeviceMount)devGetDevice(false);
			LogUtil.info(this,"MountRTD.waitMovement(). checking state=" + device.getActivityState() );
			if (device.getActivityState() == ActivityStateMount.STOP){
				LogUtil.info(this,"MountRTD.waitMovement(). Mount stop. Movement got success!!!." );
				break;
			}
			
			if (timeOut.timeOut()){
				throw new RTException("Mount movement timout exceeded:" + this.movementTimeout);
			}
			
			LogUtil.info(this,"MountRTD.waitMovement(). waiting.....: 2000" );
			Thread.sleep(2000); //sleep 2 seconds.
		}
	}
	
	private String getStatusXML() throws Exception{ 
		
		Date now = new Date();
		
		if (mountStateXml != null && now.getTime() <= mountStateTimeRecovering + 10000){
			//nothing..returning the cached state...
		}else{
			
			/*LogUtil.info(this,"MountRTD.Constructor().ServicesTelescopeGemini2Impl. ip="   + ip );
			LogUtil.info(this,"MountRTD.Constructor().ServicesTelescopeGemini2Impl. user=" + user );
			LogUtil.info(this,"MountRTD.Constructor().ServicesTelescopeGemini2Impl. pw="   + pw );
			
			ServicesTelescopeGemini2Impl serv = new ServicesTelescopeGemini2Impl(ip, user, pw);*/
			
			mountStateXml = service.getStatusXML();
			mountStateTimeRecovering = now.getTime();
			
		}
		
		return mountStateXml;
		
		
//		String result = null;
//		
//		try {
//			Thread.sleep(200);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		String lastError = "";
//		for (int x = 0;x < 3; x++){
//			try{
//				result =  service.getStatusXML();
//			}catch(Exception ex){
//				ex.printStackTrace();
//				lastError = ex.getMessage();
//			}
//			
//			if (result != null) break;
//		}
//		
//		if (result == null) throw new Exception("Impossible to retrieve the XMLStatus. " + lastError);
//		
//		return result;
	}
	
	private String getServiceConfiguration(){
		
		String[] names = {
			"ip",
			"user",
			"pw"
		};
		
		String[] values = {
				this.ip,
				this.user,
				this.pw
		};
		
		return LogUtil.getLog(names, values);
	}
	
	/**
	 * Returns true if the gemini service returned code is an error.
	 * @param codError Service error code.
	 * @return boolean.
	 */
	private boolean isGeminiError(int codError){
		return (codError != ServiceTelescopeGeminiConstants.METHOD_OK);
	}
	

	/**
	 * Translate the TAD mount gemini.
	 * @param codError TAD mount gemini error
	 * @return Description.
	 */
	private String getGeminiError(int codError){
		
		String result = "";
		
		switch(codError){
			case ServiceTelescopeGeminiConstants.METHOD_OK:
				result = "NO ERROR. OK.";
				break;
			case ServiceTelescopeGeminiConstants.METHOD_ERROR:
				result = "ERROR: METHOD_ERROR.";
				break;
			case ServiceTelescopeGeminiConstants.METHOD_ERROR_RA:
				result = "ERROR. METHOD_ERROR_RA.";
				break;
			case ServiceTelescopeGeminiConstants.METHOD_ERROR_SERIAL:
				result = "ERROR. METHOD_ERROR_SERIAL.";
				break;
			case ServiceTelescopeGeminiConstants.RA_INVALID_RESPONSE:
				result = "ERROR. RA_INVALID_RESPONSE";
				break;
			case ServiceTelescopeGeminiConstants.DEC_INVALID_RESPONSE:
				result = "ERROR. DEC_INVALID_RESPONSE";
				break;
			case ServiceTelescopeGeminiConstants.HORIZON_SLEW_RESPONSE:
				result = "ERROR. HORIZON_SLEW_RESPONSE";
				break;
			case ServiceTelescopeGeminiConstants.OBJECT_SELECTED_RESPONSE:
				result = "ERROR. OBJECT_SELECTED_RESPONSE";
				break;
			case ServiceTelescopeGeminiConstants.MANUAL_CONTROL_RESPONSE:
				result = "ERROR. MANUAL_CONTROL_RESPONSE";
				break;
			case ServiceTelescopeGeminiConstants.METHOD_ERROR_STOP:
				result = "ERROR. METHOD_ERROR_STOP.";
				break;
			case ServiceTelescopeGeminiConstants.METHOD_ERROR_ALT:
				result = "ERROR. METHOD_ERROR_ALT";
				break;
			default:
				result = "UNKNOWN ERROR(" + codError + ")";
		}
		
		return result;
	}
		

	@Override
	public Date mntGetUtcClock() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public void mntSetUtcClock(Date date) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public double mntGetSiderealDate() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean mntIsAtHome() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean mntIsParked() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			//Based on the xml state file.
			String xml = getStatusXML();
			MountStateXmlParser state = new MountStateXmlParser(xml);
			return state.isParked();
			
		}catch(RTException ex){
			LogUtil.severe(this, "Error.ServiceConfiguration::" + getServiceConfiguration());
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, "Error.ServiceConfiguration::" + getServiceConfiguration());
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntIsParked");
		}
	}

	@Override
	public double mntGetTargetRightAscension() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public double mntGetTargetDeclination() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public double mntGetTrackingDeclinationRate() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public double mntGetTrackingAscensionRate() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public TrackingRateType mntGetTrackingRate() throws RTException {
		
		return this.trackingMode;
		
	}

	@Override
	public void mntSetTrackingRate(TrackingRateType rate) throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			if (rate != TrackingRateType.DRIVE_SIDEREAL  && rate != TrackingRateType.DRIVE_SOLAR){
				throw new RTException("SUPPORTED TRACKING MODES: SOLAR(2), SIDEREAL(0)");
			}
			
			if (this.mntGetTracking()){
				throw new RTException("Mount is tracking. Stop it first.");
			}
			
			this.trackingMode = rate;
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntSetTrackingRate");
		}

	}
	
	

	@Override
	public boolean mntGetTracking() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			//Based on the xml state file.
			String xml = getStatusXML();
			MountStateXmlParser state = new MountStateXmlParser(xml);
			return state.isTracking();
			
		}catch(RTException ex){
			LogUtil.severe(this, "Error.ServiceConfiguration::" + getServiceConfiguration());
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, "Error.ServiceConfiguration::" + getServiceConfiguration());
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntGetTracking");
		}
		
	}
	
	private int convertRTDTrackingRateToDriver(TrackingRateType inputRate) throws RTException{
		
		switch(inputRate){
		case DRIVE_SIDEREAL:
			return 0;
		case DRIVE_LUNAR:
			throw new RTException("SUPPORTED TRACKING MODES: SOLAR(2), SIDEREAL(0)");	
		case DRIVE_SOLAR:
			return 3;
		case DRIVE_KING:
			throw new RTException("SUPPORTED TRACKING MODES: SOLAR(2), SIDEREAL(0)");
		default:
			throw new RTException("SUPPORTED TRACKING MODES: SOLAR(2), SIDEREAL(0)");	
		}
		
	}

	@Override
	public void mntSetTracking(boolean value) throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			if (value){ //start tracking...
				
				//Checks...
				if (this.mntGetTracking()) throw new RTException ("Already tracking.");
				
				int trackingRate = convertRTDTrackingRateToDriver(this.trackingMode);
				
				LogUtil.info(this, "mntSetTracking.Begin. TrackinMode=" + trackingMode);
				
				int code1 = 0;
				
				for (int x = 0; x < 3; x++){
					code1 = service.setTrackingSpeed(trackingRate); 
					if (!isGeminiError(code1)){
						break;
					}
				}
				
				if (isGeminiError(code1)){
					throw new RTException(getGeminiError(code1), code1);
				}
				
				//removes the cache
				mountStateXml = null;
				
				LogUtil.info(this, "mntSetTracking.END. code=" + code1);
				
				//TODO verify this code is not needed.
				/*int code2 = service.startTracking(0);
				if (isGeminiError(code2)){
					throw new RTException(getGeminiError(code2), code2);
				}*/
				
			}else{ //stop tracking....
				
				//Checks...
				//if (!this.mntGetTracking()) throw new RTException ("Mount is not tracking.");
				
				//Set to terrestial (STOP)
				int code1 = 0;
				for (int x = 0; x < 3; x++){
					code1 = service.setTrackingSpeed(4); //terrestial
					if (!isGeminiError(code1)) break;
				}
				
				if (isGeminiError(code1)){
					throw new RTException(getGeminiError(code1), code1);
				}
				
				//removes the cache
				mountStateXml = null;
				
			}
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntSetTracking");
		}

	}

	@Override
	public double mntGetGuideRateDeclination() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public double mntGetDeclinationRateRightAscension() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean mntIsSlewing() throws RTException {
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			//Based on the xml state file.
			String xml = getStatusXML();
			MountStateXmlParser state = new MountStateXmlParser(xml);
			return state.isMoving();
		}catch(RTException ex){
			LogUtil.severe(this, "Error.ServiceConfiguration::" + getServiceConfiguration());
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, "Error.ServiceConfiguration::" + getServiceConfiguration());
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntIsSlewing");
		}
	}

	@Override
	public double mntGetPosAxis1() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			String xml = getStatusXML();
			MountStateXmlParser state = new MountStateXmlParser(xml);
			
			tracerMountPosition(state);
			
			String value = state.getRa();
			
			Radec radec = new Radec(value, DegreeFormat.HHMMSS, "0:0:0", DegreeFormat.DDMMSS);
			
			return radec.getRaDecimal();
			
		}catch(RTException ex){
			LogUtil.severe(this, "Error.ServiceConfiguration::" + getServiceConfiguration());
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, "Error.ServiceConfiguration::" + getServiceConfiguration());
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntGetPosAxis1");
		}
		
	}
	
	private void tracerMountPosition(MountStateXmlParser state) throws RTException{
		
		Radec radec = new Radec(state.getRa(), DegreeFormat.HHMMSS, state.getDec(), DegreeFormat.DDMMSS);
		Observer observer = new Observer();
		observer.setLongitude(Config.getPropertyDouble("rt_config", "rts_longitude"));
		observer.setLatitude(Config.getPropertyDouble("rt_config", "rts_latitude"));
//		Altaz altaz = CatalogueTools.getAltazByRadec(observer, new Date(), radec);
		
		String[] names = {
				"RA",
				"DEC",
				//"Alt",
				//"AZ"
		};
		
		String[] values = {
				String.valueOf(radec.getRaDecimal()),
				String.valueOf(radec.getDecDecimal()),
				//String.valueOf(altaz.getAltDecimal()),
				//String.valueOf(altaz.getAzDecimal())
		};
		
		LogUtil.info(this, "MountPosition::" + LogUtil.getLog(names, values));
		
	}

	@Override
	public double mntGetPosAxis2() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			String xml = getStatusXML();
			MountStateXmlParser state = new MountStateXmlParser(xml);
			
			tracerMountPosition(state);
			
			String value = state.getDec();
			
			Radec radec = new Radec("0:0:0", DegreeFormat.HHMMSS, value , DegreeFormat.DDMMSS);

			return radec.getDecDecimal();
			
		}catch(RTException ex){
			LogUtil.severe(this, "Error.ServiceConfiguration::" + getServiceConfiguration());
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, "Error.ServiceConfiguration::" + getServiceConfiguration());
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntGetPosAxis2");
		}
	}

	@Override
	public double mntGetPosAxis3() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean mntCanPulseGuide() throws RTException {
		return false;
	}

	@Override
	public boolean mntCanSetGuideRates() throws RTException {
		return false;
	}

	@Override
	public boolean mntCanSetPark() throws RTException {
		return true;
	}

	@Override
	public void mntSetPark(double ascension, double declination)
			throws RTException {
		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public double mntGetALTParkPos() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public double mntGetAZParkPos() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean mntCanSetTracking() throws RTException {
		return true;
	}

	@Override
	public boolean mntCanSetTrackingRate() throws RTException {
		return true;
	}
	
	@Override
	public boolean mntCanSlewCoordinates() throws RTException {
		return true;
	}

	@Override
	public boolean mntCanSlewCoordinatesAsync() throws RTException {
		return true;
	}
	
	@Override
	public boolean mntCanSlewObject() throws RTException {
		return true;
	}

	@Override
	public boolean mntCanSlewAltAz() throws RTException {
		return false;
	}

	@Override
	public boolean mntCanSlewAzAsync() throws RTException {
		return false;
	}

	@Override
	public boolean mntCanMoveAzis() throws RTException {

		return false;
	}

	@Override
	public List<AxisRateType> mntAxisRate() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public List<TrackingRateType> mntTrackingRates() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		List<TrackingRateType> list = new ArrayList <TrackingRateType> ();
				
		list.add(TrackingRateType.DRIVE_SIDEREAL);
		list.add(TrackingRateType.DRIVE_SOLAR);
		
		
		rtc.stop();
		rtc.writeLog("mntTrackingRates");

		
		return list;
	}

	@Override
	public void mntGoHome() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void mntPark() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			/*pointedObject = null;
			
			this.mntSetTracking(false);
			
			int errorCode = service.parkTelescope();
			if ( errorCode != 0){
				throw new  RTException("Impossible to park.", errorCode) ;
			}*/
			
			if (mntCreateMovementPark()){
				pointedObject = null;
			}else{
				throw new  RTException("Impossible to park, already slewing.") ;
			}
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntPark");
		}

	}

	@Override
	public void mntUnpark() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			if (!mntIsParked()) throw new RTException("The mount is not parked.");
			
			//Move to north...
			mntMoveNorth();
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntMoveNorth");
		}

	}

	@Override
	public void mntSlewToAltAz(double azimuth, double altitude)
			throws RTException {
		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void mntSlewToAltAzAsync(double azimuth, double altitude)
			throws RTException {
		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void mntSlewToCoordinates(double ascension, double declination)
			throws RTException {
		
		pointedObject = null;
		
		//mntSlewToCoordinatesCommon(ascension, declination);
		mntCreateMovement(ascension, declination);

	}
	
	private void mntSlewToCoordinatesCommon(double ascension, double declination) throws RTException {
		
		LogUtil.info(this,"MountRTD.mntSlewToCoordinatesCommon(ra=" + ascension + ", dec=" + declination + "). BEGIN" );
		
		
		boolean tracking = false;
		
		try{
		
			if (mntIsSlewing()){
			
				LogUtil.info(this,"MountRTD.mntSlewToCoordinatesCommon(ra=" + ascension + ", dec=" + declination + "). Is already slewing...aborting the new movement." );
			
			}else{
			
				LogUtil.info(this,"MountRTD.mntSlewToCoordinatesCommon(ra=" + ascension + ", dec=" + declination + "). Is not slewing...requesting the movement." );
				
				tracking = mntGetTracking();
				if (tracking) mntSetTracking(false);
		
				Radec radec = new Radec(ascension, declination);  
				String ra = radec.getRaString(DegreeFormat.HHMMSS);
				String dec = radec.getDecString(DegreeFormat.DDMMSS);
				int code = 0;
				for (int x = 0; x < 3; x++){
					code = service.slewToCoords(ra, dec , "HH:MM:SS");
					if (code == 0) break;
				}
			
				waitMovement();
			
		
				LogUtil.info(this,"MountRTD.mntSlewToCoordinatesCommon(ra=" + ascension + ", dec=" + declination + ", code=" + code + ")" );
			}
		
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			if (tracking) mntSetTracking(true);
		}

	}

	@Override
	public void mntSlewToCoordinatesAsync(double ascension, double declination)
			throws RTException {
		mntSlewToCoordinates(ascension, declination);

	}

	@Override
	public void mntMoveAxis(int axisType, double rate) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void mntPulseGuide(int guideDirection, int duration)
			throws RTException {
		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void mntStopSlew(int axisType) throws RTException {
		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void mntStopSlew() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			mntStopCurrentMovement();
			
			pointedObject = null;
			
			int code = service.stopMovement();
			if (isGeminiError(code)){
				throw new RTException(getGeminiError(code), code);
			}
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntStopSlew");
		}

	}

	@Override
	public MountPointingModel mntGetPointingModel() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public void mntMoveNorth() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		/*boolean tracking = false;
		
		try{
			
			tracking = mntGetTracking();
			if (tracking) mntSetTracking(false);
			
			int code = service.moveNorth(3, 3000);
			if (isGeminiError(code)){
				throw new RTException(getGeminiError(code), code);
			}
			
			waitMovement();
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			
			if (tracking) mntSetTracking(true);
			
			rtc.stop();
			rtc.writeLog("mntMoveNorth");
		}*/
		
		try{
			
			if (mntCreateMovementCardinalPoint(MovementType.MOVE_NORTH)){
				pointedObject = null;
			}else{
				throw new  RTException("Impossible to move to NORTH, already slewing.") ;
			}
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			
			rtc.stop();
			rtc.writeLog("mntMoveNORTH");
		}
		
	}

	@Override
	public void mntMoveEast() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		/*boolean tracking = false;
		
		try{
			
			tracking = mntGetTracking();
			if (tracking) mntSetTracking(false);
			
			int code = service.moveEast(3, 3000);
			if (isGeminiError(code)){
				throw new RTException(getGeminiError(code), code);
			}
			
			waitMovement();
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			
			if (tracking) mntSetTracking(true);
			
			rtc.stop();
			rtc.writeLog("mntMoveEast");
		}*/
		
		try{
			
			if (mntCreateMovementCardinalPoint(MovementType.MOVE_EAST)){
				//Nothing
			}else{
				throw new  RTException("Impossible to move to EAST, already slewing.") ;
			}
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			
			rtc.stop();
			rtc.writeLog("mntMoveEast");
		}
		
	}

	@Override
	public void mntMoveSouth() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		/*boolean tracking = false;
		
		try{
			
			tracking = mntGetTracking();
			if (tracking) mntSetTracking(false);
			
			int code = service.moveSouth(3, 3000);
			if (isGeminiError(code)){
				throw new RTException(getGeminiError(code), code);
			}
			
			waitMovement();
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			
			if (tracking) mntSetTracking(true);
			
			rtc.stop();
			rtc.writeLog("mntMoveSouth");
		}*/
		
		try{
			
			if (mntCreateMovementCardinalPoint(MovementType.MOVE_SOUTH)){
				//NOTHING
			}else{
				throw new  RTException("Impossible to move to SOUTH, already slewing.") ;
			}
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			
			rtc.stop();
			rtc.writeLog("mntMoveSouth");
		}
		
	}

	@Override
	public void mntMoveWest() throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		/*boolean tracking = false;
		
		try{
			
			tracking = mntGetTracking();
			if (tracking) mntSetTracking(false);
			
			int code = service.moveWest(3, 3000);
			if (isGeminiError(code)){
				throw new RTException(getGeminiError(code), code);
			}
			
			waitMovement();
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			
			if (tracking) mntSetTracking(true);
			
			rtc.stop();
			rtc.writeLog("mntMoveWest");
		}*/
		
		try{
			
			if (mntCreateMovementCardinalPoint(MovementType.MOVE_WEST)){
				//NOTHING
			}else{
				throw new  RTException("Impossible to move to WEST, already slewing.") ;
			}
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			
			rtc.stop();
			rtc.writeLog("mntMoveWest");
		}
		
	}

	@Override
	public void mntSetSlewRate(String rate) throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			LogUtil.info(this, "mntSetSlewRate. Setting SlewRate=" + rate);
			
			int code = service.setSlewRate(rate);
			if (isGeminiError(code)){
				throw new RTException(getGeminiError(code), code);
			}
			
			LogUtil.info(this, "mntSetSlewRate. Set SlewRate=" + rate);
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntSetSlewRate");
		}
		
	}
	
	@Override
	public String mntGetSlewRate() throws RTException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void mntSlewObject(String object) throws RTException {
		
		/*RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			service.selectTarget(Integer.parseInt(object));
			service.slewToSelectedTarget();
	
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("mntSlewObject");
		}*/
		
		pointedObject = null;
		
		double longitude = Config.getPropertyDouble("rt_config", "rts_longitude");
		double latitude = Config.getPropertyDouble("rt_config", "rts_latitude");
		
		Observer observer = new Observer();
		observer.setLongitude(longitude);
		observer.setLatitude(latitude);
		observer.setAltitude(0);

		Catalogue catalogue = new Catalogue(longitude, latitude, 0);
		ObjInfo objInfo = catalogue.getObject(object);
		if (objInfo == null){
			
			LogUtil.info(this, "Mount. Catalogue:: Object NOT found:" + object);
			throw new RTException("Not Object Found");
			
		}else{
			Radec pos = objInfo.getPosition();
			double dec = pos.getDecDecimal();
			double ra = pos.getRaDecimal();
			
			Altaz altaz = CatalogueTools.getAltazByRadec(observer, new Date(), pos);
			
			LogUtil.info(this, "Mount. Catalogue:: Object found:" + object);
			String[] names = {
					"ra_double",
					"dec_double",
					"ra",
					"dec",
					"altitude"
			};
			
			String[] values = {
					String.valueOf(pos.getRaDecimal()),
					String.valueOf(pos.getDecDecimal()),
					String.valueOf(pos.getRaString(DegreeFormat.HHMMSS)),
					String.valueOf(pos.getDecString(DegreeFormat.DDMMSS)),
					String.valueOf(altaz.getAltDecimal())
			};
			
			LogUtil.info(this,  "Mount. Catalogue:: Object found Data: "  + LogUtil.getLog(names, values));
			
			if (altaz.getAltDecimal() < 0){
				throw new RTException("Object below the horizon. Altitude=" + altaz.getAltDecimal());
			}
			
			//pointedObject = object;
			//mntSlewToCoordinatesCommon(ra, dec);
			if (mntCreateMovement(ra, dec)){
				pointedObject = object;
			}
			
		}
		
	}
	
	

	@Override
	public Device devGetDevice(boolean allProperties)  throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("MountRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			//Based on the xml state file.
			boolean error = false;
			String xml = null;
			MountStateXmlParser state = null;
			
			try{
				
				xml = getStatusXML();
				state = new MountStateXmlParser(xml);
				
			}catch(Exception ex){
				LogUtil.severe(this, "Error.ServiceConfiguration::" + getServiceConfiguration());
				LogUtil.severe(this, "Error recovering the mount state. " + ex.getMessage());
				error = true;
			}
			
			DeviceMount dev = new DeviceMount();
				
			//	Resolve the activity state
			if (error){ //Error
				
				dev.setCommunicationState(CommunicationState.BUSY);
				dev.setAlarmState(AlarmState.MALFUNCTION);
				dev.setActivityState(ActivityStateMount.ERROR);
				
			}else { //No error.
				
				dev.setCommunicationState(CommunicationState.READY);
				dev.setAlarmState(AlarmState.NONE);
				
				if (state.isTracking()){
					dev.setActivityState(ActivityStateMount.TRACKING);
				} else if (state.isParked()){
						dev.setActivityState(ActivityStateMount.PARKED);
				} else if (state.isMoving()){
					dev.setActivityState(ActivityStateMount.MOVING);
				} else if (state.isStop()){
					dev.setActivityState(ActivityStateMount.STOP);
				} else{
					dev.setActivityState(ActivityStateMount.STOP); //By default
				}
				
			}

			dev.setBlockState(BlockState.UNBLOCK);
			dev.setActivityStateDesc("");
			
			//Other additional information
			eu.gloria.rt.entity.environment.config.device.Device devConfig = DeviceRTD.configDeviceManager.getDevice(this.getDeviceId());
			dev.setDescription(devConfig.getDescription());
			dev.setMeasureUnit(MeasureUnit.NONE);	
			dev.setShortName(devConfig.getShortName());
			dev.setType(DeviceType.MOUNT);
			dev.setVersion(devConfig.getVersion());
			
			return dev;
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}finally{
			rtc.stop();
			rtc.writeLog("getDevice");
		}
		
	}

	@Override
	public boolean mntIsPointingAtObject(String object, double raError, double decError) throws RTException {
		
		try{
			
			double longitude = Config.getPropertyDouble("rt_config", "rts_longitude");
			double latitude = Config.getPropertyDouble("rt_config", "rts_latitude");

			Catalogue catalogue = new Catalogue(longitude, latitude, 0);
			ObjInfo objInfo = catalogue.getObject(object);
			if (objInfo == null){
				
				LogUtil.info(this, "Mount.mntIsPointingAtObject(). Catalogue:: Object NOT found:" + object);
				throw new RTException("Not Object Found");
				
			}else{
				
				Radec pos = objInfo.getPosition();
				
				LogUtil.info(this, "Mount.mntIsPointingAtObject(). Catalogue:: Object found:" + object);
				String[] names = {
						"ra_double",
						"dec_double",
						"ra",
						"dec"
				};
				
				String[] values = {
						String.valueOf(pos.getRaDecimal()),
						String.valueOf(pos.getDecDecimal()),
						String.valueOf(pos.getRaString(DegreeFormat.HHMMSS)),
						String.valueOf(pos.getDecString(DegreeFormat.DDMMSS))
				};
				
				LogUtil.info(this,  "Mount.mntIsPointingAtObject(). Catalogue:: Object found Data: "  + LogUtil.getLog(names, values));
				
				return mntIsPointingAtCoordinates(pos.getRaDecimal(), pos.getDecDecimal(), raError, decError);
				
			}
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}
	}

	@Override
	public boolean mntIsPointingAtCoordinates(double ra, double dec, double raError, double decError) throws RTException {
		
		try{
			
			double raCurrent = mntGetPosAxis1();
			double decCurrent = mntGetPosAxis2();
			
			String[] names = {
					"raCurrent",
					"decCurrent",
					"ra",
					"dec",
					"raError",
					"decError",
					"pointing"
			};
			
			String[] values = {
					String.valueOf(raCurrent),
					String.valueOf(decCurrent),
					String.valueOf(ra),
					String.valueOf(dec),
					String.valueOf(raError),
					String.valueOf(decError),
					"UNKOWN"
			};
			
			LogUtil.info(this,  "Mount.mntIsPointingAtCoordinates(). Evaluating: "  + LogUtil.getLog(names, values));
			
			if (ra < 0 || ra >= 360) throw new RTException("Invalid RA value [0, 360). Value=" + ra);
			if (dec < -90 || dec > 90) throw new RTException("Invalid DEC value [-90, +90]. Value=" + dec);
			
			Radec radec = new Radec(raCurrent, decCurrent);
			boolean result = radec.equals(ra, dec, raError, decError);
			
			values[6] = String.valueOf(result);
			
			LogUtil.info(this,  "Mount.mntIsPointingAtCoordinates(). Evaluated: "  + LogUtil.getLog(names, values));
			
			return result;
			
		}catch(RTException ex){
			LogUtil.severe(this, ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			throw ex;
		}catch(Exception ex){
			LogUtil.severe(this, ex.getMessage());
			throw new RTException(ex.getMessage());
		}
	}

	@Override
	public String mntGetPointedObject() throws RTException {
		if (pointedObject == null){
			return "unknown";
		}else{
			return pointedObject;
		}
	}
	
	public Movement getMovement() {
		return movement;
	}

	public synchronized void setMovement(Movement movement) {
		this.movement = movement;
	}
	
	private synchronized boolean mntCreateMovement(double ra, double dec){
		
		if (this.movement != null){
			LogUtil.info(this,"MountRTD.mntCreateMovement(ra=" + ra + ", dec=" + dec + "). Is already slewing...aborting the new movement." );
			return false;
		}else{
			this.movement = new Movement(MovementType.MOVE_RADEC);
			this.movement.setRa(ra);
			this.movement.setDec(dec);
			Thread thread = new Thread(this.movement);
			this.movement.setThread(thread);
			thread.start();
			return true;
		}
		
	}
	
	private synchronized boolean mntCreateMovementPark(){
		
		if (this.movement != null){
			LogUtil.info(this,"MountRTD.mntCreateMovement(PARK). Is already slewing...aborting the new movement." );
			return false;
		}else{
			this.movement = new Movement(MovementType.PARKING);
			Thread thread = new Thread(this.movement);
			this.movement.setThread(thread);
			thread.start();
			return true;
		}
		
	}
	
	private synchronized boolean mntCreateMovementCardinalPoint(MovementType type){
		
		if (this.movement != null){
			LogUtil.info(this,"MountRTD.mntCreateMovement(CardinalPoint=" + type + "). Is already slewing...aborting the new movement." );
			return false;
		}else{
			this.movement = new Movement(type);
			Thread thread = new Thread(this.movement);
			this.movement.setThread(thread);
			thread.start();
			return true;
		}
		
	}
	
	private synchronized void mntStopCurrentMovement(){
		
		if (this.movement != null){
			
			try{
				this.movement.getThread().interrupt();
			}catch(Exception ex){
				LogUtil.severe(this,"MountRTD.mntStopMovement() EX=" + ex.getMessage());
			}
			
			this.setMovement(null);
			
		}
	}
	
	enum MovementType {
		PARKING,
		MOVE_RADEC,
		MOVE_NORTH,
		MOVE_SOUTH,
		MOVE_WEST,
		MOVE_EAST
	}
	
	class MountAction implements Runnable{
		
		private MovementType type;
		private double ra;
		private double dec;
		private Thread thread;
		private boolean finished;
		private int errorCode;
		
		public MountAction(MovementType type){
			
			this.type = type;
			this.finished = false;
		}
		
		@Override
		public void run() {
			
			try{
				
				LogUtil.info(this,"MountRTD.Movement.Action: Accessing to Telescope service.run(ra=" + ra + ", dec=" + dec + "). Is not slewing...requesting the movement.");
				
				this.finished = false;
				this.errorCode = 0;
				
				switch(this.type){
				case PARKING:
					LogUtil.info(this,"MountRTD.Movement.Action: Parking....");
					errorCode = service.parkTelescope();
					break;
				case MOVE_RADEC:
					LogUtil.info(this,"MountRTD.Movement.Action: Moving to RADEC(ra=" + ra + ", dec=" + dec + ").");
					Radec radec = new Radec(this.ra, this.dec);
					String ra = radec.getRaString(DegreeFormat.HHMMSS);
					String dec = radec.getDecString(DegreeFormat.DDMMSS);
					errorCode = service.slewToCoords(ra, dec , "HH:MM:SS");
					break;
				case MOVE_EAST:
					LogUtil.info(this,"MountRTD.Movement.Action: Moving to...EAST");
					errorCode = service.moveEast(3, 3000);
					break;
				case MOVE_WEST:
					LogUtil.info(this,"MountRTD.Movement.Action: Moving to...WEST");
					errorCode = service.moveWest(3, 3000);
					break;
				case MOVE_SOUTH:
					LogUtil.info(this,"MountRTD.Movement.Action: Moving to...SOUTH");
					errorCode = service.moveSouth(3, 3000);
					break;
				case MOVE_NORTH:
					LogUtil.info(this,"MountRTD.Movement.Action: Moving to...NORTH");
					errorCode = service.moveNorth(3, 3000);
					break;
				}
				
				LogUtil.info(this,"MountRTD.Movement.Action: Done!!!");
			

			} catch (Throwable e) {
				
				LogUtil.severe(this, "MountRTD.Movement.Action. Exception: " + e.getMessage());
				e.printStackTrace();
				this.errorCode = 666;

			} finally {
				
				this.finished = true;
			}
			
		}

		public MovementType getType() {
			return type;
		}

		public void setType(MovementType type) {
			this.type = type;
		}

		public Thread getThread() {
			return thread;
		}

		public void setThread(Thread thread) {
			this.thread = thread;
		}

		public boolean isFinished() {
			return finished;
		}

		public void setFinished(boolean finished) {
			this.finished = finished;
		}

		public int getErrorCode() {
			return errorCode;
		}

		public void setCode(int errorCode) {
			this.errorCode = errorCode;
		}

		public double getRa() {
			return ra;
		}

		public void setRa(double ra) {
			this.ra = ra;
		}

		public double getDec() {
			return dec;
		}

		public void setDec(double dec) {
			this.dec = dec;
		}
		
	}
	
	class Movement implements Runnable{
		
		private MovementType type;
		private double ra;
		private double dec;
		private Thread thread;
		private MountAction mountAction;
		
		public Movement(MovementType type){
			
			this.type = type;
			
		}
		
		@Override
		public void run() {
			
			try{
				
				LogUtil.info(this, "MountRTD.Movement.BEGIN");
				
				switch(this.type){
				case PARKING:
					park();
					break;
				case MOVE_RADEC:
					moveToRadec();
					break;
				case MOVE_EAST:
					moveToCardinalPoint();
					break;
				case MOVE_WEST:
					moveToCardinalPoint();
					break;
				case MOVE_SOUTH:
					moveToCardinalPoint();
					break;
				case MOVE_NORTH:
					moveToCardinalPoint();
					break;
				}

			} catch (Exception e) {
				
				LogUtil.severe(this, "MountRTD.Movement. Exception: " + e.getMessage());
				e.printStackTrace();

			} finally {
				
				//Deletes the movement
				MountRTD.this.setMovement(null);
				
				LogUtil.info(this, "MountRTD.Movement.END");
			}
			
			
			
		}
		
		private void park(){
			
			LogUtil.info(this,"MountRTD.Movement.run(PARKING) --> BEGIN" );
			
			try{
				
				pointedObject = null;
				
				MountRTD.this.mntSetTracking(false);
				
				this.mountAction = new MountAction(this.type);
				Thread thread = new Thread(this.mountAction);
				this.mountAction.setThread(thread);
				thread.start();
				
				/*int errorCode = service.parkTelescope();
				if ( errorCode != 0){
					throw new  RTException("Impossible to park.", errorCode) ;
				}*/
				
				waitPark();
				
				int errorCode = mountAction.getErrorCode();
				if ( errorCode != 0){
					throw new  RTException("Impossible to park.", errorCode) ;
				}
				
			}catch(RTException ex){
				LogUtil.severe(this, "MountRTD.Movement.run(PARKING). EX:" + ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			}catch(Exception ex){
				LogUtil.severe(this, "MountRTD.Movement.run(PARKING). EX:" + ex.getMessage());
			}finally{
				//NOTHING
			}
			
			LogUtil.info(this,"MountRTD.Movement.run(PARKING) --> END" );
			
		}
		
		private void moveToCardinalPoint(){
			
			boolean tracking = false;
			
			try{
				
				LogUtil.info(this,"MountRTD.Movement.run(MOVING).run(" + this.type + ") --> BEGIN" );
				
				tracking = mntGetTracking();
				if (tracking) mntSetTracking(false);
				//int code = 0;
				
				this.mountAction = new MountAction(this.type);
				Thread thread = new Thread(this.mountAction);
				this.mountAction.setThread(thread);
				thread.start();
				
				/*switch(this.type){
				case MOVE_NORTH:
					code = service.moveNorth(3, 3000);
					break;
				case MOVE_SOUTH:
					code = service.moveSouth(3, 3000);
					break;
				case MOVE_EAST:
					code = service.moveEast(3, 3000);
					break;
				case MOVE_WEST:
					code = service.moveWest(3, 3000);
					break;
				default:
					throw new Exception("Wrong movement, is not a CARDINAL MOVEMENT.");
				}
				
				if (isGeminiError(code)){
					throw new RTException(getGeminiError(code), code);
				}*/
				
				waitMovement();
				
				int errorCode = mountAction.getErrorCode();
				if ( errorCode != 0){
					throw new  RTException("Impossible to park.", errorCode) ;
				}
				
			}catch(RTException ex){
				LogUtil.severe(this, "MountRTD.Movement.run(MOVING).run(" + this.type + "). Ex:" + ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			}catch(Exception ex){
				LogUtil.severe(this, "MountRTD.Movement.run(MOVING).run(" + this.type + "). Ex:" + ex.getMessage());
			}finally{
				
				//Tracking
				try{
					if (tracking) mntSetTracking(true);
				}catch(Exception ex){
					//Nothing
				}
				
			}
			
			LogUtil.info(this,"MountRTD.Movement.run(MOVING).run(" + this.type + ") --> END" );
		}
		
		private void moveToRadec(){
			
			LogUtil.info(this,"MountRTD.Movement.run(MOVING).run(ra=" + ra + ", dec=" + dec + ") --> BEGIN" );
			
			boolean tracking = false;
		
			try{
			
				//boolean isSlewing= mntIsSlewing();
				boolean isSlewing = false;
		
				if (isSlewing){
			
					LogUtil.info(this,"MountRTD.Movement.run(MOVING).run(ra=" + ra + ", dec=" + dec + ") --> Is already slewing...aborting the new movement." );
			
				}else{
			
					LogUtil.info(this,"MountRTD.Movement.run(MOVING).run(ra=" + ra + ", dec=" + dec + ") --> Is not slewing...requesting the movement." );
				
					tracking = mntGetTracking();
					if (tracking) mntSetTracking(false);
		
					Radec radec = new Radec(ra, dec);
					String ra = radec.getRaString(DegreeFormat.HHMMSS);
					String dec = radec.getDecString(DegreeFormat.DDMMSS);
					//service.slewToCoords(ra, dec , "HH:MM:SS");
					
					this.mountAction = new MountAction(this.type);
					this.mountAction.setRa(this.ra);
					this.mountAction.setDec(this.dec);
					Thread thread = new Thread(this.mountAction);
					this.mountAction.setThread(thread);
					thread.start();
		
					/*int code = 0;
					for (int x = 0; x < 3; x++){
						code = service.slewToCoords(ra, dec , "HH:MM:SS");
						if (code == 0) break;
					}*/
				
					waitMovement();
					
					int errorCode = mountAction.getErrorCode();
					if ( errorCode != 0){
						throw new  RTException("Impossible to park.", errorCode) ;
					}
			
				}
			
			}catch(RTException ex){
				LogUtil.severe(this, "MountRTD.Movement.run(MOVING).run(ra=" + ra + ", dec=" + dec + ") Ex:" + ex.getMessage() + ". Error code=" + ex.getErrorCode().toString());
			}catch(Exception ex){
				LogUtil.severe(this, "MountRTD.Movement.run(MOVING).run(ra=" + ra + ", dec=" + dec + ") Ex:" + ex.getMessage());
			}finally{
			
				//Tracking
				try{
					if (tracking) mntSetTracking(true);
				}catch(Exception ex){
					//Nothing
				}
			
			}
			
			LogUtil.info(this,"MountRTD.Movement.run(MOVING).run(ra=" + ra + ", dec=" + dec + ") --> END" );
		}
		
		private void waitMovement() throws RTException, InterruptedException{ 
			
			LogUtil.info(this,"MountRTD.Movement.run(MOVING).waitMovement() -> Initial wait: 2000ms" );
			Thread.sleep(2000); //sleep 2 seconds.
			TimeOut timeOut = new TimeOut(MountRTD.this.movementTimeout);
			
			while (true){
				
				DeviceMount device = (DeviceMount)devGetDevice(false);
				
				LogUtil.info(this,"MountRTD.Movement.run(MOVING).waitMovement() -> Checking state=" + device.getActivityState() );
				if (/*device.getActivityState() == ActivityStateMount.STOP*/ device.getActivityState() != ActivityStateMount.MOVING  && this.mountAction.isFinished()){
					LogUtil.info(this,"MountRTD.Movement.run(MOVING).waitMovement() ->  Mount stop. Movement got success!!!." );
					break;
				}
				
				if (timeOut.timeOut()){
					try{
						this.mountAction.getThread().interrupt();
						LogUtil.info(this,"MountRTD.Movement.run(MOVING).waitMovement() ->  The action was interrupted!!!" );
					}catch(Exception ex){
						//Nothing
					}
					throw new RTException("Mount movement timout exceeded:" + MountRTD.this.movementTimeout);
				}
				
				LogUtil.info(this,"MountRTD.Movement.run(MOVING).waitMovement() ->  Waiting.....: 2000ms" );
				Thread.sleep(2000); //sleep 2 seconds.
			}
		}
		
		private void waitPark() throws RTException, InterruptedException{
			
			LogUtil.info(this,"MountRTD.Movement.run(PARKING).waitPark() -> Initial wait: 2000ms" );
			Thread.sleep(2000); //sleep 2 seconds.
			TimeOut timeOut = new TimeOut(MountRTD.this.movementTimeout);
			
			while (true){
				
				DeviceMount device = (DeviceMount)devGetDevice(false);
				
				LogUtil.info(this,"MountRTD.Movement.run(PARKING).waitPark() -> Checking state=" + device.getActivityState() );
				if (device.getActivityState() == ActivityStateMount.PARKED && this.mountAction.isFinished()){
					LogUtil.info(this,"MountRTD.Movement.run(PARKING).waitPark() -> Mount stop. Parking got success!!!." );
					break;
				}
				
				if (timeOut.timeOut()){
					try{
						this.mountAction.getThread().interrupt();
						LogUtil.info(this,"MountRTD.Movement.run(PARKING).waitPark() -> The action was interrupted!!!" );
					}catch(Exception ex){
						//Nothing
					}
					throw new RTException("Mount parking timout exceeded:" + MountRTD.this.movementTimeout);
				}
				
				LogUtil.info(this,"MountRTD.Movement.run(PARKING).waitPark() -> Waiting.....: 2000ms" );
				Thread.sleep(2000); //sleep 2 seconds.
			}
		}

		public double getRa() {
			return ra;
		}

		public void setRa(double ra) {
			this.ra = ra;
		}

		public double getDec() {
			return dec;
		}

		public void setDec(double dec) {
			this.dec = dec;
		}

		public Thread getThread() {
			return thread;
		}

		public void setThread(Thread thread) {
			this.thread = thread;
		}

	}

}
