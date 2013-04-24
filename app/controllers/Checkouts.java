package controllers;

import controllers.actions.Authorization;
import com.google.common.base.Optional;
import forms.PayForMe;
import forms.PaymentNetwork;
import forms.PaymentNotification;
import forms.SetAddress;
import io.sphere.client.shop.model.Cart;
import io.sphere.client.shop.model.Order;
import io.sphere.client.shop.model.PaymentState;
import play.Play;
import play.data.Form;
import play.libs.F;
import play.libs.WS;
import play.mvc.Result;
import play.mvc.With;
import sphere.ShopController;
import utils.Payment;

import java.util.List;

import static play.data.Form.form;

public class Checkouts extends ShopController {

    public static Result show() {
        Cart cart = sphere().currentCart().fetch();
        String checkoutId = sphere().currentCart().createCheckoutSummaryId();
        SetAddress draftAddress = new SetAddress(cart.getShippingAddress());
        Form<SetAddress> addressForm = form(SetAddress.class).fill(draftAddress);
        Form<PayForMe> payForMeForm = form(PayForMe.class);
        String submitUrl = Play.application().configuration().getString("optile.chargeUrl");
        return ok(views.html.checkouts.render(cart, checkoutId, submitUrl, addressForm, payForMeForm));
    }

    public static Result submitShippingAddress() {
        Form<SetAddress> form = form(SetAddress.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest();
        }
        SetAddress setAddress = form.get();
        sphere().currentCart().setShippingAddress(setAddress.getAddress());
        sphere().currentCart().setCountry(setAddress.getCountryCode());
        return ok();
    }

    public static Result notification(String checkoutId) {
        Form<PaymentNotification> form = form(PaymentNotification.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest();
        }
        System.err.println("OK from " + request().remoteAddress());
        PaymentNotification paymentNotification = form.get();
        System.err.println("Notification " + paymentNotification.transactionId);
        System.err.println(paymentNotification.entity + " - " + paymentNotification.statusCode + " - " + paymentNotification.reasonCode);
        System.err.println(paymentNotification.resultCode + ": " + paymentNotification.resultInfo);
        PaymentState state = paymentNotification.getPaymentState();
        if (state.equals(PaymentState.Paid)) {
            sphere().currentCart().createOrder(checkoutId, state);
        }
        return ok();
    }

    public static Result success() {
        String orderId = session("orderId");
        Order lastOrder = null;
        if (orderId != null) {
          Optional<Order> order = sphere().orders.byId(orderId).fetch();
          if (order.isPresent()) {
            lastOrder = order.get();
          }
        }
        return ok(views.html.checkoutsSuccess.render(lastOrder));
    }

    @With(Authorization.class)
    public static Result failure() {
        flash("error", "Payment process aborted, please start over with another payment method.");
        // TODO Redirect to somewhere
        return badRequest("Failed...");
    }

    public static Result listPaymentNetworks(String selected) {
        Cart cart = sphere().currentCart().fetch();
        if (cart.getTotalPrice().getAmount().doubleValue() <= 0) {
            return noContent();
        }
        String checkoutId = sphere().currentCart().createCheckoutSummaryId();
        Payment payment = new Payment(cart, checkoutId);
        String transactionId = payment.doRequest(Payment.NATIVE_URL, Payment.Operation.LIST);
        if (!payment.isValidResponse(transactionId)) {
            return noContent();
        }
        List<PaymentNetwork> paymentNetworks = payment.getApplicableNetworks();
        String referredId = payment.getReferredId();
        return ok(views.html.ajax.listPaymentNetworks.render(paymentNetworks, referredId, selected));
    }

    public static Result payForMe() {
        Form<PayForMe> payForMeForm = form(PayForMe.class).bindFromRequest();
        if (payForMeForm.hasErrors()){
            return badRequest();
        }
        PayForMe form = payForMeForm.get();

        play.Logger.info(form.checkoutId);
        String checkoutId = sphere().currentCart().createCheckoutSummaryId();
        try {
            // TODO: ugly hack, need to do it in order to have a valid `checkoutId`
            Thread.sleep(600);
        } catch (Exception e) {
        }
        Order order = sphere().currentCart().createOrder(checkoutId, PaymentState.BalanceDue);
        session("orderId", order.getId());

        // Notifies PayForMe WS
        String formContent =
                "name=" + form.payformeName + "&" +
                "email=" + form.payformeEmail + "&" +
                "orderId=" + order.getId() + "&" +
                "projectKey=" + Payment.PAYFORME_KEY;

        F.Promise<WS.Response> f = WS.url(Payment.PAYFORME_URL)
                .setContentType("application/x-www-form-urlencoded; charset=utf-8")
                .post(formContent);
        return async(f.map(new F.Function<WS.Response, Result>() {
            @Override public Result apply(WS.Response response) throws Throwable {
                if (response.getStatus() == 200) {
                  return redirect(controllers.routes.Checkouts.success());
                } else {
                  return badRequest("Could not send the request to the payforme payment service.");
                }
            }
        }));
    }

}
