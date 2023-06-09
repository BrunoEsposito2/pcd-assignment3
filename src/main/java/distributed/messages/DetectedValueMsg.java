package distributed.messages;

import akka.japi.Pair;

import java.time.ZonedDateTime;

public class DetectedValueMsg extends ValueMsg {

    private double waterLevel;
    private double limit;
    private ZonedDateTime dateTimeStamp;
    private int sensorZone;
    private String sensorID;
    private Pair<Integer, Integer> sensorCoords;
    private String seqNumber;

    public DetectedValueMsg(final int zone, final String sensorID, final double value, final double limit, final Pair<Integer, Integer> sensorCoords, String seqNumber) {
        this.sensorZone = zone;
        this.sensorID = sensorID;
        this.waterLevel = value;
        this.dateTimeStamp = ZonedDateTime.now();
        this.limit = limit;
        this.sensorCoords = sensorCoords;
        this.seqNumber = seqNumber;
    }

    public Pair<Integer, Integer> getSensorCoords() {
        return this.sensorCoords;
    }

    public double getWaterLevel() {
        return waterLevel;
    }

    public double getLimit() {
        return limit;
    }

    public ZonedDateTime getDateTimeStamp() {
        return dateTimeStamp;
    }

    public String getSensorID() {
        return sensorID;
    }

    public String getSeqNumber(){
        return seqNumber;
    }

    @Override
    public String toString() {
        return "DetectedValueMsg{" +
                "waterLevel=" + waterLevel +
                ", limit=" + limit +
                ", dateTimeStamp=" + dateTimeStamp +
                ", sensorZone=" + sensorZone +
                ", sensorID='" + sensorID + '\'' +
                ", sensorCoords=" + sensorCoords +
                '}';
    }
}
