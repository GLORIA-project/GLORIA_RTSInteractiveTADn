package eu.gloria.tadn.rtd;

import java.util.List;

import eu.gloria.rt.entity.device.Device;
import eu.gloria.rt.entity.device.DeviceProperty;
import eu.gloria.rt.exception.RTException;
import eu.gloria.rt.exception.UnsupportedOpException;
import eu.gloria.rtc.DeviceDiscoverer;
import eu.gloria.rtc.environment.config.device.ConfigDeviceManager;
import eu.gloria.rtd.RTDDeviceInterface;
import eu.gloria.tools.cache.CacheManager;
import eu.gloria.tools.log.LogUtil;

/**
 * Root device RTD.
 * 
 * @author jcabello
 *
 */
public abstract class  DeviceRTD implements RTDDeviceInterface {

	public static String CACHE_DEVICE_INFO = "CACHE_DEVICE_INFO";
	
	/**
	 * Device configuration list manager.
	 */
	protected static ConfigDeviceManager configDeviceManager;
	
	
	static{
		
		CacheManager.createCache(CACHE_DEVICE_INFO, -1, new CachedDeviceInfo());
		
		try{
			
			configDeviceManager = new ConfigDeviceManager();
			
		}catch(Exception ex){
			LogUtil.severe(null, "DeviceRTD. static initialization. Error loading the device list XML." + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	private String deviceId = null;
	
	/**
	 * Constructor
	 * @param deviceId
	 */
	public DeviceRTD(String deviceId){
		this.deviceId = deviceId;
	}
	
	
	
	/**
	 * Return the Device information.
	 * @param allProperties false if it is not needed all properties (performance purpose).
	 * @return Device information.
	 * @throws RTException In error case.
	 */
	abstract public Device devGetDevice(boolean allProperties)  throws RTException;
	
	
	/**
	 * Access method
	 * @return
	 */
	public String getDeviceId() {
		return deviceId;
	}

	/**
	 * Access method
	 * @return 
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	@Override
	public List<DeviceProperty> devGetDeviceProperties() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public DeviceProperty devGetDeviceProperty(String name) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean devUpdateDeviceProperty(String name, List<String> value)
			throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean devUpdateDevicePropertyAsync(String name, List<String> value)
			throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean devIsConnected() throws RTException {
		
		try{
			CachedDeviceInfo devInfo = (CachedDeviceInfo)CacheManager.getObject(deviceId, CACHE_DEVICE_INFO, null);
			return devInfo.isConnected();
		}catch(Exception ex){
			RTException newEx = new RTException(ex);
			throw newEx;
		}
	}

	@Override
	public void devConnect() throws RTException {

		CachedDeviceInfo devInfo = null;
		try{
			
			devInfo = (CachedDeviceInfo)CacheManager.getObject(deviceId, CACHE_DEVICE_INFO, null);
			
			Device device = DeviceDiscoverer.devGetDevice(deviceId, false);
			if (device.getError().getCod() != 0) {
				throw new Exception("The device is in Error state.");
			}
						
			devInfo.setConnected(true);
			
		}catch(Exception ex){
			devInfo.setConnected(false);
			RTException newEx = new RTException(ex);
			throw newEx;
		}

	}

	@Override
	public void devDisconnect() throws RTException {

		try{
			CachedDeviceInfo devInfo = (CachedDeviceInfo)CacheManager.getObject(deviceId, CACHE_DEVICE_INFO, null);
			devInfo.setConnected(false);
		}catch(Exception ex){
			RTException newEx = new RTException(ex);
			throw newEx;
		}

	}

	@Override
	public String devGetConfiguration() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean devIsBlocked() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

}
