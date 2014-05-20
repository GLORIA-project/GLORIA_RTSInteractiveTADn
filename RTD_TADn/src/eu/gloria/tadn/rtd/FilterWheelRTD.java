package eu.gloria.tadn.rtd;

import java.util.ArrayList;
import java.util.List;

import eu.gloria.rt.entity.device.ActivityStateFilter;
import eu.gloria.rt.entity.device.AlarmState;
import eu.gloria.rt.entity.device.BlockState;
import eu.gloria.rt.entity.device.CommunicationState;
import eu.gloria.rt.entity.device.Device;
import eu.gloria.rt.entity.device.DeviceFilter;
import eu.gloria.rt.entity.device.DeviceType;
import eu.gloria.rt.entity.device.FilterType;
import eu.gloria.rt.exception.RTException;
import eu.gloria.rt.exception.UnsupportedOpException;
import eu.gloria.rtd.RTDFilterWheelInterface;
import eu.gloria.tools.log.LogUtil;

public class FilterWheelRTD extends DeviceRTD implements
		RTDFilterWheelInterface {
	
	protected List<String> filtersLocalName;
	protected List<String> filtersGlobalName;
	
	private int position = 0;

	public FilterWheelRTD(String deviceId) {
		
		super(deviceId);
		this.init();		
		
	}
	
	
	/**
	 * {@inheritDoc} 
	 * <p>
	 * Recover from RTS2 the available filters.
	 */
	@Override
	public void setDeviceId(String deviceId) {
		
		super.setDeviceId(deviceId);
		
		this.init();
		
	}
	
	/**
	 * Initialization when the identifier is known.
	 */
	private void init () {
		
		//Process the mapping...
		//<filterLocalName=FilterGlobalName;filterLocalName=filterGlobalName....>
		try{
			
			filtersLocalName = new ArrayList<String>();
			filtersGlobalName = new ArrayList<String>();
			
			eu.gloria.rt.entity.environment.config.device.DeviceProperty mapping = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "FILTER_MAPPING");
			String filterMapping =  mapping.getDefaultValue();
			LogUtil.info(this, "FilterWheelRTD.Contructor(). original mapping=" + filterMapping);
			if (filterMapping != null){
				
				String[] pairs = filterMapping.split(";");
				for (int pair = 0 ; pair < pairs.length; pair++){
					String[] values = pairs[pair].split("=");
					
					try{
						
						filtersLocalName.add(values[0]);
						filtersGlobalName.add(values[1]);
						
					}catch(Exception ex){
						String[] namesLog = {
							"FilterLocalName",
							"FilterGlobalName"	,
							"message"
						};
						String[] valuesLog = {
								values[0],
								values[1],
								"Unknown Global Filter Type"
						};
						LogUtil.severe(this, "FilterWheelRTD.Contructor(). Loading filterMapping..." + LogUtil.getLog(namesLog, valuesLog));
					}
					
				}
				
			}
			
			
		}catch(Exception ex){
			LogUtil.severe(this, "FilterWheelRTD.Contructor(). Error reading the configuration parameter: FILTER_MAPPING. Error=" + ex.getMessage());
		}
		
	}

	@Override
	public String fwGetCamera() throws RTException {

		eu.gloria.rt.entity.environment.config.device.DeviceProperty mapping = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "ASSOCIATED_CAMERA");
		return mapping.getDefaultValue();
	}

	@Override
	public List<String> fwGetFilterList() throws RTException {

		return filtersGlobalName;
	}

	@Override
	public int fwGetPositionNumber() throws RTException {

		return position;
	}

	@Override
	public int fwGetSpeedSwitching() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public float fwGetFilterSize() throws RTException {
		
		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public String fwGetFilterKind() throws RTException {

		return this.filtersGlobalName.get(position);
	}

	@Override
	public boolean fwIsAtHome() throws RTException {
		
		return (this.fwGetPositionNumber()==0 ? true : false);
	}

	@Override
	public void fwSetOffset(List<Integer> positions) throws RTException {
		
		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void fwSelectFilterKind(String kind) throws RTException {
		
		int index = this.filtersGlobalName.indexOf(kind);
		if (index == -1){
			throw new RTException("Unknown filter value for mapping. value=" + kind);
		}
		
		position = index;
	}

	@Override
	public void fwSelectFilterPosition(int position) throws RTException {

		if (this.filtersGlobalName.size() == 0){
			throw new RTException ("No filter available.");
		}
		
		if (position >= this.filtersGlobalName.size()){
			throw new RTException ("Invalid position.");
		}
		
		this.position = position;

	}

	@Override
	public void fwGoHome() throws RTException {

		this.fwSelectFilterPosition(0);

	}

	@Override
	public Device devGetDevice(boolean allProperties) throws RTException {

		DeviceFilter devFilter = new DeviceFilter();
		devFilter.setBlockState(BlockState.UNBLOCK);
		devFilter.setAlarmState(AlarmState.NONE);
		devFilter.setActivityState(ActivityStateFilter.READY);
		devFilter.setCommunicationState(CommunicationState.READY);
		devFilter.setActivityStateDesc("");
		devFilter.setType(DeviceType.FW);
		devFilter.setError(null);
		devFilter.setDescription("Filter wheel");
		devFilter.setInfo("Filter wheel");
		devFilter.setShortName(this.getDeviceId());
		devFilter.setVersion("1.0");
		
		return devFilter;
	}

	public int getPosition() {
		return position;
	}
}
