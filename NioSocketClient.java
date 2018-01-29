import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class NioSocketClient {
    static BufferedReader userInputReader = null;
    static String login;
    static String password;

    public static boolean processReadySet(Set readySet) throws Exception {
        Iterator iterator = readySet.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = (SelectionKey)
            iterator.next();
            iterator.remove();
            if (key.isConnectable()) {
                boolean connected = processConnect(key);
                if (!connected) {
                    return true; // Exit
                }
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = factory.newDocumentBuilder();
                    Document document = documentBuilder.newDocument();

                    Element root = document.createElement("data");

                    Attr attrType = document.createAttribute("type");
                    attrType.setValue("message");
                    root.setAttributeNode(attrType);

                    Attr attrText = document.createAttribute("text");
                    attrText.setValue("test message");
                    root.setAttributeNode(attrText);

                    document.appendChild(root);

                    SocketChannel sChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.wrap(toXmlString(document).getBytes());
                    sChannel.write(buffer);
                    buffer.clear();
            }

            if (key.isReadable()) {
                String msg = processRead(key);
                System.out.println(msg);
            }

            if (key.isWritable()) {
                System.out.print("Please enter a message(Bye to quit):");
                String msg = userInputReader.readLine();

                if (msg.equalsIgnoreCase("bye")) {
                    SocketChannel sChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.wrap("END".getBytes());
                    sChannel.write(buffer);
                    buffer.clear();

                    return true; // Exit
                }

                SocketChannel sChannel = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                sChannel.write(buffer);
                buffer.clear();
            }
        }
        return false; // Not done yet
    }
    public static boolean processConnect(SelectionKey key) throws Exception{
        SocketChannel channel = (SocketChannel) key.channel();
        while (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        return true;
    }
    public static String processRead(SelectionKey key) throws Exception {
        SocketChannel sChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        sChannel.read(buffer);
        buffer.flip();
        String encoding = System.getProperty("file.encoding");
        Charset charset = Charset.forName(encoding);//"UTF-8");
        CharsetDecoder decoder = charset.newDecoder();
        CharBuffer charBuffer = decoder.decode(buffer);
        String msg = charBuffer.toString();
        return msg;
    }

    private static String toXmlString(Document document) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StringWriter strWriter = new StringWriter();
        StreamResult result = new StreamResult(strWriter);

        transformer.transform(source, result);

        return strWriter.getBuffer().toString();
    }

    public static void main(String[] args) throws Exception {
        InetAddress serverIPAddress = InetAddress.getByName("localhost");
        int port = 4444;
        InetSocketAddress serverAddress = new InetSocketAddress(
        serverIPAddress, port);
        Selector selector = Selector.open();
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(serverAddress);
        int operations = SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        channel.register(selector, operations);

        userInputReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Login:");
        login = userInputReader.readLine();

        System.out.print("Password:");
        password = userInputReader.readLine();


        while (true) {
            if (selector.select() > 0) {
                boolean doneStatus = processReadySet(selector.selectedKeys());
                if (doneStatus) {
                     break;
                }
            }
        }
        channel.close();
    }
}
