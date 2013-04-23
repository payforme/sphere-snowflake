package forms;

import play.data.validation.Constraints;

public class PayForMe {
    @Constraints.Required
    public String email;
}
