/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GameBackEnd;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Skorpion
 */
public class GameLobby {
    public static HashMap <Integer,Game> gameCollection = new HashMap<>();
    public static HashMap <Integer,Player> playerCollection = new HashMap<>();
    public static DB db = new DB();
    
    
    //pretty much done
    public static Game createGame(int uid){
        int tempID;
        Game game = new Game();
        Player player = findPlayer(uid);
        game.join(uid, player);
        tempID = game.returnGid();
        gameCollection.put(tempID,game);
        return game;
    }
    
    //pretty much done
    public static void joinGame(int uid, int gid){
        Game game = findGame(gid);
        Player player = findPlayer(uid);
        game.join(uid,player);      
    }
    
    //pretty much done
    //looks through gameCollection and forms a message with all the new games and the number of players and their IDs
    //drops games that have 0 players
    public static String returnGames(){
        String message="";
        int numOfPlayers;
        int gid;
        //form message
        for(Map.Entry<Integer,Game> entry : gameCollection.entrySet()){
            numOfPlayers  = entry.getValue().playerNum();
            gid = entry.getKey();
            if(numOfPlayers>0){
                message = "G" + Integer.toString(gid) + "NP" + Integer.toString(numOfPlayers) + "-" + message;
            }
            else{
                //remove the game from the hashmap
            }
        }
        return message;
    }
    
    
    public static String returnPlayers(){
        String message = "";
        String username;
        int uid;
        //form message
        for(Map.Entry<Integer,Player> entry : playerCollection.entrySet()){
            username  = entry.getValue().Username;
            uid = entry.getKey();
            message = "P" + Integer.toString(uid) + "UN" + username + "-" + message;
        }
        return message;
    }
    
    //pretty much done
    //locates a certain game given a gid, otherwise returns an error
    public static Game findGame(int gid){
        if(gameCollection.containsKey(gid))
            return gameCollection.get(gid);
        else{
            System.err.println("error in gameLobby.findGame with gid: "+gid);
            System.exit(-1);
            return null;
        }
    }
    
    //pretty much done
    public static Player findPlayer(int uid){
        if(playerCollection.containsKey(uid))
            return playerCollection.get(uid);
        else{
            System.err.println("error in gameLobby.findPlayer with uid: "+uid);
            System.exit(-1);
            return null;
        }
    }
    
    public static void delPlayer(int uid){
        if (playerCollection.containsKey(uid)){
            playerCollection.remove(uid);
        }
    }
    
    //pretty much done, need DB.java methods however
    //after a socket connects, the first thing that must happen is either sign in or register
    public static int enterLobby(String Username, String Password, boolean newUser) throws Exception{
        int uid;
        int result;
        //input into Database, receive a uid, insert the uid/socket combo into the socketList
        //mark user a valid user and continue to GameLobby interface
        if(newUser){
            uid = db.insertUser(Username,Password);
            Player player = new Player();
            player.initialize(uid, Username);
            playerCollection.put(uid, player);
            //SocketList.put(uid,sock);
            result = uid;
        }
        //check if user exists in DB, return the uid, and insert the uid/socket into the socket list
        //mark user as valid
        else{
            if((uid = db.findUser(Username,Password)) == -1){
                result = 1;
            }
            else{
                result = uid;
                Player player = new Player();
                player.initialize(uid, Username);
                playerCollection.put(uid, player);
                //SocketList.put(uid,sock);
            }
        }
        return result;
    }
}
