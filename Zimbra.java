package com.claytonkendall;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import java.io.PrintWriter;


/**
 * Created by crg on 5/11/17.
 */
public class Zimbra {

    private String authToken = "";
    private SOAPConnection soapConnection;
    private String soapUrl = "https://mailserver";
    private String msgId = "";
    private String signature = "";

    Zimbra() throws Exception{
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        soapConnection = soapConnectionFactory.createConnection();
    }

    protected boolean authenticate(String userName, String password) throws Exception{
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("urn", "urn:zimbra");
        envelope.addNamespaceDeclaration("urn1", "urn:zimbraAccount");


        SOAPBody soapBody = envelope.getBody();
        SOAPElement soapAuthRequest = soapBody.addChildElement("AuthRequest", "urn1");
        SOAPElement authRequestAccount = soapAuthRequest.addChildElement("account", "urn1");
        SOAPElement authRequestPassword = soapAuthRequest.addChildElement("password", "urn1");

        QName authRequestPersistToken = new QName("persistAuthTokenCookie");
        QName csrfTokenSecured = new QName("csrfTokenSecured");
        soapAuthRequest.addAttribute(authRequestPersistToken, "1");
        soapAuthRequest.addAttribute(csrfTokenSecured, "1");
        authRequestAccount.addTextNode(userName);
        authRequestPassword.addTextNode(password);

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", "urn:zimbraAccount/Auth");

        soapMessage.saveChanges();

        SOAPMessage soapResponse = soapConnection.call(soapMessage, soapUrl);

        SOAPBody responseBody = soapResponse.getSOAPBody();
        if(responseBody.getFault() == null) {
            javax.xml.soap.Node authResponseNode = (Node) responseBody.getFirstChild();
            Node authTokenNode = (Node) authResponseNode.getFirstChild();
            Node textNode = (Node) authTokenNode.getFirstChild();
            authToken = textNode.getNodeValue();
            PrintWriter out = new PrintWriter(System.getProperty("user.home") + "/.zimbraNavAuth");
            out.println(authToken);
            out.close();
            return true;
        }
        return false;
    }

    protected boolean createMessage(String messageContent) throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("urn", "urn:zimbra");
        envelope.addNamespaceDeclaration("urn1", "urn:zimbraMail");

        SOAPHeader soapHeader = envelope.getHeader();
        SOAPElement context = soapHeader.addChildElement("context", "urn");
        SOAPElement msgAuthToken = context.addChildElement("authToken", "urn");

        QName hops = new QName("hops");
        context.addAttribute(hops,"1");

        msgAuthToken.addTextNode(authToken);

        SOAPBody soapBody = envelope.getBody();
        SOAPElement soapAddMsgRequest = soapBody.addChildElement("AddMsgRequest", "urn1");
        SOAPElement addM = soapAddMsgRequest.addChildElement("m", "urn1");
        SOAPElement content = addM.addChildElement("content", "urn1");

        QName mf = new QName("f");
        QName ml = new QName("l");
        addM.addAttribute(mf, "d");
        addM.addAttribute(ml, "Drafts");
        content.addTextNode(messageContent);


        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", "urn:zimbraMail/AddMsg");

        soapMessage.saveChanges();



        SOAPMessage soapResponse = soapConnection.call(soapMessage, soapUrl);
        SOAPBody responseBody = soapResponse.getSOAPBody();


        if(responseBody.getFault() == null) {
            Node bodyResponseNode = (Node) responseBody.getFirstChild();
            Node msgResponseNode = (Node) bodyResponseNode.getFirstChild();
            msgId = msgResponseNode.getAttributes().getNamedItem("id").getNodeValue();

            return true;
        }
        SOAPFault fault = responseBody.getFault();
        return false;
    }

    protected void getZimbraSignature() throws Exception{
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("urn", "urn:zimbra");
        envelope.addNamespaceDeclaration("urn1", "urn:zimbraAccount");
        SOAPHeader soapHeader = envelope.getHeader();
        SOAPElement context = soapHeader.addChildElement("context", "urn");
        SOAPElement msgAuthToken = context.addChildElement("authToken", "urn");

        QName hops = new QName("hops");
        context.addAttribute(hops,"1");

        msgAuthToken.addTextNode(authToken);

        SOAPBody soapBody = envelope.getBody();
        SOAPElement soapAuthRequest = soapBody.addChildElement("GetSignaturesRequest", "urn1");

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", "urn:zimbraAccount/GetSignatures");

        soapMessage.saveChanges();



        SOAPMessage soapResponse = soapConnection.call(soapMessage, soapUrl);
        SOAPBody responseBody = soapResponse.getSOAPBody();


        if(responseBody.getFault() == null) {
            Node bodyResponseNode = (Node) responseBody.getFirstChild();
            Node msgResponseNode = (Node) bodyResponseNode.getFirstChild();
            Node content = (Node) msgResponseNode.getFirstChild();
            signature = content.getValue();


        }




    }

    public String getSignature(){
        return signature;
    }
    public String getMsgId(){
        return msgId;
    }

    public void setAuthToken(String newToken){
        authToken = newToken;
    }
}
