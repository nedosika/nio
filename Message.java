public class Message implements Serialisable{
    public String message;
    public String type;

    public Message(String type, String message){
        this.message = message;
        this.type = type;
    }
}