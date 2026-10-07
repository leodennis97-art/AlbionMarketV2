package com.example.albionmarketv2

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val SUBSCRIPTION_PRODUCT_ID = "albion_monthly_subscription"
    }

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun startConnection(onSuccess: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            queryProductDetails()
            queryActivePurchases()
            onSuccess?.invoke()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Setup Successful")
                    queryProductDetails()
                    queryActivePurchases()
                    onSuccess?.invoke()
                } else {
                    Log.e(TAG, "Billing Setup Failed: ${billingResult.debugMessage}")
                    _subscriptionState.value = SubscriptionState.Error("Play Store Verbindung fehlgeschlagen")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing Service Disconnected")
                _subscriptionState.value = SubscriptionState.Error("Play Store Verbindung getrennt")
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            val productDetailsList = queryProductDetailsResult.productDetailsList
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                _productDetails.value = productDetailsList[0]
            } else {
                Log.e(TAG, "Product details query failed: ${billingResult.debugMessage}")
            }
        }
    }

    fun queryActivePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSubscription = purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.products.contains(SUBSCRIPTION_PRODUCT_ID)
                }

                if (hasActiveSubscription) {
                    val activePurchase = purchases.first { it.products.contains(SUBSCRIPTION_PRODUCT_ID) }
                    handlePurchase(activePurchase)
                } else {
                    if (!LicenseManager.isLicenseValid(context)) {
                        _subscriptionState.value = SubscriptionState.NotSubscribed
                    } else {
                        _subscriptionState.value = SubscriptionState.Subscribed("Aktiv via Lizenzcode")
                    }
                }
            }
        }
    }

    fun launchSubscriptionFlow(activity: Activity): Boolean {
        val details = _productDetails.value
        if (details == null) {
            Log.e(TAG, "Product details not available yet")
            return false
        }

        val offerToken = details.subscriptionOfferDetails?.getOrNull(0)?.offerToken
        if (offerToken == null) {
            Log.e(TAG, "Offer token not available")
            return false
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled billing flow")
            _subscriptionState.value = SubscriptionState.NotSubscribed
        } else {
            Log.e(TAG, "Billing error: ${billingResult.debugMessage}")
            _subscriptionState.value = SubscriptionState.Error(billingResult.debugMessage)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        LicenseManager.setPlayStoreSubscriptionActive(context, purchase.purchaseToken)
                        _subscriptionState.value = SubscriptionState.Subscribed("Google Play Abo aktiv")
                    } else {
                        Log.e(TAG, "Acknowledge failed: ${billingResult.debugMessage}")
                    }
                }
            } else {
                LicenseManager.setPlayStoreSubscriptionActive(context, purchase.purchaseToken)
                _subscriptionState.value = SubscriptionState.Subscribed("Google Play Abo aktiv")
            }
        }
    }

    sealed interface SubscriptionState {
        data object Loading : SubscriptionState
        data object NotSubscribed : SubscriptionState
        data class Subscribed(val details: String) : SubscriptionState
        data class Error(val message: String) : SubscriptionState
    }
}
