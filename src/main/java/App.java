
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;
import static org.junit.Assert.fail;

public class App {

    private static final String EXPECTED_EVENT_NAME = "event";

    final static String CHAIN_CODE_FILEPATH = "Chaincode";
    final static String CHAIN_CODE_NAME = "fabcar";
    final static String CHAIN_CODE_PATH = "github.com/fabcar";
    final static String CHAIN_CODE_VERSION = "1";
    final static TransactionRequest.Type CHAIN_CODE_LANG = TransactionRequest.Type.GO_LANG;

    public static void main(String[] argv) throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        SystemInit(hfClient);
    }

    public static void SystemInit(HFClient hfclient) throws Exception {
        SampleOrg org1 = new SampleOrg("peerOrg1", "Org1MSP");
        org1.setDomainName("org1.example.com");

        org1.setCAName("ca_peerOrg1");
        org1.setCALocation("http://192.168.1.164:7054");

        org1.addPeerLocation("peer0.org1.example.com", "grpc://192.168.1.164:7051");
        org1.addPeerLocation("peer1.org1.example.com", "grpc://192.168.1.164:7056");
        org1.addOrdererLocation("orderer.example.com", "grpc://192.168.1.164:7050");

        File sampleStoreFile = new File(System.getProperty("user.home") + "/test.properties");
        if (sampleStoreFile.exists()) { //For testing start fresh
            sampleStoreFile.delete();
        }

        final SampleStore sampleStore = new SampleStore(sampleStoreFile);

        SampleUser admin = sampleStore.getMember("admin", org1.getName(), org1.getMSPID(),
                findFileSk("Resource/Org1User/Admin@org1.example.com/msp/keystore"),
                new File("Resource/Org1User/Admin@org1.example.com/msp/signcerts/Admin@org1.example.com-cert.pem"));

        org1.setPeerAdmin(admin);
        hfclient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        hfclient.setUserContext(admin);
    }

    public static void channelTest(String name, HFClient hfClient, SampleOrg org, SampleStore sampleStore) throws Exception {
        // construct channel
        Channel channel = constructChannel(name, hfClient, org);

        // run channel
        sampleStore.saveChannel(channel);
    }

    public static Channel constructChannel(String name, HFClient hfClient, SampleOrg org) throws Exception {
        Collection<Orderer> orderers = new LinkedList<>();

        for(String orderName: org.getOrdererNames()){
            orderers.add(hfClient.newOrderer(orderName, org.getOrdererLocation(orderName)));
        }
        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);

        String channelConfig = "Resource/channel.tx";
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(channelConfig));

        SampleUser admin = org.getPeerAdmin();
        assert admin!=null;
        Channel newChannel = hfClient.newChannel(name, anOrderer, channelConfiguration, hfClient.getChannelConfigurationSignature(channelConfiguration, admin));

        for(String peerName: org.getPeerNames()){
            Peer peer = hfClient.newPeer(peerName, org.getPeerLocation(peerName));
            newChannel.joinPeer(peer, createPeerOptions().setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY, Peer.PeerRole.EVENT_SOURCE)));
        }

        for(Orderer orderer: orderers){
            newChannel.addOrderer(orderer);
        }

        newChannel.initialize();

        return newChannel;
    }

    public static void chaincodeTest(HFClient client, Channel channel, SampleOrg org){
        class ChaincodeEventCapture{
            final String handle;
            final BlockEvent blockEvent;
            final ChaincodeEvent chaincodeEvent;

            ChaincodeEventCapture(String handle, BlockEvent blockEvent, ChaincodeEvent chaincodeEvent) {
                this.handle = handle;
                this.blockEvent = blockEvent;
                this.chaincodeEvent = chaincodeEvent;
            }
        }

        Vector<ChaincodeEventCapture> chaincodeEvents = new Vector<>();

        try {
            final String channelName = channel.getName();
            System.out.println(String.format("Chaincode running at %s", channelName));

            System.out.println("-- Running GO Chaincode with own endorsement --");
            final ChaincodeID chaincodeID;
            Collection<ProposalResponse> responses = new LinkedList<>();
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            String chaincodeEventListenerHandle = channel.registerChaincodeEventListener(Pattern.compile(".*"),
                    Pattern.compile(Pattern.quote(EXPECTED_EVENT_NAME)),
                    (handle, blockEvent, chaincodeEvent) -> {
                        chaincodeEvents.add(new ChaincodeEventCapture(handle, blockEvent, chaincodeEvent));

                        String es = blockEvent.getPeer()!=null? blockEvent.getPeer().getName():"peer was null!!!";
                        System.out.println(String.format("RECEIVED Chaincode event with handle: %s, chaincode Id: %s, chaincode event name: %s, "
                                        + "transaction id: %s, event payload: \"%s\", from event source: %s",
                                handle, chaincodeEvent.getChaincodeId(),
                                chaincodeEvent.getEventName(),
                                chaincodeEvent.getTxId(),
                                new String(chaincodeEvent.getPayload()), es));
                    });

            ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
                    .setVersion(CHAIN_CODE_VERSION);
            chaincodeIDBuilder.setPath(CHAIN_CODE_PATH);
            chaincodeID = chaincodeIDBuilder.build();

            client.setUserContext(org.getPeerAdmin());
            System.out.println("Creating install proposal");

            InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
            installProposalRequest.setChaincodeID(chaincodeID);

            installProposalRequest.setChaincodeSourceLocation(new File(CHAIN_CODE_PATH));

            if (CHAIN_CODE_LANG.equals(TransactionRequest.Type.GO_LANG)) {

                installProposalRequest.setChaincodeInputStream(Util.generateTarGzInputStream(
                        (Paths.get(CHAIN_CODE_FILEPATH, "src", CHAIN_CODE_PATH).toFile()),
                        Paths.get("src", CHAIN_CODE_PATH).toString()));
            } else {
                installProposalRequest.setChaincodeInputStream(Util.generateTarGzInputStream(
                        (Paths.get(CHAIN_CODE_FILEPATH).toFile()),
                        "src"));
            }

            installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION);
            installProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);

            System.out.println("Sending install proposal");

            int numInstallProposal = 0;

            Collection<Peer> peers = channel.getPeers();

            numInstallProposal = numInstallProposal + peers.size();

            responses = client.sendInstallProposal(installProposalRequest, peers);

            for (ProposalResponse response : responses) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    System.out.printf("Successful install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }

            System.out.printf("Received %d install proposal responses. Successful+verified: %d . Failed: %d", numInstallProposal, successful.size(), failed.size());

            if (failed.size() > 0) {
                ProposalResponse first = failed.iterator().next();
                fail("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
            }
        }catch (Exception e){

        }
    }

    private static File findFileSk(String directorys) {

        File directory = new File(directorys);
        System.out.printf(String.valueOf(directory.exists()));

        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));

        if (null == matches) {
            throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }

        if (matches.length != 1) {
            throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }

        return matches[0];

    }
}
