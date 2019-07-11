package test;

import Sample.SampleOrg;
import Sample.SampleUser;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static java.nio.charset.StandardCharsets.UTF_8;


public class App {

    private static final String EXPECTED_EVENT_NAME = "event";
    private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);

    final static String CHAIN_CODE_FILEPATH = "Resource/Chaincode";
    final static String CHAIN_CODE_PATH = "github.com/fabcar";
    final static String CHAIN_CODE_NAME = "fabcar";
    final static String CHAIN_CODE_VERSION = "1";
    final static TransactionRequest.Type CHAIN_CODE_LANG = TransactionRequest.Type.GO_LANG;

    public static void main(String[] argv) throws Exception {
        // 初始化client
        HFClient hfClient = HFClient.createNewInstance();
        HFClient hfClient2 = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        hfClient2.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        SampleUser admin1 = FabricConfig.getAdminUser1(hfClient);
        SampleUser admin2 = FabricConfig.getAdminUser2(hfClient2);
        SampleOrg org1 = FabricConfig.getSampleOrg1();
        SampleOrg org2 = FabricConfig.getSampleOrg2();
        org1.setPeerAdmin(admin1);
        org2.setPeerAdmin(admin2);


        Collection<Orderer> orderers = new LinkedList<>();
        for(String ordererName: org1.getOrdererNames()){
            hfClient2.newOrderer(ordererName, org2.getOrdererLocation(ordererName));
            orderers.add(hfClient.newOrderer(ordererName, org1.getOrdererLocation(ordererName)));
        }
        Collection<Peer> peers = new LinkedList<>();
        Collection<Peer> peers2 = new LinkedList<>();
        for(String peerName: org1.getPeerNames()){
            peers.add(hfClient.newPeer(peerName, org1.getPeerLocation(peerName)));
        }
        for(String peerName: org2.getPeerNames()){
            peers2.add(hfClient2.newPeer(peerName, org2.getPeerLocation(peerName)));
        }

        /**
         * 第二步：从配置文件中读取Channel，之后的步骤不用将代码注释
         * */
        Channel channel = ChannelFactory.fromSampleStore(hfClient);
//        Channel channel2 = test.ChannelFactory.fromSampleStore(hfClient2);

        /**
         * 第一步：使用Org1的Client新建channel，之后将Org2的节点加入该channel, 之后将这段代码注释，进行第二部
         * */
//        Channel channel = test.ChannelFactory.constructChannel("mychannel", admin1, hfClient, orderers, peers);
//        test.ChannelFactory.joinChannel(channel, peers2);
        /**
         * 第三步：install链码，之后将代码注释，进行第四步
         * */
//        installChaincode(hfClient, channel, org1, true);
//        installChaincode(hfClient2, channel2, org2, true);


        ChaincodeID chaincodeID = chaincodeInit();
        /**
         * 第四步：实例化链码
         * */
//        instantiateChaincode(hfClient, chaincodeID, channel);
        /**
         * 第五步：调用链码
         * */
        // invodeChaincode(hfClient, channel, chaincodeID);

        /**
         * 第六步：查询链码
         * */
        queryChaincode(hfClient, channel, chaincodeID);
    }

    public static Boolean installChaincode(HFClient client, Channel channel, SampleOrg org, boolean installChaincode){
        try {
            final String channelName = channel.getName();
            System.out.println(String.format("Chaincode running at %s", channelName));

            System.out.println("-- Running GO Chaincode with own endorsement --");
            final ChaincodeID chaincodeID = chaincodeInit();
            if(installChaincode){
                client.setUserContext(org.getPeerAdmin());
                System.out.println("Creating install proposal");
                InstallProposalRequest installProposalRequest = installChaincodeProposalRequestInit(client, chaincodeID);
                System.out.println("Sending install proposal");
                Collection<Peer> peers = channel.getPeers();
                int numInstallProposal = 0;
                numInstallProposal = numInstallProposal + peers.size();
                boolean ret = sendChaincodeInstallProposalRequest(client, installProposalRequest, peers, numInstallProposal);
                if(!ret) {
                    return false;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void queryChaincode(HFClient hfClient, Channel channel, ChaincodeID chaincodeID) throws ProposalException, InvalidArgumentException {
        QueryByChaincodeRequest queryByChaincodeRequest = hfClient.newQueryProposalRequest();
        queryByChaincodeRequest.setChaincodeID(chaincodeID);
        queryByChaincodeRequest.setFcn("queryAllCars");
        Collection<ProposalResponse> response = channel.queryByChaincode(queryByChaincodeRequest);
        for (ProposalResponse pres : response) {
            String stringResponse = new String(pres.getChaincodeActionResponsePayload());
            System.out.println(stringResponse);
        }
    }

    public static void invodeChaincode(HFClient hfClient, Channel channel, ChaincodeID chaincodeID) throws InvalidArgumentException, ProposalException {
        TransactionProposalRequest transactionProposalRequest = initTransactionProposalRequest(hfClient, chaincodeID,channel);
        sendTransactionProposalRequest(channel, transactionProposalRequest);
    }

    private static TransactionProposalRequest initTransactionProposalRequest(HFClient client, ChaincodeID chaincodeID, Channel channel) throws InvalidArgumentException {
        TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
        transactionProposalRequest.setFcn("initLedger");
        transactionProposalRequest.setProposalWaitTime(1000);
        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8)); //Just some extra junk in transient map
        tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8)); // ditto
        tm2.put("result", ":)".getBytes(UTF_8));  // This should be returned in the payload see chaincode why.
        transactionProposalRequest.setTransientMap(tm2);
        return transactionProposalRequest;
    }

    private static boolean sendTransactionProposalRequest(Channel channel, TransactionProposalRequest transactionProposalRequest) throws InvalidArgumentException, ProposalException {
        Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        for (ProposalResponse response : transactionPropResp) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                System.out.println(String.format("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
                successful.add(response);
            } else {
                failed.add(response);
            }
        }
        if(failed.size() > 0){
            return false;
        }
        CompletableFuture<BlockEvent.TransactionEvent> cf = channel.sendTransaction(transactionPropResp);
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

    private static Boolean instantiateChaincode(HFClient client, ChaincodeID chaincodeID, Channel channel) throws ChaincodeEndorsementPolicyParseException, IOException, InvalidArgumentException, ProposalException {
        String functionName = "initLedger";
        String[] arguments = { "" };
        InstantiateProposalRequest instantiateProposalRequest = instantiateChaincodeProposalRequestInit(client, chaincodeID, functionName, arguments, null);
        return sendChaincodeInstantiateProposalRequest(channel, instantiateProposalRequest);
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

        if(policyPath != null){
            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File(policyPath));
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
        }
        instantiateProposalRequest.setProposalWaitTime(180000);
        return instantiateProposalRequest;
    }

    private static boolean sendChaincodeInstantiateProposalRequest(Channel channel, InstantiateProposalRequest instantiateProposalRequest) throws ProposalException, InvalidArgumentException {
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        Collection<ProposalResponse> responses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());
        System.out.print("Sending instantiateProposalRequest to all peers with arguments");
        CompletableFuture<BlockEvent.TransactionEvent> cf = channel.sendTransaction(responses);

        System.out.println("Chaincode " + instantiateProposalRequest.getChaincodeName() + " on channel " + channel.getName() + " instantiation " + cf);
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
