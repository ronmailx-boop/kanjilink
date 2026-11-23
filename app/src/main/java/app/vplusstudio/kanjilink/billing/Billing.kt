package app.vplusstudio.kanjilink.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import app.vplusstudio.kanjilink.data.Prefs
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Google Play Billing for one non-consumable in-app product, [PRODUCT_ID] (Kanjilink Pro).
 * Google is the seller of record (VAT, receipts). Create the product with this id in Play Console:
 * Monetize → Products → In-app products.
 */
class Billing(
    context: Context,
    /** Called on the main thread when Pro, price or availability change. */
    private val onChange: (State) -> Unit,
) {
    data class State(
        val isPro: Boolean,
        /** Localised price ("€4.99"), null until Google Play answers or if the product is missing. */
        val price: String? = null,
        /** False when Google Play Billing is not reachable (no Play Store, sideloaded test build, offline). */
        val available: Boolean = false,
        /** A short message for the user after a purchase attempt, or null. */
        val message: String? = null,
    )

    companion object {
        const val PRODUCT_ID = "kanjilink_pro"

        /** SHA-256 of the review code (the code itself is only in Play Console's App access form). */
        private const val REVIEW_CODE_SHA256 = "de0c3ad17cf6ea171867f039308bc5fb5ed47e133905647d886b57196044b748"
        private const val TAG = "KanjilinkBilling"
    }

    private val appContext = context.applicationContext
    private val prefs = Prefs(appContext)
    private var details: ProductDetails? = null
    var state = State(isPro = prefs.isPro)
        private set

    private val client = BillingClient.newBuilder(appContext)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    update(state.copy(available = true))
                    refreshPurchases()
                    loadProduct()
                } else {
                    Log.w(TAG, "setup: ${result.responseCode} ${result.debugMessage}")
                    update(state.copy(available = false))
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() reconnects on the next call.
            }
        })
    }

    fun end() = client.endConnection()

    /** Restores Pro from Google Play (also used by the "Restore purchase" button). */
    fun refreshPurchases() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handlePurchases(purchases, fromQuery = true)
        }
    }

    private fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            ),
        ).build()
        client.queryProductDetailsAsync(params) { result, productResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "product: ${result.responseCode} ${result.debugMessage}")
                return@queryProductDetailsAsync
            }
            details = productResult.productDetailsList.firstOrNull()
            update(state.copy(price = details?.oneTimePurchaseOfferDetails?.formattedPrice))
        }
    }

    /** Opens Google Play's purchase sheet. Returns false if the product is not ready. */
    fun buy(activity: Activity): Boolean {
        val d = details ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> handlePurchases(purchases.orEmpty(), fromQuery = false)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refreshPurchases()
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> update(state.copy(message = "The purchase did not go through. Please try again."))
        }
    }

    private fun handlePurchases(purchases: List<Purchase>, fromQuery: Boolean) {
        val pro = purchases.filter { PRODUCT_ID in it.products }
        val owned = pro.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pending = pro.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        pro.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }.forEach { p ->
            // Unacknowledged purchases are refunded by Google after 3 days.
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { r ->
                if (r.responseCode != BillingClient.BillingResponseCode.OK) Log.w(TAG, "acknowledge: ${r.debugMessage}")
            }
        }
        // A query that finds nothing means not owned (for example after a refund).
        val bought = owned || (!fromQuery && prefs.playPro)
        prefs.playPro = bought
        val isPro = bought || prefs.reviewUnlock
        val message = when {
            owned && !fromQuery -> "Thank you! Kanjilink Pro is unlocked."
            pending && !owned -> "Your payment is pending. Pro unlocks when it completes."
            else -> null
        }
        update(state.copy(isPro = isPro, message = message ?: state.message))
    }

    /** Unlocks Pro for Google Play's reviewers with the code given in Play Console (App access). */
    fun redeemReviewCode(code: String): Boolean {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(code.trim().uppercase().toByteArray())
            .joinToString("") { "%02x".format(it) }
        if (digest != REVIEW_CODE_SHA256) return false
        prefs.reviewUnlock = true
        update(state.copy(isPro = true, message = "Review access: Kanjilink Pro is unlocked."))
        return true
    }

    fun clearMessage() = update(state.copy(message = null))

    private fun update(s: State) {
        state = s
        android.os.Handler(android.os.Looper.getMainLooper()).post { onChange(s) }
    }
}
