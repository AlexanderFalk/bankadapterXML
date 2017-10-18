# Bank Adapter XML  
  
This adapter helps to translate the input from the RabbitMQ to https://github.com/AlexanderFalk/bankwsdl , which is the Bank itself.  
The adapter sends a SOAP request to the Bank WSDL with the following payload:  
```
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:prog="http://program/">
  <soapenv:Header/>
    <soapenv:Body>
      <prog:getWSDLBank>
        <arg0>"+ loanInformations.get(0) + "</arg0>
        <arg1>"+ loanInformations.get(1) +"</arg1>
        <arg2>"+ loanInformations.get(2) +"</arg2>
      </prog:getWSDLBank>
     </soapenv:Body>
  </soapenv:Envelope>
                    
```
The information for the arguments are gathered from the message queue. When a message is sent, a consumer is gathering the message and sends it to a method called **requestParser(parsedText: String)**. This method takes the message, grabs the XML fields (creditScore, loanAmount, loanDuration) and put them into a list and then returns it. The list is used as an argument for another method, in a different class, called: **execute(targetUrl: String, loanInformations: List)** .  
The method sends the SOAP request with the above payload, filling in the inputs from the list gathered from the message, open up a stream for the response, put the response into a new String, and then returns the String.   
When the string with the response has been returned, the consumers next step is to call a method that will parse and add the response to the original consumed XML message.  
The final output to the message queue forwarded to the normalizer will look like this:  
```
<LoanRequest>
  <ssn>0103721785</ssn>
  <creditScore>799</creditScore>
  <loanAmount>1000.0</loanAmount>
  <loanDuration>1971-10-28 01:00:00.0 CET</loanDuration>
</LoanRequest>
<LoanResponse>
  <name>BankWSDL</name>
  <interestRate>3.9174370708800303</interestRate>
  <refund>1078.0</refund>
</LoanResponse>

```
