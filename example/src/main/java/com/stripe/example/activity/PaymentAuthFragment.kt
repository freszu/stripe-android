package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.Settings
import com.stripe.example.StripeFactory
import com.stripe.example.databinding.PaymentAuthActivityBinding
import com.stripe.example.module.StripeIntentViewModel
import org.json.JSONObject


class PaymentAuthFragmentHolderActivity : AppCompatActivity(R.layout.payment_auth_fragment_holder_activity) {

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e(this::class.simpleName, "Activity result in activity!")
    }
}

class PaymentAuthFragment : BaseIntentFragment() {
    private val viewBinding: PaymentAuthActivityBinding by lazy {
        PaymentAuthActivityBinding.inflate(layoutInflater)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return viewBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.inProgress.observe(this, Observer { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        val stripeAccountId = Settings(requireContext()).stripeAccountId

        val uiCustomization =
            PaymentAuthConfig.Stripe3ds2UiCustomization.Builder().build()
        PaymentAuthConfig.init(PaymentAuthConfig.Builder()
            .set3ds2Config(PaymentAuthConfig.Stripe3ds2Config.Builder()
                .setTimeout(6)
                .setUiCustomization(uiCustomization)
                .build())
            .build())

        viewBinding.confirmWith3ds1Button.setOnClickListener {
            createAndConfirmPaymentIntent("us",
                confirmParams3ds1,
                stripeAccountId = stripeAccountId)
        }
        viewBinding.confirmWith3ds2Button.setOnClickListener {
            createAndConfirmPaymentIntent("us",
                confirmParams3ds2,
                shippingDetails = SHIPPING,
                stripeAccountId = stripeAccountId)
        }

        viewBinding.confirmWithNewCardButton.setOnClickListener {
            viewBinding.cardInputWidget.paymentMethodCreateParams?.let {
                createAndConfirmPaymentIntent("us",
                    it,
                    shippingDetails = SHIPPING,
                    stripeAccountId = stripeAccountId)
            }
        }

        viewBinding.setupButton.setOnClickListener {
            createAndConfirmSetupIntent("us",
                confirmParams3ds2,
                stripeAccountId = stripeAccountId)
        }
    }

    private fun enableUi(enable: Boolean) {
        viewBinding.progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
        viewBinding.confirmWith3ds2Button.isEnabled = enable
        viewBinding.confirmWith3ds1Button.isEnabled = enable
        viewBinding.confirmWithNewCardButton.isEnabled = enable
        viewBinding.setupButton.isEnabled = enable
    }

    private companion object {

        /**
         * See https://stripe.com/docs/payments/3d-secure#three-ds-cards for more options.
         */
        private val confirmParams3ds2 =
            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.Builder()
                    .setNumber("4000000000003238")
                    .setExpiryMonth(1)
                    .setExpiryYear(2025)
                    .setCvc("123")
                    .build()
            )

        private val confirmParams3ds1 =
            PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.Builder()
                    .setNumber("4000000000003063")
                    .setExpiryMonth(1)
                    .setExpiryYear(2025)
                    .setCvc("123")
                    .build()
            )

        private val SHIPPING = ConfirmPaymentIntentParams.Shipping(
            address = Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build(),
            name = "Jenny Rosen",
            carrier = "Fedex",
            trackingNumber = "12345"
        )
    }
}

abstract class BaseIntentFragment : Fragment() {

    internal val viewModel: StripeIntentViewModel by viewModels()
    private val stripeAccountId: String? by lazy {
        Settings(requireContext()).stripeAccountId
    }
    private val stripe: Stripe by lazy {
        StripeFactory(requireContext(), stripeAccountId).create()
    }

    protected fun createAndConfirmPaymentIntent(
        country: String,
        paymentMethodCreateParams: PaymentMethodCreateParams?,
        shippingDetails: ConfirmPaymentIntentParams.Shipping? = null,
        stripeAccountId: String? = null,
        existingPaymentMethodId: String? = null,
        mandateDataParams: MandateDataParams? = null
    ) {
        requireNotNull(paymentMethodCreateParams ?: existingPaymentMethodId)

        // quick adapt of old sample
        viewModel.createPaymentIntent(country).observe(this, Observer {
            it.fold(
                onSuccess = { handleCreatePaymentIntentResponse(it, paymentMethodCreateParams, shippingDetails, stripeAccountId, existingPaymentMethodId, mandateDataParams) },
                onFailure = { error("Just crash here quick adapt") }
            )
        })
    }

    protected fun createAndConfirmSetupIntent(
        country: String,
        params: PaymentMethodCreateParams,
        stripeAccountId: String? = null
    ) {
        // quick adapt of old sample
        viewModel.createSetupIntent(country).observe(this, Observer {
            it.fold(
                onSuccess = { handleCreateSetupIntentResponse(it, params, stripeAccountId) },
                onFailure = { error("Just crash here quick adapt") }
            )
        })
    }

    private fun handleCreatePaymentIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams?,
        shippingDetails: ConfirmPaymentIntentParams.Shipping?,
        stripeAccountId: String?,
        existingPaymentMethodId: String?,
        mandateDataParams: MandateDataParams?
    ) {
        val secret = responseData.getString("secret")
        viewModel.status.postValue(
            viewModel.status.value +
                "\n\nStarting PaymentIntent confirmation" + (stripeAccountId?.let {
                " for $it"
            } ?: ""))
        val confirmPaymentIntentParams = if (existingPaymentMethodId == null) {
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = requireNotNull(params),
                clientSecret = secret,
                shipping = shippingDetails
            )
        } else {
            ConfirmPaymentIntentParams.createWithPaymentMethodId(
                paymentMethodId = existingPaymentMethodId,
                clientSecret = secret,
                mandateData = mandateDataParams
            )
        }
        stripe.confirmPayment(this, confirmPaymentIntentParams, stripeAccountId)
    }

    private fun handleCreateSetupIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams,
        stripeAccountId: String?
    ) {
        val secret = responseData.getString("secret")
        viewModel.status.postValue(
            viewModel.status.value +
                "\n\nStarting SetupIntent confirmation" + (stripeAccountId?.let {
                " for $it"
            } ?: ""))
        stripe.confirmSetupIntent(
            this,
            ConfirmSetupIntentParams.create(
                paymentMethodCreateParams = params,
                clientSecret = secret
            ),
            stripeAccountId
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e(this::class.simpleName, "Activity result in Fragment!")

        viewModel.status.value += "\n\nPayment authentication completed, getting result"
        val isPaymentResult =
            stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
                override fun onSuccess(result: PaymentIntentResult) {
                    onConfirmSuccess(result)
                }

                override fun onError(e: Exception) {
                    onConfirmError(e)
                }

            })
        if (!isPaymentResult) {
            // remove nonsense callbacks with weak references...
            stripe.onSetupResult(requestCode, data, object : ApiResultCallback<SetupIntentResult> {
                override fun onSuccess(result: SetupIntentResult) {
                    onConfirmSuccess(result)
                }

                override fun onError(e: Exception) {
                    onConfirmError(e)
                }

            })
        }
    }

    protected open fun onConfirmSuccess(result: PaymentIntentResult) {
        val paymentIntent = result.intent
        viewModel.status.value += "\n\n" +
            "PaymentIntent confirmation outcome: ${result.outcome}\n\n" +
            getString(R.string.payment_intent_status, paymentIntent.status)
        viewModel.inProgress.value = false
    }

    protected open fun onConfirmSuccess(result: SetupIntentResult) {
        val setupIntentResult = result.intent
        viewModel.status.value += "\n\n" +
            "SetupIntent confirmation outcome: ${result.outcome}\n\n" +
            getString(R.string.setup_intent_status, setupIntentResult.status)
        viewModel.inProgress.value = false
    }

    protected open fun onConfirmError(e: Exception) {
        viewModel.status.value += "\n\nException: " + e.message
        viewModel.inProgress.value = false
    }
}
