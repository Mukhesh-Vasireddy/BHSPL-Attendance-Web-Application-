import java.net.*;
import java.nio.*;
import java.util.*;

public class FindUsers {
    public static void main(String[] args) throws Exception {
        String ip = "10.2.1.100";
        int port = 4370;
        DatagramSocket sock = new DatagramSocket();
        sock.setSoTimeout(5000);
        InetAddress addr = InetAddress.getByName(ip);

        // 1. Connect
        send(sock, addr, port, 1000, null, 0, 0);
        byte[] resp = recv(sock);
        int session = getShort(resp, 4);
        System.out.println("Session: " + session);

        // 2. Read All Data (CMD 13)
        byte[] payload = new byte[4];
        send(sock, addr, port, 13, payload, session, 0);
        resp = recv(sock);
        System.out.println("CMD 13 Response CMD: " + getShort(resp, 0));
        
        byte[] allData = null;
        if (getShort(resp, 0) == 1501) { // PREPARE_DATA
            int totalSize = getInt(resp, 8);
            System.out.println("Total Size (PREPARE): " + totalSize);
            allData = new byte[totalSize];
            int received = 0;
            while (received < totalSize) {
                byte[] chunk = recv(sock);
                if (chunk == null) break;
                int len = chunk.length - 8;
                System.arraycopy(chunk, 8, allData, received, Math.min(len, totalSize - received));
                received += len;
            }
        } else if (getShort(resp, 0) == 1500) { // ACK_OK
            allData = new byte[resp.length - 8];
            System.arraycopy(resp, 8, allData, 0, allData.length);
            System.out.println("Total Size (ACK): " + allData.length);
        }
        
        if (allData != null && allData.length >= 8) {
            System.out.println("--- Header ---");
            System.out.println("Offset 0 (Int): " + getInt(allData, 0));
            System.out.println("Offset 4 (Int): " + getInt(allData, 4));
            
            System.out.println("\n--- First 10 Records (40 bytes) ---");
            for (int k = 0; k < 10; k++) {
                int off = 8 + k * 40;
                if (off + 40 > allData.length) break;
                String uid = readStr(allData, off + 2, 10);
                String name = readStr(allData, off + 12, 28);
                System.out.println("Slot " + k + ": UID=[" + uid + "] Name=[" + name + "]");
            }
        }
        
        // Disconnect
        send(sock, addr, port, 1001, null, session, 1);
    }

    static void send(DatagramSocket s, InetAddress a, int p, int cmd, byte[] pay, int session, int reply) throws Exception {
        byte[] pkt = new byte[8 + (pay == null ? 0 : pay.length)];
        putShort(pkt, 0, cmd);
        putShort(pkt, 4, session);
        putShort(pkt, 6, reply);
        if (pay != null) System.arraycopy(pay, 0, pkt, 8, pay.length);
        putShort(pkt, 2, checksum(pkt));
        s.send(new DatagramPacket(pkt, pkt.length, a, p));
    }

    static byte[] recv(DatagramSocket s) {
        try {
            byte[] buf = new byte[2048];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            s.receive(p);
            byte[] res = new byte[p.getLength()];
            System.arraycopy(buf, 0, res, 0, p.getLength());
            return res;
        } catch (Exception e) { return null; }
    }

    static int getInt(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24); }
    static int getShort(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8); }
    static void putShort(byte[] b, int o, int v) { b[o] = (byte)(v & 0xFF); b[o+1] = (byte)((v >> 8) & 0xFF); }
    static String readStr(byte[] b, int o, int l) {
        int len = 0;
        while (len < l && b[o + len] != 0) len++;
        return new String(b, o, len).trim();
    }
    static int checksum(byte[] b) {
        int chk = 0;
        for (int i = 0; i < b.length; i += 2) {
            if (i == 2) continue;
            int v = getShort(b, i);
            chk += v;
            if (chk > 0xFFFF) chk -= 0xFFFF;
        }
        return (~chk) & 0xFFFF;
    }
}
