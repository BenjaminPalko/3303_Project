package tftpConnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class TFTPPacket {
    protected static final byte[] MODE_OCTET = "octet".getBytes();
    protected static final byte[] MODE_NETASCII = "netascii".getBytes();

    public static final byte OP_RRQ = 1;
    public static final byte OP_WRQ = 2;
    public static final byte OP_DATA = 3;
    public static final byte OP_ACK = 4;
    public static final byte OP_ERROR = 5;

    protected static final byte ZERO_BYTE = 0;

    protected static Map<Byte, String> PacketTypes;
    static {
	PacketTypes = new HashMap<>();
	// RequestTypes.put( (byte) 0, "null"); Perhaps use as a shutdown request?
	PacketTypes.put((byte) OP_RRQ, "RRQ");
	PacketTypes.put((byte) OP_WRQ, "WRQ");
	PacketTypes.put((byte) OP_DATA, "DATA");
	PacketTypes.put((byte) OP_ACK, "ACK");
	PacketTypes.put((byte) OP_ERROR, "ERROR");
    }

    /**
     * creates request packet adding timeout to this method when creating request
     * packet. timeout needs to be agreed by both Client and Server.
     * 
     * @param opCode
     *            - either 1 or 2 for read or write request
     * @param file
     *            - the name of the file the server will be operating on
     * @param mode
     *            - the mode in which the data will be handeled
     * @return the packet in the form of a byte array
     * @author Eric
     */
    public static byte[] createRQ(byte opCode, byte[] file, byte[] mode) {
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	try {
	    outputStream.write(ZERO_BYTE);
	    outputStream.write(opCode);
	    outputStream.write(file);
	    outputStream.write(ZERO_BYTE);
	    outputStream.write(mode);
	    outputStream.write(ZERO_BYTE);

	} catch (IOException e1) {
	    e1.printStackTrace();
	}
	return outputStream.toByteArray();
    }

    /**
     * creates acknowledge packet
     * 
     * @param blockNum
     *            - the current number the packet is acknowledging
     * @return byte array with acknowledge data
     * @author bloo
     */
    public static byte[] createAck(int blockNum) {
	byte[] ack = new byte[] { 0, OP_ACK, (byte) (blockNum / 256), (byte) (blockNum % 256) };
	return ack;
    }

    /**
     * Creates Data packet
     * 
     * @param blockNum
     *            - the block num of the blcok being sent
     * @param data
     *            - the data being sent
     * @return packet in the form of a byte array
     * @author BLoo
     */
    public static byte[] createData(int blockNum, byte[] data) {
	byte[] packet = new byte[4 + data.length];
	ByteBuffer dBuff = ByteBuffer.wrap(packet);
	dBuff.put(new byte[] { 0, OP_DATA, (byte) (blockNum / 256), (byte) (blockNum % 256) });
	dBuff.put(data);
	return packet;
    }

    /**
     * Creates error packet
     * 
     * @param error
     *            - the corresponding error number
     * @param data
     *            - the data being sent
     * @return packet in the form of a byte array
     * @author BLoo
     */
    public static byte[] createError(int error, byte[] msg) {
	byte[] packet = new byte[5 + msg.length];
	ByteBuffer dBuff = ByteBuffer.wrap(packet);

	dBuff.put(new byte[] { 0, OP_ERROR, (byte) (error / 256), (byte) (error % 256) });
	dBuff.put(msg);
	dBuff.put((byte) 0);
	return packet;
    }

    /**
     * Reads bytes from a byte array to terminating zero or to the end of the
     * available data.
     * 
     * @param index
     *            - Starting index of the data
     * @param packet
     *            - byte array of packet data
     * @param dataLength
     *            - the number of bytes of data
     * @return resulting String of data
     * 
     * @author bloo
     */
    static byte[] readToStop(int offset, byte[] packet, int dataLength) {
	byte[] data = new byte[512];
	int index;

	/*
	 * Iterate, starting at the given offset as long as a terminating zero or the
	 * end of the array hasn't been reached
	 */
	for (index = offset; index < dataLength && packet[index] != 0; index++) {
	    data[index - offset] = packet[index];
	}
	return Arrays.copyOf(data, index - offset);
    }

    /**
     * Authenticates packet
     * 
     * @author Benjamin, BLoo
     * @param packet
     * @throws IOException
     */
    public static void checkPacket(DatagramPacket packet) throws IOException {
	byte[] data = packet.getData();
	int size = packet.getLength();
	byte zero = 0;
	Set<Byte> validPackets = PacketTypes.keySet();
	if (data[0] == zero && validPackets.contains(data[1])) // Checks packet type formatting
	{
	    switch (data[1]) {
	    case (byte) 1:
	    case (byte) 2: /* RRQ & WRQ Packet */
	    {
		if (data[size + 1] == zero)
		    if (data[-1] == zero) {
			System.out.println("Packet Authenticated\n");
			return;
		    }
		break;
	    }
	    case (byte) 3: /* DATA Packet */
	    {
		if (data[2] == zero && data[3] == zero)
		    break;
		return;
	    }
	    case (byte) 4: /* ACK Packet */
	    {
		if (data[2] == zero && data[3] == zero)
		    break;
		return;
	    }
	    case (byte) 5: /* ERROR Packet */
	    {
		if (data[2] == zero && data[3] == zero)
		    break;
		return;
	    }
	    }
	}
	throw new IOException();
    }

    /**
     * Converts byte[] to string using utf-8 encoding when available uses default
     * when utf-8 isn't available
     * 
     * @param data
     * @return decoded Sring
     */
    private static String bytesToString(byte[] data) {
	try {
	    return new String(data, "UTF-8");
	} catch (UnsupportedEncodingException e) {
	    e.printStackTrace();
	    return new String(data);
	}
    }

    /**
     * Parses a tftp packet in byte form and returns relevant information
     * 
     * @param packet
     *            - packet to convert
     * @return contents of a packet
     * @author bloo
     */
    public static String toString(DatagramPacket packet) {
	String descriptor = "";
	byte[] data = packet.getData();
	if (data.length > 0) {
	    switch (getType(packet)) {
	    case OP_RRQ:
		descriptor += "RRQ\nFile name: " + getFileName(packet) + "\nMode: " + getMode(packet) + "\n";
		break;
	    case OP_WRQ:
		descriptor += "WRQ\nFile name: " + getFileName(packet) + "\nMode: " + getMode(packet) + "\n";
		break;
	    case OP_DATA:
		descriptor += "DATA\nBlock #: " + getBlockNum(packet) + "\nBytes of data: " + getDataLength(packet)
			+ "\n";
		break;
	    case OP_ACK:
		descriptor += "ACK\nBlock #: " + getBlockNum(packet) + "\n";
		break;
	    case OP_ERROR:
		descriptor += "ERROR\nError Num: " + getError(packet) + "\nError Msg: " + getErrorMsg(packet) + "\n";
		break;
	    default:
		break;

	    }
	    return descriptor;

	}

	return null;
    }

    /**
     * Retreive the data in a packet in the form of a string
     * 
     * @param packet
     *            - the packet the data will be extracted from
     * @return the data in the form of a string
     * @author BLoo
     */
    public static String getData(DatagramPacket packet) {
	String data = bytesToString(readToStop(4, packet.getData(), packet.getLength()));
	return data;
    }

    /**
     * Gets the data from a packet as a byte array
     * 
     * @param packet
     *            - where the data will be extracted from
     * @return the data the packet was holding
     */
    public static byte[] getByteData(DatagramPacket packet) {
	return readToStop(4, packet.getData(), packet.getLength());
    }

    /**
     * Parses a tftp packet in byte form and returns info
     * 
     * @param packet
     *            - where data will be extracted
     * @return contents of a packet
     * @author bloo
     */
    public static String getFileName(DatagramPacket packet) {
	return bytesToString(readToStop(2, packet.getData(), packet.getLength()));
    }

    /**
     * Gets tftp mode from request packet
     * 
     * @param packet
     *            - where data will be extracted
     * @return requested mode as a String
     */
    public static String getMode(DatagramPacket packet) {
	int offset = readToStop(2, packet.getData(), packet.getLength()).length + 3; // Find offset of mode field by
										     // reading the first field and
										     // adding that fields length,
										     // accounting for the
										     // terminating zero and initial
										     // offset, to the offset
	return bytesToString(readToStop(offset, packet.getData(), packet.getLength()));
    }

    /**
     * Gets the error number from error packet
     * 
     * @param packet
     *            - where data will be extracted
     * @return Error num
     */
    public static int getError(DatagramPacket packet) {
	return packet.getData()[3];
    }

    /**
     * Gets error msg from error packet
     * 
     * @param packet
     *            - where data will be extracted
     * @return Error message
     */
    public static String getErrorMsg(DatagramPacket packet) {
	return bytesToString(readToStop(4, packet.getData(), packet.getLength()));
    }

    /**
     * Gets block number from ack or data packets
     * 
     * @param packet
     *            - where data will be extracted
     * @return block number the packet is holding
     */
    public static int getBlockNum(DatagramPacket packet) {
	return Byte.toUnsignedInt(packet.getData()[2]) * 256 + Byte.toUnsignedInt(packet.getData()[3]);
    }

    /**
     * gets the type of packet
     * 
     * @param packet
     *            - where data will be extracted
     * @return the type of packet
     */
    public static int getType(DatagramPacket packet) {
	return packet.getData()[1];
    }

    /**
     * Gets the number of bytes in the data section of a data packet ONLY ACCEPTS
     * DATA PACKETS
     * 
     * @param packet
     *            - where the data will be extracted
     * @return number of bytes in the packets data section
     */
    public static int getDataLength(DatagramPacket packet) {
	return readToStop(4, packet.getData(), packet.getLength()).length;
    }

}
