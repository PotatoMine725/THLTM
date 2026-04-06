import com.wifichat.shared.dto.AuthResponse;
import com.wifichat.tcp.TcpChatClient;
import com.wifichat.tcp.TcpEventListener;
import com.wifichat.shared.dto.MessageRecord;

public class TcpProbe3 {
    public static void main(String[] args) throws Exception {
        TcpChatClient client = new TcpChatClient("127.0.0.1", 61002);
        client.addListener(new TcpEventListener() {
            @Override public void onMessageEvent(MessageRecord message) {}
            @Override public void onDisconnected(String reason) { System.out.println("DISCONNECTED:" + reason); }
        });
        client.connect();
        String u = "probe" + System.currentTimeMillis();
        AuthResponse auth = client.register(u, "1234", "Probe");
        for (int i = 0; i < 6; i++) {
            client.heartbeat(auth.sessionToken);
            System.out.println("heartbeat ok #" + (i + 1));
            Thread.sleep(700);
        }
        client.close();
    }
}
