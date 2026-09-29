package br.com.tasknoteapp.server.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * This record represents the request body sent to the Resend API to send an email.
 *
 * @param from The sender email address.
 * @param to The recipient email addresses.
 * @param cc The carbon copy email addresses, if any.
 * @param subject The email subject.
 * @param html The email HTML body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResendEmailRequest(
    String from, List<String> to, List<String> cc, String subject, String html) {}
