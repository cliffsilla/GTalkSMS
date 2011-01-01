package com.googlecode.gtalksms.receivers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.data.contacts.ContactsManager;


public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        
        if (bundle != null)  {
            if (MainService.running) {
                Object[] pdus = (Object[]) bundle.get("pdus");                
                int nbrOfpdus = pdus.length;
                msgs = new SmsMessage[nbrOfpdus];
                
                // There can be multiple SMS from multiple senders, there can be a maximum of nbrOfpdus different senders
                // However, send long SMS of same sender in one message
                ArrayList<String> senders = new ArrayList<String>();
                Map<String, String> msg = new HashMap<String, String>();
                
                for (int i = 0; i < nbrOfpdus; i++) {
                    msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                    
                    String msgString = msg.get(msgs[i].getOriginatingAddress()); // Check if index with number exists
                    
                    if(msgString == null) { // Index with number doesn't exist                                               
                        senders.add(msgs[i].getOriginatingAddress());  // Save sender for accessing associative array later

                        StringBuilder builder = new StringBuilder();    // Build string  
                        builder.append("SMS from ");
                        builder.append(ContactsManager.getContactName(context, msgs[i].getOriginatingAddress()));
                        builder.append(": ");
                        builder.append(msgs[i].getMessageBody().toString());
                        // Save string into associative array with sendernumber as index
                        msg.put(msgs[i].getOriginatingAddress(), builder.toString()); 
                        
                    } else {    // Number has been there, add content
                        // msgString already contains sms:sndrNbr:previousparts of SMS, just add this part
                        msgString = msgString + msgs[i].getMessageBody().toString();
                        msg.put(msgs[i].getOriginatingAddress(), msgString);
                    }
                }

                // Finally, send all SMS via XMPP by sender
                for(String sender : senders) {
                    Intent svcintent = MainService.newSvcIntent(context, ".GTalkSMS.SMS_RECEIVED", msg.get(sender) + "\n");
                    svcintent.putExtra("sender", sender);
                    context.startService(svcintent);
                }
            }
        }
    }
}