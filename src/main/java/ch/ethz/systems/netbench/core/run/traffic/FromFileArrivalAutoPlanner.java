package ch.ethz.systems.netbench.core.run.traffic;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.Simulator;
import java.util.Map;

import java.io.*; // for file reading

public class FromFileArrivalAutoPlanner extends TrafficPlanner {

    private final String arrivalFilename;

    /**
     * Constructor.
     *
     * @param idToTransportLayerMap     Maps a network device identifier to its corresponding transport layer
     * @param arrivals                  File name of arrival plan
     */
    public FromFileArrivalAutoPlanner(Map<Integer, TransportLayer> idToTransportLayerMap, String filename) {
        super(idToTransportLayerMap);
        this.arrivalFilename = filename;
        SimulationLogger.logInfo("Flow planner", "FROM_FILE_ARRIVAL_AUTO_PLANNER(arrival file name=" + arrivalFilename + ")");
    }

    /**
     * Creates plan based on the given string:
     * (start_time, src_id, dst_id, flow_size_byte);(start_time, src_id, dst_id, flow_size_byte);...
     *
     * @param durationNs    Duration in nanoseconds
     */
    @Override
    public void createPlan(long durationNs) {
        // Create traffic plans in sequence of batches
        File file = new File(this.arrivalFilename);
        try (FileReader fr = new FileReader(file)) {
            BufferedReader br = new BufferedReader(fr);
            String st;
            while ((st = br.readLine()) != null) {
                // check if the first character forms a comment
                if (st.charAt(0) != '#') {
                    String[] arrivalSpl = st.split(",");
                    if (Long.valueOf(arrivalSpl[0].trim()) == 0) {
                        // Start the first flows with timeFronNowNs == 0
                        this.registerFlow(
                                Long.valueOf(arrivalSpl[0].trim()), // time of entry
                                Integer.valueOf(arrivalSpl[1].trim()), // source id
                                Integer.valueOf(arrivalSpl[2].trim()), // destination id
                                Long.valueOf(arrivalSpl[3].trim()) // size in terms of bytes
                        );
                    } else {
                        // Enqueue the rest of the flows into the sequence (auxiliary) queue in simulator
                        this.registerFlowSequence(
                                Long.valueOf(arrivalSpl[0].trim()), // time of entry
                                Integer.valueOf(arrivalSpl[1].trim()), // source id
                                Integer.valueOf(arrivalSpl[2].trim()), // destination id
                                Long.valueOf(arrivalSpl[3].trim())
                        );
                    }

                }
            }
            br.close();
        } catch (FileNotFoundException fe) {
            System.out.println("File not found");
        } catch (IOException ie) {
            System.out.println("IO exception not found");
        }
    }
    
    /**
     * Register the flow from [srcId] to [dstId] in the main Simulator.
     * However, do not register these flows into the main event queue yet.
     * Instead, register these flows into a backup event queue that will be called upon prior flows' completion.
     * @param time
     * @param srcId
     * @param dstId
     * @param flowSizeByte
     */
    protected void registerFlowSequence(long time, int srcId, int dstId, long flowSizeByte) {
        
        // Some checking
        if (srcId == dstId) {
            throw new RuntimeException("Invalid traffic pair; source (" + srcId + ") and destination (" + dstId + ") are the same.");
        } else if (idToTransportLayerMap.get(srcId) == null) {
            throw new RuntimeException("Source network device " + srcId + " does not have a transport layer.");
        } else if (idToTransportLayerMap.get(dstId) == null) {
            throw new RuntimeException("Destination network device " + dstId + ") does not have a transport layer.");
        } else if (time < 0) {
            throw new RuntimeException("Cannot register a flow with a negative timestamp of " + time);
        } else if (flowSizeByte < 0) {
            throw new RuntimeException("Cannot register a flow with a negative flow size (in bytes) of " + flowSizeByte);
        }

        // Create event
        // Fill the time parameter with 0 and let the event priority queue decide the sequence based on the sequence the event is added
        // Fill the flowSequencePriority parameter with time that indicates the order of the current batch
        BackLoggedFlowStartEvent event = new BackLoggedFlowStartEvent(0, idToTransportLayerMap.get(srcId), dstId, flowSizeByte, time);

        // Register event
        Simulator.registerEventSequence(event);
    }

}
