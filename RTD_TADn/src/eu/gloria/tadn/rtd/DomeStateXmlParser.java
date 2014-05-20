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
 * Dome State Xml parser.
 * 
 * @author jcabello
 *
 */
public class DomeStateXmlParser {
	
	private String status;
	private String flux1;
	
	private String rightDome;
	private String leftDome;
	
	public static void main(String[] args) {
		
		try {
			/*String xml = "<xml><status>INTEGER</status><flux1>INTEGER</flux1></xml>";
			MountStateXmlParser obj = new MountStateXmlParser(xml);*/
			
			String xml = "<xml><id>TAD-WEST</id><event>0</event><flux1>871</flux1><flux2>0</flux2><leftDome>false</leftDome><rightDome>false</rightDome><status>20</status></xml>";
			DomeStateXmlParser obj = new DomeStateXmlParser(xml);
			
			System.out.println(obj.getState());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public DomeStateXmlParser(String xml) throws Exception{
		
		InputStream inputStream = null;
		
		try{
			
			LogUtil.info(this, "TAD-DOME:: XML to Parse:" + xml);
		
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		
			inputStream = new ByteArrayInputStream(xml.getBytes());
		
			Document document = dBuilder.parse(inputStream);
			document.getDocumentElement().normalize();
		
			Node node = document.getElementsByTagName("xml").item(0);
			NodeList nList =  node.getChildNodes();
		
			rightDome = getNodeValue(nList, "rightDome");
			leftDome = getNodeValue(nList, "leftDome");
			status = getNodeValue(nList, "status");
			
			System.out.println("rightDome: "+ rightDome);
			System.out.println("leftDome: "+ leftDome);
			System.out.println("status: "+ status);
			
			flux1 = getNodeValue(nList, "flux1");			
			
		}catch(Exception ex){
			LogUtil.severe(this, "Error parsing TAD-DOME-XML: " + xml);
			throw ex;
		}
		
	}
	
	/**
	 * Returns the Dome state
	 * @return
	 */
	public DomeState getState(){
		
		DomeState result = DomeState.NOT_DEFINED;
		
		try{
			
			
			int stateInt = Integer.parseInt(status);
			
			/*switch(stateInt){
			case -1:
				result = DomeState.ERROR;
				break;
			case 20:
				result = DomeState.CLOSED_BOXES;
				break;
			case 10:
				result = DomeState.OPENED_BOXES;
				break;
			case 18:
				result = DomeState.RIGHT_BOX_OPENED;
				break;
			case 12:
				result = DomeState.LEFT_BOX_OPENED;
				break;
			default:
				result = DomeState.NOT_DEFINED;
				break;		
			}*/
			
			boolean leftClosed  =  ((stateInt & 0x0018) & 0x0010) == 0x0010;
			boolean leftOpen    =  ((stateInt & 0x0018) & 0x0008) == 0x0008;
			boolean rightClosed =  ((stateInt & 0x0006) & 0x0004) == 0x0004;
			boolean rightOpen   =  ((stateInt & 0x0006) & 0x0002) == 0x0002;
			
			/*boolean leftClosed = (stateInt & 0x0010) == 0x0010;
			boolean leftOpen  =  (stateInt & 0x0008) == 0x0008;
			boolean rightClosed = (stateInt & 0x0004) == 0x0004;
			boolean rightOpen  =  (stateInt & 0x0002) == 0x0002;*/
			
			LogUtil.info(this, "-getState()::stateInt: " + stateInt);
			LogUtil.info(this, "-getState()::leftClosed: " + leftClosed);
			LogUtil.info(this, "-getState()::leftOpen: " + leftOpen);
			LogUtil.info(this, "-getState()::rightClosed: " + rightClosed);
			LogUtil.info(this, "-getState()::rightOpen: " + rightOpen);
			
			if (leftClosed){
				result = DomeState.LEFT_BOX_CLOSED;
			} else if (leftOpen){
				result = DomeState.LEFT_BOX_OPENED;
			} else{
				result = DomeState.NOT_DEFINED;
			}
			
			/*if (leftClosed && rightClosed){
				result = DomeState.CLOSED_BOXES;
			}else if (leftOpen && rightOpen){
				result = DomeState.OPENED_BOXES;
			}else if (rightOpen){					//Don't take care of the other part of the dome
				result = DomeState.RIGHT_BOX_OPENED;
			}else if (leftOpen){
				result = DomeState.LEFT_BOX_OPENED;
			}else if (rightClosed){					//Don't take care of the other part of the dome
				result = DomeState.RIGHT_BOX_CLOSED;
			}else if (leftClosed){
				result = DomeState.LEFT_BOX_CLOSED;
//			}else if (rightOpen && leftClosed){
//				result = DomeState.RIGHT_BOX_OPENED;
//			}else if (rightClosed && leftOpen){
//				result = DomeState.LEFT_BOX_OPENED;
			}else{
				result = DomeState.NOT_DEFINED;
			}*/
			
		}catch(Exception ex){
			LogUtil.severe(this, "Error parsing Dome state: " + status + ". " + ex.getMessage());
		}
		
		return result;
		
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

	public String getFlux1() {
		return flux1;
	}


}