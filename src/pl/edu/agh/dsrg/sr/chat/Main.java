package pl.edu.agh.dsrg.sr.chat;

import java.util.Scanner;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatMessage;

import com.google.protobuf.InvalidProtocolBufferException;


public class Main extends ReceiverAdapter {

	private static Scanner commandLineScanner;

	public static void main(String[] args) {
		Main main = new Main();
		ChatManager manager = new ChatManager();
		try {
			manager.start();
		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
		System.out.println("Nickname : ");
		commandLineScanner = new Scanner(System.in);
		manager.NICKNAME = commandLineScanner.nextLine();
		boolean stopped = false;
		while(!stopped ){
			System.out.println("connect [ip]\ndisconnect [ip]\nquit\nshow");
			String readLine = commandLineScanner.nextLine();
			if(readLine.startsWith("connect")){
				try {
					JChannel chatChannel = manager.joinChannel(readLine.substring(8));
					chatChannel.setReceiver(main);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}else if(readLine.startsWith("disconnect")){
				try {
					manager.leaveChannel(readLine.substring(11));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}else if(readLine.startsWith("quit")){
				stopped = true;
			}else if(readLine.startsWith("show")){
				manager.printState();
			}else{
				manager.send(readLine);
			}
		}
		
		
	}

	@Override
	public void receive(Message arg0) {
		try {
			ChatMessage chatMessage = ChatMessage.parseFrom(arg0.getBuffer());
			System.out.println(arg0.getSrc().toString() + " " + chatMessage.getMessage());
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
		
	}

}
