package distributed.model;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.pubsub.Topic;
import distributed.messages.*;

import java.util.ArrayList;

import java.time.Duration;
import java.util.List;
import java.util.Random;

public class CoordinatorZone extends AbstractBehavior<ValueMsg> {
    private final String id;
    private final String barrackAddress;
    private final int zone;
    private final List<SensorSnapshot> sensorSnapshots;
    private String seqNumber;
    private akka.actor.typed.ActorRef<Topic.Command<ValueMsg>> topic;
    private final int numSensors;
    private boolean firstIterationFlag = true;

    private CoordinatorZone(final ActorContext<ValueMsg> context, final String id, final String barrackAddress, final int z,
                            final akka.actor.typed.ActorRef<Topic.Command<ValueMsg>> topic, final int nSensors) {
        super(context);
        this.id = id;
        this.barrackAddress = barrackAddress;
        this.zone = z;
        this.sensorSnapshots = new ArrayList<>();
        this.topic = topic;
        this.numSensors = nSensors;
    }

    public static Behavior<ValueMsg> create(final String id, final String barrackAddress, final int z,
                                            final akka.actor.typed.ActorRef<Topic.Command<ValueMsg>> topic, final int numSensors) {
        return Behaviors.setup(ctx -> {
            CoordinatorZone coordinator = new CoordinatorZone(ctx, id, barrackAddress, z, topic, numSensors);
            return Behaviors.withTimers(t -> {
                t.startTimerAtFixedRate(new FirstIterationMsg(), Duration.ofMillis(8000));
                return coordinator;
            });
        });
    }

    @Override
    public Receive<ValueMsg> createReceive() {
        return newReceiveBuilder()
                .onMessage(DetectedValueMsg.class, this::evaluateData)
                .onMessage(FirstIterationMsg.class, this::executeFirstIteration)
                .onMessage(TriggerSendToBarrack.class, this::sendStatusAndReset)
                .build();
    }

    private Behavior<ValueMsg> executeFirstIteration(FirstIterationMsg msg) {
        if(firstIterationFlag){
            this.firstIterationFlag = false;
            seqNumber = String.valueOf(new Random().nextInt());
            topic.tell(Topic.publish(new RequestSensorDataMsg(seqNumber)));
        }
        return Behaviors.same();
    }
    private Behavior<ValueMsg> sendStatusAndReset(final TriggerSendToBarrack msg) {
        long overflownSensorNumber = this.sensorSnapshots.stream().filter(ss -> ss.getValue() > ss.getLimit()).count();
        String status = overflownSensorNumber > (sensorSnapshots.size() / 2) ? "FLOOD" : "OK";
        System.out.println("zone " + this.zone + " status: " + status + this.sensorSnapshots.size());

        //send message to barrack
        getContext().classicActorContext()
                .actorSelection(ActorPath.fromString(this.barrackAddress))
                .tell(new ZoneStatus(status, this.sensorSnapshots), ActorRef.noSender());

        this.sensorSnapshots.clear();
        seqNumber = String.valueOf(new Random().nextInt());
        topic.tell(Topic.publish(new RequestSensorDataMsg(seqNumber)));
        return Behaviors.same();
    }

    private Behavior<ValueMsg> evaluateData(final DetectedValueMsg msg) {
        if (msg.getSeqNumber().equals(this.seqNumber)) {
            SensorSnapshot snapshot = new SensorSnapshot(msg.getSensorCoords(), msg.getWaterLevel(), msg.getLimit(), msg.getSensorID(), msg.getDateTimeStamp());
            this.sensorSnapshots.removeIf(s -> s.getId().equals(msg.getSensorID()));
            this.sensorSnapshots.add(snapshot);
        }
        if (sensorSnapshots.size() == this.numSensors) {
            System.out.println("all sensors received of zone " + this.zone);
            this.getContext().getSelf().tell(new ValueMsg());
        }
        return Behaviors.same();
    }

}
