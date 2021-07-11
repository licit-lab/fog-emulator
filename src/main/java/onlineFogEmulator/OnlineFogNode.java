package onlineFogEmulator;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.*;
import util.SettingReader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class OnlineFogNode extends Thread {

    private String name;
    private ClientSession sessionOut;
    private ClientProducer producer;
    private static String urlOut;

    public OnlineFogNode(String name){
        this.name = name;

        // Reading the broker URL
        SettingReader st = new SettingReader();
        urlOut = st.readElementFromFileXml("settings.xml", "areaNode", "urlIn");
        System.out.println("Broker out: " + urlOut);
    }

    @Override
    public void run() {
        // Parameters and variables to read the corresponding area.csv data
        String csvFile = this.name + ".csv";
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(csvFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // create producer and its queue on broker
        createProducer();

        while (true) {
            // all fog threads start by sleeping for an indefinite time.
            try {
                System.out.println("Thread \"" + this.name + "\": Sleeping for an indefinite time...");
                sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Thread \"" + this.name + "\" has been awaken.");
            }

            String csvLine = null;
            try {
                if ((csvLine = bufferedReader.readLine()) == null) {
                    System.out.println("Thread \"" + this.name + "\": End of CSV file.");
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // process the csv line.
            if (!isInteger(csvLine)){
                System.out.println("Error in file: " + csvFile);
                break;
            }

            // reading numLinks from retrieved csvLine
            int numLinks = Integer.parseInt(csvLine);

            if (numLinks == 0){
                System.out.print("Thread \"" + this.name + "\": No packets to be sent (numLinks = " + numLinks + "). ");
            }
            else {;
                // we have numLinks lines to be read and sent. Thenn the fog thread has to sleep until being awakened in next interval.

                //System.out.println("Thread \"" + this.name + "\": Sending " + numLinks + " packets to the remote broker. ");
                for (int i = 0; i < numLinks; i++) {
                    String JSLine = null;
                    try {
                        JSLine = bufferedReader.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //System.out.println("Sending packet #" + i);
                    //System.out.println(JSLine);
                    sendMessage(JSLine);
                }
                System.out.print("Thread \"" + this.name + "\": " + numLinks + " packets have been sent. ");

            }

        }

        // Thread stops running
        Thread.currentThread().interrupt();

    }

    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }


    public void createProducer(){
        ClientSessionFactory factoryOut;
        try{
            ServerLocator locatorOut = ActiveMQClient.createServerLocator(urlOut);
            factoryOut = locatorOut.createSessionFactory();
            sessionOut = factoryOut.createSession(true,true);
            sessionOut.start();

            // A queue per producer (multipleNorthboundQueues)
            String NORTHBOUND_SUFFIX = "-Northbound";
            sessionOut.createQueue(new SimpleString(this.name + NORTHBOUND_SUFFIX), RoutingType.ANYCAST, new SimpleString(this.name + NORTHBOUND_SUFFIX), true);
            producer = sessionOut.createProducer(new SimpleString(this.name + NORTHBOUND_SUFFIX));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String messageBody){
        ClientMessage msg;
        try {
            msg = sessionOut.createMessage(true);
            msg.getBodyBuffer().writeString(messageBody);
            if(producer == null){
                System.out.println("ERRORRRRRRR");
            }
            producer.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}


