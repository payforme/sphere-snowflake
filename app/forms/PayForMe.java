package forms;

import play.data.validation.Constraints;

public class PayForMe {
    @Constraints.Required
    public String payformePayersEmail;
    @Constraints.Required
    public String payformePayersName;
    @Constraints.Required
    public String payformeBuyersName;
    public String checkoutId;
}
