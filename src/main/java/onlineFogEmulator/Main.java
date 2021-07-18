package onlineFogEmulator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {

        // Reading area names and counting their number
        String file = "areaNames.txt";
        ArrayList<String> areaNames = new ArrayList<>();
        int numAreas = 0;

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String areaName;
        while ((areaName = bufferedReader.readLine()) != null) {
            areaNames.add(areaName);
            numAreas++;
        }

        System.out.println("Num areas: " + numAreas);

        //numAreas = 20;

        // just for test: supposing that we have only five areas

        // Printing the area names
        for (int i = 0; i < numAreas; i++) {
            System.out.println(areaNames.get(i));
        }

        // Main thread creating online fog objects/threads (a node per area)
        OnlineFogNode[] fogArray = new OnlineFogNode[numAreas];
        for (int i = 0; i < numAreas; i++) {
            fogArray[i] = new OnlineFogNode(areaNames.get(i));;
        }

        // Main Thread starting all fog threads
        System.out.println("Main Thread starting all fog threads...\n");
        for (int i = 0; i < numAreas; i++) {
            fogArray[i].start();
        }

        // NB: just after starting them, all fog threads sleep for an indefinite time (and wait to be awakened).

        // Main thread waits for a moment (to give time for threads creation and sleeping)
        // instead, we can use the below delay parameter

        // Main thread sets a timer to keep awakening fog threads (every 1 minute(s))
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new FogTask(fogArray), 3*60*1000, 3*60*1000);
        // Delay : delay in milliseconds before task is to be executed.
        // Period: time in milliseconds between successive task executions.

    }

}
