import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public class HttpClientWithPublicCertificate {

    public static CloseableHttpClient createHttpClientWithPublicCertificate(String certFilePath) throws Exception {
        // Load the public certificate from the file
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Certificate certificate;
        try (FileInputStream fis = new FileInputStream(new File(certFilePath))) {
            certificate = certificateFactory.generateCertificate(fis);
        }

        // Set up HttpClient with the public certificate
        return HttpClients.custom()
                .setSSLHostnameVerifier((hostname, session) -> true) // Bypass hostname verification for self-signed certificates
                .setSslcontext(SSLContexts.custom().loadTrustMaterial(new TrustSelfSignedStrategy()).build())
                .build();
    }

    public static void main(String[] args) throws Exception {
        String certFilePath = "/path/to/public_certificate.crt";

        try (CloseableHttpClient httpClient = createHttpClientWithPublicCertificate(certFilePath)) {
            // Replace "https://example.com/api" with your API endpoint
            HttpGet httpGet = new HttpGet("https://example.com/api");

            // Execute the request
            HttpResponse response = httpClient.execute(httpGet);

            // Process the response
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            System.out.println("Response: " + responseBody);
        }
    }
}
