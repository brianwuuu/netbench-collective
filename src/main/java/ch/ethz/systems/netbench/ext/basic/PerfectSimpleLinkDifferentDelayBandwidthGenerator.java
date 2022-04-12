package ch.ethz.systems.netbench.ext.basic;

import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.Link;
import ch.ethz.systems.netbench.core.network.NetworkDevice;
import ch.ethz.systems.netbench.core.run.infrastructure.LinkGenerator;

// For reading different link delay
import java.util.HashMap;
import java.io.*; // for file reading
import org.javatuples.Pair;

public class PerfectSimpleLinkDifferentDelayBandwidthGenerator extends LinkGenerator {

    private final long delayNs;
    private final double networkBandwidthBitPerNs;
    private final String linkDelayFilename;
    private HashMap<Pair<Integer, Integer>, Integer> srcDstToLinkDelayNs;
    private HashMap<Pair<Integer, Integer>, Double> srcDstToLinkBwGbps;

    public PerfectSimpleLinkDifferentDelayBandwidthGenerator(long delayNs, double networkBandwidthBitPerNs, String linkDelayFilenameArg) {
        // Need to generate a link delay map; read the map when generating the link
        this.delayNs = delayNs; // this is the default link delay in ns if the delay is not specified in the file
        this.networkBandwidthBitPerNs = networkBandwidthBitPerNs;
        this.linkDelayFilename = linkDelayFilenameArg;
        this.srcDstToLinkDelayNs = new HashMap<Pair<Integer, Integer>, Integer>();
        this.srcDstToLinkBwGbps = new HashMap<Pair<Integer, Integer>, Double>();
        readLinkDelayFile(this.linkDelayFilename);
        SimulationLogger.logInfo("Link", 
                                    "PERFECT_SIMPLE_LINK_DIFF_DELAY_BW(delayNs=" + 
                                    delayNs + 
                                    ", networkBandwidthBitPerNs=" + 
                                    networkBandwidthBitPerNs + 
                                    ")");
    }

    private void readLinkDelayFile(String filename) {
        // The file should contain all of the elements and the corresponding pod IDs.
        File file = new File(filename); 
        try (FileReader fr = new FileReader(file)) {
            BufferedReader br = new BufferedReader(fr);     
            String st; 
            while ((st = br.readLine()) != null) {
                if (st.length() > 0 && st.charAt(0) != '#') {
                    // Format must be: network_device_id, has_reconfigurable_port (T/F), pod_id 
                    String[] strArray = st.split(",", 0);
                    int src_id = Integer.parseInt(strArray[0]);
                    int dst_id = Integer.parseInt(strArray[1]);                    
                    int link_delay_ns = Integer.parseInt(strArray[2]);
                    double link_bw_gbps = Double.parseDouble(strArray[3]);
                    srcDstToLinkDelayNs.put(new Pair<Integer, Integer>(src_id, dst_id), link_delay_ns);
                    srcDstToLinkBwGbps.put(new Pair<Integer, Integer>(src_id, dst_id), link_bw_gbps);
                }
            }
            br.close();
        } catch (FileNotFoundException fe) {
            System.out.println("File not found");
        } catch (IOException ie) {
            System.out.println("IO exception not found");
        }
    }

    @Override
    public Link generate(NetworkDevice fromNetworkDevice, NetworkDevice toNetworkDevice, long link_multiplicity) {
        double actualBandwidth = link_multiplicity * this.networkBandwidthBitPerNs;
        Pair<Integer, Integer> srcDstPair = new Pair<Integer, Integer>(fromNetworkDevice.getIdentifier(), toNetworkDevice.getIdentifier());
        if (this.srcDstToLinkDelayNs.containsKey(srcDstPair)){
            return new PerfectSimpleLink(this.srcDstToLinkDelayNs.get(srcDstPair), this.srcDstToLinkBwGbps.get(srcDstPair)*link_multiplicity);
        }
        return new PerfectSimpleLink(delayNs, actualBandwidth);
    }

}
