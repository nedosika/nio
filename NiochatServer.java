import java.lang.reflect.Array;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.io.IOException;
import java.util.*;

public class NiochatServer implements Runnable {
    private int port;
    private ServerSocketChannel ssc;
    private Selector selector;
    private ByteBuffer buffer = ByteBuffer.allocate(256);
    private String encoding = System.getProperty("file.encoding");
    private Charset cs = Charset.forName(encoding);
    private Map<SocketChannel,String> userMap = new HashMap<SocketChannel, String>();

    NiochatServer(int port) throws IOException {
        this.port = port;
        ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(port));
        ssc.configureBlocking(false);
        selector = Selector.open();
    }

    @Override public void run() {
        try {
            System.out.println("Server starting on port " + port);

            SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);

            while(ssc.isOpen()) {

                selector.select();

                Iterator iterator = selector.selectedKeys().iterator();

                while(iterator.hasNext()) {

                    key = (SelectionKey)iterator.next();

                    iterator.remove();

                    if(key.isAcceptable()) handleAccept(key);

                    if(key.isReadable()) handleRead(key);
                }
            }
        }
        catch(IOException e) {

            System.out.println("IOException, server of port " + port + " terminating. Stack trace:");

            e.printStackTrace();
        }
    }

    //private final ByteBuffer welcomeBuf = ByteBuffer.wrap("Welcome!\n".getBytes());

    private void handleAccept(SelectionKey key) throws IOException {

        SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();

        String address = (new StringBuilder( sc.socket().getInetAddress().getHostName() )).append(":").append( sc.socket().getPort() ).toString();

        sc.configureBlocking(false);

        sc.register(selector, SelectionKey.OP_READ);

        System.out.println("accepted connection from " + address);
    }

    private void handleRead(SelectionKey key) throws IOException {

        SocketChannel ch = (SocketChannel) key.channel();

        ch.read(buffer);

        CharBuffer cb = cs.decode((ByteBuffer) buffer.flip());

        String response = cb.toString();

        System.out.print("Echoing : " + response);

        //ch.write((ByteBuffer) buffer.rewind());


        if (response.indexOf("END") != -1)
            ch.close();
        if (response.indexOf("STOP") != -1)
            ssc.close();

        buffer.clear();
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

    private void broadcast(String broadcastMessage) throws IOException {

        ByteBuffer buffer = ByteBuffer.wrap(broadcastMessage.getBytes());

        for(SelectionKey key : selector.keys()) {

            if(key.isValid() && key.channel() instanceof SocketChannel) {

                SocketChannel sch = (SocketChannel) key.channel();

                sch.write(buffer);

                buffer.rewind();
            }
        }
    }

    public static void main(String[] args) throws IOException {

        NiochatServer server = new NiochatServer(4444);
        (new Thread(server)).start();

    }
}
