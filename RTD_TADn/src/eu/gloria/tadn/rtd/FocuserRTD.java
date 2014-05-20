package eu.gloria.tadn.rtd;

import java.io.IOException;

import eu.gloria.rt.entity.device.ActivityStateFocuser;
import eu.gloria.rt.entity.device.AlarmState;
import eu.gloria.rt.entity.device.BlockState;
import eu.gloria.rt.entity.device.CommunicationState;
import eu.gloria.rt.entity.device.Device;
import eu.gloria.rt.entity.device.DeviceCamera;
import eu.gloria.rt.entity.device.DeviceFocuser;
import eu.gloria.rt.entity.device.DeviceType;
import eu.gloria.rt.exception.RTException;
import eu.gloria.rt.exception.UnsupportedOpException;
import eu.gloria.tadn.rtd.DeviceRTD;
import eu.gloria.rtd.RTDFocuserInterface;

public class FocuserRTD extends DeviceRTD implements RTDFocuserInterface {

	
	private String ip = null;
	private String port = null;
	private String path = null;
	
	public static void main(String[] args) {
		
		FocuserRTD focuser = new FocuserRTD("FocuserSBIG");
		
		
		try {
			DeviceFocuser dev = (DeviceFocuser) focuser.devGetDevice(false);
			System.out.println(dev.getCommunicationState()+"\n");
		} catch (RTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public FocuserRTD(String deviceId) {
		super(deviceId);
		
		ip = (DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "IP")).getDefaultValue();
		port = (DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PORT")).getDefaultValue();
		path = (DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "PATH")).getDefaultValue();
	}

	@Override
	public String focGetCamera() throws RTException {
		
		eu.gloria.rt.entity.environment.config.device.DeviceProperty mapping = DeviceRTD.configDeviceManager.getProperty(this.getDeviceId(), "ASSOCIATED_CAMERA");
		return mapping.getDefaultValue();
	}

	@Override
	public boolean focIsAbsolute() throws RTException {
		
		return false;
	}

	@Override
	public double focGetStepSize() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public long focGetMaxIncrement() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public long focGetMaxStep() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public long focGetPosition() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public boolean focIsTempCompAvailable() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public double focGetTemperature() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");
	}

	@Override
	public void focSetTempComp(boolean trackingMode) throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void focHalt() throws RTException {

		throw new UnsupportedOpException ("Operation not supported");

	}

	@Override
	public void focMove(long position) throws RTException {
		
		 //String cmd = path + "tadclient " + ip + port + "2" + position;
		
		String cmd = path + "tadclient " + ip + " " + port + " 2 " + position;
		 
		 try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {			
			e.printStackTrace();
			
			throw new RTException (e.getMessage());
		}

	}

	@Override
	public Device devGetDevice(boolean allProperties) throws RTException {

		DeviceFocuser devFocuser = new DeviceFocuser();
		devFocuser.setBlockState(BlockState.UNBLOCK);
		devFocuser.setAlarmState(AlarmState.NONE);
		devFocuser.setActivityState(ActivityStateFocuser.READY);
		devFocuser.setCommunicationState(CommunicationState.READY);
		devFocuser.setActivityStateDesc("");
		
		//sets the type
		devFocuser.setType(DeviceType.FOCUS);
		
		//Error
		devFocuser.setError(null);
		
		//Description
		devFocuser.setDescription("Unavailable");
		
		//Info
		devFocuser.setInfo("Unavailable");
		
		//ShortName
		devFocuser.setShortName(this.getDeviceId());
		
		//Version
		devFocuser.setVersion("Unavailable");
		
		return devFocuser;
	}
	
	@Override
	public long focGetMinStep() throws RTException {
		throw new UnsupportedOpException ("Operation not supported");
	}

}
