package com.example.demo.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por enviar emails de notificação (novos pedidos,
 * aceitação/rejeição, cancelamento, etc.) usando o SMTP configurado em
 * application.properties (spring.mail.*).
 *
 * O envio nunca lança exceção para quem o chama: qualquer falha (SMTP mal
 * configurado, sem rede, etc.) é apenas registada em log, para não impedir
 * a operação de negócio (ex: aceitar um pedido continua a funcionar mesmo
 * que o email falhe).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remetente;

    @Value("${app.mail.enabled:true}")
    private boolean mailAtivado;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviar(String destinatario, String assunto, String corpo) {
        if (!mailAtivado) {
            log.debug("[Email] Envio desativado (app.mail.enabled=false). Assunto: '{}' não enviado.", assunto);
            return;
        }
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("[Email] Destinatário em branco — email '{}' não foi enviado.", assunto);
            return;
        }
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            if (remetente != null && !remetente.isBlank()) {
                mensagem.setFrom(remetente);
            }
            mensagem.setTo(destinatario);
            mensagem.setSubject(assunto);
            mensagem.setText(corpo);
            mailSender.send(mensagem);
            log.info("[Email] Notificação enviada para {} — assunto: '{}'", destinatario, assunto);
        } catch (Exception e) {
            log.error("[Email] Falha ao enviar para {} — {}", destinatario, e.getMessage(), e);
        }
    }
}
