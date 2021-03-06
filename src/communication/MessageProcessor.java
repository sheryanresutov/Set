/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package communication;

import GameBackEnd.*;
import static communication.SetServer.SocketList;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Skorpion
 */
public class MessageProcessor implements Runnable {

    @Override
    public void run() {
        String message;
        while(true){
            try {
                message = SetServer.bqueue.take();
                try {
                    processMessage(message);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } catch (InterruptedException ex) {
                    ex.printStackTrace();
            }
        }
    }
    public static void processMessage(String message) throws Exception {
         
        switch(message.charAt(0)){
            case 'S': 
                {
                    int uid, addr;
                    String username, password;
                    int result;
                    Socket X;
                    String[] data = message.substring(1,message.length()).split("`");
                    username = data[0];
                    password = data[1];
                    addr = Integer.parseInt(data[2]);
                    X =  SetServer.waitingSockets.get(addr);
                    result = GameLobby.enterLobby(username,password,false);
                    if(result >1){
                        SocketList.put(result, X);
                        SetServer.waitingSockets.remove(addr);
                        PrintWriter OUT=new PrintWriter(X.getOutputStream());
                        OUT.println("V"+result);
                        OUT.flush();
                        OUT.println(GameLobby.returnGames()); 
                        OUT.flush();
                        OUT.println(GameLobby.returnPlayers()); 
                        OUT.flush();
                        for(Map.Entry<Integer,Player> entry1 : GameLobby.playerCollection.entrySet()){
                            uid = entry1.getKey();
                            Socket Temp_Sock = SocketList.get(uid);
                            PrintWriter OUT2=new PrintWriter(Temp_Sock.getOutputStream());
                            OUT2.println(GameLobby.returnPlayers());
                            OUT2.flush();
                        }
                    }
                    else{
                        PrintWriter OUT=new PrintWriter(X.getOutputStream());
                        if(result == -1){
                            OUT.println("E2");
                        }
                        else{
                            OUT.println("I");
                        }
                        OUT.flush();
                        SetServer.waitingSockets.remove(addr);
                        X.close();
                    }
                    break;
                }
            case 'R':
            {       
                    int uid, addr;
                    String username, password;
                    int result;
                    Socket X;
                    String[] data = message.substring(1,message.length()).split("`");
                    username = data[0];
                    password = data[1];
                    addr = Integer.parseInt(data[2]);
                    X =  SetServer.waitingSockets.get(addr);
                    SetServer.waitingSockets.remove(addr);
                    result = GameLobby.enterLobby(username,password,true);
                    PrintWriter OUT=new PrintWriter(X.getOutputStream());
                    if(result == -1){
                        OUT.println("E1");
                        OUT.flush();
                        break;
                    }
                    SocketList.put(result, X);
                    OUT.println("V"+result);
                    OUT.flush();
                    OUT.println(GameLobby.returnGames()); 
                    OUT.flush();
                    OUT.println(GameLobby.returnPlayers()); 
                    OUT.flush();
                    for(Map.Entry<Integer,Player> entry1 : GameLobby.playerCollection.entrySet()){
                        uid = entry1.getKey();
                        Socket Temp_Sock = SocketList.get(uid);
                        PrintWriter OUT2=new PrintWriter(Temp_Sock.getOutputStream());
                        OUT2.println(GameLobby.returnPlayers());
                        OUT2.flush();
                    }
                    break;
            }
            case 'C':
                {   
                    String[] data = message.substring(1,message.length()).split("`");
                    String uid_s = data[0];
                    String gameName = data[1];
                    int uid =Integer.parseInt(uid_s);
                    Game game = GameLobby.createGame(uid);
                    game.name = gameName;
                    sendMessage(uid,game.board.returnCardsOnBoard());
                    sendMessage(uid,game.returnScoreBoard());                    
                    for(Map.Entry<Integer,Player> entry : GameLobby.playerCollection.entrySet()){
                        sendMessage(entry.getKey(),GameLobby.returnGames());
                    }
                    break;
                }

            case 'J': 
                {
                    String[] data = message.substring(1,message.length()).split("`");
                    int gid = Integer.parseInt(data[0]);
                    int uid = Integer.parseInt(data[1]);
                    GameLobby.joinGame(uid, gid);
                    Game game = GameLobby.findGame(gid);
                    if(game!=null){
                        
                        sendMessage(uid,game.board.returnCardsOnBoard());
                        for(Map.Entry<Integer,Player> entry : game.playerCollection.entrySet()){
                            if(entry.getKey() == uid)
                                sendMessage(entry.getKey(),game.returnScoreBoard());
                            else{
                                sendMessage(entry.getKey(),game.addPlayerToSB(uid));
                            }
                        }
                        for(Map.Entry<Integer,Player> entry : GameLobby.playerCollection.entrySet()){
                            sendMessage(entry.getKey(),GameLobby.returnGames());
                        }
                        if(game.lockOwner()!=0)
                           sendMessage(uid,game.block(0)); 
                    }
                    break;
                }

            case 'D':
                {       
                    String[] data = message.substring(1,message.length()).split("`");
                    int gid = Integer.parseInt(data[0]);
                    int uid = Integer.parseInt(data[1]);
                    Game game = GameLobby.findGame(gid);
                    GameLobby.db.updateUserScore(uid, game.findPlayer(uid).returnScore());
                    game.leave(uid);
                    for(Map.Entry<Integer,Player> entry : game.playerCollection.entrySet()){
                        sendMessage(entry.getKey(),game.dropPlayerFromSB(uid));
                    }
                    for(Map.Entry<Integer,Player> entry : GameLobby.playerCollection.entrySet()){
                        sendMessage(entry.getKey(),GameLobby.returnGames());
                        sendMessage(entry.getKey(),GameLobby.returnPlayers());
                    }
                    break;
                }
            
            case 'A':{
                String[] data = message.substring(1,message.length()).split("`");
                int gid = Integer.parseInt(data[0]);
                int uid = Integer.parseInt(data[1]);
                Game game = GameLobby.findGame(gid);
                sendMessage(uid,game.returnScoreBoard());    
                break;
                }

            case 'B':
                {      
                    String[] data = message.substring(1,message.length()).split("`");
                    int gid = Integer.parseInt(data[0]);
                    int uid = Integer.parseInt(data[1]);
                    int time = 5;
                    Game game = GameLobby.findGame(gid);
                    if(game.lockOwner()==0)
                        game.setLock(uid);
                    else
                        break;
                    for(Map.Entry<Integer,Player> entry : game.playerCollection.entrySet()){
                        if(entry.getKey()!=uid){
                            sendMessage(entry.getKey(),game.block(time));
                        }
                        else{
                            sendMessage(uid,"A"+gid+"`"+uid+"`"+time);
                        }
                    }
                    break;
                }

            case 'F': 
                {        
                    String[] data = message.substring(1,message.length()).split("`");
                    int gid = Integer.parseInt(data[0]);
                    int uid = Integer.parseInt(data[1]);
                    Game game = GameLobby.findGame(gid);
                    if(uid==game.lockOwner()){
                        Player player = GameLobby.findPlayer(uid);
                        if(player!=null){
                            player.decScore();
                            game.resetLock();
                            for(Map.Entry<Integer,Player> entry : game.playerCollection.entrySet()){
                                sendMessage(entry.getKey(),game.returnScore(uid,player.returnScore()));
                                sendMessage(entry.getKey(),game.unblock());
                            }
                        }
                        else{
                            GameLobby.db.updateUserScore(uid, -1);
                            game.resetLock();
                            for(Map.Entry<Integer,Player> entry : game.playerCollection.entrySet()){
                                sendMessage(entry.getKey(),game.unblock());
                            }
                        }
                    }
                    break;
                }

            case 'P': 
                {
                    String new_message;
                    String[] data = message.substring(1,message.length()).split("`");
                    String [] cmds = {""};
                    int gid = Integer.parseInt(data[0]);
                    int uid = Integer.parseInt(data[1]);
                    int c1 = Integer.parseInt(data[2]);
                    int c2 = Integer.parseInt(data[3]);
                    int c3 = Integer.parseInt(data[4]);
                    Game game = GameLobby.findGame(gid);
                    if(uid == game.lockOwner()){
                        new_message = game.onSubmit(uid, c1, c2, c3);
                        if(new_message.contains("~"))
                            cmds = new_message.split("~");
                        else{
                            cmds[0] = new_message;
                        }
                        for(Map.Entry<Integer,Player> entry : game.playerCollection.entrySet()){
                            for (String cmd : cmds) {
                                sendMessage(entry.getKey(),cmd);
                            }
                        }
                        game.resetLock();
                        for(Map.Entry<Integer,Player> entry : game.playerCollection.entrySet()){
                            sendMessage(entry.getKey(),game.unblock());
                        }
                    }
                    break;
                }

            case 'M': 
                {   int uid,gid;
                    String actual_msg;
                    if(message.substring(1,2).equals("`")){
                        String[] data = message.substring(2,message.length()).split("`");
                        gid = Integer.parseInt(data[0]);
                        uid = Integer.parseInt(data[1]);
                        actual_msg = data[2];
                        Player player = GameLobby.findPlayer(uid);
                        Game game = GameLobby.findGame(gid);
                        for(Map.Entry<Integer,Player> entry : game.playerCollection.entrySet()){
                            sendMessage(entry.getKey(),"M`"+gid+'`'+uid +'`'+player.Username+'`'+ actual_msg);
                        }
                    }                       
                    else{
                        String[] data = message.substring(1,message.length()).split("`");
                        uid = Integer.parseInt(data[0]);
                        actual_msg = data[1];
                        Player player = GameLobby.findPlayer(uid);
                        for(Map.Entry<Integer,Player> entry : GameLobby.playerCollection.entrySet()){
                            sendMessage(entry.getKey(),"M"+uid +'`'+player.Username+'`'+ actual_msg);
                        }
                    }
                    break;
                }
            case 'L': 
                {
                    int uid;
                    uid = Integer.parseInt(message.substring(1,message.length())); 
                    sendMessage(uid,GameLobby.db.returnRankings());
                    break;
                }
            case 'K':
            {
                int uid = Integer.parseInt(message.substring(1,message.length()));
                GameLobby.delPlayer(uid);
                sendMessage(uid,"L");
                Socket sock = SetServer.SocketList.get(uid);
                sock.close();
                SetServer.SocketList.remove(uid);
                for(Map.Entry<Integer,Player> entry : GameLobby.playerCollection.entrySet()){
                    sendMessage(entry.getKey(),GameLobby.returnPlayers());
                }
                break;
            
            }
            case 'E':
            {
                int uid;
                uid = Integer.parseInt(message.substring(1,message.length()));
                sendMessage(uid,GameLobby.returnPlayers());
                break;
            }
            case 'G':
            {
                int uid;
                uid = Integer.parseInt(message.substring(1,message.length())); 
                sendMessage(uid,GameLobby.returnGames());
                break;
            }
            case 'T':{
                String[] data = message.substring(1,message.length()).split("`");
                int gid = Integer.parseInt(data[0]);
                int uid = Integer.parseInt(data[1]);
                String cards="";
                for(int i=2;i<data.length;i++){
                    cards = cards +data[i]+"`";
                }
                Game game = GameLobby.findGame(gid);
                for(Map.Entry<Integer,Player> entry: game.playerCollection.entrySet()){
                    if(uid != entry.getKey())
                        sendMessage(entry.getKey(),"T"+gid+"`"+uid+"`"+cards);
                }
                break;
            }
            default:{
                System.err.println("Error: Received message is not in protocol: " + message);                
            }
        }
    }
    
    private static void sendMessage(int uid, String message) throws IOException{
            Socket sock = SetServer.SocketList.get(uid);
            PrintWriter TEMP_OUT=new PrintWriter(sock.getOutputStream());
            TEMP_OUT.println(message);
            TEMP_OUT.flush();
    }    
}
