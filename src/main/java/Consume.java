import com.rabbitmq.client.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Consume {

    public static void main(String[] args) {
        consumeQueue();
    }

    public static void consumeQueue() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("207.154.228.245");
            Connection connection = factory.newConnection();

            final Channel requestChannel = connection.createChannel();
            String queueReqName = "Get_Banks_Queue";


            requestChannel.queueDeclare(queueReqName, false, false, false, null);

            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            //requestChannel.basicQos(1);

            final String[] requestMessage = {null};
            do {


            // Consuming messages from the Get Banks channel
            final Consumer requestConsumer = new DefaultConsumer(requestChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    System.out.println(" [x] Received '" + message + "'");
                   try {
                       requestMessage[0] = new String(body);

                    //Parsing the value from the body
                    List<String> loanInformations = requestParser(requestMessage[0]);
                    // Execute the loan informations against the Bank WebService
                    String bankReturnedValues = Payload.execute("http://94.130.57.246:9000/bankwsdl/BankAppService?wsdl", loanInformations);
                    // Parsing the returned values into a list
                    List<String> listReturnedValues = Payload.XMLparser(bankReturnedValues);
                    gatherAndSend(requestMessage[0], listReturnedValues);
                    //System.out.println("Final Doc: " + finalDoc);
                    requestChannel.basicAck(envelope.getDeliveryTag(), false);
                    Thread.sleep(1000);
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   } finally {
                       System.out.println(" [x] Done");

                       requestChannel.basicAck(envelope.getDeliveryTag(), false);
                   }
                }
            };

                requestChannel.basicConsume(queueReqName, false, requestConsumer);
            }while((requestMessage[0].length() != 0));

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param parsedText - Parsing the content of the retrieved loan request.
     * @return - List with score, loan amount, and loan duration
     */
    public static List<String> requestParser(String parsedText) {
        List<String> returnedValues = new ArrayList<>();
        try {

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource src = new InputSource();
            src.setCharacterStream(new StringReader(parsedText));

            Document doc = builder.parse(src);
            returnedValues.add(doc.getElementsByTagName("creditScore").item(0).getTextContent());
            returnedValues.add(doc.getElementsByTagName("loanAmount").item(0).getTextContent());
            double calculatedYears = calculateYears((doc.getElementsByTagName("loanDuration").item(0).getTextContent()));
            returnedValues.add(String.valueOf(calculatedYears));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return returnedValues;
    }

    /**
     *
     * @param consumedMessage - The message consumed from the Message Queue
     * @param bankCalculations - The returned values from the Bank Webservice
     * @return - The message that has to be sent to the normalizer to return back to the requester
     */
    public static void gatherAndSend(String consumedMessage, List<String> bankCalculations) {
        Document doc = null;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            InputSource src = new InputSource();
            src.setCharacterStream(new StringReader(consumedMessage));
            doc = builder.parse(src);

            // Root Element added
            Element bankRootElement = doc.createElement("bank");
            doc.appendChild(bankRootElement);

            // Append Childs to rootElement : Bank
            Element bankChildName = doc.createElement("name");
            bankChildName.appendChild(doc.createTextNode(bankCalculations.get(0)));
            bankRootElement.appendChild(bankChildName);

            Element bankChildInterestRate = doc.createElement("interestrate");
            bankChildInterestRate.appendChild(doc.createTextNode(bankCalculations.get(1)));
            bankRootElement.appendChild(bankChildInterestRate);

            Element bankChildRefund = doc.createElement("refund");
            bankChildRefund.appendChild(doc.createTextNode(bankCalculations.get(2)));
            bankRootElement.appendChild(bankChildRefund);

            Publish.publishQueue(toString(doc));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    private static double calculateYears(String customerLoanDuration) {
        double inYears = 0;
        try {
            Date stringToDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS zzz").parse(customerLoanDuration);

            // Converted to milliseconds
            inYears = stringToDate.getTime() / 1000 / 60 / 60 / 24 / 364.24;
            //                                  ms   sec   min  hour   days

            return inYears;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String toString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }
}
