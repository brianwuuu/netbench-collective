package ch.ethz.systems.netbench.core.run.traffic;

import ch.ethz.systems.netbench.core.network.Event;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.Simulator;

public class BackLoggedFlowStartEvent extends Event {

    private final TransportLayer transportLayer;
    private final int targetId;
    private final long flowSizeByte;
    public final long flowSequencePriority;

    /**
     * Create event which will happen the given amount of nanoseconds later.
     *
     * @param timeFromNowNs     Time it will take before happening from now in nanoseconds
     * @param transportLayer    Source transport layer that wants to send the flow to the target
     * @param targetId          Target network device identifier
     * @param flowSizeByte      Size of the flow to send in bytes
     */
    public BackLoggedFlowStartEvent(long timeFromNowNs, TransportLayer transportLayer, int targetId, long flowSizeByte, long flowSequencePriority) {
        super(timeFromNowNs);
        this.transportLayer = transportLayer;
        this.targetId = targetId;
        this.flowSizeByte = flowSizeByte;
        this.flowSequencePriority = flowSequencePriority;
    }

    @Override
    public void trigger() {
        // Flow starts at the current moment: time=0
        FlowStartEvent event = new FlowStartEvent(0, this.transportLayer, this.targetId, this.flowSizeByte);
        // Register event
        Simulator.registerEvent(event);
    }
}
