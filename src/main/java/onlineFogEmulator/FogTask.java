package onlineFogEmulator;

import java.util.TimerTask;

public class FogTask extends TimerTask {
    private int numAggIntervals = 0;
    private OnlineFogNode[] fogArray;

    public FogTask(OnlineFogNode[] fogArray){
        this.fogArray = fogArray;
    }

    @Override
    public void run() {
        System.out.println("Interval #" + numAggIntervals + ": " + java.time.LocalDateTime.now());
        System.out.println("Main Thread waking up all fog threads...\n");

        numAggIntervals++;

        int numAliveThreads = 0;
        for (int i = 0; i < fogArray.length; i++) {
            if (fogArray[i].isAlive()){
                numAliveThreads++;
                fogArray[i].interrupt();
            }
        }

        if(numAliveThreads == 0) {
            this.cancel();
            System.out.println("End of Main Thread!");
        }
    }
}
