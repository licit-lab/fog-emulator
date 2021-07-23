package onlineFogEmulator;

import java.util.TimerTask;

public class FogTask extends TimerTask {
    private int numAggIntervals = 0;
    private final OnlineFogNode[] fogArray;

    public FogTask(OnlineFogNode[] fogArray){
        this.fogArray = fogArray;
    }

    @Override
    public void run() {
        System.out.println("Interval #" + numAggIntervals + ": " + java.time.LocalDateTime.now());
        System.out.println("Main Thread waking up all fog threads...\n");

        numAggIntervals++;

        int numAliveThreads = 0;
        for (OnlineFogNode onlineFogNode : fogArray) {
            if (onlineFogNode.isAlive()) {
                numAliveThreads++;
                onlineFogNode.interrupt();
            }
        }

        if(numAliveThreads == 0) {
            this.cancel();
            System.out.println("End of Main Thread!");
        }
    }
}
