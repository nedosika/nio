import java.lang.reflect.Array;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;
import java.util.*;

public class NiochatServer implements Runnable {
    private int port;
    private ServerSocketChannel ssc;
    private Selector selector;
    private ByteBuffer buffer = ByteBuffer.allocate(256);
    private Map<SocketChannel,String> userMap = new HashMap<SocketChannel, String>();

    NiochatServer(int port) throws IOException {
        this.port = port;
        this.ssc = ServerSocketChannel.open();
        this.ssc.socket().bind(new InetSocketAddress(port));
        this.ssc.configureBlocking(false);
        this.selector = Selector.open();
        this.ssc.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override public void run() {
        try {
            System.out.println("Server starting on port " + this.port);

            SelectionKey key;

            while(this.ssc.isOpen()) {

                selector.select();

                Iterator it = sel.selectedKeys().iterator();

                while(iter.hasNext()) {

                    key = iter.next();

                    iter.remove();

                    if(key.isAcceptable()) this.handleAccept(key);
                    if(key.isReadable()) this.handleRead(key);
                }
            }
        } catch(IOException e) {
            System.out.println("IOException, server of port " +this.port+ " terminating. Stack trace:");
            e.printStackTrace();
        }
    }

    private final ByteBuffer welcomeBuf = ByteBuffer.wrap("Welcome!\n".getBytes());

    private void handleAccept(SelectionKey key) throws IOException {

        SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();

        String address = (new StringBuilder( sc.socket().getInetAddress().toString() )).append(":").append( sc.socket().getPort() ).toString();

        sc.configureBlocking(false);

        sc.register(selector, SelectionKey.OP_READ, address);

        userMap.put(sc,"");

        System.out.println("accepted connection from: "+address+"["+userMap.size()+"]");
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder();

        buf.clear();

        int read = 0;

        while( (read = ch.read(buf)) > 0 ) {

            buf.flip();

            byte[] bytes = new byte[buf.limit()];

            buf.get(bytes);

            sb.append(new String(bytes));

            buf.clear();
        }

        String msg;

        if(read<0) {

            msg = "[" + userMap.get(ch) + "] left the chat.\n";

            System.out.println(key.attachment() + " left the chat.");

            ch.close();

            userMap.remove(ch);

            system(msg);
        }
        else {

            msg = sb.toString();

            System.out.println(msg);

            if (msg.indexOf("//list") > -1){

                msg = "";

                for (Map.Entry<SocketChannel, String> entry: userMap.entrySet()){

                    msg = msg + entry.getValue() + "\n";
                }

                ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());

                ch.write(msgBuf);

                msgBuf.rewind();
            }
            else if(msg.indexOf("<policy-file-request/>") > -1){

                msg = "<?xml version=\"1.0\"?><!DOCTYPE cross-domain-policy SYSTEM \"http://www.adobe.com/xml/dtds/cross-domain-policy.dtd\"><!-- Policy file for xmlsocket://socks.mysite.com --><cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"4444\" /></cross-domain-policy>\0";

                ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());

                ch.write(msgBuf);

                System.out.println("send policy file");

                msgBuf.rewind();
            }
            else if (msg.indexOf("//rename") > -1){
                if(msg.length()>8){

                    String name = msg.substring(9,msg.length());

                    userMap.replace(ch,name);
                }
            }
            else {

                ArrayList<String> recipient = getPrivateRecipient(msg);

                if (recipient.size() > 0) {

                    ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());

                    for (int i=0;i<recipient.size();i++) {
                        for (Map.Entry<SocketChannel, String> entry : userMap.entrySet()) {
                            if (entry.getValue().equals(recipient.get(i)))
                                entry.getKey().write(msgBuf);
                        }
                    }

                    ch.write(msgBuf);

                } else {

                    broadcast(ch, msg);
                }
            }
        }
    }

    private ArrayList<String> getPrivateRecipient(String msg){

        ArrayList<String> arrayPrivateRecipient = new ArrayList<String>();

        if (msg.indexOf("private") > -1)

            for (String retval: msg.split("private")) {

                int beginIndex = retval.indexOf("[");

                int endIndex = retval.indexOf("]");

                if (beginIndex>-1 && endIndex>-1)

                    arrayPrivateRecipient.add(retval.substring(beginIndex+1,endIndex));
            }

        return arrayPrivateRecipient;
    }

    private void system(String msg) throws IOException{

        ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());

        for(SelectionKey key : selector.keys()) {

            if(key.isValid() && key.channel() instanceof SocketChannel) {

                SocketChannel sch = (SocketChannel) key.channel();

                sch.write(msgBuf);

                msgBuf.rewind();
            }
        }
    }

    private void broadcast(SocketChannel ch,String msg) throws IOException {

        msg = "[" + userMap.get(ch) + "] " + msg + "\n";

        ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());

        for(SelectionKey key : selector.keys()) {

            if(key.isValid() && key.channel() instanceof SocketChannel) {

                SocketChannel sch = (SocketChannel) key.channel();

                sch.write(msgBuf);

                msgBuf.rewind();
            }
        }
    }

    public static void main(String[] args) throws IOException {

        NiochatServer server = new NiochatServer(4444);

        (new Thread(server)).start();
    }
}
