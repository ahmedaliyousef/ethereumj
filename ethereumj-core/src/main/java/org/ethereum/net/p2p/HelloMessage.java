package org.ethereum.net.p2p;

import static org.ethereum.net.p2p.P2pMessageCodes.HELLO;
import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;

import org.ethereum.net.client.Capability;
import org.ethereum.net.p2p.P2pMessage;
import org.ethereum.util.*;
import org.spongycastle.util.encoders.Hex;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an Ethereum HelloMessage on the network
 * 
 * @see org.ethereum.net.p2p.P2pMessageCodes#HELLO
 */
public class HelloMessage extends P2pMessage {

	/** The implemented version of the P2P protocol. */
	private byte p2pVersion;
	/** The underlying client. A user-readable string. */
	private String clientId;
	/** A peer-network capability code, readable ASCII and 3 letters. 
	 * Currently only "eth", "shh" and "bzz" are known. */
	private List<Capability> capabilities;
	/** The port on which the peer is listening for an incoming connection */
	private int listenPort;
	/** The identity and public key of the peer */
	private String peerId;

	public HelloMessage(byte[] encoded) {
		super(encoded);
	}

	public HelloMessage(byte p2pVersion, String clientId,
			List<Capability> capabilities, int listenPort, String peerId) {
		this.p2pVersion = p2pVersion;
		this.clientId = clientId;
		this.capabilities = capabilities;
		this.listenPort = listenPort;
		this.peerId = peerId;
		this.parsed = true;
	}

	private void parse() {
		RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);
		// TODO: find out if it can be 0x00. Do we need to check for this?
		// The message does not distinguish between 0 and null,
		// so we check command code for null.

		byte[] p2pVersionBytes = ((RLPItem) paramsList.get(1)).getRLPData();
		this.p2pVersion = p2pVersionBytes != null ? p2pVersionBytes[0] : 0;

		byte[] clientIdBytes = ((RLPItem) paramsList.get(2)).getRLPData();
		this.clientId = new String(clientIdBytes != null ? clientIdBytes : EMPTY_BYTE_ARRAY);

		RLPList capabilityList = (RLPList) paramsList.get(3);
		this.capabilities = new ArrayList<>();
		for (int i = 0; i < capabilityList.size(); i++) {

            RLPElement capId = ((RLPList)capabilityList.get(i)).get(0);
            RLPElement capVersion = ((RLPList)capabilityList.get(i)).get(1);
            
            String name = new String(capId.getRLPData());
            byte version = capVersion.getRLPData() == null ? 0 : capVersion.getRLPData()[0];
            
            Capability cap = new Capability(name, version);
			this.capabilities.add(cap);
		}

		byte[] peerPortBytes = ((RLPItem) paramsList.get(4)).getRLPData();
		this.listenPort = ByteUtil.byteArrayToInt(peerPortBytes);

		byte[] peerIdBytes = ((RLPItem) paramsList.get(5)).getRLPData();
		this.peerId = Hex.toHexString(peerIdBytes);
		this.parsed = true;
	}

	private void encode() {
		byte[] command = RLP.encodeByte(HELLO.asByte());
		byte[] p2pVersion = RLP.encodeByte(this.p2pVersion);
		byte[] clientId = RLP.encodeString(this.clientId);
		byte[][] capabilities = new byte[this.capabilities.size()][];
		for (int i = 0; i < this.capabilities.size(); i++) {
			Capability capability = this.capabilities.get(i);
			capabilities[i] = RLP.encodeList(
                    RLP.encodeElement(capability.getName().getBytes()),
                    RLP.encodeElement(new byte[] {capability.getVersion() }));
		}
		byte[] capabilityList = RLP.encodeList(capabilities);
		byte[] peerPort = RLP.encodeInt(this.listenPort);
		byte[] peerId = RLP.encodeElement(Hex.decode(this.peerId));

		this.encoded = RLP.encodeList(command, p2pVersion, clientId,
				capabilityList, peerPort, peerId);
	}

	@Override
	public byte[] getEncoded() {
		if (encoded == null) encode();
		return encoded;
	}

	public byte getP2PVersion() {
		if (!parsed) parse();
		return p2pVersion;
	}

	public String getClientId() {
		if (!parsed) parse();
		return clientId;
	}

	public List<Capability> getCapabilities() {
		if (!parsed) parse();
		return capabilities;
	}

	public int getListenPort() {
		if (!parsed) parse();
		return listenPort;
	}

	public String getPeerId() {
		if (!parsed) parse();
		return peerId;
	}

    @Override
    public P2pMessageCodes getCommand(){
        return P2pMessageCodes.HELLO;
    }


	@Override
	public Class<?> getAnswerMessage() {
		return null;
	}

	public String toString() {
		if (!parsed) parse();
		return "[" + this.getCommand().name() + " p2pVersion="
				+ this.p2pVersion + " clientId=" + this.clientId
				+ " capabilities=[" + Joiner.on(" ").join(this.capabilities)
				+ "]" + " peerPort=" + this.listenPort + " peerId="
				+ this.peerId + "]";
	}
}