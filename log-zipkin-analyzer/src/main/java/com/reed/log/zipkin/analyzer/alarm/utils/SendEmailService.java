package com.reed.log.zipkin.analyzer.alarm.utils;

import java.io.File;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.util.Pair;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Email service
 * @author reed
 *
 */
@Service
public class SendEmailService {
	private static Logger logger = LoggerFactory.getLogger(SendEmailService.class);

	@Value("${spring.mail.username}")
	private String DEFAULT_FROM;

	@Autowired
	private JavaMailSender mailSender;

	/**
	 * 发送简单文本邮件
	 * @param sendTo
	 * @param title
	 * @param content
	 */
	public void sendSimpleMail(String from, String sendTo, String title, String content) {
		from = StringUtils.isBlank(from) ? DEFAULT_FROM : from;
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(from);
			message.setTo(sendTo);
			message.setSubject(title);
			message.setText(content);
			mailSender.send(message);
		} catch (Exception e) {
			logger.error("=========Send email error:{}==========", e.getCause());
		}
	}

	/**
	 * 发送Html邮件
	 * @param sendTo
	 * @param title
	 * @param content
	 */
	public void sendHtmlMail(String from, String sendTo, String title, String content) {
		MimeMessage message = null;
		from = StringUtils.isBlank(from) ? DEFAULT_FROM : from;
		try {
			message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setFrom(from);
			helper.setTo(sendTo);
			helper.setSubject(title);
			helper.setText(content, true);
			mailSender.send(message);
		} catch (Exception e) {
			logger.error("=========Send email error:{}==========", e.getCause());
		}

	}

	/**
	 * 发送带附件的邮件
	 * @param sendTo
	 * @param title
	 * @param content
	 * @param attachments
	 */
	public void sendAttachmentsMail(String from, String sendTo, String title, String content,
			List<Pair<String, File>> attachments) {
		from = StringUtils.isBlank(from) ? DEFAULT_FROM : from;
		MimeMessage mimeMessage = null;
		try {
			mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
			helper.setFrom(from);
			helper.setTo(sendTo);
			helper.setSubject(title);
			helper.setText(content);
			for (Pair<String, File> pair : attachments) {
				helper.addAttachment(pair.getFirst(), new FileSystemResource(pair.getSecond()));
			}
			mailSender.send(mimeMessage);
		} catch (Exception e) {
			logger.error("=========Send email error:{}==========", e.getCause());
		}

	}

	/**
	 * 发送带静态资源的邮件
	 * @param sendTo
	 * @param title
	 * @param content
	 * @param attachment
	 */
	public void sendInlineMail(String from, String sendTo, String title, String content, File attachment) {
		MimeMessage message = null;
		from = StringUtils.isBlank(from) ? DEFAULT_FROM : from;
		try {
			message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setFrom(from);
			helper.setTo(sendTo);
			helper.setSubject(title);
			// 第二个参数指定发送的是HTML格式,同时cid:是固定的写法
			content = "<html><body>" + content + "<br>静态资源：<img src='cid:picture' /></body></html>";
			helper.setText(content, true);
			FileSystemResource file = new FileSystemResource(attachment);
			helper.addInline("picture", file);
			mailSender.send(message);
		} catch (Exception e) {
			logger.error("=========Send email error:{}==========", e.getCause());
		}

	}
}
