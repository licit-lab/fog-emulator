package onlineFogEmulator;

import com.google.gson.Gson;
import model.AggregateVehiclesTravelTimeSample;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;

public class OnlineFogNode extends Thread {

    private final String name;
    private ClientSession sessionOut;
    private ClientProducer producer;
    private static String urlOut;
    private final BufferedWriter bufferedWriter;
    private final CSVPrinter csvPrinter;

    public OnlineFogNode(String name) throws IOException {
        this.name = name;

        // Reading the broker URL
        SettingReader st = new SettingReader();
        urlOut = st.readElementFromFileXml("settings.xml", "areaNode", "urlOut");
        System.out.println("Broker out: " + urlOut);
        createProducer();
        bufferedWriter = new BufferedWriter(new FileWriter(name+"_logs.csv",true));
        csvPrinter = CSVFormat.DEFAULT.withDelimiter(';')
                .withHeader("numLinks","domainAggTimestamp","beforeTimestamp","afterTimestamp").print(bufferedWriter);
    }

    @Override
    public void run() {
        // Parameters and variables to read the corresponding area.csv data
        String csvFile = this.name + ".txt";
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(csvFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

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
                assert bufferedReader != null;
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

            // we have numLinks lines to be read and sent. Thenn the fog thread has to sleep until being awakened in next interval.

            //System.out.println("Thread \"" + this.name + "\": Sending " + numLinks + " packets to the remote broker. ");

            //timestamp before entering the loop
            AggregateVehiclesTravelTimeSample s = null;
            long beforeTimestamp = System.currentTimeMillis();
            for (int i = 0; i < numLinks; i++) {
                String JSLine = null;
                try {
                    JSLine = bufferedReader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Setting the aggregationTimestamp
                Gson g = new Gson();
                s = g.fromJson(JSLine, AggregateVehiclesTravelTimeSample.class);
                s.setAggTimestamp(System.currentTimeMillis());
                JSLine = new Gson().toJson(s);

                //System.out.println("Sending packet #" + i);
                //System.out.println(JSLine);
                sendMessage(JSLine);
            }
            long afterTimestamp = System.currentTimeMillis();
            long domainAggTimestamp = 0L;
            if(s != null)
                domainAggTimestamp = s.getDomainAggTimestamp();
            long differenceTimestamp = afterTimestamp - beforeTimestamp;
            try {
                csvPrinter.printRecord(numLinks,domainAggTimestamp,beforeTimestamp,afterTimestamp);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.print("Thread \"" + this.name + "\": " + numLinks + " packets have been sent. ");
        }

        try {
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
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
            if(producer == null)
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