package eu.gloria.tadn.rtd;

import java.util.Map;

import eu.gloria.rt.exception.RTException;
import eu.gloria.tools.cache.ICacheRetriever;

public class CachedDeviceInfo implements ICacheRetriever {

	/**
	 * Connected flag.
	 */
	private boolean connected;
	
	/**
	 * Constructor. 
	 */
	public CachedDeviceInfo(){
		connected = false;
	}
	
	@Override
	public Object retrieve(Map<String, Object> params) throws RTException {

		return new CachedDeviceInfo();
	}
	
	/**
	 * Access method
	 * @return boolean
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Access method
	 * @param connected value
	 */
	public void setConnected(boolean connected) {
		this.connected = connected;
	}

}
