package eu.gloria.tadn.rtd;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;

import eu.gloria.rt.entity.device.Device;
import eu.gloria.rt.entity.device.DeviceType;
import eu.gloria.rt.entity.device.MeasureUnit;
import eu.gloria.rt.entity.environment.config.device.DeviceList;
import eu.gloria.rt.exception.RTException;
import eu.gloria.rtc.DeviceDiscovererInterface;
import eu.gloria.rtc.environment.config.device.ConfigDeviceManager;
import eu.gloria.rtd.RTDDeviceInterface;
import eu.gloria.tools.cache.CacheManager;
import eu.gloria.tools.log.LogUtil;

/**
 * Device Discoverer for TAD Telescope.
 * 
 * @author jcabello
 *
 */
public class DeviceDiscoverer implements DeviceDiscovererInterface {
	
	/**
	 * Cache name to caching RTDs.
	 */
	private static final String CACHE_NAME_RTDS = "CACHE_NAME_RTDS";
	
	/**
	 * Device configuration list manager.
	 */
	private static ConfigDeviceManager configDeviceManager;
	
	/**
	 * Static initialization.
	 */
	static{
		
		CacheManager.createCache(CACHE_NAME_RTDS, -1, null);
		
		try{
			
			configDeviceManager = new ConfigDeviceManager();
			
		}catch(Exception ex){
			LogUtil.severe(null, "DeviceDiscoverer. static initialization. Error loadin the device list XML." + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	/**
	 * Main method for testing purpose.
	 * @param args arguments
	 */
	public static void main(String[] args) throws Exception{
		
		DeviceDiscoverer disc = new DeviceDiscoverer();
		
		//eu.gloria.rt.entity.environment.config.device.Device dev = configDeviceManager.getDevice("DBx 41AU02.AS 1");
		
		System.out.print(disc. getDevices(false));

	}

	/**
	 * {@inheritDoc} 
	 *
	 */
	@Override
	public List<String> getDeviceIds() throws RTException {
		List<String> result = new ArrayList<String>();
		if (DeviceDiscoverer.configDeviceManager.getDeviceList() != null && configDeviceManager.getDeviceList().getDevice() != null){
			
			for (eu.gloria.rt.entity.environment.config.device.Device dev : configDeviceManager.getDeviceList().getDevice()) {
				result.add(dev.getShortName());
			}
		}
		return result;
	}

	/**
	 * {@inheritDoc} 
	 *
	 */
	@Override
	public List<Device> getDevices(boolean allProperties) throws RTException {
		
		List<Device> result = new ArrayList<Device>();
		if (configDeviceManager.getDeviceList() != null && configDeviceManager.getDeviceList().getDevice() != null){
			
			for (eu.gloria.rt.entity.environment.config.device.Device dev : configDeviceManager.getDeviceList().getDevice()) {
				result.add(getDevice(dev.getShortName(), true));
			}
		}
		return result;
	}

	/**
	 * {@inheritDoc} 
	 *
	 */
	@Override
	public Device getDevice(String deviceId, boolean allProperties)	throws RTException {
		
		RTDDeviceInterface dev =  (RTDDeviceInterface) getRTD(deviceId);
		return dev.devGetDevice(allProperties);
		
	}

	/**
	 * {@inheritDoc} 
	 *
	 */
	@Override
	public Device getDevice(String deviceId, List<String> propertyNames)
			throws RTException {
		
		return getDevice(deviceId, false);
	}

	/**
	 * {@inheritDoc} 
	 *
	 */
	@Override
	public RTDDeviceInterface getRTD(String deviceId) throws RTException {
		
		RTDDeviceInterface genericRTD = null;
		
		String devType = configDeviceManager.getDevice(deviceId).getType().toString();
		
		String devSubtype = configDeviceManager.getDevice(deviceId).getSubtype();
				
		//Device device = getDevice(configDeviceManager.getDevice(deviceId));
		
		try{
			
			//Is it in cache?
			RTDDeviceInterface cachedRtd = (RTDDeviceInterface)CacheManager.getCache(CACHE_NAME_RTDS).getObject(deviceId, null);
			if (cachedRtd != null){
				return cachedRtd;
			}
		
			HierarchicalINIConfiguration iniConfObj = null;
			try {
				iniConfObj = new HierarchicalINIConfiguration("rtd_tadn_discovery.ini");
			} catch (ConfigurationException e) {
				e.printStackTrace();
				throw new RTException("DeviceDiscoverer. Impossible to load the file: rtd_tadn_discovery.ini");
			}
			
			String className = (String) iniConfObj.getProperty(devType+"."+devSubtype);
			
			//Build the instance.
			try{
				Class<?> cls = Class.forName(className);
				Class[] constructorParamTypes = {String.class};
				Constructor<?> ct = cls.getConstructor(constructorParamTypes);	
				Object[] params = {deviceId};
				genericRTD = (RTDDeviceInterface) ct.newInstance(params);

				//genericRTD.setDeviceId(deviceId);
				
			}catch (Exception e){
				throw new RTException("Error reflexing the RTD class"+ e.getCause().getMessage());
			}		
			
			
			CacheManager.getCache(CACHE_NAME_RTDS).putObject(deviceId, genericRTD);
			
		}catch(Exception ex){
			throw new RTException(ex.getMessage());
		}		
		
		return genericRTD;
	}


}
