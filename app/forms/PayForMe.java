package forms;

import play.data.validation.Constraints;

public class PayForMe {
    @Constraints.Required
    public String payformeEmail;
    @Constraints.Required
    public String payformeBuyerName;
    @Constraints.Required
    public String payformeName;
    public String checkoutId;
}
