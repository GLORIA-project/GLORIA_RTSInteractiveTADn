package eu.gloria.tadn.rtd;

import java.util.Timer;
import java.util.TimerTask;

import eu.gloria.rt.exception.RTException;
import eu.gloria.tools.http.HttpUtil;
import eu.gloria.tools.log.LogUtil;
import eu.gloria.tools.time.RunTimeCounter;

public class WeatherStation {
	
	public static WeatherStation singleton;
	
	private String xml;
	private WeatherRetriever retriever;
	private String weatherWsUrl;
	private Timer timer = null;
	private String locker = "LOCKER";
	
	public static WeatherStation getStation(String weatherWsUrl){
		
		if (singleton == null){
			singleton = new WeatherStation(weatherWsUrl);
		}
		
		return singleton;
	}
	
	
	private WeatherStation(String weatherWsUrl){
		
		this.xml = null;
		this.retriever = new WeatherRetriever();
		this.weatherWsUrl = weatherWsUrl;
		this.timer = new Timer(true);
		this.timer.schedule(this.retriever, 0, 60000);	
	}
	
	
	@Override
	protected void finalize() throws Throwable
	{
	  
		timer.cancel();
		
		super.finalize(); 
	} 
	
	public String getMeasure(String parameterName) throws RTException {
		
		try{
			String xmlTmp;
			synchronized (locker) {
				xmlTmp = xml;
			}
			
			if (xmlTmp == null){
				LogUtil.info(this, "WeatherStation. (1)Recovering XML....");
				LogUtil.info(this, "WeatherStation. xml is null.");
				xmlTmp = HttpUtil.readHTTPContent(weatherWsUrl);
				LogUtil.info(this, "WeatherStation. XMLCONTENT[" + xml + "]");
				LogUtil.info(this, "WeatherStation. (1)XML recovered");
				
				synchronized (locker) {
					xml = xmlTmp;
				}
				
			}
				
			WeatherXMLParser parser = new WeatherXMLParser(xmlTmp);
			return parser.getValue(parameterName);
			
		}catch (Exception ex) {
			String msg = "Error recovering XML state. " + ex.getMessage();
			LogUtil.severe(this, msg);
			throw new RTException(msg);
		}
		
	}
	
	public String getAlarm(String parameterName) throws RTException {
		
		try{
			String xmlTmp;
			synchronized (locker) {
				xmlTmp = xml;
			}
			
			if (xmlTmp == null){
				LogUtil.info(this, "WeatherStation. (1)Recovering XML....");
				LogUtil.info(this, "WeatherStation. xml is null.");
				xmlTmp = HttpUtil.readHTTPContent(weatherWsUrl);
				LogUtil.info(this, "WeatherStation. XMLCONTENT[" + xml + "]");
				LogUtil.info(this, "WeatherStation. (1)XML recovered");
				
				synchronized (locker) {
					xml = xmlTmp;
				}
				
			}
				
			WeatherXMLParser parser = new WeatherXMLParser(xmlTmp);
			return parser.getAlarm(parameterName);
			
		}catch (Exception ex) {
			String msg = "Error recovering XML state. " + ex.getMessage();
			LogUtil.severe(this, msg);
			throw new RTException(msg);
		}
		
	}
	
	public class WeatherRetriever extends TimerTask {
		@Override
		public void run() {

			RunTimeCounter rtc = new RunTimeCounter("WeatherStation", "RetrieveXmlContent");
			rtc.start();

			try{

				LogUtil.info(this, "WeatherStation. Retrieving xml content. run BEGIN");
				
				try{
					
					LogUtil.info(this, "WeatherStation. (2)Recovering XML....");
					String xml2 = HttpUtil.readHTTPContent(weatherWsUrl);
					LogUtil.info(this, "WeatherStation. XMLCONTENT[" + xml2 + "]");
					LogUtil.info(this, "WeatherStation. (2)XML recovered");
					synchronized (locker) {
						xml = xml2;
					}
					
					
				}catch(Exception ex){
					LogUtil.severe(this, "WeatherStation. Error: " + ex.getMessage());
				};
				
			}finally{
				rtc.stop();
				rtc.writeLog("WeatherStation. Retrieving xml content. run END");
			}

		}		
	}

}
