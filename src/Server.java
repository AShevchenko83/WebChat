package com.javarush.test.level30.lesson15.big01;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Admin on 11.09.16.
 */
public class Server
{
    private static class Handler extends Thread{
        private Socket socket;

        public Handler(Socket socket)
        {
            this.socket = socket;
        }

        public void run(){
            String name=null;
            try(Connection connection = new Connection(socket))
            {
                    ConsoleHelper.writeMessage("Установленно новое соединение с адресом: "+socket.getRemoteSocketAddress());
                    name = serverHandshake(connection);
                    sendBroadcastMessage(new Message(MessageType.USER_ADDED, name));
                    sendListOfUsers(connection,name);
                    serverMainLoop(connection, name);

            }
            catch (IOException|ClassNotFoundException e)
            {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене с удалённым адоресом.");
            }
            if(name!=null) {connectionMap.remove(name);
            sendBroadcastMessage(new Message(MessageType.USER_REMOVED, name)); }
            ConsoleHelper.writeMessage("Cоединение с удаленным адресом закрыто.");
        }

        private String serverHandshake(Connection connection) throws IOException,
                ClassNotFoundException{
            String name = null;
            while (true)
            {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message mess = connection.receive();
                if (mess==null||mess.getType()!=MessageType.USER_NAME||
                        mess.getData()==null||mess.getData().isEmpty()||
                        connectionMap.containsKey(mess.getData())) continue;
                name=mess.getData();
                connectionMap.put(name,connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return name;
            }
        }

        private void sendListOfUsers(Connection connection, String userName) throws
                IOException{
            for (String name:connectionMap.keySet()                 )
            {
                if(!name.equals(userName)) connection.send(new Message(MessageType.USER_ADDED, name));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws
                IOException, ClassNotFoundException{
            while (true)
            {
                Message message = connection.receive();
                if(message!=null&&message.getType()==MessageType.TEXT){
                    String newMess = userName+": "+message.getData();
                    sendBroadcastMessage(new Message(MessageType.TEXT, newMess));
                }
                else ConsoleHelper.writeMessage("Ошибка типа сообщения!");
            }

        }
    }

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args)
    {
        ConsoleHelper.writeMessage("Введите номер порта сервера");
        try(ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt());)
        {
            ConsoleHelper.writeMessage("Сервер запущен");
            while (true){
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        }
        catch (IOException e)
        {
            ConsoleHelper.writeMessage("Произошла ошибка в сокете!");
        }
    }

    public static void sendBroadcastMessage(Message message){
        try
        {
            for (Connection connect: connectionMap.values() )
            {
                connect.send(message);
            }
        }
        catch (IOException e)
        {
            ConsoleHelper.writeMessage("Сообщение не было отправлено, пользователю!");
        }
    }


}
