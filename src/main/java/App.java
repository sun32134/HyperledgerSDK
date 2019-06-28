
import jdk.nashorn.internal.runtime.linker.Bootstrap;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class App {

    private static final String EXPECTED_EVENT_NAME = "event";

    final static String CHAIN_CODE_FILEPATH = "Resource/Chaincode";
    final static String CHAIN_CODE_PATH = "github.com/fabcar";
    final static String CHAIN_CODE_NAME = "fabcar";
    final static String CHAIN_CODE_VERSION = "1";
    final static TransactionRequest.Type CHAIN_CODE_LANG = TransactionRequest.Type.GO_LANG;

    public static void main(String[] argv) throws Exception {
        HFClient hfClient = HFClient.createNewInstance();
        SampleOrg org1 = FabricConfig.getSampleOrg();
        SampleUser admin = FabricConfig.getSampleUser(hfClient);
        org1.setPeerAdmin(admin);

        String channelName = "mychannel";
        Channel channel = constructChannel(channelName, hfClient, org1, false);
        chaincodeTest(hfClient, channel, org1, true);
    }

    public static void channelTest(String name, HFClient hfClient, SampleOrg org, SampleStore sampleStore) throws Exception {
        // construct channel
        Channel channel = constructChannel(name, hfClient, org, true);

        // run channel
        sampleStore.saveChannel(channel);
    }

    public static Channel constructChannel(String name, HFClient hfClient, SampleOrg org, boolean joinPeer) throws Exception {
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

        if(!joinPeer){
           return newChannel;
        }
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

    public static Boolean chaincodeTest(HFClient client, Channel channel, SampleOrg org, boolean installChaincode){
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
            final ChaincodeID chaincodeID = chaincodeInit();;
            if(installChaincode){
                client.setUserContext(org.getPeerAdmin());
                System.out.println("Creating install proposal");
                InstallProposalRequest installProposalRequest = installChaincodeProposalRequestInit(client, chaincodeID);
                System.out.println("Sending install proposal");
                Collection<Peer> peers = channel.getPeers();
                int numInstallProposal = 0;
                numInstallProposal = numInstallProposal + peers.size();
                boolean ret = sendChaincodeInstallProposalRequest(client, installProposalRequest, peers, numInstallProposal);
                if(!ret){
                    return false;
                }

                String functionName = "createCar";
                String[] functionArgs = new String[]{"CAR1", "Chevy", "Volt", "Red", "Nick"};
                String policyPath = "Resource/chaincodeendorsementpolicy.yaml";
                InstantiateProposalRequest instantiateProposalRequest = instantiateChaincodeProposalRequestInit(client, chaincodeID, functionName, functionArgs, policyPath);
                ret = sendChaincodeInstantiateProposalRequest(channel, instantiateProposalRequest);
                if(!ret){
                    return false;
                }
                // TODO send instantiateTransaction to orderer

            }

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static ChaincodeID chaincodeInit(){
        ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(CHAIN_CODE_VERSION);
        chaincodeIDBuilder.setPath(CHAIN_CODE_PATH);
        return chaincodeIDBuilder.build();
    }

    private static InstallProposalRequest installChaincodeProposalRequestInit(HFClient client, ChaincodeID chaincodeID) throws Exception{
        InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        installProposalRequest.setChaincodeID(chaincodeID);

        installProposalRequest.setChaincodeSourceLocation(Paths.get(CHAIN_CODE_FILEPATH).toFile());

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
        return installProposalRequest;
    }

    private static boolean sendChaincodeInstallProposalRequest(HFClient client, InstallProposalRequest installProposalRequest, Collection<Peer> peers, int numInstallProposal) throws Exception{
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        Collection<ProposalResponse> responses = client.sendInstallProposal(installProposalRequest, peers);

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
            System.out.print("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
            return false;
        }
        return true;
    }

    private static InstantiateProposalRequest instantiateChaincodeProposalRequestInit(HFClient client, ChaincodeID chaincodeID, String functionName, String[] functionArgs, String policyPath) throws IOException, InvalidArgumentException, ChaincodeEndorsementPolicyParseException {
        InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
        instantiateProposalRequest.setProposalWaitTime(180000);
        instantiateProposalRequest.setChaincodeID(chaincodeID);
        instantiateProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
        instantiateProposalRequest.setFcn(functionName);
        instantiateProposalRequest.setArgs(functionArgs);

        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        instantiateProposalRequest.setTransientMap(tm);

        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File(policyPath));
        instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

        System.out.print("Sending instantiateProposalRequest to all peers with arguments");

        return instantiateProposalRequest;
    }

    private static boolean sendChaincodeInstantiateProposalRequest(Channel channel, InstantiateProposalRequest instantiateProposalRequest) throws ProposalException, InvalidArgumentException {
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        Collection<ProposalResponse> responses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());

        for(ProposalResponse response: responses){
            if(response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS){
                successful.add(response);
                System.out.printf("Successfully instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
            }
            else{
                failed.add(response);
            }
        }

        if(failed.size() > 0){
            System.out.print("Not enough endorses for instantiate");
            return false;
        }
        return true;
    }
}
