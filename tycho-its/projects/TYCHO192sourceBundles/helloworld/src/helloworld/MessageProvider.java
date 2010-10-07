package helloworld;

public class MessageProvider {

    public String getGreeting() {
        return getGreeting("World");
    }

    public String getGreeting(String receiver) {
        return "Hello " + receiver + "!!";
    }

}
