package eu.gloria.tadn.rtd;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



import eu.gloria.tools.log.LogUtil;

/**
 * Parser for the TADs Weather Xml.
 *  
 * @author jcabello
 *
 */
public class WeatherXMLParser {
	
	
	private HashMap<String, String> atts;
	private HashMap<String, String> alarm;
	
	public static void main(String[] args) {
		
		try {
			String xml = "<xml><parameters><parameter><name>Humedad relativa(%)</name><value>6.65</value><alarm>0</alarm></parameter><parameter><name>Direcci&amp;oacute;n del viento(m/s)</name><value>262.78</value><alarm>0</alarm></parameter><parameter><name>Velocidad del viento(m/s)</name><value>4.72</value><alarm>0</alarm></parameter><parameter><name>Presi&amp;oacute;n</name><value>764.2</value><alarm>0</alarm></parameter><parameter><name>Temperatura del aire(&amp;deg;C)</name><value>9.41</value><alarm>0</alarm></parameter><parameter><name>Bater&amp;iacute;a(V)</name><value>13.39</value><alarm>0</alarm></parameter></parameters></xml>";
			WeatherXMLParser obj = new WeatherXMLParser(xml);
			System.out.println("");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public WeatherXMLParser(String xml) throws Exception{
		
		atts = new HashMap<String, String>();
		alarm = new HashMap<String, String>();
		
		InputStream inputStream = null;
		
		try{
		
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		
			inputStream = new ByteArrayInputStream(xml.getBytes());
		
			Document document = dBuilder.parse(inputStream);
			document.getDocumentElement().normalize();
		
			Node node = document.getElementsByTagName("xml").item(0);
			NodeList nList =  node.getChildNodes();
			
			if (nList.getLength() != 1){
				throw new Exception("WeatherXMLParser. Invalid 'parameters' nodes count:" + nList.getLength());
			}
			NodeList nListParameter = nList.item(0).getChildNodes();
			
			for (int x = 0; x < nListParameter.getLength(); x++) {
				
				Node parameterNode = nListParameter.item(x);
				NodeList parameterAttNodeList =  parameterNode.getChildNodes();
				String name = getNodeValue(parameterAttNodeList, "name").trim();
				String value = getNodeValue(parameterAttNodeList, "value");
				String alarmValue = getNodeValue(parameterAttNodeList, "alarm");
				
				atts.put(name, value);
				alarm.put(name, alarmValue);
				
				LogUtil.info(this, "WeatherXMLParser.loading: " + "[name="+ name + ", value=" + value + "]");
				
			}
			
			
		}catch(Exception ex){
			LogUtil.severe(this, "WeatherXMLParser. Error parsing XML: " + xml);
			throw ex;
		}
		
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
	
	public String getValue(String paramName){
		
		
		String value = atts.get(paramName.trim());
		LogUtil.info(this, "WeatherXMLParser.getValue(" + paramName + ")=" + value);
		return value;
	}
	
	public String getAlarm(String paramName){
		
		
		String value = alarm.get(paramName.trim());
		LogUtil.info(this, "WeatherXMLParser.getAlarm(" + paramName + ")=" + value);
		return value;
	}

}
