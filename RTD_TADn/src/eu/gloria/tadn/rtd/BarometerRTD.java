package eu.gloria.tadn.rtd;

import java.util.List;

import eu.gloria.rt.entity.device.ActivityState;
import eu.gloria.rt.entity.device.AlarmState;
import eu.gloria.rt.entity.device.BlockState;
import eu.gloria.rt.entity.device.CommunicationState;
import eu.gloria.rt.entity.device.Device;
import eu.gloria.rt.entity.device.DeviceGeneral;
import eu.gloria.rt.entity.device.DeviceType;
import eu.gloria.rt.entity.device.MeasureUnit;
import eu.gloria.rt.entity.device.SensorStateIntervalDouble;
import eu.gloria.rt.exception.RTException;
import eu.gloria.rtd.RTDBarometerInterface;
import eu.gloria.tools.http.HttpUtil;
import eu.gloria.tools.log.LogUtil;
import eu.gloria.tools.time.RunTimeCounter;

/**
 * RTD implementation for Barometer.
 * 
 * @author jcabello
 *
 */
public class BarometerRTD extends WeatherRTDBasedOnXml implements RTDBarometerInterface {

	public BarometerRTD(String deviceId) throws RTException {
		super(deviceId);
	}

	@Override
	public MeasureUnit barGetMeasureUnit() throws RTException {
		return MeasureUnit.PASCAL;
	}

	@Override
	public double barGetMeasure() throws RTException {
		try{
			/*String xmlContent = HttpUtil.readHTTPContent(this.weatherWsUrl);
			WeatherXMLParser parser = new WeatherXMLParser(xmlContent);
			
			//millibars
			double result = Double.parseDouble(parser.getValue(this.configParameterName));
			//convert to pascals (100 pascal = 1 millibar; 1 millibar = 1000 bar)
			result = result * 100;*/
			
			double result = Double.parseDouble(WeatherStation.getStation(this.weatherWsUrl).getMeasure(this.configParameterName));
			//convert to pascals (100 pascal = 1 millibar; 1 millibar = 1000 bar)
			result = result * 100;
			
			return result;
			
		}catch (Exception ex) {
			String msg = "Error recovering XML state. " + ex.getMessage();
			LogUtil.severe(this, msg);
			throw new RTException(msg);
		}
	}

	@Override
	public void barSetMeasureStates(List<SensorStateIntervalDouble> states)
			throws RTException {
		// TODO 
		
	}

	@Override
	public List<SensorStateIntervalDouble> barGetMeasureStates()
			throws RTException {
		return null;
	}

	@Override
	public Device devGetDevice(boolean allProperties) throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("BarometerRTD", this.getDeviceId());
		rtc.start();
		
		try{
			
			//Based on the xml file.
			boolean error = false;
			/*String xmlContent = null;
			try{				
				xmlContent = HttpUtil.readHTTPContent(this.weatherWsUrl);
				WeatherXMLParser parser = new WeatherXMLParser(xmlContent);
				String value = parser.getValue(this.configParameterName);
				if (value == null){
					throw new Exception("Impossible to retrieve a value from xml. ParamName=" + this.configParameterName);
				}
					
			}catch(Exception ex){
				LogUtil.severe(this, "Error recovering information from weather xml. Error=" + ex.getMessage());
				LogUtil.severe(this, "Error recovering information from weather xml. Xml=" + xmlContent);
				error = true;
			}*/
			
			String value = WeatherStation.getStation(this.weatherWsUrl).getMeasure(this.configParameterName);
			if (value == null){
				throw new Exception("Impossible to retrieve a value from xml. ParamName=" + this.configParameterName);
			}
			
			DeviceGeneral  dev = new DeviceGeneral();
				
			//	Resolve the activity state
			if (error){ //Error
				
				dev.setCommunicationState(CommunicationState.BUSY);
				dev.setAlarmState(AlarmState.MALFUNCTION);
				dev.setActivityState(ActivityState.ERROR);
				dev.setActivityStateDesc("Error recovering XML information from WS.");
				
			}else { //No error.
				
				dev.setCommunicationState(CommunicationState.READY);
				
				if (Integer.parseInt(WeatherStation.getStation(this.weatherWsUrl).getAlarm(this.configParameterName))==1)
					dev.setAlarmState(AlarmState.WEATHER);
				else
					dev.setAlarmState(AlarmState.NONE);
				
				dev.setActivityState(ActivityState.READY); //FIXED
				dev.setActivityStateDesc("");
					
			}

			dev.setBlockState(BlockState.UNBLOCK);
		
			
			//Other additional information
			eu.gloria.rt.entity.environment.config.device.Device devConfig = DeviceRTD.configDeviceManager.getDevice(this.getDeviceId());
			dev.setDescription(devConfig.getDescription());
			dev.setMeasureUnit(MeasureUnit.PASCAL);	
			dev.setShortName(devConfig.getShortName());
			dev.setType(DeviceType.BAROMETER);
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
			rtc.writeLog("BarometerRTD");
		}
		
	}

}
