package eu.gloria.tadn.rtd;

import eu.gloria.rt.entity.environment.config.device.DeviceProperty;
import eu.gloria.rt.exception.RTException;
import eu.gloria.rtd.RTDRHSensorInterface;
import eu.gloria.tools.log.LogUtil;
import eu.gloria.tools.time.RunTimeCounter;

/**
 * BaseClass for Weather RTDs.
 * 
 * @author jcabello
 *
 */
public abstract class WeatherRTDBasedOnXml extends DeviceRTD  {
	
	
	protected String weatherWsUrl;
	protected String configParameterName;
	
	public WeatherRTDBasedOnXml(String deviceId) throws RTException {
		super(deviceId);
		
		RunTimeCounter rtc = new RunTimeCounter("WeatherRTDBasedOnXml", this.getDeviceId());
		rtc.start();
		
		readConfig();
		
		rtc.stop();
		rtc.writeLog("WeatherRTDBasedOnXml");
	}
	
	private void readConfig() throws RTException {
		
		try{
			
			DeviceProperty configWsURL = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "WEATHER_WS_URL");
			weatherWsUrl = configWsURL.getDefaultValue();
			
			DeviceProperty configParamName = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PARAM_NAME");
			configParameterName = configParamName.getDefaultValue();
			
			String[] names ={"WEATHER_WS_URL", "PARAM_NAME"};
			String[] values = {configWsURL.getDefaultValue(), configParamName.getDefaultValue()};
			LogUtil.info(this, "WeatherRTDBasedOnXml Config =" + LogUtil.getLog(names, values));
			
			
		}catch(Exception ex){
			LogUtil.severe(this, "Constructor. Error:" + ex.getMessage());
			//Exception not thrown to get the device in error state
		}
		
		
	}
	
	
	
	
	
	

}
