import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

const val PAYSTACK_BASE = "https://api.paystack.co"

/** Load .env from project root (current dir when you run `gradle run`). Values in .env override system env. */
private fun loadEnv(): Map<String, String> {
    val env = System.getenv().toMutableMap()
    val envFile = File(".env")
    if (!envFile.isFile) return env
    envFile.readLines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
            val eq = trimmed.indexOf('=')
            if (eq > 0) {
                val key = trimmed.substring(0, eq).trim()
                val value = trimmed.substring(eq + 1).trim().removeSurrounding("\"", "\"")
                env[key] = value
            }
        }
    }
    return env
}

private fun env(envMap: Map<String, String>, key: String): String? = envMap[key]

@Serializable
data class InitializeRequest(
    val email: String,
    val amountKobo: Long,
    val productName: String? = null
)

@Serializable
data class PaystackInitBody(
    val email: String,
    val amount: String,
    val subaccount: String,
    val transaction_charge: String,
    val callback_url: String? = null,
    val metadata: Map<String, String>? = null
)

@Serializable
data class PaystackInitResponse(
    val status: Boolean,
    val message: String,
    val data: PaystackInitData?
)

@Serializable
data class PaystackInitData(
    val authorization_url: String,
    val access_code: String,
    val reference: String
)

fun main() {
    val envMap = loadEnv()
    val secretKey = env(envMap, "PAYSTACK_SECRET_KEY")
    val subaccountSellerA = env(envMap, "PAYSTACK_SUBACCOUNT_SELLER_A")
    val platformFeeKobo = env(envMap, "PLATFORM_FEE_KOBO")?.toLongOrNull() ?: 100_00L // 100 Naira default

    if (secretKey.isNullOrBlank()) {
        System.err.println("Missing PAYSTACK_SECRET_KEY. Set it and run again.")
        return
    }
    if (subaccountSellerA.isNullOrBlank()) {
        System.err.println("Missing PAYSTACK_SUBACCOUNT_SELLER_A. Create a subaccount in Paystack Dashboard and set the code.")
        return
    }

    val client = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    embeddedServer(Netty, port = 8080) {
        install(ServerContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "Unknown error")))
            }
        }
        routing {
            get("/") { call.respondRedirect("/index.html", permanent = false) }
            staticResources("/", "static")
            post("/api/initialize-payment") {
                val req = call.receive<InitializeRequest>()
                val body = PaystackInitBody(
                    email = req.email,
                    amount = req.amountKobo.toString(),
                    subaccount = subaccountSellerA,
                    transaction_charge = platformFeeKobo.toString(),
                    callback_url = "http://localhost:8080/success.html",
                    metadata = req.productName?.let { mapOf("product_name" to it) }
                )
                val resp = client.post("$PAYSTACK_BASE/transaction/initialize") {
                    header("Authorization", "Bearer $secretKey")
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.body<PaystackInitResponse>()
                if (!resp.status || resp.data == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (resp.message)))
                    return@post
                }
                call.respond(mapOf(
                    "authorization_url" to resp.data.authorization_url,
                    "access_code" to resp.data.access_code,
                    "reference" to resp.data.reference
                ))
            }
        }
    }.start(wait = true)
}
