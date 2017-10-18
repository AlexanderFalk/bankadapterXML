import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Payload {

    public static String execute(String targetURL, List<String> loanInformations) {
        HttpURLConnection connection = null;
        StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+

        try{
            URL targetUrl = new URL(targetURL);
            connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type",
                    "text/xml");

            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);


            String loanrequest =
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:prog=\"http://program/\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <prog:getWSDLBank>\n" +
                    "         <arg0>"+ loanInformations.get(0) + "</arg0>\n" +
                    "         <arg1>"+ loanInformations.get(1) +"</arg1>\n" +
                    "         <arg2>"+ loanInformations.get(2) +"</arg2>\n" +
                    "      </prog:getWSDLBank>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";

            //Send request
            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.writeBytes(loanrequest);
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }

        return response.toString();
    }

    public static List<String> XMLparser(String parsedText) {
        List<String> returnedValues = new ArrayList<>();
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource src = new InputSource();
            src.setCharacterStream(new StringReader(parsedText));

            Document doc = builder.parse(src);
            returnedValues.add(doc.getElementsByTagName("return").item(0).getTextContent());
            returnedValues.add(doc.getElementsByTagName("return").item(1).getTextContent());
            returnedValues.add(doc.getElementsByTagName("return").item(2).getTextContent());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return returnedValues;
    }



}
