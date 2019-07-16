package main

import (
	"github.com/hyperledger/fabric/core/chaincode/shim"
	sc "github.com/hyperledger/fabric/protos/peer"
	)

// Define the Smart Contract structure
type CRM struct {
}

type Table struct {
	CopyrightOwner       string
	Title                string
	Type                 string
	RequestTime          string
	CreationCompleteTime string
	FirstPublishTime     string
	RequestUserId        string
	Fingerprint          string
	DownloadUrl          string
	RequestHash          string
	SigAlgorithm         string

	DCI             string
	DCIHash         string
	DCISigAlgorithm string
	DCIStatus       string
}

type Event struct {
	Txid string
	Hash string
}

/*
 * The Init method is called when the Smart Contract "fabcar" is instantiated by the blockchain network
 * Best practice is to have any Ledger initialization in separate function -- see initLedger()
 */
func (s *CRM) Init(APIstub shim.ChaincodeStubInterface) sc.Response {
	return shim.Success(nil)
}

/*
 * The Invoke method is called as a result of an application request to run the Smart Contract "fabcar"
 * The calling application program has also specified the particular smart contract function to be called, with arguments
 */
func (s *CRM) Invoke(APIstub shim.ChaincodeStubInterface) sc.Response {

	// Retrieve the requested Smart Contract function and arguments
	function, args := APIstub.GetFunctionAndParameters()
	// Route to the appropriate handler function to interact with the ledger appropriately
	if function == "uploadPreRegister" {
		return s.uploadPreRegister(APIstub, args)
	} else if function == "uploadDCI" {
		return s.uploadDCI(APIstub, args)
	} else if function == "expiredDCI" {
		return s.expiredDCI(APIstub, args)
	} else if function == "queryPreRegister" {
		return s.queryPreRegister(APIstub, args)
	} else if function == "queryDCI" {
		return s.queryDCI(APIstub, args)
	}

	return shim.Error("Invalid Smart Contract function name.")
}

func (s *CRM) uploadPreRegister(stubInterface shim.ChaincodeStubInterface, args []string) sc.Response {

	return shim.Success(nil)
}

func (s *CRM) uploadDCI(stubInterface shim.ChaincodeStubInterface, args []string) sc.Response {
	return shim.Success(nil)
}

func (s *CRM) expiredDCI(stubInterface shim.ChaincodeStubInterface, args []string) sc.Response {
	return shim.Success(nil)
}

func (s *CRM) queryPreRegister(stubInterface shim.ChaincodeStubInterface, args []string) sc.Response {
	return shim.Success(nil)
}

func (s *CRM) queryDCI(stubInterface shim.ChaincodeStubInterface, args []string) sc.Response {
	return shim.Success(nil)
}