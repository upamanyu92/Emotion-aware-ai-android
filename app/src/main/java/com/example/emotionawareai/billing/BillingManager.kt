package com.example.emotionawareai.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.emotionawareai.BuildConfig
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
	@ApplicationContext context: Context
) {
	private val billingClient: BillingClient
	private val productDetails = mutableMapOf<String, ProductDetails>()

	private val _isBillingReady = MutableStateFlow(false)
	val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

	private val _isPremium = MutableStateFlow(false)
	val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

	private val _billingMessage = MutableStateFlow<String?>(null)
	val billingMessage: StateFlow<String?> = _billingMessage.asStateFlow()

	private val _offers = MutableStateFlow<List<PremiumOffer>>(emptyList())
	val offers: StateFlow<List<PremiumOffer>> = _offers.asStateFlow()

	private val _isPurchaseInProgress = MutableStateFlow(false)
	val isPurchaseInProgress: StateFlow<Boolean> = _isPurchaseInProgress.asStateFlow()

	private val _isRestoreInProgress = MutableStateFlow(false)
	val isRestoreInProgress: StateFlow<Boolean> = _isRestoreInProgress.asStateFlow()

	init {
		val listener = PurchasesUpdatedListener { result, purchases ->
			_isPurchaseInProgress.update { false }
			if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
				handlePurchases(purchases)
			} else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
				_billingMessage.update { "Billing failed: ${result.debugMessage}" }
			}
		}

		billingClient = BillingClient.newBuilder(context)
			.setListener(listener)
			.enablePendingPurchases()
			.build()

		connect()
	}

	fun connect() {
		if (billingClient.isReady) {
			_isBillingReady.update { true }
			queryProducts()
			restorePurchases()
			return
		}

		billingClient.startConnection(object : BillingClientStateListener {
			override fun onBillingServiceDisconnected() {
				_isBillingReady.update { false }
				_billingMessage.update { "Billing disconnected. Tap retry." }
			}

			override fun onBillingSetupFinished(result: BillingResult) {
				val ready = result.responseCode == BillingClient.BillingResponseCode.OK
				_isBillingReady.update { ready }
				if (ready) {
					queryProducts()
					restorePurchases()
				} else {
					_billingMessage.update { "Billing unavailable: ${result.debugMessage}" }
				}
			}
		})
	}

	fun retryConnection() {
		_billingMessage.update { null }
		connect()
	}

	fun launchUpgradeFlow(activity: Activity, planType: PremiumPlanType) {
		val offer = _offers.value.firstOrNull { it.planType == planType && it.available }
		val details = offer?.let { productDetails[it.productId] }
		if (!_isBillingReady.value || details == null) {
			_billingMessage.update { "Upgrade is not available yet. Please retry." }
			return
		}

		val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
			.setProductDetails(details)

		if (planType == PremiumPlanType.MONTHLY) {
			val token = details.subscriptionOfferDetails
				?.firstOrNull()
				?.offerToken
			if (token.isNullOrBlank()) {
				_billingMessage.update { "Subscription offer unavailable right now." }
				return
			}
			productParamsBuilder.setOfferToken(token)
		}

		_isPurchaseInProgress.update { true }
		val flowParams = BillingFlowParams.newBuilder()
			.setProductDetailsParamsList(listOf(productParamsBuilder.build()))
			.build()

		val result = billingClient.launchBillingFlow(activity, flowParams)
		if (result.responseCode != BillingClient.BillingResponseCode.OK) {
			_isPurchaseInProgress.update { false }
			_billingMessage.update { "Unable to start purchase: ${result.debugMessage}" }
		}
	}

	fun restorePurchases() {
		if (!billingClient.isReady) return
		_isRestoreInProgress.update { true }

		var allPurchases = emptyList<Purchase>()
		billingClient.queryPurchasesAsync(
			QueryPurchasesParams.newBuilder()
				.setProductType(BillingClient.ProductType.INAPP)
				.build()
		) { inAppResult, inAppPurchases ->
			if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
				allPurchases = allPurchases + inAppPurchases
			}

			billingClient.queryPurchasesAsync(
				QueryPurchasesParams.newBuilder()
					.setProductType(BillingClient.ProductType.SUBS)
					.build()
			) { subsResult, subsPurchases ->
				_isRestoreInProgress.update { false }
				if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
					allPurchases = allPurchases + subsPurchases
				}
				handlePurchases(allPurchases)
			}
		}
	}

	private fun queryProducts() {
		val products = listOf(
			QueryProductDetailsParams.Product.newBuilder()
				.setProductId(BuildConfig.BILLING_INAPP_PREMIUM_SKU)
				.setProductType(BillingClient.ProductType.INAPP)
				.build(),
			QueryProductDetailsParams.Product.newBuilder()
				.setProductId(BuildConfig.BILLING_SUBS_MONTHLY_SKU)
				.setProductType(BillingClient.ProductType.SUBS)
				.build()
		)

		billingClient.queryProductDetailsAsync(
			QueryProductDetailsParams.newBuilder().setProductList(products).build()
		) { result, detailsList ->
			if (result.responseCode != BillingClient.BillingResponseCode.OK) {
				_billingMessage.update { "Price fetch failed: ${result.debugMessage}" }
				return@queryProductDetailsAsync
			}

			productDetails.clear()
			detailsList.forEach { details -> productDetails[details.productId] = details }

			val inApp = detailsList.firstOrNull { it.productId == BuildConfig.BILLING_INAPP_PREMIUM_SKU }
			val subs = detailsList.firstOrNull { it.productId == BuildConfig.BILLING_SUBS_MONTHLY_SKU }

			_offers.update {
				listOf(
					PremiumOffer(
						productId = BuildConfig.BILLING_SUBS_MONTHLY_SKU,
						planType = PremiumPlanType.MONTHLY,
						title = "Premium Monthly",
						priceText = subs?.subscriptionOfferDetails
							?.firstOrNull()
							?.pricingPhases
							?.pricingPhaseList
							?.firstOrNull()
							?.formattedPrice ?: "Not available",
						available = subs != null
					),
					PremiumOffer(
						productId = BuildConfig.BILLING_INAPP_PREMIUM_SKU,
						planType = PremiumPlanType.LIFETIME,
						title = "Premium Lifetime",
						priceText = inApp?.oneTimePurchaseOfferDetails?.formattedPrice ?: "Not available",
						available = inApp != null
					)
				)
			}
		}
	}

	private fun handlePurchases(purchases: List<Purchase>) {
		val premiumOwned = purchases.any { purchase ->
			purchase.products.any {
				it == BuildConfig.BILLING_INAPP_PREMIUM_SKU || it == BuildConfig.BILLING_SUBS_MONTHLY_SKU
			} && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
		}
		_isPremium.update { premiumOwned }

		purchases
			.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
			.forEach { acknowledgePurchase(it) }
	}

	private fun acknowledgePurchase(purchase: Purchase) {
		val params = AcknowledgePurchaseParams.newBuilder()
			.setPurchaseToken(purchase.purchaseToken)
			.build()

		billingClient.acknowledgePurchase(params) { result ->
			if (result.responseCode != BillingClient.BillingResponseCode.OK) {
				Log.w(TAG, "Acknowledge failed: ${result.debugMessage}")
			}
		}
	}

	fun clearMessage() {
		_billingMessage.update { null }
	}

	companion object {
		private const val TAG = "BillingManager"
	}
}

enum class PremiumPlanType {
	MONTHLY,
	LIFETIME
}

data class PremiumOffer(
	val productId: String,
	val planType: PremiumPlanType,
	val title: String,
	val priceText: String,
	val available: Boolean
)

