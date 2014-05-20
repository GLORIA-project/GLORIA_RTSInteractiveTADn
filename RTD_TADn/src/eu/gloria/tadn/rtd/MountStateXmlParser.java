package eu.gloria.tadn.rtd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.gloria.tools.log.LogUtil;

/**
 * Mount state xml parser.
 * 
 * @author jcabello
 *
 */
public class MountStateXmlParser {
	
	private static final String MOUNT_NOT_DEFINED = "-1";
	private static final String MOUNT_NO_MOTION = "0";
	private static final String MOUNT_MOTION = "1";
	private static final String MOUNT_TRACKING = "2";
	private static final String MOUNT_PARKING = "3";
	private static final String MOUNT_PARKED = "4";
	
	private String move;
	private String ra;
	private String dec;
	private String azimuth;
	private String elevation;
	
	public static void main(String[] args) {
		
		try {
			String xml = "<xml><RA>ra1</RA><DEC>dec</DEC><AZ>az</AZ><ALT>alt</ALT><MOVE>move</MOVE></xml>";
			MountStateXmlParser obj = new MountStateXmlParser(xml);
			System.out.println("");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public MountStateXmlParser(String xml) throws Exception{
		
		InputStream inputStream = null;
		
		try{
		
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		
			inputStream = new ByteArrayInputStream(xml.getBytes());
		
			Document document = dBuilder.parse(inputStream);
			document.getDocumentElement().normalize();
		
			Node node = document.getElementsByTagName("xml").item(0);
			NodeList nList =  node.getChildNodes();
		
			move = getNodeValue(nList, "MOV");
			ra = getNodeValue(nList, "RA");
			dec = getNodeValue(nList, "DEC");
			azimuth = getNodeValue(nList, "AZ");
			elevation = getNodeValue(nList, "ALT");
			
		}catch(Exception ex){
			LogUtil.severe(this, "Error parsing TAD-MOUNT-XML: " + xml);
			throw ex;
		}
		
//		this.move = "0";
//		this.ra = "0:0:0";
//		this.dec = "0:0:0";
//		this.azimuth = "0";
//		this.elevation = "0";
		
		
	}
	
	public boolean isTracking() throws Exception{
		
		if (move == null){
			throw new Exception("Impossible to resolve the mount-tracking-state.");
		}
		
		return (MOUNT_TRACKING.equals(move));
	}
	
	public boolean isMoving() throws Exception{
		
		if (move == null){
			throw new Exception("Impossible to resolve the mount-move-state.");
		}
		
		return (MOUNT_MOTION.equals(move) || MOUNT_PARKING.equals(move) /* || MOUNT_TRACKING.equals(move)*/);
	}
	
	public boolean isStop() throws Exception{
		
		if (move == null){
			throw new Exception("Impossible to resolve the mount-stop-state.");
		}
		
		return (MOUNT_NO_MOTION.equals(move) || MOUNT_PARKED.equals(move));
	}
	
	public boolean isParked() throws Exception{
		
		if (move == null){
			throw new Exception("Impossible to resolve the mount-parked-state.");
		}
		
		return (MOUNT_PARKED.equals(move));
	}
	
	private String getNodeValue(NodeList nList, String name) throws Exception {
		
		String result = null;
		
		try{
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				 
				   Node nNode = nList.item(temp);
				   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
		 
				      Element eElement = (Element) nNode;
				      if (eElement.getNodeName().equals(name)){
				    	  result = eElement.getChildNodes().item(0).getNodeValue();
				    	  break;
				      }
				   }
			}
			
		}catch(Exception ex){
			result = null;
		}
		
		return result;
		
	}

	public String getMove() {
		return move;
	}

	public void setMove(String move) {
		this.move = move;
	}

	public String getRa() {
		return ra;
	}

	public void setRa(String ra) {
		this.ra = ra;
	}

	public String getDec() {
		return dec;
	}

	public void setDec(String dec) {
		this.dec = dec;
	}

	public String getAzimuth() {
		return azimuth;
	}

	public void setAzimuth(String azimuth) {
		this.azimuth = azimuth;
	}

	public String getElevation() {
		return elevation;
	}

	public void setElevation(String elevation) {
		this.elevation = elevation;
	}

}
