import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bloo
 *
 */
public abstract class UDPConnection {
	protected boolean verbose;
	
	protected static Map<Byte, String> RequestTypes;
	static {
		RequestTypes = new HashMap<>();
		// RequestTypes.put( (byte) 0, "null"); Perhaps use as a shutdown request?
		RequestTypes.put((byte) 1, "RRQ");
		RequestTypes.put((byte) 2, "WRQ");
		RequestTypes.put((byte) 3, "DATA");
		RequestTypes.put((byte) 4, "ACK");
		RequestTypes.put((byte) 5, "ERROR");
	}
	
	/**
	 * @author bloo
	 * Base Send method
	 * 
	 * Sends byte[] msg to the returnAddress
	 * 
	 * @param msg
	 * @param returnAddress
	 */
	protected void send(byte[] msg, SocketAddress returnAddress) {
		DatagramSocket socket;
		DatagramPacket sendPacket;
		try {
			socket = new DatagramSocket();

			sendPacket = new DatagramPacket(msg, msg.length, returnAddress);
			
			if(verbose) {
				System.out.println("Sending:");
				System.out.println(packetToString(sendPacket));
			}

			socket.send(sendPacket);
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @author bloo
	 * Sends msg to return address over the given socket
	 * 
	 * @param msg - byte array to be sent 
	 * @param sendSocket - socket that will be used to send packet
	 * @param returnAddress - address the packet will be send too
	 */
	protected void send(byte[] msg, DatagramSocket sendSocket, SocketAddress returnAddress) {
		DatagramPacket sendPacket;
		try {

			sendPacket = new DatagramPacket(msg, msg.length, returnAddress);

			if(verbose) {
				System.out.println("Sending:");
				System.out.println(packetToString(sendPacket));
			}

			sendSocket.send(sendPacket);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	

	/**
	 * @author bloo
	 * creates a packet to be sent by base send() method
	 * 
	 * @param msg - byte array to be sent 
	 * @param socket - socket that will be used to send packet
	 * @param address -  address the packet will be send too
	 * @param port -  port the packet will be send too
	 */
	protected void send(byte[] msg, DatagramSocket socket, InetAddress address, int port) {
		DatagramPacket sendPacket = new DatagramPacket(msg, msg.length, address, port);
		send(socket, sendPacket);
	}

	/**
	 * @author bloo
	 * Base send method, used to send sendPacket over given socket
	 * 
	 * @param socket - socket the packet will sent too
	 * @param sendPacket - packet to be sent
	 */
	protected void send(DatagramSocket socket, DatagramPacket sendPacket) {
		try {
			if (verbose) {
				System.out.println("Sending: ");
				System.out.println(packetToString(sendPacket));
			}

			socket.send(sendPacket);

		} catch (IOException e) {
			socket.close();
			socket.close();
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * @author bloo
	 * Base receive method
	 * 
	 * Receives a DatagramPacket over the given socket
	 * 
	 * @param socket - socket packet will be received at
	 * @return receivedPacket unless there is an exception trying to receive
	 */
	DatagramPacket receive(DatagramSocket socket) {
		return receive(socket, 516);
	}

	/**
	 * @author bloo
	 * Base receive method
	 * 
	 * Receives a DatagramPacket over the given socket
	 * 
	 * @param socket - socket to receive from
	 * @param length - size of potential packet
	 * @return receivePacket unless there is an exception trying to receive
	 */
	DatagramPacket receive(DatagramSocket socket, int length) {
		DatagramPacket receivedPacket;

		byte[] msg;
		receivedPacket = new DatagramPacket(new byte[length], length);

		try {
			socket.receive(receivedPacket);
			msg = receivedPacket.getData();

			if(verbose) {
				System.out.println("Received:");
				System.out.println(packetToString(receivedPacket));
			}
			return receivedPacket;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return new DatagramPacket(new byte[0], 0);
	}

	
	/**
	 * @author bloo
	 * Reads bytes from a byte array at the start index into a string
	 * 
	 * @param index - statring index of the data
	 * @param packet - byte array of packet data
	 * @param dataLength - the number of bytes of data
	 * @return resulting String of data
	 */
	protected String readBytes(int offset, byte[] packet, int dataLength) {
		String data = "";
		int index = offset;
		while (index < dataLength && packet[index] != 0) {
			data += (char) packet[index++];
		}
		return data;
	}
	
	/**
	 * @author bloo
	 * Parses a tftp packet in byte form and returns info
	 * 
	 * @param packet - packet to convert
	 * @return contents of a packet
	 */
	protected String packetToString(DatagramPacket packet) {
		String descriptor = "";
		String msg;
		byte[] data = packet.getData();
		int index = 2;
		int blockNum;
		if (data.length > 0) {
			if (data[0] == 0) {
				descriptor += "Type: ";
				if (data[1] == 1) {
					descriptor += "RRQ\nFile name: ";

					msg = readBytes(index, data, packet.getLength()); // read filename from the packets
					descriptor += msg + "\nMode: "; // add the name to the packet descriptor
					index += msg.length() + 1; // update the index

					msg = readBytes(index, data, packet.getLength());
					descriptor += msg + "\n";
					index += msg.length() + 1;

				} else if (data[1] == 2) {
					descriptor += "WRQ\nFile name: ";
					msg = readBytes(index, data, packet.getLength());
					descriptor += msg + "\nMode: ";
					index += msg.length() + 1;

					msg = readBytes(index, data, packet.getLength());
					descriptor += msg + "\n";
					index += msg.length() + 1;
				} else if (data[1] == 3) {
					descriptor += "DATA\nBlock #: ";
					blockNum = data[2] * 256 + data[1]; // convert 2 byte number to decimal
					descriptor += blockNum + "\nBytes of data: " + readBytes(4, data, packet.getLength()).length()
							+ "\n"; // add block number to
					// descriptor as
					// well as read the
					// number of bytes
					// in data
				} else if (data[1] == 4) {
					descriptor += "ACK\nBlock #: ";
					blockNum = data[2] * 256 + data[1];
					descriptor += blockNum + "\n";
				} else if (data[1] == 5) {
					descriptor += "ERROR\nError Code";
					blockNum = data[2] * 256 + data[1];
					descriptor += blockNum + "ErrMsg: " + readBytes(4, data, packet.getLength()) + "\n";
				}
			}

			return descriptor;
		}

		return null;
	}
}
