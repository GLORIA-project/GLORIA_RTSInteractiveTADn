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
import eu.gloria.rtd.RTDRHSensorInterface;
import eu.gloria.rtd.RTDWeatherVaneInterface;
import eu.gloria.tools.http.HttpUtil;
import eu.gloria.tools.log.LogUtil;
import eu.gloria.tools.time.RunTimeCounter;

/**
 * Weather Vane RTD.
 * 
 * @author jcabello
 *
 */
public class WeatherVaneRTD extends WeatherRTDBasedOnXml implements RTDWeatherVaneInterface {

	public WeatherVaneRTD(String deviceId) throws RTException {
		super(deviceId);
	}

	@Override
	public MeasureUnit wvnGetMeasureUnit() throws RTException {
		return MeasureUnit.DEGREE;
	}

	@Override
	public double wvnGetMeasure() throws RTException {
		
		try{
			
			/*String xmlContent = HttpUtil.readHTTPContent(this.weatherWsUrl);
			WeatherXMLParser parser = new WeatherXMLParser(xmlContent);
			return Double.parseDouble(parser.getValue(this.configParameterName));*/
			
			return Double.parseDouble(WeatherStation.getStation(this.weatherWsUrl).getMeasure(this.configParameterName));
			
		}catch (Exception ex) {
			String msg = "Error recovering XML state. " + ex.getMessage();
			LogUtil.severe(this, msg);
			throw new RTException(msg);
		}
	}

	@Override
	public double wvnGetAbosluteDegrees() throws RTException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void wvnSetMeasureStates(List<SensorStateIntervalDouble> states)
			throws RTException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<SensorStateIntervalDouble> wvnGetMeasureStates()
			throws RTException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Device devGetDevice(boolean allProperties) throws RTException {
		
		RunTimeCounter rtc = new RunTimeCounter("WeatherVaneRTD", this.getDeviceId());
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
			dev.setMeasureUnit(MeasureUnit.DEGREE);	
			dev.setShortName(devConfig.getShortName());
			dev.setType(DeviceType.WEATHER_VANE);
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
			rtc.writeLog("WeatherVaneRTD");
		}
	}

}
