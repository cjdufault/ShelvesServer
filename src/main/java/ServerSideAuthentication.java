import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.encoders.Hex;
import java.io.*;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Random;

class ServerSideAuthentication {

    private String passwordHash;
    private HashMap<InetAddress, String> nonceMap = new HashMap<>();

    ServerSideAuthentication(Path credFilePath) {
        try {
            readCredentials(credFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // reads credentials in from a config file
    private void readCredentials(Path credFilePath) throws IOException {
        try {
            BufferedReader br = new BufferedReader(new FileReader(credFilePath.toString()));

            passwordHash = br.readLine();

            br.close();
            if (passwordHash != null){ // if the file is empty
                return;
            }

            // if file not found or file is empty, create the file, ask for a password, and write the pw hash to the file
            setPassword(credFilePath);
        }
        catch (FileNotFoundException e){
            setPassword(credFilePath);
        }
    }

    // generate the expected hash from valid credentials and the randomly generated nonce
    private String hash(String input) {
        SHA3.DigestSHA3 digestSHA3 = new SHA3.Digest512();
        byte[] digest = digestSHA3.digest(input.getBytes());
        return Hex.toHexString(digest);
    }

    // generate a random long int and put it in a hash map with the client's IP address to be checked and removed later
    String getNonce(InetAddress remoteAddress) {
        Random random = new Random();
        long seed = random.nextLong();

        String nonce = hash(Long.toString(seed));

        nonceMap.remove(remoteAddress); // remove any existing entry for this IP address
        nonceMap.put(remoteAddress, nonce);

        return nonce;
    }

    // compare hashed credentials provided by the client with the hash of the expected credentials
    boolean checkCredentials(String clientHash, InetAddress remoteAddress) {
        String expectedHash = hash(passwordHash + nonceMap.get(remoteAddress));
        nonceMap.remove(remoteAddress); // remove the client's IP address and nonce from the hash map
        return expectedHash.equals(clientHash);
    }

    // asks the user to input a password that will be used to control access to POST requests
    private void setPassword(Path credFilePath) throws IOException{
        BufferedWriter bw = new BufferedWriter(new FileWriter(credFilePath.toString()));
        System.out.println("No password set. Enter a password: ");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String password = "";

        try {
            password = in.readLine();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        passwordHash = hash(password);
        bw.write(passwordHash);
        bw.close();
    }
}