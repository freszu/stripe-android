package com.stripe.android;

import android.app.Activity;
import android.content.Intent;

import com.stripe.android.model.PaymentIntentFixtures;
import com.stripe.android.view.AuthActivityStarter;
import com.stripe.android.view.StripeIntentResultExtras;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class Stripe3ds2CompletionStarterTest {

    private Stripe3ds2CompletionStarter mStarter;

    @Mock private Activity mActivity;

    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mStarter = new Stripe3ds2CompletionStarter(
                AuthActivityStarter.Host.create(mActivity), 500);
    }

    @Test
    public void start_withSuccessfulCompletion_shouldAddClientSecretAndOutcomeToIntent() {
        mStarter.start(new Stripe3ds2CompletionStarter.StartData(
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
                Stripe3ds2CompletionStarter.ChallengeFlowOutcome.COMPLETE_SUCCESSFUL));
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(), eq(500));
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.getClientSecret(),
                intent.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET));
        assertEquals(StripeIntentResult.Outcome.SUCCEEDED,
                intent.getIntExtra(StripeIntentResultExtras.FLOW_OUTCOME,
                        StripeIntentResult.Outcome.UNKNOWN));
    }

    @Test
    public void start_withUnsuccessfulCompletion_shouldAddClientSecretAndOutcomeToIntent() {
        mStarter.start(new Stripe3ds2CompletionStarter.StartData(
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
                Stripe3ds2CompletionStarter.ChallengeFlowOutcome.COMPLETE_UNSUCCESSFUL));
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(), eq(500));
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.getClientSecret(),
                intent.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET));
        assertEquals(StripeIntentResult.Outcome.FAILED,
                intent.getIntExtra(StripeIntentResultExtras.FLOW_OUTCOME,
                        StripeIntentResult.Outcome.UNKNOWN));
    }

    @Test
    public void start_withTimeout_shouldAddClientSecretAndOutcomeToIntent() {
        mStarter.start(new Stripe3ds2CompletionStarter.StartData(
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
                Stripe3ds2CompletionStarter.ChallengeFlowOutcome.TIMEOUT));
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(), eq(500));
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.getClientSecret(),
                intent.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET));
        assertEquals(StripeIntentResult.Outcome.TIMEDOUT,
                intent.getIntExtra(StripeIntentResultExtras.FLOW_OUTCOME,
                        StripeIntentResult.Outcome.UNKNOWN));
    }

    @Test
    public void start_withProtocolError_shouldAddClientSecretAndOutcomeToIntent() {
        mStarter.start(new Stripe3ds2CompletionStarter.StartData(
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
                Stripe3ds2CompletionStarter.ChallengeFlowOutcome.PROTOCOL_ERROR));
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(), eq(500));
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.getClientSecret(),
                intent.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET));
        assertEquals(StripeIntentResult.Outcome.FAILED,
                intent.getIntExtra(StripeIntentResultExtras.FLOW_OUTCOME,
                        StripeIntentResult.Outcome.UNKNOWN));
    }
}
