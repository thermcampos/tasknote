package br.com.tasknoteapp.server.templates;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MailgunTemplateTest {

  @Test
  void mailgunTemplateResetPwdTest() {
    MailgunTemplateResetPwd reset = new MailgunTemplateResetPwd();

    Assertions.assertNotNull(reset.getName());
    Assertions.assertNotNull(reset.getVariables());
    Assertions.assertNotNull(reset.getVariableValuesJson());
    Assertions.assertFalse(reset.getVariableValuesJson().isBlank());
  }

  @Test
  void mailgunTemplateResetPwdConfirmTest() {
    MailgunTemplateResetPwdConfirm confirm = new MailgunTemplateResetPwdConfirm();

    Assertions.assertNotNull(confirm.getName());
    Assertions.assertNotNull(confirm.getVariables());
    Assertions.assertNotNull(confirm.getVariableValuesJson());
    Assertions.assertFalse(confirm.getVariableValuesJson().isBlank());
  }

  @Test
  void mailgunTemplateSignUpTest() {
    MailgunTemplateSignUp signUp = new MailgunTemplateSignUp();

    Assertions.assertNotNull(signUp.getName());
    Assertions.assertNotNull(signUp.getVariables());
    Assertions.assertNotNull(signUp.getVariableValuesJson());
    Assertions.assertFalse(signUp.getVariableValuesJson().isBlank());
  }

  @Test
  void mailgunTemplateEmailChangedTest() {
    MailgunTemplateEmailChanged emailChanged = new MailgunTemplateEmailChanged();

    Assertions.assertNotNull(emailChanged.getName());
    Assertions.assertEquals("email_changed", emailChanged.getName());
    Assertions.assertNotNull(emailChanged.getVariables());
    Assertions.assertNotNull(emailChanged.getVariableValuesJson());
    Assertions.assertFalse(emailChanged.getVariableValuesJson().isBlank());
  }
}
