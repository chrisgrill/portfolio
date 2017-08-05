package com.claytonkendall;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.io.*;


/**
 * Handles mail activity including stripping attachments.
 * @author Chris Grill
 * @version .5
 *
 */
public class Mail {

    /** Java mail session. Used for extracting info from messagesand sending messages*/
    private Session mailSession;
    /** Properties for the session in #mailSession*/
    private Properties props = System.getProperties();

    private MimeMessage msg = null;

    Mail(){
        props.put("mail.host", "localhost");
        props.put("mail.transport.protocol", "smtp");
        mailSession = Session.getDefaultInstance(props, null);
    }

    Mail(InputStream is) throws MessagingException{
        props.put("mail.host", "localhost");
        props.put("mail.transport.protocol", "smtp");
        mailSession = Session.getDefaultInstance(props, null);
        msg = new MimeMessage(mailSession,is);
    }

    /**
     * Loops through parts of multipart message and saves the first attachment found.
     *
     * @return String containing the path of the file
     */
    public String saveFirstAttachment() throws MessagingException{
        String fileName = "";
        Address[] fromAddress = null;
        try {

            fromAddress = msg.getFrom();
            Multipart multimsg = (Multipart) msg.getContent();
            for (int i = 0; i < multimsg.getCount(); i++){
                BodyPart bodyPart = multimsg.getBodyPart(i);
                if(Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && !bodyPart.getFileName().equals("") ) {
                    fileName = "/tmp/"+bodyPart.getFileName();
                    ((MimeBodyPart) bodyPart).saveFile(fileName);
                }
            }


        }
        catch (MessagingException mx ){
            MimeMessage msg = new MimeMessage(mailSession);
            msg.setRecipients(Message.RecipientType.TO,fromAddress);
        }

        catch (java.io.IOException iox){

        }
        return fileName;
    }

    public String getRecipientEmailAddress() throws MessagingException{
        List<String> toAddresses = new ArrayList<String>();
        Address[] recipients = msg.getRecipients(Message.RecipientType.TO);

        for (Address address : recipients) {
            toAddresses.add(address.toString());
        }
        System.out.println("Recipient: "+toAddresses.get(0));
        return toAddresses.get(0);
    }


}
