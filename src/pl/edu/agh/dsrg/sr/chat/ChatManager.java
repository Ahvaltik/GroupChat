package pl.edu.agh.dsrg.sr.chat;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.protocols.BARRIER;
import org.jgroups.protocols.FD_ALL;
import org.jgroups.protocols.FD_SOCK;
import org.jgroups.protocols.FRAG2;
import org.jgroups.protocols.MERGE2;
import org.jgroups.protocols.MFC;
import org.jgroups.protocols.PING;
import org.jgroups.protocols.UDP;
import org.jgroups.protocols.UFC;
import org.jgroups.protocols.UNICAST2;
import org.jgroups.protocols.VERIFY_SUSPECT;
import org.jgroups.protocols.pbcast.FLUSH;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;
import org.jgroups.stack.ProtocolStack;

import com.google.protobuf.InvalidProtocolBufferException;

import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatMessage;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.ActionType;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatState;

public class ChatManager implements Receiver {
	private JChannel chatManagementChannel;
	private Map<String,JChannel> chatMap;
	private Map<String,List<String>> userMap;
	private List<String> chatList;
	public String NICKNAME;
	
	public ChatManager(){
		chatMap = new HashMap<String,JChannel>();
		userMap = new HashMap<String, List<String>>();
		chatList = new LinkedList<String>();
	}
	
	public void start() throws Exception{
		chatManagementChannel = new JChannel(false);
		ProtocolStack stack = new ProtocolStack();
		chatManagementChannel.setProtocolStack(stack);
		
		stack.addProtocol(new UDP())
		.addProtocol(new PING())
		.addProtocol(new MERGE2())
		.addProtocol(new FD_SOCK())
		.addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
		.addProtocol(new VERIFY_SUSPECT())
		.addProtocol(new BARRIER())
		.addProtocol(new NAKACK())
		.addProtocol(new UNICAST2())
		.addProtocol(new STABLE())
		.addProtocol(new GMS())
		.addProtocol(new UFC())
		.addProtocol(new MFC())
		.addProtocol(new FRAG2())
		.addProtocol(new STATE_TRANSFER())
		.addProtocol(new FLUSH());
		stack.init();
		
		chatManagementChannel.connect("ChatManagement768264");
		
		chatManagementChannel.setReceiver(this);
	}
	
	public JChannel joinChannel(String channelName) throws Exception{
		byte[] bytesChatAction = ChatAction.newBuilder().
				setNickname(NICKNAME).
				setChannel(channelName).
				setAction(ActionType.JOIN).build().toByteArray();
		Message message = new Message();
		message.setBuffer(bytesChatAction);
		chatManagementChannel.send(message);
		
		//---------------------------------
		JChannel chatChannel = getChannel(channelName);
		add(NICKNAME,channelName);
		this.chatList.add(channelName);
		return chatChannel;
	}
	
	public void add(String user, String channelName) throws Exception{
		
		if(!this.userMap.containsKey(channelName))
			this.userMap.put(channelName, new LinkedList<String>());
		this.userMap.get(channelName).add(user);
		
		
		
	}
	
	public void remove(String user, String channelName) throws Exception{
		
		userMap.get(channelName).remove(user);
		removeIfEmptyChannel(channelName);
		
	}
	
	private JChannel getChannel(String channelName) throws Exception{
		
		if(this.chatMap.containsKey(channelName))
			return chatMap.get(channelName);
		
		JChannel chatChannel = new JChannel(false);
		ProtocolStack stack = new ProtocolStack();
		chatChannel.setProtocolStack(stack);
		
		stack.addProtocol(new UDP().setValue("mcast_group_addr", InetAddress.getByName(channelName)))
		.addProtocol(new PING())
		.addProtocol(new MERGE2())
		.addProtocol(new FD_SOCK())
		.addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
		.addProtocol(new VERIFY_SUSPECT())
		.addProtocol(new BARRIER())
		.addProtocol(new NAKACK())
		.addProtocol(new UNICAST2())
		.addProtocol(new STABLE())
		.addProtocol(new GMS())
		.addProtocol(new UFC())
		.addProtocol(new MFC())
		.addProtocol(new FRAG2())
		.addProtocol(new STATE_TRANSFER())
		.addProtocol(new FLUSH());
		stack.init();
		
		chatChannel.connect(channelName);
		
		chatMap.put(channelName, chatChannel);
		this.userMap.put(channelName, new LinkedList<String>());
		
		return chatChannel;
	}
	
	public void leaveChannel(String channelName) throws Exception{
		byte[] bytesChatAction = ChatAction.newBuilder().
				setNickname(NICKNAME).
				setChannel(channelName).
				setAction(ActionType.LEAVE).build().toByteArray();
		Message message = new Message();
		message.setBuffer(bytesChatAction);
		chatManagementChannel.send(message);
		chatMap.get(channelName).disconnect();
		chatList.remove(channelName);
		remove(NICKNAME,channelName);
	}
	
	private void removeIfEmptyChannel(String channelName){
		if(userMap.get(channelName).isEmpty()){
			chatMap.remove(channelName);
			userMap.remove(channelName);
		}
	}
	
	public void printState(){
		for(String channelName :userMap.keySet()){
			System.out.print(channelName + " : ");
			for(String nickName :userMap.get(channelName)){
				System.out.print(nickName + " ");
			}
			System.out.println();
		}
		for(String channelName :chatMap.keySet()){
			System.out.print(channelName + " ");
		}
		System.out.println();
	}

	@Override
	public void getState(OutputStream arg0) throws Exception {
		ChatState.Builder builder = ChatState.newBuilder();
		for(String channelName :userMap.keySet()){
			for(String nickName :userMap.get(channelName)){
				builder.addState(ChatAction.newBuilder().
						setNickname(nickName).
						setChannel(channelName).
						setAction(ActionType.JOIN).build());
			}
		}
		builder.build().writeTo(arg0);
		
	}

	@Override
	public void receive(Message arg0) {
		
		try {
			ChatAction chatAction = ChatAction.parseFrom(arg0.getBuffer());
			if(chatAction.getAction() == ActionType.JOIN){
				this.add(chatAction.getNickname(), chatAction.getChannel());
			} else if(chatAction.getAction() == ActionType.LEAVE) {
				this.remove(chatAction.getNickname(), chatAction.getChannel());
			}
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void setState(InputStream arg0) throws Exception {
		ChatState state = ChatState.parseFrom(arg0);
		for(ChatAction chatAction : state.getStateList()){
			if(chatAction.getAction() == ActionType.JOIN){
				this.add(chatAction.getNickname(), chatAction.getChannel());
			} else if(chatAction.getAction() == ActionType.LEAVE) {
				this.remove(chatAction.getNickname(), chatAction.getChannel());
			}
		}
		
	}

	@Override
	public void block() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void suspect(Address arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unblock() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void viewAccepted(View arg0) {
		List<String> visibleUsers = new LinkedList<String>();
		for(Address address : arg0.getMembers()) {
			visibleUsers.add(address.toString());
		}
		for(List<String> userLists : this.userMap.values()) {
			for(String user: userLists){
				if(!visibleUsers.contains(user)) {
					userLists.remove(user);
				}
			}
			
		}
		
	}

	public List<JChannel> getChannelList() {
		List<JChannel> result = new LinkedList<JChannel>();
		for(String channelName : chatList){
			result.add(chatMap.get(channelName));
		}
		return result;
	}

	public void send(String readLine) {
		for(String chatName: chatList)
			try {
				chatMap.get(chatName).send(null, ChatMessage.newBuilder().setMessage(NICKNAME + ":" +readLine).build().toByteArray());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
}
