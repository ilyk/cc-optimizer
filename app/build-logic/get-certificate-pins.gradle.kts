import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Base64

/**
 * Gradle script to automatically extract certificate pins from backend server
 * Certificate pins are PUBLIC information (SHA-256 of public key)
 */

tasks.register("getCertificatePins") {
    group = "security"
    description = "Fetches SSL certificate pins from backend server"
    
    doLast {
        val backendUrl = project.findProperty("BACKEND_URL")?.toString() 
            ?: System.getenv("BACKEND_URL")
            ?: "https://your-production-backend.com"
        
        println("Fetching certificate pins from: $backendUrl")
        
        try {
            val pins = extractCertificatePins(backendUrl)
            
            if (pins.isEmpty()) {
                throw GradleException("No certificate pins found for $backendUrl")
            }
            
            println("\n=== Certificate Pins Found ===")
            pins.forEachIndexed { index, pin ->
                println("Pin ${index + 1}: sha256/$pin")
            }
            println("==============================\n")
            
            // Save to gradle.properties (local only, gitignored)
            val localPropsFile = rootProject.file("local.properties")
            val props = java.util.Properties()
            
            if (localPropsFile.exists()) {
                props.load(localPropsFile.inputStream())
            }
            
            pins.forEachIndexed { index, pin ->
                props.setProperty("CERT_PIN_${index + 1}", pin)
            }
            
            props.store(localPropsFile.outputStream(), "Auto-generated certificate pins")
            println("Saved pins to local.properties")
            
        } catch (e: Exception) {
            throw GradleException("Failed to fetch certificate pins: ${e.message}", e)
        }
    }
}

/**
 * Extract certificate pins from HTTPS URL
 */
fun extractCertificatePins(urlString: String): List<String> {
    // Remove trailing slash and ensure HTTPS
    val cleanUrl = urlString.trimEnd('/').let {
        if (!it.startsWith("https://") && !it.startsWith("http://")) {
            "https://$it"
        } else {
            it.replace("http://", "https://")
        }
    }
    
    // Extract hostname
    val url = URL(cleanUrl)
    val hostname = url.host
    val port = if (url.port == -1) 443 else url.port
    
    println("Connecting to $hostname:$port...")
    
    val pins = mutableListOf<String>()
    
    // Create SSL socket to get certificate chain
    val connection = URL("https://$hostname:$port").openConnection() as HttpsURLConnection
    connection.connect()
    
    val certs = connection.serverCertificates
    connection.disconnect()
    
    // Get pin for each cert in chain (leaf + intermediates)
    certs.forEach { cert ->
        if (cert is X509Certificate) {
            val publicKey = cert.publicKey.encoded
            val pin = MessageDigest.getInstance("SHA-256")
                .digest(publicKey)
                .let { Base64.getEncoder().encodeToString(it) }
            
            pins.add(pin)
            println("Found certificate: ${cert.subjectDN}")
        }
    }
    
    return pins.distinct()
}

