package br.ufs.dcomp.ChatRabbitMQ;


import com.rabbitmq.client.*;
import java.io.*;
import java.util.Scanner;
import com.google.protobuf.*;
import java.nio.file.*;


public class Chat {
  
  static String user = ""; // usuario logado
  static String sentBy = ""; // origem da mensagem
  static String sendTo = ""; // destinatario da mensagem
  static String destinationPrefix = ""; // indicador do destinatario
  static String typeMime; // identificador do tipo de arquivo
  static String username = "maasoares"; // usuario administrador do servidor Rabbit
  static String password = "abc123"; // senha de acesso ao servidor Rabbit
  static String hostrmq = "ec2-3-88-66-6.compute-1.amazonaws.com"; // IP da instancia no servidor Rabbit
  
  public static void toWait() { // retorna para espera de entrada do usuario logado 
    System.out.print( ( !sendTo.isEmpty() ? ( destinationPrefix + sendTo ) : "" )  + ">> " );
  }
                    
  public static void main(String[] argv)      throws Exception  {
    
    ConnectionFactory factory = new ConnectionFactory();
    
    factory.setHost( hostrmq ); // IP da instancia ChatRabbitMQ-0 //("ip-da-instancia-da-aws");
    factory.setUsername(username); //("usuário-do-rabbitmq-server");
    factory.setPassword(password); //("senha-do-rabbitmq-server");
    factory.setVirtualHost("/");
  
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    
    // iniciando o chat, logando o usuario
    Scanner scan = new Scanner(System.in);
    
    while ( user.isEmpty() ) {
      System.out.print( "User: " );
      user = scan.nextLine();
      if ( user.isEmpty() ) // verificando se foi digitado o nome do usuario
        System.out.println( "Por favor forneça o nome do usuário !" );
    }

    // criando a fila
                      //(  queue-name,     durable, exclusive, auto-delete, params); 
    channel.queueDeclare( (user + "Text"), false,   false,     false,       null); // cria fila de textos
    
    // aguardando entrada do usuario logado
    toWait();

    // monitorando o recebimento de mensagens de texto
    Consumer consumer = new DefaultConsumer(channel) {
      
      public void handleDelivery( String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body )    throws IOException {
        String messageReceived = Message.deserialize( body );
        if ( !messageReceived.isEmpty() ) {
        System.out.println( "\n" + messageReceived ); // imprime a mensagem recebida
        toWait();
        }
      }
    };
    
    // consome a fila
                      //(   queue-name,      autoAck, consumer);    
    channel.basicConsume( ( user + "Text" ), true,    consumer ); // consome a fila 
    
    // enviando mensagens
    while ( scan.hasNextLine() ) {
        
        String messageText = scan.nextLine(); // recebe a entrada do usuario logado
        
        if ( !messageText.isEmpty() ) {
          
          switch ( messageText.charAt(0) ) {
              
            case '@': // identificando usuario destino
              sendTo = messageText.substring(1); // indicando o destinatario
              destinationPrefix = "@";
                                //( queue-name,        durable, exclusive, auto-delete, params);               
              channel.queueDeclare( (sendTo + "Text"), false,   false,     false,       null ); // cria a fila 
              toWait();
              break;
      
            default: 
              if ( !sendTo.isEmpty() ) {
                if ( destinationPrefix.equals("@") ) {
                // publicando mensagem de texto na fila do rabitmq (usuario)
                channel.basicPublish( "", ( sendTo + "Text" ), null, Message.serialize( user , ( destinationPrefix + sendTo ), "text/message", messageText, "" ) );
                }
              } else {
                System.out.println( "Não há destinatário para envio da mensagem !");
              }
              toWait();
              break;
          }
        }
        


    }
  }
  
  
}