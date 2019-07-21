package main

import (
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	sc "github.com/hyperledger/fabric/protos/peer"
)

type smartcontract struct {
}

type Info struct {
	RequestHash          string `json:"RequestHash"`
	CopyrightOwner       string `json:"CopyrightOwner"`
	Title                string `json:"Title"`
	Type                 string `json:"Type"`
	RequestTime          string `json:"RequestTime"`
	CreationCompleteTime string `json:"CreationCompleteTime"`
	FirstPublishTime     string `json:"FirstPublishTime"`
	RequestUserId        string `json:"RequestUserId"`
	Fingerprint          string `json:"Fingerprint"`
	DownloadUrl          string `json:"DownloadUrl"`
	SigAlgorithm         string `json:"SigAlgorithm"`
	DCI                  string `json:"DCI"`
	DCIHash              string `json:"DCIHash"`
	DCISigAlgorithm      string `json:"DCISigAlgorithm"`
	DCIStatus            string `json:"DCIStatus"`
}

type Event struct {
	Txid string `json:"Txid"`
	Hash string `json:"Hash"`
}

func (s *smartcontract) Init(APIstub shim.ChaincodeStubInterface) sc.Response {
	return shim.Success(nil)
}

func (s *smartcontract) Invoke(APIstub shim.ChaincodeStubInterface) sc.Response {

	// Retrieve the requested Smart Contract function and arguments
	function, args := APIstub.GetFunctionAndParameters()
	// Route to the appropriate handler function to interact with the ledger appropriately
	if function == "uploadRegister" {
		return s.uploadRegister(APIstub, args)
	} else if function == "queryRegister" {
		return s.queryInfo(APIstub, args)
	} else if function == "uploadDCI" {
		return s.uploadDCI(APIstub, args)
	} else if function == "expiredDCI" {
		return s.expiredDCI(APIstub, args)
	}
	return shim.Success([]byte("Invoke success"))
}

func (s *smartcontract) uploadRegister(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 11 {
		return shim.Error("Incorrect number of arguments. Expecting 11, get " + string(len(args)))
	}
	var newRow = Info{RequestHash: args[0], CopyrightOwner: args[1], Title: args[2], Type: args[3], RequestTime: args[4], CreationCompleteTime: args[5], FirstPublishTime: args[6], RequestUserId: args[7], Fingerprint: args[8], DownloadUrl: args[9], SigAlgorithm: args[10], DCI: "", DCIHash: "", DCISigAlgorithm: "", DCIStatus: ""}
	//var newRow = Info{RequestHash:"1", CopyrightOwner:"sunaiying", Title:"crm", Type:"23423", RequestTime:"asdf", CreationCompleteTime:"asdf", FirstPublishTime:"sdf", RequestUserId:"sadf", Fingerprint:"asdfae", DownloadUrl:"sdf", SigAlgorithm:"we", DCI:"sadf", DCIHash:"sdfawe", DCIStatus:"true", DCISigAlgorithm:"ECDSA"}
	requestAsBytes, _ := json.Marshal(newRow)
	APIstub.PutState(args[0], requestAsBytes)

	var event = Event{Txid: APIstub.GetTxID(), Hash: args[0]}
	eventAsBytes, _ := json.Marshal(event)
	APIstub.SetEvent("upload success", eventAsBytes)

	return shim.Success([]byte("upload success"))
}

func (s *smartcontract) queryInfo(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1, get " + string(len(args)))
	}
	infoAsBytes, _ := APIstub.GetState(args[0])
	return shim.Success(infoAsBytes)
}

func (s *smartcontract) uploadDCI(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 5 {
		return shim.Error("Incorrect number of arguments. Expecting 5")
	}
	rowAsBytes, _ := APIstub.GetState(args[0])
	row := Info{}
	json.Unmarshal(rowAsBytes, &row)
	row.DCI = args[1]
	row.DCIHash = args[2]
	row.DCISigAlgorithm = args[3]
	row.DCIStatus = args[4]

	infoAsBytes, _ := json.Marshal(row)
	APIstub.PutState(args[0], infoAsBytes)

	var event = Event{Txid: APIstub.GetTxID(), Hash: args[0]}
	eventAsBytes, _ := json.Marshal(event)
	APIstub.SetEvent("DCI upload", eventAsBytes)
	return shim.Success([]byte("DCI upload success"))
}

func (s *smartcontract) expiredDCI(APIstub shim.ChaincodeStubInterface, args []string) sc.Response {
	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2")
	}
	rowAsBytes, _ := APIstub.GetState(args[0])
	row := Info{}
	json.Unmarshal(rowAsBytes, &row)
	row.DCIStatus = args[1]

	infoAsBytes, _ := json.Marshal(row)
	APIstub.PutState(args[0], infoAsBytes)

	var event = Event{Txid: APIstub.GetTxID(), Hash: args[0]}
	eventAsBytes, _ := json.Marshal(event)
	APIstub.SetEvent("expired DCI upload", eventAsBytes)
	return shim.Success([]byte("DCI expired upload success"))
}

func main() {

	// Create a new Smart Contract
	err := shim.Start(new(smartcontract))
	if err != nil {
		fmt.Printf("Error creating new Smart Contract: %s", err)
	}
}
