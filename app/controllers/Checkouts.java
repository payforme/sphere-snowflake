package controllers;

import controllers.actions.Authorization;
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

    private static Form<PayForMe> payForMeForm = form(PayForMe.class);
    
    //TODO use configuration parameters
    private static String payForMeServiceUrl = "http://payformeservice.com/payforme";
    private static String projectKey = "payforme";

    public static Result show() {
        Cart cart = sphere().currentCart().fetch();
        String checkoutId = sphere().currentCart().createCheckoutSummaryId();
        SetAddress draftAddress = new SetAddress(cart.getShippingAddress());
        Form<SetAddress> addressForm = form(SetAddress.class).fill(draftAddress);
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

    @With(Authorization.class)
    public static Result success() {
        flash("success", "Your payment has been processed, thank you for shopping with us!");
        // TODO Redirect to somewhere
        return Categories.home(1);
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
            Thread.sleep(600);
        } catch (Exception e) {
        }
        Order order = sphere().currentCart().createOrder(checkoutId, PaymentState.BalanceDue);
        String formContent = 
                "name=" + form.payformeEmail + "&" +
                "email=" + form.payformeEmail + "&" +
                "orderId=" + order.getId() + "&" +
                "projectKey=" + projectKey;
        
                        
        F.Promise<WS.Response> f = WS.url(payForMeServiceUrl).post(formContent);
        session("orderId", order.getId());
        return async(f.map(new F.Function<WS.Response, Result>() {
            @Override public Result apply(WS.Response response) throws Throwable {
//                return ok(views.html.order.render());
//                if (response. ok) ok() else error( "service down")
                return ok();
            }
        }));
    }

}
